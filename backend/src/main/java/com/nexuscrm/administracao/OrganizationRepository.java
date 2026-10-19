package com.nexuscrm.administracao;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
  Optional<Organization> findByCodeIgnoreCase(String code);

  boolean existsByCodeIgnoreCase(String code);
}
