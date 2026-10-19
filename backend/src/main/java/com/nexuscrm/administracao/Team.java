package com.nexuscrm.administracao;

import com.nexuscrm.shared.TenantEntity;
import jakarta.persistence.*;
import org.hibernate.type.NumericBooleanConverter;

@Entity
@Table(name = "TEAMS")
public class Team extends TenantEntity {
  @Column(nullable = false, length = 120)
  String name;

  @Column(name = "DEPARTMENT_ID")
  Long departmentId;

  @Convert(converter = NumericBooleanConverter.class)
  @Column(nullable = false)
  boolean active = true;
}
