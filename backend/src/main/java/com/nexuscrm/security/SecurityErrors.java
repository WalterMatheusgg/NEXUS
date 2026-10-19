package com.nexuscrm.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

public final class SecurityErrors {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private SecurityErrors() {}

  public static void write(HttpServletResponse response, int status, String detail)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/problem+json");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-store");
    JSON.writeValue(
        response.getWriter(),
        Map.of(
            "status",
            status,
            "title",
            status == 401 ? "Autenticação necessária" : "Acesso bloqueado",
            "detail",
            detail));
  }
}
