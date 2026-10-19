package com.nexuscrm.chamados;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class TicketResponses {
  private TicketResponses() {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record TicketResponse(
      Long id,
      Long version,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt,
      String number,
      String title,
      String description,
      Long customerId,
      String customerName,
      String category,
      String priority,
      String status,
      Long assigneeId,
      String assigneeName,
      OffsetDateTime dueAt,
      OffsetDateTime resolvedAt,
      Boolean overdue) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record CommentResponse(
      Long id,
      Long version,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt,
      String body,
      String authorName) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record AttachmentResponse(
      Long id,
      Long version,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt,
      String fileName,
      Long size,
      String contentType) {}
}
