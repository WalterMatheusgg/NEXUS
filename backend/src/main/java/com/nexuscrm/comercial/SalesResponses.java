package com.nexuscrm.comercial;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class SalesResponses {
  private SalesResponses() {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record LeadResponse(
      Long id,
      Long version,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt,
      String name,
      String email,
      String phone,
      String source,
      String status,
      Long assigneeId,
      String assigneeName,
      String notes,
      Long customerId) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record OpportunityResponse(
      Long id,
      Long version,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt,
      String title,
      Long customerId,
      String customerName,
      Long leadId,
      Long assigneeId,
      String assigneeName,
      String stage,
      BigDecimal amount,
      Integer probability,
      LocalDate expectedCloseDate,
      String description) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record OpportunityInteractionResponse(
      Long id,
      Long version,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt,
      String note,
      String authorName) {}
}
