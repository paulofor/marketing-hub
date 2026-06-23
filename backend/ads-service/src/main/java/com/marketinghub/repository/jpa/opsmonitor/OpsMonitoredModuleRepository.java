package com.marketinghub.repository.jpa.opsmonitor;

import com.marketinghub.opsmonitor.OpsMonitoredModule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Centraliza consultas JPA dos módulos monitorados pelo Ops Monitor. */
public interface OpsMonitoredModuleRepository extends JpaRepository<OpsMonitoredModule, Long> {
    /** Busca um módulo monitorado pelo código operacional. */
    Optional<OpsMonitoredModule> findByCode(String code);

    /** Lista módulos habilitados para consumo pelo worker. */
    List<OpsMonitoredModule> findByEnabledTrueOrderByCodeAsc();
}
