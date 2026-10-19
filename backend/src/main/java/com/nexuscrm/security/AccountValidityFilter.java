package com.nexuscrm.security;

import com.nexuscrm.administracao.*;
import com.nexuscrm.auth.NexusPrincipal;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class AccountValidityFilter extends OncePerRequestFilter {
  private final UserAccountRepository users;
  private final OrganizationRepository organizations;

  public AccountValidityFilter(UserAccountRepository users, OrganizationRepository organizations) {
    this.users = users;
    this.organizations = organizations;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof NexusPrincipal principal) {
      var user = users.findByIdAndOrganizationId(principal.userId(), principal.organizationId());
      boolean valid =
          user.filter(UserAccount::isActive)
                  .filter(u -> u.getAuthVersion() == principal.authVersion())
                  .isPresent()
              && organizations
                  .findById(principal.organizationId())
                  .filter(o -> "ACTIVE".equals(o.getStatus()))
                  .isPresent();
      if (!valid) {
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) request.getSession().invalidate();
        SecurityErrors.write(response, 401, "A sessão expirou ou o acesso foi revogado.");
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
