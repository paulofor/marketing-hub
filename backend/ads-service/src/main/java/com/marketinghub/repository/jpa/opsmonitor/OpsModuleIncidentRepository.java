package com.marketinghub.repository.jpa.opsmonitor;

import com.marketinghub.opsmonitor.OpsModuleIncident;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Centraliza consultas JPA dos incidentes operacionais. */
public interface OpsModuleIncidentRepository extends JpaRepository<OpsModuleIncident, Long> {
    /** Lista incidentes por status operacional. */
    List<OpsModuleIncident> findByStatusOrderByStartedAtDesc(String status);

    /** Lista os incidentes mais recentes. */
    List<OpsModuleIncident> findTop100ByOrderByStartedAtDesc();
}
