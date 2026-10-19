package com.nexuscrm.processos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class ProcessResponses {
  private ProcessResponses() {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ProcessResponse(
      Long id,
      Long version,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt,
      String title,
      String description,
      String status,
      Long assigneeId,
      String assigneeName,
      @JsonInclude(JsonInclude.Include.NON_NULL) List<StepResponse> steps) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record StepResponse(
      Long id,
      Long version,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt,
      String title,
      Long assigneeId,
      String assigneeName,
      Boolean requiresApproval,
      Integer position,
      String status,
      Long approvedBy,
      OffsetDateTime completedAt) {}
}
