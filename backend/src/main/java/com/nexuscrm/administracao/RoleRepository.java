package com.nexuscrm.administracao;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<AppRole, Long> {
  List<AppRole> findByOrganizationIdOrderByNameAsc(Long organizationId);

  Optional<AppRole> findByIdAndOrganizationId(Long id, Long organizationId);
}
