package com.nexuscrm.auth;

import jakarta.persistence.*;
import org.hibernate.type.NumericBooleanConverter;

@Entity
@Table(name = "INSTALLATION_STATE")
public class InstallationState {
  @Id Long id;

  @Convert(converter = NumericBooleanConverter.class)
  boolean initialized;
}
