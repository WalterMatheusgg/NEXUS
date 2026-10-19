package com.nexuscrm.comercial;

import com.nexuscrm.shared.TenantEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "OPPORTUNITIES")
public class Opportunity extends TenantEntity {
  @Column(nullable = false, length = 200)
  private String title;

  @Column(name = "CUSTOMER_ID")
  private Long customerId;

  @Column(name = "LEAD_ID")
  private Long leadId;

  @Column(name = "ASSIGNEE_ID")
  private Long assigneeId;

  @Column(nullable = false, length = 20)
  private String stage;

  @Column(nullable = false, precision = 17, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false)
  private Integer probability;

  @Column(name = "EXPECTED_CLOSE_DATE")
  private LocalDate expectedCloseDate;

  @Column(length = 4000)
  private String description;

  public String getTitle() {
    return title;
  }

  public void setTitle(String value) {
    this.title = value;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Long value) {
    this.customerId = value;
  }

  public Long getLeadId() {
    return leadId;
  }

  public void setLeadId(Long value) {
    this.leadId = value;
  }

  public Long getAssigneeId() {
    return assigneeId;
  }

  public void setAssigneeId(Long value) {
    this.assigneeId = value;
  }

  public String getStage() {
    return stage;
  }

  public void setStage(String value) {
    this.stage = value;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal value) {
    this.amount = value;
  }

  public Integer getProbability() {
    return probability;
  }

  public void setProbability(Integer value) {
    this.probability = value;
  }

  public LocalDate getExpectedCloseDate() {
    return expectedCloseDate;
  }

  public void setExpectedCloseDate(LocalDate value) {
    this.expectedCloseDate = value;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String value) {
    this.description = value;
  }
}
