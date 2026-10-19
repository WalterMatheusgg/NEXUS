package com.nexuscrm.tarefas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class TaskResponses {
  private TaskResponses() {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record TaskResponse(
      Long id,
      Long version,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt,
      String title,
      String description,
      String priority,
      String status,
      Long assigneeId,
      String assigneeName,
      LocalDate dueDate,
      Long customerId,
      Long ticketId,
      Long opportunityId,
      OffsetDateTime completedAt,
      Boolean overdue) {}
}
