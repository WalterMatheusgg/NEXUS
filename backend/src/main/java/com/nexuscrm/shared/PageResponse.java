package com.nexuscrm.shared;

import java.util.List;
import java.util.Set;
import org.springframework.data.domain.*;

public record PageResponse<T>(
    List<T> content, long totalElements, int totalPages, int page, int size) {
  public static <T> PageResponse<T> from(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.getNumber(),
        page.getSize());
  }

  public static Pageable pageable(
      int page, int size, String sort, String direction, Set<String> allowed) {
    if (page < 0 || size < 1 || size > 100)
      throw ApiException.badRequest("Página ou tamanho inválido (máximo 100).");
    if (!allowed.contains(sort)) throw ApiException.badRequest("Ordenação inválida.");
    if (!"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction))
      throw ApiException.badRequest("Direção inválida.");
    return PageRequest.of(
        page, size, Sort.by(Sort.Direction.fromString(direction), sort).and(Sort.by("id")));
  }
}
