package com.nexuscrm.clientes;

import com.nexuscrm.shared.TenantEntity;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface TenantRepository<T extends TenantEntity>
    extends JpaRepository<T, Long>, JpaSpecificationExecutor<T> {
  Optional<T> findByIdAndOrganizationId(Long id, Long organizationId);

  List<T> findByOrganizationId(Long organizationId);
}
