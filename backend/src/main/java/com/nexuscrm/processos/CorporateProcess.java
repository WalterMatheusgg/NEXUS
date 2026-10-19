package com.nexuscrm.processos;

import com.nexuscrm.shared.TenantEntity;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "CORPORATE_PROCESSES")
public class CorporateProcess extends TenantEntity {
  @Column(nullable = false, length = 200)
  private String title;

  @Column(length = 4000)
  private String description;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(name = "ASSIGNEE_ID")
  private Long assigneeId;

  public String getTitle() {
    return title;
  }

  public void setTitle(String value) {
    this.title = value;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String value) {
    this.description = value;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    this.status = value;
  }

  public Long getAssigneeId() {
    return assigneeId;
  }

  public void setAssigneeId(Long value) {
    this.assigneeId = value;
  }
}
