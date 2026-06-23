package com.marketinghub.repository.jpa.opsmonitor;

import com.marketinghub.opsmonitor.OpsModuleHealthCheck;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Centraliza consultas JPA das verificações de saúde operacional. */
public interface OpsModuleHealthCheckRepository extends JpaRepository<OpsModuleHealthCheck, Long> {
    /** Lista as verificações mais recentes de um módulo. */
    List<OpsModuleHealthCheck> findTop30ByModuleCodeOrderByCheckedAtDesc(String moduleCode);

    /** Busca a última verificação registrada de um módulo. */
    java.util.Optional<OpsModuleHealthCheck> findTop1ByModuleCodeOrderByCheckedAtDesc(String moduleCode);
}
