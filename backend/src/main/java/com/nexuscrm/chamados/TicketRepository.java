package com.nexuscrm.chamados;

import com.nexuscrm.clientes.TenantRepository;
import java.util.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends TenantRepository<Ticket> {

  long countByOrganizationIdAndStatusIn(Long orgId, Collection<String> statuses);

  long countByOrganizationIdAndStatusInAndDueAtBefore(
      Long orgId, Collection<String> statuses, java.time.OffsetDateTime now);

  long countByOrganizationIdAndAssigneeIdAndStatusIn(
      Long orgId, Long assigneeId, Collection<String> statuses);

  @Query("select t.status,count(t) from Ticket t where t.organizationId=:orgId group by t.status")
  List<Object[]> groupStatuses(@Param("orgId") Long orgId);
}
