package com.nexuscrm.comercial;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class SalesRequests {
  private SalesRequests() {}

  public record LeadInput(
      @NotBlank @Size(max = 160) String name,
      @Email @Size(max = 254) String email,
      @Size(max = 40) String phone,
      @Size(max = 100) String source,
      @NotBlank @Pattern(regexp = "NEW|CONTACTED|QUALIFIED|CONVERTED|LOST") String status,
      Long assigneeId,
      @Size(max = 4000) String notes,
      Long version) {}

  public record ConvertInput(
      @NotBlank @Size(max = 30) String document,
      @NotBlank @Pattern(regexp = "PERSON|COMPANY") String type,
      @Size(max = 160) String name) {}

  public record OpportunityInput(
      @NotBlank @Size(max = 200) String title,
      Long customerId,
      Long leadId,
      Long assigneeId,
      @NotBlank @Pattern(regexp = "NEW_LEAD|CONTACTED|QUALIFICATION|PROPOSAL|NEGOTIATION|WON|LOST")
          String stage,
      @NotNull @DecimalMin("0") @Digits(integer = 15, fraction = 2) BigDecimal amount,
      @NotNull @Min(0) @Max(100) Integer probability,
      LocalDate expectedCloseDate,
      @Size(max = 4000) String description,
      Long version) {}

  public record StageInput(
      @NotBlank @Pattern(regexp = "NEW_LEAD|CONTACTED|QUALIFICATION|PROPOSAL|NEGOTIATION|WON|LOST")
          String stage,
      Long version) {}
}
