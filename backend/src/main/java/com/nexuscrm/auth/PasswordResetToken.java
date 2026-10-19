package com.nexuscrm.auth;

import com.nexuscrm.shared.TenantEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "PASSWORD_RESET_TOKENS")
public class PasswordResetToken extends TenantEntity {
  @Column(nullable = false)
  Long userId;

  @Column(nullable = false, length = 64)
  String tokenHash;

  @Column(nullable = false)
  OffsetDateTime expiresAt;

  OffsetDateTime usedAt;
}
