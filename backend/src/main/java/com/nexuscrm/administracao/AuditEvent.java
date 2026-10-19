package com.nexuscrm.administracao;

import com.nexuscrm.shared.TenantEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "AUDIT_EVENTS")
public class AuditEvent extends TenantEntity {
  Long actorId;

  @Column(nullable = false, length = 120)
  String actorName;

  @Column(nullable = false, length = 80)
  String action;

  @Column(nullable = false, length = 80)
  String entityType;

  Long entityId;

  @Column(length = 1000)
  String detail;
}
