package com.nexuscrm.processos;

import jakarta.validation.constraints.*;

public final class ProcessRequests {
  private ProcessRequests() {}

  public record ProcessInput(
      @NotBlank @Size(max = 200) String title,
      @Size(max = 4000) String description,
      @NotBlank @Pattern(regexp = "OPEN|IN_PROGRESS|COMPLETED|CANCELLED") String status,
      Long assigneeId,
      Long version) {}

  public record StepInput(
      @NotBlank @Size(max = 200) String title,
      Long assigneeId,
      @NotNull Boolean requiresApproval) {}

  public record StepStatusInput(
      @NotBlank @Pattern(regexp = "PENDING|IN_PROGRESS|APPROVED|REJECTED|COMPLETED") String status,
      Long version) {}
}
