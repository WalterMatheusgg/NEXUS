package com.nexuscrm.administracao;

import com.nexuscrm.shared.TenantEntity;
import jakarta.persistence.*;
import org.hibernate.type.NumericBooleanConverter;

@Entity
@Table(name = "NOTIFICATIONS")
public class Notification extends TenantEntity {
  Long userId;

  @Column(nullable = false, length = 160)
  String title;

  @Column(length = 1000)
  String message;

  @Column(length = 250)
  String link;

  @Column(name = "IS_READ", nullable = false)
  @Convert(converter = NumericBooleanConverter.class)
  boolean read;
}
