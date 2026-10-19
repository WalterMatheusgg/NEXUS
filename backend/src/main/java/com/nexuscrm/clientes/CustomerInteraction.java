package com.nexuscrm.clientes;

import com.nexuscrm.shared.TenantEntity;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "CUSTOMER_INTERACTIONS")
public class CustomerInteraction extends TenantEntity {
  @Column(name = "CUSTOMER_ID", nullable = false)
  private Long customerId;

  @Column(name = "AUTHOR_ID", nullable = false)
  private Long authorId;

  @Column(nullable = false, length = 4000)
  private String note;

  public Long getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Long value) {
    this.customerId = value;
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
