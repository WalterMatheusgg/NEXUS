package com.nexuscrm.administracao;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository
    extends JpaRepository<UserAccount, Long>, JpaSpecificationExecutor<UserAccount> {
  Optional<UserAccount> findByIdAndOrganizationId(Long id, Long organizationId);

  Optional<UserAccount> findByOrganizationIdAndEmailIgnoreCase(Long organizationId, String email);

  List<UserAccount> findByOrganizationIdAndActiveTrue(Long organizationId);

  long countByOrganizationIdAndRoleIdAndActiveTrue(Long organizationId, Long roleId);

  long countByOrganizationIdAndActiveTrue(Long organizationId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from UserAccount u where u.id=:id and u.organizationId=:org")
  Optional<UserAccount> lock(@Param("id") Long id, @Param("org") Long org);
}
