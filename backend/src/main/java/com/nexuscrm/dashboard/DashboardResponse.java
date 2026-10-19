package com.nexuscrm.dashboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nexuscrm.administracao.AuditService.AuditView;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DashboardResponse(
    long totalCustomers,
    long newCustomersThisMonth,
    long openOpportunities,
    BigDecimal pipelineValue,
    long openTickets,
    long resolvedTickets,
    long pendingTasks,
    long overdueTickets,
    long overdueTasks,
    double conversionRate,
    List<StageMetric> opportunitiesByStage,
    List<StatusMetric> ticketsByStatus,
    List<AuditView> recentActivities) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record StageMetric(String label, long count, BigDecimal value) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record StatusMetric(String label, long count) {}
}
