package com.nexuscrm.administracao;

import com.nexuscrm.shared.TenantEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import org.hibernate.type.NumericBooleanConverter;

@Entity
@Table(name = "APP_USERS")
public class UserAccount extends TenantEntity {
  @Column(nullable = false, length = 120)
  String name;

  @Column(nullable = false, length = 254)
  String email;

  @Column(name = "PASSWORD_HASH", nullable = false, length = 255)
  String passwordHash;

  @Column(name = "ROLE_ID", nullable = false)
  Long roleId;

  @Column(name = "DEPARTMENT_ID")
  Long departmentId;

  @Column(name = "TEAM_ID")
  Long teamId;

  @Convert(converter = NumericBooleanConverter.class)
  @Column(nullable = false)
  boolean active = true;

  @Column(name = "FAILED_ATTEMPTS", nullable = false)
  int failedAttempts;

  @Column(name = "LOCKED_UNTIL")
  OffsetDateTime lockedUntil;

  @Column(name = "LAST_LOGIN_AT")
  OffsetDateTime lastLoginAt;

  @Column(name = "AUTH_VERSION", nullable = false)
  long authVersion;

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public Long getRoleId() {
    return roleId;
  }

  public Long getDepartmentId() {
    return departmentId;
  }

  public Long getTeamId() {
    return teamId;
  }

  public boolean isActive() {
    return active;
  }

  public int getFailedAttempts() {
    return failedAttempts;
  }

  public OffsetDateTime getLockedUntil() {
    return lockedUntil;
  }

  public long getAuthVersion() {
    return authVersion;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setRoleId(Long roleId) {
    this.roleId = roleId;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
    authVersion++;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public void loginFailed() {
    failedAttempts++;
    if (failedAttempts >= 5) {
      lockedUntil = OffsetDateTime.now().plusMinutes(15);
      failedAttempts = 0;
    }
  }

  public void loginSucceeded() {
    clearLoginFailures();
    lastLoginAt = OffsetDateTime.now();
  }

  public void clearLoginFailures() {
    failedAttempts = 0;
    lockedUntil = null;
  }
}
