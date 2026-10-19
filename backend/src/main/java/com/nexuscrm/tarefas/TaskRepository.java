package com.nexuscrm.tarefas;

import com.nexuscrm.clientes.TenantRepository;
import java.util.*;

public interface TaskRepository extends TenantRepository<Task> {

  long countByOrganizationIdAndStatusNot(Long orgId, String status);

  long countByOrganizationIdAndStatusNotAndDueDateBefore(
      Long orgId, String status, java.time.LocalDate date);

  long countByOrganizationIdAndAssigneeIdAndStatus(Long orgId, Long assigneeId, String status);
}
