package com.marketinghub.repository.jpa.opsmonitor;

import com.marketinghub.opsmonitor.OpsModuleAvailabilityDaily;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Centraliza consultas JPA dos resumos diários de disponibilidade. */
public interface OpsModuleAvailabilityDailyRepository extends JpaRepository<OpsModuleAvailabilityDaily, Long> {
    /** Lista histórico diário recente de um módulo. */
    List<OpsModuleAvailabilityDaily> findTop30ByModuleCodeOrderByAvailabilityDateDesc(String moduleCode);
}
