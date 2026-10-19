package com.nexuscrm.comercial;

import com.nexuscrm.clientes.TenantRepository;
import java.util.*;

public interface LeadRepository extends TenantRepository<Lead> {
  long countByOrganizationId(Long orgId);

  long countByOrganizationIdAndStatus(Long orgId, String status);
}
