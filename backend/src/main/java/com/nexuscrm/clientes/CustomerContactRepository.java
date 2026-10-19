package com.nexuscrm.clientes;

import java.util.*;

public interface CustomerContactRepository extends TenantRepository<CustomerContact> {

  List<CustomerContact> findByOrganizationIdAndCustomerIdOrderByName(Long orgId, Long customerId);

  Optional<CustomerContact> findByIdAndOrganizationIdAndCustomerId(
      Long id, Long orgId, Long customerId);
}
