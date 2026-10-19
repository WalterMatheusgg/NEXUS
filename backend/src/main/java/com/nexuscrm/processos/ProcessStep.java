package com.nexuscrm.processos;

import com.nexuscrm.shared.TenantEntity;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "PROCESS_STEPS")
public class ProcessStep extends TenantEntity {
  @Column(name = "PROCESS_ID", nullable = false)
  private Long processId;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(name = "ASSIGNEE_ID")
  private Long assigneeId;

  @Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
  @Column(name = "REQUIRES_APPROVAL", nullable = false)
  private Boolean requiresApproval;

  @Column(name = "STEP_POSITION", nullable = false)
  private Integer position;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(name = "APPROVED_BY")
  private Long approvedBy;

  @Column(name = "COMPLETED_AT")
  private OffsetDateTime completedAt;

  public Long getProcessId() {
    return processId;
  }

  public void setProcessId(Long value) {
    this.processId = value;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String value) {
    this.title = value;
  }

  public Long getAssigneeId() {
    return assigneeId;
  }

  public void setAssigneeId(Long value) {
    this.assigneeId = value;
  }

  public Boolean getRequiresApproval() {
    return requiresApproval;
  }

  public void setRequiresApproval(Boolean value) {
    this.requiresApproval = value;
  }

  public Integer getPosition() {
    return position;
  }

  public void setPosition(Integer value) {
    this.position = value;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    this.status = value;
  }

  public Long getApprovedBy() {
    return approvedBy;
  }

  public void setApprovedBy(Long value) {
    this.approvedBy = value;
  }

  public OffsetDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(OffsetDateTime value) {
    this.completedAt = value;
  }
}
