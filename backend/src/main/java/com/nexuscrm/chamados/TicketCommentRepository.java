package com.nexuscrm.chamados;

import com.nexuscrm.clientes.TenantRepository;
import java.util.*;

public interface TicketCommentRepository extends TenantRepository<TicketComment> {
  List<TicketComment> findByOrganizationIdAndTicketIdOrderByCreatedAtAsc(Long orgId, Long ticketId);
}
