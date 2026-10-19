package com.nexuscrm.chamados;

import com.nexuscrm.chamados.TicketResponses.*;
import com.nexuscrm.clientes.ResponseMapping;
import com.nexuscrm.clientes.RelationshipResponses.HistoryResponse;

import com.nexuscrm.chamados.TicketRequests.*;
import com.nexuscrm.clientes.DomainSupport;
import com.nexuscrm.shared.*;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/tickets")
public class TicketController {
  private final ResponseMapping responses;
  private final TicketService service;
  private final AttachmentService attachments;
  private final DomainSupport support;

  public TicketController(
      TicketService service, AttachmentService attachments, DomainSupport support, ResponseMapping responses) {
    this.responses = responses;
    this.service = service;
    this.attachments = attachments;
    this.support = support;
  }

  @GetMapping
  public PageResponse<TicketResponse> list(
      @RequestParam(defaultValue = "") String search,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long assigneeId,
      @RequestParam(required = false) Long customerId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt") String sort,
      @RequestParam(defaultValue = "desc") String direction) {
    return responses.page(service.list(
        search,
        status,
        assigneeId,
        customerId,
        from,
        to,
        support.pageable(
            page,
            size,
            sort,
            direction,
            Set.of("title", "number", "priority", "status", "dueAt", "createdAt", "updatedAt"))), TicketResponse.class);
  }

  @GetMapping("/{id}")
  public TicketResponse detail(@PathVariable Long id) {
    return responses.one(service.detail(id), TicketResponse.class);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TicketResponse create(@Valid @RequestBody TicketInput input) {
    return responses.one(service.create(input), TicketResponse.class);
  }

  @PutMapping("/{id}")
  public TicketResponse update(@PathVariable Long id, @Valid @RequestBody TicketInput input) {
    return responses.one(service.update(id, input), TicketResponse.class);
  }

  @PatchMapping("/{id}/status")
  public TicketResponse status(@PathVariable Long id, @Valid @RequestBody StatusInput input) {
    return responses.one(service.status(id, input), TicketResponse.class);
  }

  @GetMapping("/{id}/comments")
  public List<CommentResponse> comments(@PathVariable Long id) {
    return responses.list(service.comments(id), CommentResponse.class);
  }

  @PostMapping("/{id}/comments")
  @ResponseStatus(HttpStatus.CREATED)
  public CommentResponse comment(
      @PathVariable Long id, @Valid @RequestBody CommentInput input) {
    return responses.one(service.addComment(id, input), CommentResponse.class);
  }

  @GetMapping("/{id}/history")
  public List<HistoryResponse> history(@PathVariable Long id) {
    return responses.list(service.history(id), HistoryResponse.class);
  }

  @GetMapping("/{id}/attachments")
  public List<AttachmentResponse> attachments(@PathVariable Long id) {
    return responses.list(attachments.list(id), AttachmentResponse.class);
  }

  @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public AttachmentResponse upload(
      @PathVariable Long id, @RequestPart("file") MultipartFile file) {
    return responses.one(attachments.upload(id, file), AttachmentResponse.class);
  }

  @GetMapping("/{id}/attachments/{attachmentId}")
  public ResponseEntity<Resource> download(@PathVariable Long id, @PathVariable Long attachmentId) {
    var value = attachments.download(id, attachmentId);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(value.fileName(), StandardCharsets.UTF_8)
                .build()
                .toString())
        .header("X-Content-Type-Options", "nosniff")
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .contentType(MediaType.parseMediaType(value.contentType()))
        .contentLength(value.size())
        .body(value.resource());
  }
}
