package com.nexuscrm.relatorios;

import com.nexuscrm.administracao.*;
import com.nexuscrm.chamados.*;
import com.nexuscrm.clientes.*;
import com.nexuscrm.comercial.*;
import com.nexuscrm.shared.*;
import com.nexuscrm.tarefas.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportService {
  public static final int MAX_ROWS = 10000;
  private final CustomerRepository customers;
  private final OpportunityRepository opportunities;
  private final TicketRepository tickets;
  private final TaskRepository tasks;
  private final UserAccountRepository users;
  private final DomainSupport support;

  public ReportService(
      CustomerRepository customers,
      OpportunityRepository opportunities,
      TicketRepository tickets,
      TaskRepository tasks,
      UserAccountRepository users,
      DomainSupport support) {
    this.customers = customers;
    this.opportunities = opportunities;
    this.tickets = tickets;
    this.tasks = tasks;
    this.users = users;
    this.support = support;
  }

  public record Report(List<String> columns, List<Map<String, Object>> rows, long total) {}

  public Report generate(
      String type, LocalDate from, LocalDate to, String status, Long assigneeId) {
    support.current.require("REPORTS_READ");
    if (assigneeId != null
        && users.findByIdAndOrganizationId(assigneeId, support.current.organizationId()).isEmpty())
      throw ApiException.badRequest("Funcionário não encontrado nesta organização.");
    if (from != null && to != null && from.isAfter(to))
      throw ApiException.badRequest("Período inválido.");
    var pageable = PageRequest.of(0, MAX_ROWS, Sort.by(Sort.Direction.DESC, "createdAt"));
    return switch (type) {
      case "customers" -> {
        var spec = support.<Customer>filter(null, List.of(), null, null, null, from, to);
        if (status != null && !status.isBlank()) {
          if (!Set.of("ACTIVE", "INACTIVE").contains(status))
            throw ApiException.badRequest("Situação inválida para clientes.");
          boolean active = "ACTIVE".equals(status);
          spec = spec.and((r, q, c) -> c.equal(r.get("active"), active));
        }
        var page = customers.findAll(spec, pageable);
        yield new Report(
            List.of("id", "name", "type", "document", "email", "phone", "active", "createdAt"),
            page.stream()
                .map(
                    value -> {
                      var row = support.view(value);
                      row.put("name", value.getName());
                      row.put("type", value.getType());
                      row.put("document", value.getDocument());
                      row.put("email", value.getEmail());
                      row.put("phone", value.getPhone());
                      row.put("active", value.getActive());
                      return row;
                    })
                .toList(),
            page.getTotalElements());
      }
      case "opportunities" -> {
        var page =
            opportunities.findAll(
                support.filter(null, List.of(), status, "stage", assigneeId, from, to), pageable);
        yield new Report(
            List.of(
                "id",
                "title",
                "stage",
                "amount",
                "probability",
                "assigneeName",
                "expectedCloseDate",
                "createdAt"),
            page.stream()
                .map(
                    value -> {
                      var row = support.view(value);
                      row.put("title", value.getTitle());
                      row.put("stage", value.getStage());
                      row.put("amount", value.getAmount());
                      row.put("probability", value.getProbability());
                      row.put("assigneeName", support.memberName(value.getAssigneeId()));
                      row.put("expectedCloseDate", value.getExpectedCloseDate());
                      return row;
                    })
                .toList(),
            page.getTotalElements());
      }
      case "tickets" -> {
        var page =
            tickets.findAll(
                support.filter(null, List.of(), status, "status", assigneeId, from, to), pageable);
        yield new Report(
            List.of(
                "number",
                "title",
                "category",
                "priority",
                "status",
                "assigneeName",
                "dueAt",
                "resolvedAt",
                "createdAt"),
            page.stream()
                .map(
                    value -> {
                      var row = support.view(value);
                      row.put("number", value.getNumber());
                      row.put("title", value.getTitle());
                      row.put("category", value.getCategory());
                      row.put("priority", value.getPriority());
                      row.put("status", value.getStatus());
                      row.put("assigneeName", support.memberName(value.getAssigneeId()));
                      row.put("dueAt", value.getDueAt());
                      row.put("resolvedAt", value.getResolvedAt());
                      return row;
                    })
                .toList(),
            page.getTotalElements());
      }
      case "productivity" -> productivity(from, to, status, assigneeId);
      default -> throw ApiException.notFound("Relatório não encontrado.");
    };
  }

  private Report productivity(LocalDate from, LocalDate to, String status, Long assigneeId) {
    if (status != null
        && !status.isBlank()
        && !Set.of("ALL", "PENDING", "COMPLETED").contains(status))
      throw ApiException.badRequest("Produtividade aceita situação ALL, PENDING ou COMPLETED.");
    var members =
        users
            .findAll(
                (orgRoot, query, criteria) ->
                    criteria.equal(orgRoot.get("organizationId"), support.current.organizationId()))
            .stream()
            .filter(user -> assigneeId == null || user.getId().equals(assigneeId))
            .limit(MAX_ROWS)
            .toList();
    List<Map<String, Object>> rows = new ArrayList<>();
    for (var member : members) {
      Long id = member.getId();
      Specification<Task> taskSpec = support.filter(null, List.of(), null, null, id, from, to);
      Specification<Ticket> ticketSpec = support.filter(null, List.of(), null, null, id, from, to);
      Specification<Opportunity> opportunitySpec =
          support.filter(null, List.of(), null, null, id, from, to);
      long completed = tasks.count(taskSpec.and((r, q, c) -> c.equal(r.get("status"), "DONE")));
      long pending = tasks.count(taskSpec.and((r, q, c) -> c.notEqual(r.get("status"), "DONE")));
      long resolved =
          tickets.count(
              ticketSpec.and((r, q, c) -> r.get("status").in(TicketService.RESOLVED_STATUSES)));
      long open =
          tickets.count(
              ticketSpec.and((r, q, c) -> r.get("status").in(TicketService.OPEN_STATUSES)));
      long won =
          opportunities.count(opportunitySpec.and((r, q, c) -> c.equal(r.get("stage"), "WON")));
      long negotiating =
          opportunities.count(
              opportunitySpec.and((r, q, c) -> c.not(r.get("stage").in(Set.of("WON", "LOST")))));
      if ("COMPLETED".equals(status) && completed + resolved + won == 0
          || "PENDING".equals(status) && pending + open + negotiating == 0) continue;
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("employee", member.getName());
      row.put("completedTasks", completed);
      row.put("pendingTasks", pending);
      row.put("resolvedTickets", resolved);
      row.put("openTickets", open);
      row.put("wonOpportunities", won);
      row.put("openOpportunities", negotiating);
      rows.add(row);
    }
    return new Report(
        List.of(
            "employee",
            "completedTasks",
            "pendingTasks",
            "resolvedTickets",
            "openTickets",
            "wonOpportunities",
            "openOpportunities"),
        rows,
        rows.size());
  }

  public byte[] csv(Report report) {
    StringBuilder csv = new StringBuilder("\uFEFF");
    csv.append(
            report.columns().stream()
                .map(ReportService::csvCell)
                .collect(java.util.stream.Collectors.joining(";")))
        .append("\r\n");
    for (var row : report.rows())
      csv.append(
              report.columns().stream()
                  .map(column -> csvCell(row.get(column)))
                  .collect(java.util.stream.Collectors.joining(";")))
          .append("\r\n");
    return csv.toString().getBytes(StandardCharsets.UTF_8);
  }

  public static String csvCell(Object value) {
    String text = value == null ? "" : String.valueOf(value);
    String stripped = text.stripLeading();
    if (!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0
        || !text.isEmpty()
            && (text.charAt(0) == '\t' || text.charAt(0) == '\r' || text.charAt(0) == '\n'))
      text = "'" + text;
    return "\"" + text.replace("\"", "\"\"") + "\"";
  }
}
