package com.nexuscrm.clientes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class CustomerResponses {
  private CustomerResponses() {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record CustomerResponse(
      Long id,
      Long version,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt,
      String type,
      String name,
      String document,
      String email,
      String phone,
      String address,
      Boolean active,
      @JsonInclude(JsonInclude.Include.NON_NULL) List<ContactResponse> contacts,
      @JsonInclude(JsonInclude.Include.NON_NULL) List<CustomerInteractionResponse> interactions) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ContactResponse(
      Long id,
      Long version,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt,
      String name,
      String email,
      String phone,
      String position) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record CustomerInteractionResponse(
      Long id,
      Long version,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt,
      String note,
      String authorName) {}
}
