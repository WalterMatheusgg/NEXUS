package com.nexuscrm.administracao;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface AuditRepository extends JpaRepository<AuditEvent, Long> {
  Page<AuditEvent> findByOrganizationId(Long organizationId, Pageable pageable);

  List<AuditEvent> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId, Pageable pageable);

  long deleteByCreatedAtBefore(OffsetDateTime before);
}
