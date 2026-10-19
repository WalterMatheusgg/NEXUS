package com.nexuscrm.chamados;

import com.nexuscrm.chamados.TicketRequests.*;
import com.nexuscrm.clientes.*;
import com.nexuscrm.comercial.HistoryService;
import com.nexuscrm.shared.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TicketService {
  public static final Set<String> OPEN_STATUSES = Set.of("OPEN", "IN_PROGRESS", "WAITING");
  public static final Set<String> RESOLVED_STATUSES = Set.of("RESOLVED", "CLOSED");
  private final TicketRepository tickets;
  private final TicketCommentRepository comments;
  private final CustomerService customers;
  private final DomainSupport support;
  private final HistoryService history;

  public TicketService(
      TicketRepository tickets,
      TicketCommentRepository comments,
      CustomerService customers,
      DomainSupport support,
      HistoryService history) {
    this.tickets = tickets;
    this.comments = comments;
    this.customers = customers;
    this.support = support;
    this.history = history;
  }

  public Ticket require(Long id) {
    return support.get(tickets, id, "Chamado");
  }

  @Transactional(readOnly = true)
  public PageResponse<Map<String, Object>> list(
      String search,
      String status,
      Long assigneeId,
      Long customerId,
      LocalDate from,
      LocalDate to,
      Pageable pageable) {
    support.current.require("TICKETS_READ");
    var spec =
        support.<Ticket>filter(
            search,
            List.of("title", "description", "number", "category"),
            status,
            "status",
            assigneeId,
            from,
            to);
    if (customerId != null) {
      customers.require(customerId);
      spec = spec.and((r, q, c) -> c.equal(r.get("customerId"), customerId));
    }
    return PageResponse.from(tickets.findAll(spec, pageable).map(this::view));
  }

  @Transactional(readOnly = true)
  public Map<String, Object> detail(Long id) {
    support.current.require("TICKETS_READ");
    return view(require(id));
  }

  public Map<String, Object> create(TicketInput input) {
    support.current.require("TICKETS_WRITE");
    var entity = support.initialize(new Ticket());
    entity.setNumber(
        "T-"
            + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 16)
                .toUpperCase(Locale.ROOT));
    apply(entity, input);
    tickets.saveAndFlush(entity);
    history.record("TICKET", entity.getId(), "CREATE", "Chamado aberto: " + entity.getNumber());
    support.notifyAssignment(
        entity.getAssigneeId(), entity.getTitle(), "/tickets/" + entity.getId());
    return view(entity);
  }

  public Map<String, Object> update(Long id, TicketInput input) {
    support.current.require("TICKETS_WRITE");
    var entity = require(id);
    support.version(entity, input.version());
    String previous = entity.getStatus();
    Long oldAssignee = entity.getAssigneeId();
    apply(entity, input);
    tickets.saveAndFlush(entity);
    history.record(
        "TICKET",
        id,
        "UPDATE",
        "Chamado atualizado; situação " + previous + " → " + entity.getStatus());
    if (!Objects.equals(oldAssignee, entity.getAssigneeId()))
      support.notifyAssignment(entity.getAssigneeId(), entity.getTitle(), "/tickets/" + id);
    return view(entity);
  }

  private void apply(Ticket entity, TicketInput input) {
    customers.require(input.customerId());
    support.member(input.assigneeId());
    entity.setTitle(input.title().strip());
    entity.setDescription(input.description().strip());
    entity.setCustomerId(input.customerId());
    entity.setCategory(input.category().strip());
    entity.setPriority(input.priority());
    entity.setAssigneeId(input.assigneeId());
    entity.setDueAt(input.dueAt());
    setStatus(entity, input.status());
  }

  private void setStatus(Ticket entity, String status) {
    entity.setStatus(status);
    if (RESOLVED_STATUSES.contains(status)) {
      if (entity.getResolvedAt() == null) entity.setResolvedAt(OffsetDateTime.now(ZoneOffset.UTC));
    } else entity.setResolvedAt(null);
  }

  public Map<String, Object> status(Long id, StatusInput input) {
    support.current.require("TICKETS_WRITE");
    var entity = require(id);
    support.version(entity, input.version());
    String previous = entity.getStatus();
    setStatus(entity, input.status());
    tickets.saveAndFlush(entity);
    history.record("TICKET", id, "STATUS", previous + " → " + entity.getStatus());
    return view(entity);
  }

  public Map<String, Object> view(Ticket entity) {
    var data = support.view(entity);
    data.put("number", entity.getNumber());
    data.put("title", entity.getTitle());
    data.put("description", entity.getDescription());
    data.put("customerId", entity.getCustomerId());
    data.put("customerName", customers.require(entity.getCustomerId()).getName());
    data.put("category", entity.getCategory());
    data.put("priority", entity.getPriority());
    data.put("status", entity.getStatus());
    data.put("assigneeId", entity.getAssigneeId());
    data.put("assigneeName", support.memberName(entity.getAssigneeId()));
    data.put("dueAt", entity.getDueAt());
    data.put("resolvedAt", entity.getResolvedAt());
    data.put(
        "overdue",
        entity.getDueAt() != null
            && entity.getDueAt().isBefore(OffsetDateTime.now())
            && OPEN_STATUSES.contains(entity.getStatus()));
    return data;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> comments(Long id) {
    support.current.require("TICKETS_READ");
    require(id);
    return comments
        .findByOrganizationIdAndTicketIdOrderByCreatedAtAsc(support.current.organizationId(), id)
        .stream()
        .map(this::commentView)
        .toList();
  }

  public Map<String, Object> addComment(Long id, CommentInput input) {
    support.current.require("TICKETS_WRITE");
    var ticket = require(id);
    var entity = support.initialize(new TicketComment());
    entity.setTicketId(id);
    entity.setAuthorId(support.current.userId());
    entity.setBody(input.body().strip());
    comments.saveAndFlush(entity);
    history.record("TICKET", id, "COMMENT", "Comentário registrado");
    support.notifyAssignment(
        ticket.getAssigneeId(), "Novo comentário: " + ticket.getTitle(), "/tickets/" + id);
    return commentView(entity);
  }

  private Map<String, Object> commentView(TicketComment entity) {
    var data = support.view(entity);
    data.put("body", entity.getBody());
    data.put("authorName", support.memberName(entity.getAuthorId()));
    return data;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> history(Long id) {
    support.current.require("TICKETS_READ");
    require(id);
    return history.list("TICKET", id);
  }
}
