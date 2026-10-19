package com.nexuscrm.tarefas;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class TaskRequests {
  private TaskRequests() {}

  public record TaskInput(
      @NotBlank @Size(max = 200) String title,
      @Size(max = 4000) String description,
      @NotBlank @Pattern(regexp = "LOW|MEDIUM|HIGH|URGENT") String priority,
      @NotBlank @Pattern(regexp = "TODO|IN_PROGRESS|DONE") String status,
      Long assigneeId,
      LocalDate dueDate,
      Long customerId,
      Long ticketId,
      Long opportunityId,
      Long version) {}

  public record StatusInput(
      @NotBlank @Pattern(regexp = "TODO|IN_PROGRESS|DONE") String status, Long version) {}
}
