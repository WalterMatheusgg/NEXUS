package com.nexuscrm.clientes;

import java.util.*;

public interface CustomerInteractionRepository extends TenantRepository<CustomerInteraction> {
  List<CustomerInteraction> findByOrganizationIdAndCustomerIdOrderByCreatedAtDesc(
      Long orgId, Long customerId);
}
