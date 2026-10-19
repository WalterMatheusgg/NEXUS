package com.nexuscrm.comercial;

import com.nexuscrm.shared.TenantEntity;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "BUSINESS_HISTORY")
public class BusinessHistory extends TenantEntity {
  @Column(name = "ENTITY_TYPE", nullable = false, length = 30)
  private String entityType;

  @Column(name = "ENTITY_ID", nullable = false)
  private Long entityId;

  @Column(name = "ACTION_NAME", nullable = false, length = 40)
  private String action;

  @Column(nullable = false, length = 2000)
  private String detail;

  @Column(name = "ACTOR_ID", nullable = false)
  private Long actorId;

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String value) {
    this.entityType = value;
  }

  public Long getEntityId() {
    return entityId;
  }

  public void setEntityId(Long value) {
    this.entityId = value;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String value) {
    this.action = value;
  }

  public String getDetail() {
    return detail;
  }

  public void setDetail(String value) {
    this.detail = value;
  }

  public Long getActorId() {
    return actorId;
  }

  public void setActorId(Long value) {
    this.actorId = value;
  }
}
