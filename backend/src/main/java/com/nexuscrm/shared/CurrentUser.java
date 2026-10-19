package com.nexuscrm.shared;

import com.nexuscrm.administracao.*;
import com.nexuscrm.auth.NexusPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
  private final UserAccountRepository users;
  private final RoleRepository roles;
  private final OrganizationRepository organizations;

  public CurrentUser(
      UserAccountRepository users, RoleRepository roles, OrganizationRepository organizations) {
    this.users = users;
    this.roles = roles;
    this.organizations = organizations;
  }

  public NexusPrincipal principal() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof NexusPrincipal principal))
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Entre na sua conta para continuar.");
    return principal;
  }

  public long organizationId() {
    return account().getOrganizationId();
  }

  public long userId() {
    return account().getId();
  }

  public UserAccount account() {
    var principal = principal();
    var user =
        users
            .findByIdAndOrganizationId(principal.userId(), principal.organizationId())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
    var org =
        organizations
            .findById(principal.organizationId())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
    if (!user.isActive()
        || !"ACTIVE".equals(org.getStatus())
        || user.getAuthVersion() != principal.authVersion())
      throw new ApiException(HttpStatus.UNAUTHORIZED, "A sessão expirou ou o acesso foi revogado.");
    return user;
  }

  public boolean can(String permission) {
    var user = account();
    return roles
        .findByIdAndOrganizationId(user.getRoleId(), user.getOrganizationId())
        .map(role -> role.permissions().contains(permission))
        .orElse(false);
  }

  public void require(String permission) {
    if (!can(permission)) throw ApiException.forbidden("Seu perfil não permite esta operação.");
  }

  public void requireMember(Long id) {
    if (id != null
        && users
            .findByIdAndOrganizationId(id, organizationId())
            .filter(UserAccount::isActive)
            .isEmpty())
      throw ApiException.badRequest("O responsável deve ser um usuário ativo desta organização.");
  }
}
