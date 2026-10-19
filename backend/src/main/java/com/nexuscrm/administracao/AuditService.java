package com.nexuscrm.administracao;

import com.nexuscrm.shared.*;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
  private final AuditRepository repository;
  private final CurrentUser current;

  public AuditService(AuditRepository repository, CurrentUser current) {
    this.repository = repository;
    this.current = current;
  }

  @Transactional
  public void record(String action, String entityType, Long entityId, String detail) {
    UserAccount actor = current.account();
    recordFor(actor, action, entityType, entityId, detail);
  }

  @Transactional
  public void recordFor(
      UserAccount actor, String action, String entityType, Long entityId, String detail) {
    AuditEvent event = new AuditEvent();
    event.setOrganizationId(actor.getOrganizationId());
    event.actorId = actor.getId();
    event.actorName = actor.getName();
    event.action = action;
    event.entityType = entityType;
    event.entityId = entityId;
    event.detail = detail == null ? null : detail.substring(0, Math.min(1000, detail.length()));
    repository.save(event);
  }

  public List<AuditView> recentActivities(int limit) {
    return repository
        .findByOrganizationIdOrderByCreatedAtDesc(
            current.organizationId(), PageRequest.of(0, Math.max(1, Math.min(limit, 50))))
        .stream()
        .map(this::view)
        .toList();
  }

  public PageResponse<AuditView> list(int page, int size) {
    current.require("AUDIT_READ");
    return PageResponse.from(
        repository
            .findByOrganizationId(
                current.organizationId(),
                PageRequest.of(
                    Math.max(0, page),
                    Math.max(1, Math.min(size, 100)),
                    Sort.by("createdAt").descending()))
            .map(this::view));
  }

  private AuditView view(AuditEvent e) {
    return new AuditView(
        e.getId(), e.action, e.entityType, e.entityId, e.detail, e.actorName, e.getCreatedAt());
  }

  public record AuditView(
      Long id,
      String action,
      String entityType,
      Long entityId,
      String detail,
      String actorName,
      OffsetDateTime createdAt) {}
}
