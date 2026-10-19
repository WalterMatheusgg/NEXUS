package com.nexuscrm.administracao;

import com.nexuscrm.shared.TenantEntity;
import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "APP_ROLES")
public class AppRole extends TenantEntity {
  @Column(nullable = false, length = 50)
  String code;

  @Column(nullable = false, length = 100)
  String name;

  @Column(name = "PERMISSIONS", nullable = false, length = 2000)
  String permissionCodes = "";

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public Set<String> permissions() {
    return permissionCodes == null || permissionCodes.isBlank() || "-".equals(permissionCodes)
        ? Set.of()
        : Set.copyOf(Arrays.asList(permissionCodes.split(",")));
  }

  public void configure(String code, String name, Set<String> permissions) {
    this.code = code;
    this.name = name;
    this.permissionCodes =
        permissions.isEmpty() ? "-" : String.join(",", new TreeSet<>(permissions));
  }
}
