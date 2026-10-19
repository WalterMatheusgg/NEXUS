package com.nexuscrm.administracao;

import com.nexuscrm.auth.*;
import com.nexuscrm.shared.*;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdministrationService {
  private final CurrentUser current;
  private final UserAccountRepository users;
  private final RoleRepository roles;
  private final DepartmentRepository departments;
  private final TeamRepository teams;
  private final OrganizationRepository organizations;
  private final PasswordEncoder encoder;
  private final AuditService audit;
  private final SessionTracker sessions;
  private final PasswordResetRepository resets;

  public AdministrationService(
      CurrentUser current,
      UserAccountRepository users,
      RoleRepository roles,
      DepartmentRepository departments,
      TeamRepository teams,
      OrganizationRepository organizations,
      PasswordEncoder encoder,
      AuditService audit,
      SessionTracker sessions,
      PasswordResetRepository resets) {
    this.current = current;
    this.users = users;
    this.roles = roles;
    this.departments = departments;
    this.teams = teams;
    this.organizations = organizations;
    this.encoder = encoder;
    this.audit = audit;
    this.sessions = sessions;
    this.resets = resets;
  }

  public PageResponse<UserView> users(int page, int size, String search) {
    current.require("USERS_MANAGE");
    long org = current.organizationId();
    String term = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
    Specification<UserAccount> scope =
        (root, q, cb) ->
            cb.and(
                cb.equal(root.get("organizationId"), org),
                cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + escape(term) + "%", '\\'),
                    cb.like(cb.lower(root.get("email")), "%" + escape(term) + "%", '\\')));
    return PageResponse.from(
        users
            .findAll(scope, PageResponse.pageable(page, size, "name", "asc", Set.of("name")))
            .map(this::view));
  }

  public List<Member> members() {
    return users.findByOrganizationIdAndActiveTrue(current.organizationId()).stream()
        .map(u -> new Member(u.getId(), u.name, u.email))
        .toList();
  }

  @Transactional
  public UserView saveUser(Long id, UserRequest input) {
    current.require("USERS_MANAGE");
    long org = current.organizationId();
    UserAccount user =
        id == null
            ? new UserAccount()
            : users
                .lock(id, org)
                .orElseThrow(() -> ApiException.notFound("Usuário não encontrado."));
    if (id != null
        && !currentRole()
            .permissions()
            .containsAll(
                roles.findByIdAndOrganizationId(user.roleId, org).orElseThrow().permissions()))
      throw ApiException.forbidden(
          "Você não pode administrar uma conta com permissões superiores às suas.");
    checkVersion(user.getVersion(), input.version());
    AppRole role =
        roles
            .findByIdAndOrganizationId(input.roleId(), org)
            .orElseThrow(() -> ApiException.badRequest("Perfil inválido."));
    if (!currentRole().permissions().containsAll(role.permissions()))
      throw ApiException.forbidden("Você não pode conceder permissões que não possui.");
    boolean active = input.active() == null || input.active();
    if (id != null
        && id == current.userId()
        && (!active || !Objects.equals(user.roleId, input.roleId())))
      throw ApiException.conflict(
          "Seu próprio acesso administrativo não pode ser removido nesta operação.");
    if (id != null
        && (!active || !Objects.equals(user.roleId, input.roleId()))
        && "ADMIN".equals(roles.findByIdAndOrganizationId(user.roleId, org).orElseThrow().code)
        && users.countByOrganizationIdAndRoleIdAndActiveTrue(org, user.roleId) <= 1)
      throw ApiException.conflict("Mantenha pelo menos um administrador ativo.");
    validateUnit(input.departmentId(), input.teamId());
    if (id == null) {
      Passwords.validate(input.password());
      user.setOrganizationId(org);
      user.passwordHash = encoder.encode(input.password());
    }
    user.name = input.name().trim();
    user.email = input.email().trim().toLowerCase(Locale.ROOT);
    user.roleId = input.roleId();
    user.departmentId = input.departmentId();
    user.teamId = input.teamId();
    user.active = active;
    users.saveAndFlush(user);
    if (!active) sessions.invalidateAll(user.getId());
    audit.record(
        id == null ? "CREATED" : "UPDATED",
        "USER",
        user.getId(),
        "Cadastro de usuário atualizado.");
    return view(user);
  }

  @Transactional
  public void password(Long id, String password) {
    current.require("USERS_MANAGE");
    UserAccount actor = current.account();
    Passwords.validate(password);
    UserAccount user =
        users
            .lock(id, current.organizationId())
            .orElseThrow(() -> ApiException.notFound("Usuário não encontrado."));
    if (!currentRole()
        .permissions()
        .containsAll(
            roles
                .findByIdAndOrganizationId(user.roleId, user.getOrganizationId())
                .orElseThrow()
                .permissions()))
      throw ApiException.forbidden("Seu perfil não permite alterar esta conta.");
    user.setPasswordHash(encoder.encode(password));
    user.clearLoginFailures();
    resets.invalidate(user.getOrganizationId(), user.getId(), OffsetDateTime.now());
    sessions.invalidateAll(id);
    audit.recordFor(
        actor,
        "PASSWORD_CHANGED_BY_ADMIN",
        "USER",
        id,
        "Senha alterada por administrador; sessões revogadas.");
  }

  public List<RoleView> roles() {
    readAdminCatalog();
    return roles.findByOrganizationIdOrderByNameAsc(current.organizationId()).stream()
        .map(this::view)
        .toList();
  }

  @Transactional
  public RoleView saveRole(Long id, RoleRequest input) {
    current.require("ROLES_MANAGE");
    if (!Permission.all().containsAll(input.permissions()))
      throw ApiException.badRequest("Permissão desconhecida.");
    if (!currentRole().permissions().containsAll(input.permissions()))
      throw ApiException.forbidden("Você não pode conceder permissões que não possui.");
    AppRole role =
        id == null
            ? new AppRole()
            : roles
                .findByIdAndOrganizationId(id, current.organizationId())
                .orElseThrow(() -> ApiException.notFound("Perfil não encontrado."));
    if (id != null && !currentRole().permissions().containsAll(role.permissions()))
      throw ApiException.forbidden(
          "Você não pode alterar um perfil com permissões superiores às suas.");
    if (id == null && (input.code() == null || input.code().isBlank()))
      throw ApiException.badRequest("Código do perfil obrigatório.");
    checkVersion(role.getVersion(), input.version());
    String code = id == null ? input.code().toUpperCase(Locale.ROOT) : role.code;
    if (!code.matches("[A-Z][A-Z0-9_]{1,49}"))
      throw ApiException.badRequest("Código de perfil inválido.");
    if ("ADMIN".equals(code) && !input.permissions().equals(Permission.all()))
      throw ApiException.conflict("O perfil Administrador deve manter todas as permissões.");
    role.setOrganizationId(current.organizationId());
    role.configure(code, input.name().trim(), input.permissions());
    roles.saveAndFlush(role);
    audit.record(
        id == null ? "CREATED" : "UPDATED",
        "ROLE",
        role.getId(),
        "Perfil e permissões atualizados.");
    return view(role);
  }

  public List<UnitView> departments() {
    readAdminCatalog();
    return departments.findByOrganizationIdOrderByNameAsc(current.organizationId()).stream()
        .map(d -> new UnitView(d.getId(), d.name, d.active, null, d.getVersion()))
        .toList();
  }

  public List<UnitView> teams() {
    readAdminCatalog();
    return teams.findByOrganizationIdOrderByNameAsc(current.organizationId()).stream()
        .map(t -> new UnitView(t.getId(), t.name, t.active, t.departmentId, t.getVersion()))
        .toList();
  }

  @Transactional
  public UnitView saveDepartment(Long id, UnitRequest input) {
    current.require("SETTINGS_MANAGE");
    Department d =
        id == null
            ? new Department()
            : departments
                .findByIdAndOrganizationId(id, current.organizationId())
                .orElseThrow(() -> ApiException.notFound("Departamento não encontrado."));
    checkVersion(d.getVersion(), input.version());
    d.setOrganizationId(current.organizationId());
    d.name = input.name().trim();
    d.active = input.active() == null || input.active();
    departments.saveAndFlush(d);
    audit.record("SAVED", "DEPARTMENT", d.getId(), "Departamento atualizado.");
    return new UnitView(d.getId(), d.name, d.active, null, d.getVersion());
  }

  @Transactional
  public UnitView saveTeam(Long id, UnitRequest input) {
    current.require("SETTINGS_MANAGE");
    validateUnit(input.departmentId(), null);
    Team t =
        id == null
            ? new Team()
            : teams
                .findByIdAndOrganizationId(id, current.organizationId())
                .orElseThrow(() -> ApiException.notFound("Equipe não encontrada."));
    checkVersion(t.getVersion(), input.version());
    t.setOrganizationId(current.organizationId());
    t.name = input.name().trim();
    t.active = input.active() == null || input.active();
    t.departmentId = input.departmentId();
    teams.saveAndFlush(t);
    audit.record("SAVED", "TEAM", t.getId(), "Equipe atualizada.");
    return new UnitView(t.getId(), t.name, t.active, t.departmentId, t.getVersion());
  }

  public OrganizationView organization() {
    current.require("SETTINGS_MANAGE");
    return organizationView(organizations.findById(current.organizationId()).orElseThrow());
  }

  @Transactional
  public OrganizationView organization(OrganizationRequest input) {
    current.require("SETTINGS_MANAGE");
    Organization org = organizations.findById(current.organizationId()).orElseThrow();
    org.name = input.name().trim();
    audit.record("UPDATED", "ORGANIZATION", org.id, "Configurações empresariais atualizadas.");
    return organizationView(org);
  }

  private void validateUnit(Long departmentId, Long teamId) {
    if (departmentId != null
        && departments
            .findByIdAndOrganizationId(departmentId, current.organizationId())
            .filter(d -> d.active)
            .isEmpty()) throw ApiException.badRequest("Departamento inválido ou inativo.");
    if (teamId != null) {
      Team team =
          teams
              .findByIdAndOrganizationId(teamId, current.organizationId())
              .filter(t -> t.active)
              .orElseThrow(() -> ApiException.badRequest("Equipe inválida ou inativa."));
      if (departmentId != null && !Objects.equals(departmentId, team.departmentId))
        throw ApiException.badRequest("A equipe pertence a outro departamento.");
    }
  }

  private void readAdminCatalog() {
    if (!current.can("USERS_MANAGE")
        && !current.can("ROLES_MANAGE")
        && !current.can("SETTINGS_MANAGE"))
      throw ApiException.forbidden("Seu perfil não permite esta operação.");
  }

  private AppRole currentRole() {
    UserAccount actor = current.account();
    return roles.findByIdAndOrganizationId(actor.roleId, actor.getOrganizationId()).orElseThrow();
  }

  private void checkVersion(Long actual, Long expected) {
    if (expected != null && !Objects.equals(actual, expected))
      throw ApiException.conflict("Registro alterado. Atualize antes de salvar.");
  }

  private String escape(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  private UserView view(UserAccount u) {
    return new UserView(
        u.getId(),
        u.name,
        u.email,
        u.roleId,
        roles
            .findByIdAndOrganizationId(u.roleId, u.getOrganizationId())
            .map(r -> r.name)
            .orElse(""),
        u.departmentId,
        u.teamId,
        u.active,
        u.getVersion(),
        u.getCreatedAt());
  }

  private RoleView view(AppRole r) {
    return new RoleView(r.getId(), r.code, r.name, r.permissions(), r.getVersion());
  }

  private OrganizationView organizationView(Organization o) {
    return new OrganizationView(o.id, o.code, o.name, o.status);
  }

  public record UserRequest(
      @NotBlank @Size(max = 120) String name,
      @NotBlank @Email @Size(max = 254) String email,
      @Size(max = 72) String password,
      @NotNull Long roleId,
      Long departmentId,
      Long teamId,
      Boolean active,
      Long version) {}

  public record RoleRequest(
      @Size(max = 50) String code,
      @NotBlank @Size(max = 100) String name,
      @NotNull Set<String> permissions,
      Long version) {}

  public record UnitRequest(
      @NotBlank @Size(max = 120) String name, Boolean active, Long departmentId, Long version) {}

  public record OrganizationRequest(@NotBlank @Size(max = 160) String name) {}

  public record PasswordRequest(@NotBlank @Size(min = 12, max = 72) String password) {}

  public record UserView(
      Long id,
      String name,
      String email,
      Long roleId,
      String roleName,
      Long departmentId,
      Long teamId,
      boolean active,
      Long version,
      OffsetDateTime createdAt) {}

  public record RoleView(
      Long id, String code, String name, Set<String> permissions, Long version) {}

  public record UnitView(Long id, String name, boolean active, Long departmentId, Long version) {}

  public record OrganizationView(Long id, String code, String name, String status) {}

  public record Member(Long id, String name, String email) {}
}
