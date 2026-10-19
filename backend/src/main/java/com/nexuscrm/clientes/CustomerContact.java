package com.nexuscrm.clientes;

import com.nexuscrm.shared.TenantEntity;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "CUSTOMER_CONTACTS")
public class CustomerContact extends TenantEntity {
  @Column(name = "CUSTOMER_ID", nullable = false)
  private Long customerId;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(length = 254)
  private String email;

  @Column(length = 40)
  private String phone;

  @Column(name = "POSITION_NAME", length = 100)
  private String position;

  public Long getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Long value) {
    this.customerId = value;
  }

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

  public String getPosition() {
    return position;
  }

  public void setPosition(String value) {
    this.position = value;
  }
}
