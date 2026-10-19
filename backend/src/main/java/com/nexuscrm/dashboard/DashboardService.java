package com.nexuscrm.dashboard;

import com.nexuscrm.chamados.*;
import com.nexuscrm.clientes.*;
import com.nexuscrm.comercial.*;
import com.nexuscrm.tarefas.TaskRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {
  private final CustomerRepository customers;
  private final OpportunityRepository opportunities;
  private final TicketRepository tickets;
  private final TaskRepository tasks;
  private final DomainSupport support;

  public DashboardService(
      CustomerRepository customers,
      OpportunityRepository opportunities,
      TicketRepository tickets,
      TaskRepository tasks,
      DomainSupport support) {
    this.customers = customers;
    this.opportunities = opportunities;
    this.tickets = tickets;
    this.tasks = tasks;
    this.support = support;
  }

  public Map<String, Object> dashboard() {
    support.current.require("DASHBOARD_READ");
    Long org = support.current.organizationId();
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put(
        "totalCustomers", customers.count((r, q, c) -> c.equal(r.get("organizationId"), org)));
    result.put(
        "newCustomersThisMonth",
        customers.countByOrganizationIdAndCreatedAtGreaterThanEqual(
            org, now.toLocalDate().withDayOfMonth(1).atStartOfDay().atOffset(ZoneOffset.UTC)));
    result.put(
        "openOpportunities",
        opportunities.countByOrganizationIdAndStageNotIn(org, Set.of("WON", "LOST")));
    result.put("pipelineValue", opportunities.pipelineValue(org));
    result.put(
        "openTickets", tickets.countByOrganizationIdAndStatusIn(org, TicketService.OPEN_STATUSES));
    result.put(
        "resolvedTickets",
        tickets.countByOrganizationIdAndStatusIn(org, TicketService.RESOLVED_STATUSES));
    result.put("pendingTasks", tasks.countByOrganizationIdAndStatusNot(org, "DONE"));
    result.put(
        "overdueTickets",
        tickets.countByOrganizationIdAndStatusInAndDueAtBefore(
            org, TicketService.OPEN_STATUSES, now));
    result.put(
        "overdueTasks",
        tasks.countByOrganizationIdAndStatusNotAndDueDateBefore(org, "DONE", now.toLocalDate()));
    long won = opportunities.countByOrganizationIdAndStage(org, "WON"),
        lost = opportunities.countByOrganizationIdAndStage(org, "LOST");
    result.put(
        "conversionRate", won + lost == 0 ? 0 : Math.round(10000.0 * won / (won + lost)) / 100.0);
    Map<String, Object[]> stageData = new HashMap<>();
    opportunities.groupStages(org).forEach(value -> stageData.put((String) value[0], value));
    result.put(
        "opportunitiesByStage",
        List.of("NEW_LEAD", "CONTACTED", "QUALIFICATION", "PROPOSAL", "NEGOTIATION", "WON", "LOST")
            .stream()
            .map(
                stage -> {
                  var value = stageData.get(stage);
                  return Map.of(
                      "label",
                      stage,
                      "count",
                      value == null ? 0L : ((Number) value[1]).longValue(),
                      "value",
                      value == null ? BigDecimal.ZERO : value[2]);
                })
            .toList());
    Map<String, Long> statusData = new HashMap<>();
    tickets
        .groupStatuses(org)
        .forEach(value -> statusData.put((String) value[0], ((Number) value[1]).longValue()));
    result.put(
        "ticketsByStatus",
        List.of("OPEN", "IN_PROGRESS", "WAITING", "RESOLVED", "CLOSED").stream()
            .map(status -> Map.of("label", status, "count", statusData.getOrDefault(status, 0L)))
            .toList());
    result.put("recentActivities", support.audit.recentActivities(12));
    return result;
  }
}
