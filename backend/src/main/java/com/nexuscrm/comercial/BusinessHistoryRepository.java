package com.nexuscrm.comercial;

import com.nexuscrm.clientes.TenantRepository;
import java.util.*;

public interface BusinessHistoryRepository extends TenantRepository<BusinessHistory> {
  List<BusinessHistory> findByOrganizationIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
      Long orgId, String entityType, Long entityId);
}
