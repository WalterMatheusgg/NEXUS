package com.nexuscrm.comercial;

import com.nexuscrm.shared.TenantEntity;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "SALES_LEADS")
public class Lead extends TenantEntity {
  @Column(nullable = false, length = 160)
  private String name;

  @Column(length = 254)
  private String email;

  @Column(length = 40)
  private String phone;

  @Column(length = 100)
  private String source;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(name = "ASSIGNEE_ID")
  private Long assigneeId;

  @Column(length = 4000)
  private String notes;

  @Column(name = "CUSTOMER_ID")
  private Long customerId;

  public String getName() {
    return name;
  }

  public void setName(String value) {
    this.name = value;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String value) {
    this.email = value;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String value) {
    this.phone = value;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String value) {
    this.source = value;
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

  public String getNotes() {
    return notes;
  }

  public void setNotes(String value) {
    this.notes = value;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Long value) {
    this.customerId = value;
  }
}
