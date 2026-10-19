package com.nexuscrm.auth;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface InstallationRepository extends JpaRepository<InstallationState, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from InstallationState i where i.id=1")
  Optional<InstallationState> lock();
}
