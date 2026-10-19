package com.nexuscrm.shared;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@MappedSuperclass
public abstract class TenantEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  protected Long id;

  @Column(name = "ORGANIZATION_ID", nullable = false, updatable = false)
  protected Long organizationId;

  @Column(name = "CREATED_AT", nullable = false, updatable = false)
  protected OffsetDateTime createdAt;

  @Column(name = "UPDATED_AT", nullable = false)
  protected OffsetDateTime updatedAt;

  @Version protected Long version;

  @PrePersist
  protected void created() {
    createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    updatedAt = createdAt;
  }

  @PreUpdate
  protected void updated() {
    updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public Long getId() {
    return id;
  }

  public Long getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(Long organizationId) {
    this.organizationId = organizationId;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public Long getVersion() {
    return version;
  }
}
