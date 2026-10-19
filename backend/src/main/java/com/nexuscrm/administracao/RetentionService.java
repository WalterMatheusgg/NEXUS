package com.nexuscrm.administracao;

import com.nexuscrm.auth.*;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RetentionService {
  private final AuditRepository audit;
  private final NotificationRepository notifications;
  private final PasswordResetRepository resets;
  private final MailOutboxRepository mail;
  private final int auditDays, notificationDays;

  public RetentionService(
      AuditRepository audit,
      NotificationRepository notifications,
      PasswordResetRepository resets,
      MailOutboxRepository mail,
      @Value("${nexus.audit-retention-days:365}") int auditDays,
      @Value("${nexus.notification-retention-days:90}") int notificationDays) {
    this.audit = audit;
    this.notifications = notifications;
    this.resets = resets;
    this.mail = mail;
    this.auditDays = auditDays;
    this.notificationDays = notificationDays;
  }

  @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
  @Transactional
  public void clean() {
    OffsetDateTime now = OffsetDateTime.now();
    resets.deleteByExpiresAtBefore(now.minusDays(1));
    mail.deleteByCreatedAtBefore(now.minusDays(1));
    if (auditDays > 0) audit.deleteByCreatedAtBefore(now.minusDays(Math.max(30, auditDays)));
    if (notificationDays > 0)
      notifications.deleteByReadTrueAndCreatedAtBefore(
          now.minusDays(Math.max(7, notificationDays)));
  }
}
