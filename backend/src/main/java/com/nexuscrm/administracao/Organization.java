package com.nexuscrm.administracao;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "ORGANIZATIONS")
public class Organization {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Column(nullable = false, length = 50)
  String code;

  @Column(nullable = false, length = 160)
  String name;

  @Column(nullable = false, length = 20)
  String status = "ACTIVE";

  @Column(name = "CREATED_AT", insertable = false, updatable = false)
  OffsetDateTime createdAt;

  @Column(name = "UPDATED_AT", insertable = false, updatable = false)
  OffsetDateTime updatedAt;

  public Organization() {}

  public Organization(String code, String name) {
    this.code = code;
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getStatus() {
    return status;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
