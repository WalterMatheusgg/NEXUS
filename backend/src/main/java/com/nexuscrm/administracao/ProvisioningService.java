package com.nexuscrm.administracao;

import com.nexuscrm.auth.Passwords;
import com.nexuscrm.shared.ApiException;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProvisioningService {
  private final OrganizationRepository organizations;
  private final RoleRepository roles;
  private final UserAccountRepository users;
  private final PasswordEncoder encoder;

  public ProvisioningService(
      OrganizationRepository organizations,
      RoleRepository roles,
      UserAccountRepository users,
      PasswordEncoder encoder) {
    this.organizations = organizations;
    this.roles = roles;
    this.users = users;
    this.encoder = encoder;
  }

  @Transactional
  public UserAccount provision(
      String code, String organizationName, String name, String email, String password) {
    Passwords.validate(password);
    code = code.trim().toUpperCase(Locale.ROOT);
    if (!code.matches("[A-Z0-9][A-Z0-9_-]{2,49}"))
      throw ApiException.badRequest(
          "O código da empresa deve ter 3 a 50 letras, números, hífen ou sublinhado.");
    if (organizations.existsByCodeIgnoreCase(code))
      throw ApiException.conflict("O código desta organização já está cadastrado.");
    Organization org = organizations.saveAndFlush(new Organization(code, organizationName.trim()));
    AppRole admin = role(org.getId(), "ADMIN", "Administrador", Permission.all());
    Set<String> common =
        Set.of(
            "DASHBOARD_READ", "CUSTOMERS_READ", "TASKS_READ", "TASKS_WRITE", "NOTIFICATIONS_READ");
    role(
        org.getId(),
        "MANAGER",
        "Gestor",
        union(
            common,
            Set.of(
                "CUSTOMERS_WRITE",
                "SALES_READ",
                "SALES_WRITE",
                "TICKETS_READ",
                "TICKETS_WRITE",
                "PROCESSES_READ",
                "PROCESSES_WRITE",
                "REPORTS_READ")));
    role(
        org.getId(),
        "SALES",
        "Vendedor",
        union(common, Set.of("CUSTOMERS_WRITE", "SALES_READ", "SALES_WRITE")));
    role(
        org.getId(),
        "SUPPORT",
        "Atendimento",
        union(common, Set.of("TICKETS_READ", "TICKETS_WRITE")));
    role(org.getId(), "EMPLOYEE", "Funcionário", common);
    UserAccount user = new UserAccount();
    user.setOrganizationId(org.getId());
    user.name = name.trim();
    user.email = email.trim().toLowerCase(Locale.ROOT);
    user.roleId = admin.getId();
    user.passwordHash = encoder.encode(password);
    return users.saveAndFlush(user);
  }

  private AppRole role(Long org, String code, String name, Set<String> permissions) {
    AppRole role = new AppRole();
    role.setOrganizationId(org);
    role.configure(code, name, permissions);
    return roles.saveAndFlush(role);
  }

  private Set<String> union(Set<String> a, Set<String> b) {
    Set<String> result = new HashSet<>(a);
    result.addAll(b);
    return result;
  }
}
