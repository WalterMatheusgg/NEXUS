package com.nexuscrm.processos;

import com.nexuscrm.clientes.TenantRepository;
import java.util.*;

public interface ProcessStepRepository extends TenantRepository<ProcessStep> {

  List<ProcessStep> findByOrganizationIdAndProcessIdOrderByPosition(Long orgId, Long processId);

  Optional<ProcessStep> findByIdAndOrganizationIdAndProcessId(Long id, Long orgId, Long processId);
}
