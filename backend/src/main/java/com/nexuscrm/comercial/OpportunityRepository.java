package com.nexuscrm.comercial;

import com.nexuscrm.clientes.TenantRepository;
import java.util.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpportunityRepository extends TenantRepository<Opportunity> {

  long countByOrganizationIdAndStageNotIn(Long orgId, Collection<String> stages);

  long countByOrganizationIdAndStage(Long orgId, String stage);

  long countByOrganizationIdAndAssigneeIdAndStage(Long orgId, Long assigneeId, String stage);

  @Query(
      "select coalesce(sum(o.amount),0) from Opportunity o where o.organizationId=:orgId and"
          + " o.stage not in ('WON','LOST')")
  java.math.BigDecimal pipelineValue(@Param("orgId") Long orgId);

  @Query(
      "select o.stage,count(o),coalesce(sum(o.amount),0) from Opportunity o where"
          + " o.organizationId=:orgId group by o.stage")
  List<Object[]> groupStages(@Param("orgId") Long orgId);
}
