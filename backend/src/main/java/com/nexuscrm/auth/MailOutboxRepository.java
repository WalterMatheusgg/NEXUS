package com.nexuscrm.auth;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

public interface MailOutboxRepository extends JpaRepository<MailOutbox, Long> {
  @Query(
      "select m from MailOutbox m where m.sentAt is null and m.attempts<8 and (m.nextAttemptAt is"
          + " null or m.nextAttemptAt<=:now) order by m.id")
  List<MailOutbox> pending(OffsetDateTime now, Pageable page);

  long deleteByCreatedAtBefore(OffsetDateTime before);
}
