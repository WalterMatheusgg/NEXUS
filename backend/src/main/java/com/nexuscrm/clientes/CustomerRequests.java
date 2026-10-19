package com.nexuscrm.clientes;

import jakarta.validation.constraints.*;

public final class CustomerRequests {
  private CustomerRequests() {}

  public record CustomerInput(
      @NotBlank @Pattern(regexp = "PERSON|COMPANY") String type,
      @NotBlank @Size(max = 160) String name,
      @NotBlank @Size(max = 30) String document,
      @Email @Size(max = 254) String email,
      @Size(max = 40) String phone,
      @Size(max = 1000) String address,
      @NotNull Boolean active,
      Long version) {}

  public record ContactInput(
      @NotBlank @Size(max = 160) String name,
      @Email @Size(max = 254) String email,
      @Size(max = 40) String phone,
      @Size(max = 100) String position) {}

  public record NoteInput(@NotBlank @Size(max = 4000) String note) {}
}
