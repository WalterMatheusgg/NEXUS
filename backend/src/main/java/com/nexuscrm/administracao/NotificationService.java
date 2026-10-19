package com.nexuscrm.administracao;

import com.nexuscrm.shared.*;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
  private final NotificationRepository repository;
  private final CurrentUser current;

  public NotificationService(NotificationRepository repository, CurrentUser current) {
    this.repository = repository;
    this.current = current;
  }

  @Transactional
  public void notifyUser(Long userId, String title, String message, String link) {
    if (userId == null) return;
    current.requireMember(userId);
    Notification n = new Notification();
    n.setOrganizationId(current.organizationId());
    n.userId = userId;
    n.title = title;
    n.message = message;
    n.link = link;
    repository.save(n);
  }

  public List<NotificationView> list() {
    current.require("NOTIFICATIONS_READ");
    return repository
        .findByOrganizationIdAndUserIdOrderByCreatedAtDesc(
            current.organizationId(), current.userId(), PageRequest.of(0, 100))
        .stream()
        .map(
            n ->
                new NotificationView(
                    n.getId(), n.title, n.message, n.link, n.read, n.getCreatedAt()))
        .toList();
  }

  @Transactional
  public void read(Long id) {
    current.require("NOTIFICATIONS_READ");
    Notification n =
        repository
            .findByIdAndOrganizationIdAndUserId(id, current.organizationId(), current.userId())
            .orElseThrow(() -> ApiException.notFound("Notificação não encontrada."));
    n.read = true;
  }

  public record NotificationView(
      Long id, String title, String message, String link, boolean read, OffsetDateTime createdAt) {}
}
