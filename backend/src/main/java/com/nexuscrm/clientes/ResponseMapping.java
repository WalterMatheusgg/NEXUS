package com.nexuscrm.clientes;

import com.nexuscrm.shared.PageResponse;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Converts internal service views into explicit, documented HTTP contracts. */
@Component
public final class ResponseMapping {
  private final ObjectMapper mapper;

  public ResponseMapping(ObjectMapper mapper) { this.mapper = mapper; }

  public <T> T one(Object value, Class<T> responseType) {
    return mapper.convertValue(value, responseType);
  }

  public <T> List<T> list(List<?> values, Class<T> responseType) {
    return values.stream().map(value -> one(value, responseType)).toList();
  }

  public <T> PageResponse<T> page(PageResponse<?> values, Class<T> responseType) {
    return new PageResponse<>(list(values.content(), responseType), values.totalElements(),
        values.totalPages(), values.page(), values.size());
  }
}
