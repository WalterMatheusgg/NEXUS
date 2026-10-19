package com.nexuscrm.chamados;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

public final class TicketRequests {
  private TicketRequests() {}

  public record TicketInput(
      @NotBlank @Size(max = 200) String title,
      @NotBlank @Size(max = 4000) String description,
      @NotNull Long customerId,
      @NotBlank @Size(max = 100) String category,
      @NotBlank @Pattern(regexp = "LOW|MEDIUM|HIGH|URGENT") String priority,
      @NotBlank @Pattern(regexp = "OPEN|IN_PROGRESS|WAITING|RESOLVED|CLOSED") String status,
      Long assigneeId,
      OffsetDateTime dueAt,
      Long version) {}

  public record StatusInput(
      @NotBlank @Pattern(regexp = "OPEN|IN_PROGRESS|WAITING|RESOLVED|CLOSED") String status,
      Long version) {}

  public record CommentInput(@NotBlank @Size(max = 4000) String body) {}
}
