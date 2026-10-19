package com.nexuscrm.chamados;

import com.nexuscrm.clientes.DomainSupport;
import com.nexuscrm.comercial.HistoryService;
import com.nexuscrm.shared.ApiException;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class AttachmentService {
  public static final long MAX_SIZE = 10L * 1024 * 1024;
  private final TicketAttachmentRepository attachments;
  private final TicketService tickets;
  private final DomainSupport support;
  private final HistoryService history;
  private final Path directory;

  public AttachmentService(
      TicketAttachmentRepository attachments,
      TicketService tickets,
      DomainSupport support,
      HistoryService history,
      @Value("${nexus.storage-dir:${NEXUS_STORAGE_DIR:../var/attachments}}") String directory) {
    this.attachments = attachments;
    this.tickets = tickets;
    this.support = support;
    this.history = history;
    this.directory = Path.of(directory).toAbsolutePath().normalize();
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> list(Long id) {
    support.current.require("TICKETS_READ");
    tickets.require(id);
    return attachments
        .findByOrganizationIdAndTicketIdOrderByCreatedAtDesc(support.current.organizationId(), id)
        .stream()
        .map(this::view)
        .toList();
  }

  public Map<String, Object> upload(Long id, MultipartFile file) {
    support.current.require("TICKETS_WRITE");
    tickets.require(id);
    if (file.isEmpty() || file.getSize() > MAX_SIZE)
      throw ApiException.badRequest("Anexo deve conter entre 1 byte e 10 MB.");
    String fileName = sanitize(file.getOriginalFilename());
    byte[] bytes;
    try (var input = file.getInputStream()) {
      bytes = input.readNBytes((int) MAX_SIZE + 1);
    } catch (IOException exception) {
      throw ApiException.badRequest("Não foi possível ler o arquivo.");
    }
    if (bytes.length > MAX_SIZE) throw ApiException.badRequest("O anexo excede 10 MB.");
    String contentType = validate(fileName, bytes);
    String key = UUID.randomUUID().toString();
    Path target = directory.resolve(key).normalize();
    try {
      Files.createDirectories(directory);
      if (Files.isSymbolicLink(directory)) throw new IOException("Invalid storage directory");
      try {
        Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx------"));
      } catch (UnsupportedOperationException ignored) {
      }
      Files.write(target, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
      try {
        Files.setPosixFilePermissions(target, PosixFilePermissions.fromString("rw-------"));
      } catch (UnsupportedOperationException ignored) {
      }
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
              if (status != STATUS_COMMITTED)
                try {
                  Files.deleteIfExists(target);
                } catch (IOException ignored) {
                }
            }
          });
      var entity = support.initialize(new TicketAttachment());
      entity.setTicketId(id);
      entity.setAuthorId(support.current.userId());
      entity.setFileName(fileName);
      entity.setStorageKey(key);
      entity.setContentType(contentType);
      entity.setSize((long) bytes.length);
      attachments.saveAndFlush(entity);
      history.record("TICKET", id, "ATTACHMENT", "Anexo incluído");
      return view(entity);
    } catch (IOException exception) {
      try {
        Files.deleteIfExists(target);
      } catch (IOException ignored) {
      }
      throw ApiException.badRequest("Não foi possível armazenar o anexo.");
    }
  }

  @Transactional(readOnly = true)
  public Download download(Long id, Long attachmentId) {
    support.current.require("TICKETS_READ");
    tickets.require(id);
    var entity =
        attachments
            .findByIdAndOrganizationIdAndTicketId(
                attachmentId, support.current.organizationId(), id)
            .orElseThrow(() -> ApiException.notFound("Anexo não encontrado."));
    Path file = directory.resolve(entity.getStorageKey()).normalize();
    if (!file.getParent().equals(directory)
        || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
      throw ApiException.notFound("Arquivo do anexo indisponível.");
    return new Download(
        new FileSystemResource(file),
        entity.getFileName(),
        entity.getContentType(),
        entity.getSize());
  }

  public record Download(Resource resource, String fileName, String contentType, long size) {}

  private Map<String, Object> view(TicketAttachment entity) {
    var value = support.view(entity);
    value.put("fileName", entity.getFileName());
    value.put("size", entity.getSize());
    value.put("contentType", entity.getContentType());
    return value;
  }

  public static String sanitize(String original) {
    if (original == null) throw ApiException.badRequest("Nome de arquivo obrigatório.");
    String result = original.replace('\\', '/');
    result = result.substring(result.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").strip();
    if (result.isBlank() || result.length() > 200 || result.startsWith("."))
      throw ApiException.badRequest("Nome de arquivo inválido.");
    return result;
  }

  public static String validate(String fileName, byte[] bytes) {
    String lower = fileName.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".pdf")
        && bytes.length >= 5
        && new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-"))
      return "application/pdf";
    byte[] png = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
    if (lower.endsWith(".png") && bytes.length >= 8 && Arrays.equals(Arrays.copyOf(bytes, 8), png))
      return "image/png";
    if ((lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
        && bytes.length >= 3
        && (bytes[0] & 255) == 255
        && (bytes[1] & 255) == 216
        && (bytes[2] & 255) == 255) return "image/jpeg";
    if (lower.endsWith(".txt") || lower.endsWith(".csv")) {
      String text;
      try {
        text =
            StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
      } catch (CharacterCodingException exception) {
        throw ApiException.badRequest("Arquivos de texto devem usar UTF-8.");
      }
      String prefix = text.stripLeading().toLowerCase(Locale.ROOT);
      if (text.codePoints()
              .anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t')
          || prefix.startsWith("<")
          || prefix.startsWith("#!")
          || prefix.startsWith("mz")) throw ApiException.badRequest("Conteúdo de texto inválido.");
      return lower.endsWith(".csv") ? "text/csv" : "text/plain";
    }
    throw ApiException.badRequest(
        "Formato ou conteúdo inválido. Permitidos PDF, PNG, JPEG, TXT e CSV.");
  }
}
