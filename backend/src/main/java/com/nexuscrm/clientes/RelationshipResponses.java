package com.nexuscrm.clientes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class RelationshipResponses {
  private RelationshipResponses() {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record HistoryResponse(
      Long id,
      Long version,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt,
      String action,
      String detail,
      String actorName) {}
}
