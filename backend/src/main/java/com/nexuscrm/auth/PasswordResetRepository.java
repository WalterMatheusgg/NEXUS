package com.nexuscrm.auth;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface PasswordResetRepository extends JpaRepository<PasswordResetToken, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<PasswordResetToken> findByTokenHash(String hash);

  @Modifying
  @Query(
      "update PasswordResetToken t set t.usedAt=:now where t.organizationId=:org and t.userId=:user"
          + " and t.usedAt is null")
  void invalidate(Long org, Long user, OffsetDateTime now);

  long deleteByExpiresAtBefore(OffsetDateTime before);
}
