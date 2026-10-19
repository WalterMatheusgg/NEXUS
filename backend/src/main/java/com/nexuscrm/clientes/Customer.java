package com.nexuscrm.clientes;

import com.nexuscrm.shared.TenantEntity;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "CUSTOMERS")
public class Customer extends TenantEntity {
  @Column(name = "CUSTOMER_TYPE", nullable = false, length = 12)
  private String type;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(nullable = false, length = 14)
  private String document;

  @Column(length = 254)
  private String email;

  @Column(length = 40)
  private String phone;

  @Column(length = 1000)
  private String address;

  @Column(nullable = false)
  @Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
  private Boolean active;

  public String getType() {
    return type;
  }

  public void setType(String value) {
    this.type = value;
  }

  public String getName() {
    return name;
  }

  public void setName(String value) {
    this.name = value;
  }

  public String getDocument() {
    return document;
  }

  public void setDocument(String value) {
    this.document = value;
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

  public String getAddress() {
    return address;
  }

  public void setAddress(String value) {
    this.address = value;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean value) {
    this.active = value;
  }
}
