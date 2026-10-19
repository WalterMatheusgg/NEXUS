package com.nexuscrm.auth;

import jakarta.validation.constraints.*;

public final class AuthRequests {
  private AuthRequests() {}

  public record Setup(
      @NotBlank @Size(max = 200) String setupToken,
      @NotBlank @Size(max = 50) String organizationCode,
      @NotBlank @Size(max = 160) String organizationName,
      @NotBlank @Size(max = 120) String name,
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 12, max = 72) String password) {}

  public record Login(
      @NotBlank @Size(max = 50) String organizationCode,
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(max = 200) String password) {}

  public record Forgot(
      @NotBlank @Size(max = 50) String organizationCode,
      @NotBlank @Email @Size(max = 254) String email) {}

  public record Reset(
      @NotBlank @Pattern(regexp = "[a-f0-9]{64}") String token,
      @NotBlank @Size(min = 12, max = 72) String password) {}

  public record Change(
      @NotBlank @Size(max = 200) String currentPassword,
      @NotBlank @Size(min = 12, max = 72) String password) {}
}
