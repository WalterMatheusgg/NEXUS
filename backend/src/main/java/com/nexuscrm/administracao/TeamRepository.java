package com.nexuscrm.administracao;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
  List<Team> findByOrganizationIdOrderByNameAsc(Long organizationId);

  Optional<Team> findByIdAndOrganizationId(Long id, Long organizationId);
}
