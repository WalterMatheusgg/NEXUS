package com.nexuscrm.tarefas;

import com.nexuscrm.chamados.TicketService;
import com.nexuscrm.clientes.*;
import com.nexuscrm.comercial.*;
import com.nexuscrm.shared.*;
import com.nexuscrm.tarefas.TaskRequests.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaskService {
  private final TaskRepository tasks;
  private final CustomerService customers;
  private final TicketService tickets;
  private final SalesService sales;
  private final DomainSupport support;
  private final HistoryService history;

  public TaskService(
      TaskRepository tasks,
      CustomerService customers,
      TicketService tickets,
      SalesService sales,
      DomainSupport support,
      HistoryService history) {
    this.tasks = tasks;
    this.customers = customers;
    this.tickets = tickets;
    this.sales = sales;
    this.support = support;
    this.history = history;
  }

  public Task require(Long id) {
    return support.get(tasks, id, "Tarefa");
  }

  @Transactional(readOnly = true)
  public PageResponse<Map<String, Object>> list(
      String search,
      String status,
      Long assigneeId,
      LocalDate from,
      LocalDate to,
      Pageable pageable) {
    support.current.require("TASKS_READ");
    return PageResponse.from(
        tasks
            .findAll(
                support.filter(
                    search,
                    List.of("title", "description"),
                    status,
                    "status",
                    assigneeId,
                    from,
                    to),
                pageable)
            .map(this::view));
  }

  @Transactional(readOnly = true)
  public Map<String, Object> detail(Long id) {
    support.current.require("TASKS_READ");
    return view(require(id));
  }

  public Map<String, Object> create(TaskInput input) {
    support.current.require("TASKS_WRITE");
    var entity = support.initialize(new Task());
    apply(entity, input);
    tasks.saveAndFlush(entity);
    history.record("TASK", entity.getId(), "CREATE", "Tarefa cadastrada");
    support.notifyAssignment(entity.getAssigneeId(), entity.getTitle(), "/tasks");
    return view(entity);
  }

  public Map<String, Object> update(Long id, TaskInput input) {
    support.current.require("TASKS_WRITE");
    var entity = require(id);
    support.version(entity, input.version());
    Long old = entity.getAssigneeId();
    apply(entity, input);
    tasks.saveAndFlush(entity);
    history.record("TASK", id, "UPDATE", "Tarefa atualizada");
    if (!Objects.equals(old, entity.getAssigneeId()))
      support.notifyAssignment(entity.getAssigneeId(), entity.getTitle(), "/tasks");
    return view(entity);
  }

  private void apply(Task entity, TaskInput input) {
    support.member(input.assigneeId());
    if (input.customerId() != null) customers.require(input.customerId());
    if (input.ticketId() != null) {
      var linked = tickets.require(input.ticketId());
      if (input.customerId() != null && !input.customerId().equals(linked.getCustomerId()))
        throw ApiException.badRequest("O chamado vinculado pertence a outro cliente.");
    }
    if (input.opportunityId() != null) {
      var linked = sales.requireOpportunity(input.opportunityId());
      if (input.customerId() != null
          && linked.getCustomerId() != null
          && !input.customerId().equals(linked.getCustomerId()))
        throw ApiException.badRequest("A oportunidade vinculada pertence a outro cliente.");
    }
    entity.setTitle(input.title().strip());
    entity.setDescription(input.description());
    entity.setPriority(input.priority());
    entity.setAssigneeId(input.assigneeId());
    entity.setDueDate(input.dueDate());
    entity.setCustomerId(input.customerId());
    entity.setTicketId(input.ticketId());
    entity.setOpportunityId(input.opportunityId());
    setStatus(entity, input.status());
  }

  private void setStatus(Task entity, String status) {
    entity.setStatus(status);
    entity.setCompletedAt(
        "DONE".equals(status)
            ? (entity.getCompletedAt() == null
                ? OffsetDateTime.now(ZoneOffset.UTC)
                : entity.getCompletedAt())
            : null);
  }

  public Map<String, Object> status(Long id, StatusInput input) {
    support.current.require("TASKS_WRITE");
    var entity = require(id);
    support.version(entity, input.version());
    String previous = entity.getStatus();
    setStatus(entity, input.status());
    tasks.saveAndFlush(entity);
    history.record("TASK", id, "STATUS", previous + " → " + entity.getStatus());
    return view(entity);
  }

  public Map<String, Object> view(Task entity) {
    var value = support.view(entity);
    value.put("title", entity.getTitle());
    value.put("description", entity.getDescription());
    value.put("priority", entity.getPriority());
    value.put("status", entity.getStatus());
    value.put("assigneeId", entity.getAssigneeId());
    value.put("assigneeName", support.memberName(entity.getAssigneeId()));
    value.put("dueDate", entity.getDueDate());
    value.put("customerId", entity.getCustomerId());
    value.put("ticketId", entity.getTicketId());
    value.put("opportunityId", entity.getOpportunityId());
    value.put("completedAt", entity.getCompletedAt());
    value.put(
        "overdue",
        entity.getDueDate() != null
            && entity.getDueDate().isBefore(LocalDate.now(ZoneOffset.UTC))
            && !"DONE".equals(entity.getStatus()));
    return value;
  }
}
