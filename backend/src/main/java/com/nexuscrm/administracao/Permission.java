package com.nexuscrm.administracao;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum Permission {
  DASHBOARD_READ,
  CUSTOMERS_READ,
  CUSTOMERS_WRITE,
  SALES_READ,
  SALES_WRITE,
  TICKETS_READ,
  TICKETS_WRITE,
  TASKS_READ,
  TASKS_WRITE,
  PROCESSES_READ,
  PROCESSES_WRITE,
  REPORTS_READ,
  USERS_MANAGE,
  ROLES_MANAGE,
  SETTINGS_MANAGE,
  AUDIT_READ,
  NOTIFICATIONS_READ;

  public static Set<String> all() {
    return Arrays.stream(values()).map(Enum::name).collect(Collectors.toUnmodifiableSet());
  }
}
