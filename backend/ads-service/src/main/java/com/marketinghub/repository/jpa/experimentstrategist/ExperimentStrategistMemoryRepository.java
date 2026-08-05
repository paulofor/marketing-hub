package com.marketinghub.repository.jpa.experimentstrategist;

import com.marketinghub.experimentstrategist.memory.ExperimentStrategistMemory;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: consultar aprendizados estruturados do Estrategista. */
public interface ExperimentStrategistMemoryRepository
    extends JpaRepository<ExperimentStrategistMemory, Long> {
  /** Lista memorias ainda validas de um planejamento em ordem recente. */
  List<ExperimentStrategistMemory> findByCommercialPlanIdAndValidUntilAfterOrderByCreatedAtDesc(
      Long planId, Instant now);
}
