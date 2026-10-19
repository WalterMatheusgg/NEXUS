package com.nexuscrm.comercial;

import com.nexuscrm.shared.TenantEntity;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "OPPORTUNITY_INTERACTIONS")
public class OpportunityInteraction extends TenantEntity {
  @Column(name = "OPPORTUNITY_ID", nullable = false)
  private Long opportunityId;

  @Column(name = "AUTHOR_ID", nullable = false)
  private Long authorId;

  @Column(nullable = false, length = 4000)
  private String note;

  public Long getOpportunityId() {
    return opportunityId;
  }

  public void setOpportunityId(Long value) {
    this.opportunityId = value;
  }

  public Long getAuthorId() {
    return authorId;
  }

  public void setAuthorId(Long value) {
    this.authorId = value;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String value) {
    this.note = value;
  }
}
