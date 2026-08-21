package com.marketinghub.repository.jpa.experimentstrategist;

import com.marketinghub.experimentstrategist.ExperimentStrategistBehavioralSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir snapshots comportamentais agregados do Estrategista. */
public interface ExperimentStrategistBehavioralSnapshotRepository
    extends JpaRepository<ExperimentStrategistBehavioralSnapshot, Long> {
  /** Conta consultas reservadas para uma execução. */
  long countByExecutionId(Long executionId);

  /** Conta o uso diário do provedor para proteger sua cota externa. */
  long countByProviderAndRequestedAtGreaterThanEqual(String provider, Instant dayStart);

  /** Busca um snapshot segregado por sua execução. */
  Optional<ExperimentStrategistBehavioralSnapshot> findByIdAndExecutionId(
      Long id, Long executionId);

  /** Lista os snapshots da execução na ordem em que foram solicitados. */
  List<ExperimentStrategistBehavioralSnapshot> findByExecutionIdOrderByRequestedAtAsc(
      Long executionId);
}
