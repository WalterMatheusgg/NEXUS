package com.nexuscrm.administracao;

import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  List<Notification> findByOrganizationIdAndUserIdOrderByCreatedAtDesc(
      Long organizationId, Long userId, Pageable pageable);

  Optional<Notification> findByIdAndOrganizationIdAndUserId(Long id, Long org, Long user);

  long deleteByReadTrueAndCreatedAtBefore(OffsetDateTime before);
}
