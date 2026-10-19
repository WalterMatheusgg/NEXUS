package com.nexuscrm.clientes;

import java.util.*;

public interface CustomerRepository extends TenantRepository<Customer> {

  boolean existsByOrganizationIdAndDocument(Long orgId, String document);

  boolean existsByOrganizationIdAndDocumentAndIdNot(Long orgId, String document, Long id);

  long countByOrganizationIdAndActiveTrue(Long orgId);

  long countByOrganizationIdAndCreatedAtGreaterThanEqual(Long orgId, java.time.OffsetDateTime from);
}
