package com.nexuscrm.administracao;

import com.nexuscrm.shared.TenantEntity;
import jakarta.persistence.*;
import org.hibernate.type.NumericBooleanConverter;

@Entity
@Table(name = "DEPARTMENTS")
public class Department extends TenantEntity {
  @Column(nullable = false, length = 120)
  String name;

  @Convert(converter = NumericBooleanConverter.class)
  @Column(nullable = false)
  boolean active = true;
}
