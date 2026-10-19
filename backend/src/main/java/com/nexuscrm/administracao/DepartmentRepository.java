package com.nexuscrm.administracao;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
  List<Department> findByOrganizationIdOrderByNameAsc(Long organizationId);

  Optional<Department> findByIdAndOrganizationId(Long id, Long organizationId);
}
