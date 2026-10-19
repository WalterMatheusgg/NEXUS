package com.nexuscrm.tarefas;

import com.nexuscrm.shared.TenantEntity;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "WORK_TASKS")
public class Task extends TenantEntity {
  @Column(nullable = false, length = 200)
  private String title;

  @Column(length = 4000)
  private String description;

  @Column(nullable = false, length = 12)
  private String priority;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(name = "ASSIGNEE_ID")
  private Long assigneeId;

  @Column(name = "DUE_DATE")
  private LocalDate dueDate;

  @Column(name = "CUSTOMER_ID")
  private Long customerId;

  @Column(name = "TICKET_ID")
  private Long ticketId;

  @Column(name = "OPPORTUNITY_ID")
  private Long opportunityId;

  @Column(name = "COMPLETED_AT")
  private OffsetDateTime completedAt;

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

  public LocalDate getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDate value) {
    this.dueDate = value;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Long value) {
    this.customerId = value;
  }

  public Long getTicketId() {
    return ticketId;
  }

  public void setTicketId(Long value) {
    this.ticketId = value;
  }

  public Long getOpportunityId() {
    return opportunityId;
  }

  public void setOpportunityId(Long value) {
    this.opportunityId = value;
  }

  public OffsetDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(OffsetDateTime value) {
    this.completedAt = value;
  }
}
