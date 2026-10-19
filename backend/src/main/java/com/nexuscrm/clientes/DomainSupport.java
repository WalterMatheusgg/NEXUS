package com.nexuscrm.clientes;

import com.nexuscrm.administracao.*;
import com.nexuscrm.shared.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class DomainSupport {
  public final CurrentUser current;
  public final AuditService audit;
  public final NotificationService notifications;
  private final UserAccountRepository users;
  private final RoleRepository roles;

  public DomainSupport(
      CurrentUser current,
      AuditService audit,
      NotificationService notifications,
      UserAccountRepository users,
      RoleRepository roles) {
    this.current = current;
    this.audit = audit;
    this.notifications = notifications;
    this.users = users;
    this.roles = roles;
  }

  public <T extends TenantEntity> T get(TenantRepository<T> repository, Long id, String label) {
    if (id == null) throw ApiException.badRequest(label + " é obrigatório.");
    return repository
        .findByIdAndOrganizationId(id, current.organizationId())
        .orElseThrow(() -> ApiException.notFound(label + " não encontrado."));
  }

  public <T extends TenantEntity> T initialize(T entity) {
    entity.setOrganizationId(current.organizationId());
    return entity;
  }

  public void version(TenantEntity entity, Long version) {
    if (version != null && !version.equals(entity.getVersion()))
      throw ApiException.conflict("O registro foi alterado. Atualize a página e tente novamente.");
  }

  public String memberName(Long id) {
    return id == null
        ? null
        : users
            .findByIdAndOrganizationId(id, current.organizationId())
            .map(UserAccount::getName)
            .orElse(null);
  }

  public boolean administrator() {
    var actor = current.account();
    return roles
        .findByIdAndOrganizationId(actor.getRoleId(), actor.getOrganizationId())
        .map(role -> role.permissions().containsAll(Permission.all()))
        .orElse(false);
  }

  public void member(Long id) {
    current.requireMember(id);
  }

  public void notifyAssignment(Long id, String title, String link) {
    if (id != null && !id.equals(current.userId()))
      notifications.notifyUser(id, "Nova atribuição", title, link);
  }

  public Map<String, Object> view(TenantEntity value) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("id", value.getId());
    data.put("version", value.getVersion());
    data.put("createdAt", value.getCreatedAt());
    data.put("updatedAt", value.getUpdatedAt());
    return data;
  }

  public Pageable pageable(int page, int size, String sort, String direction, Set<String> allowed) {
    if (page < 0 || size < 1 || size > 100)
      throw ApiException.badRequest("Paginação inválida: página >= 0 e tamanho entre 1 e 100.");
    if (!allowed.contains(sort)) throw ApiException.badRequest("Ordenação não permitida.");
    if (!Set.of("asc", "desc").contains(direction.toLowerCase(Locale.ROOT)))
      throw ApiException.badRequest("Direção de ordenação inválida.");
    return PageRequest.of(
        page, size, Sort.by(Sort.Direction.fromString(direction), sort).and(Sort.by("id")));
  }

  public <T extends TenantEntity> Specification<T> filter(
      String search,
      List<String> searchFields,
      String status,
      String statusField,
      Long assigneeId,
      LocalDate from,
      LocalDate to) {
    if (from != null && to != null && from.isAfter(to))
      throw ApiException.badRequest("O período inicial deve preceder o final.");
    return (root, query, cb) -> {
      var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
      predicates.add(cb.equal(root.get("organizationId"), current.organizationId()));
      if (search != null && !search.isBlank()) {
        String term =
            "%"
                + search
                    .strip()
                    .toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_")
                + "%";
        predicates.add(
            cb.or(
                searchFields.stream()
                    .map(field -> cb.like(cb.lower(root.get(field)), term, '\\'))
                    .toArray(jakarta.persistence.criteria.Predicate[]::new)));
      }
      if (status != null && !status.isBlank() && statusField != null)
        predicates.add(cb.equal(root.get(statusField), status));
      if (assigneeId != null) predicates.add(cb.equal(root.get("assigneeId"), assigneeId));
      if (from != null)
        predicates.add(
            cb.greaterThanOrEqualTo(
                root.get("createdAt"), from.atStartOfDay().atOffset(ZoneOffset.UTC)));
      if (to != null)
        predicates.add(
            cb.lessThan(
                root.get("createdAt"), to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)));
      return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
    };
  }
}
