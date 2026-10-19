package com.nexuscrm.chamados;

import com.nexuscrm.clientes.TenantRepository;
import java.util.*;

public interface TicketAttachmentRepository extends TenantRepository<TicketAttachment> {

  List<TicketAttachment> findByOrganizationIdAndTicketIdOrderByCreatedAtDesc(
      Long orgId, Long ticketId);

  Optional<TicketAttachment> findByIdAndOrganizationIdAndTicketId(
      Long id, Long orgId, Long ticketId);
}
