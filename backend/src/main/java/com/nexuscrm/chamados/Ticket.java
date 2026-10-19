package com.nexuscrm.chamados;

import com.nexuscrm.shared.TenantEntity;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "SUPPORT_TICKETS")
public class Ticket extends TenantEntity {
  @Column(name = "TICKET_NUMBER", nullable = false, length = 40)
  private String number;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false, length = 4000)
  private String description;

  @Column(name = "CUSTOMER_ID", nullable = false)
  private Long customerId;

  @Column(nullable = false, length = 100)
  private String category;

  @Column(nullable = false, length = 12)
  private String priority;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(name = "ASSIGNEE_ID")
  private Long assigneeId;

  @Column(name = "DUE_AT")
  private OffsetDateTime dueAt;

  @Column(name = "RESOLVED_AT")
  private OffsetDateTime resolvedAt;

  public String getNumber() {
    return number;
  }

  public void setNumber(String value) {
    this.number = value;
  }

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

  public Long getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Long value) {
    this.customerId = value;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String value) {
    this.category = value;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String value) {
    this.priority = value;
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

  public OffsetDateTime getDueAt() {
    return dueAt;
  }

  public void setDueAt(OffsetDateTime value) {
    this.dueAt = value;
  }

  public OffsetDateTime getResolvedAt() {
    return resolvedAt;
  }

  public void setResolvedAt(OffsetDateTime value) {
    this.resolvedAt = value;
  }
}
