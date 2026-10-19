package com.nexuscrm.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.web.filter.OncePerRequestFilter;

public class AuthRateLimitFilter extends OncePerRequestFilter {
  private static final Set<String> SENSITIVE =
      Set.of(
          "/auth/login",
          "/auth/setup",
          "/auth/forgot-password",
          "/auth/reset-password",
          "/auth/change-password");
  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if ("POST".equals(request.getMethod()) && SENSITIVE.contains(request.getServletPath())) {
      long now = System.currentTimeMillis();
      String key = request.getRemoteAddr();
      if (windows.size() > 10000)
        windows.entrySet().removeIf(e -> now - e.getValue().start > 60000);
      if (windows.size() >= 10000 && !windows.containsKey(key)) {
        response.setHeader("Retry-After", "60");
        SecurityErrors.write(response, 429, "Muitas tentativas. Aguarde um minuto.");
        return;
      }
      Window window =
          windows.compute(
              key,
              (k, current) ->
                  current == null || now - current.start >= 60000 ? new Window(now) : current);
      if (window.hits.incrementAndGet() > 20) {
        response.setHeader("Retry-After", "60");
        SecurityErrors.write(response, 429, "Muitas tentativas. Aguarde um minuto.");
        return;
      }
    }
    chain.doFilter(request, response);
  }

  private static final class Window {
    final long start;
    final AtomicInteger hits = new AtomicInteger();

    Window(long start) {
      this.start = start;
    }
  }
}
