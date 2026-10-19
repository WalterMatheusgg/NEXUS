package com.nexuscrm.auth;

import com.nexuscrm.shared.TenantEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "MAIL_OUTBOX")
public class MailOutbox extends TenantEntity {
  @Column(nullable = false, length = 254)
  String recipient;

  @Column(nullable = false, length = 160)
  String subject;

  @Column(nullable = false, length = 4000)
  String body;

  int attempts;
  OffsetDateTime sentAt;
  OffsetDateTime nextAttemptAt;
}
