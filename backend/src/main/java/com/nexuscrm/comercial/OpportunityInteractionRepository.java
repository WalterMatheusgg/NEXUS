package com.nexuscrm.comercial;

import com.nexuscrm.clientes.TenantRepository;
import java.util.*;

public interface OpportunityInteractionRepository extends TenantRepository<OpportunityInteraction> {
  List<OpportunityInteraction> findByOrganizationIdAndOpportunityIdOrderByCreatedAtDesc(
      Long orgId, Long opportunityId);
}
