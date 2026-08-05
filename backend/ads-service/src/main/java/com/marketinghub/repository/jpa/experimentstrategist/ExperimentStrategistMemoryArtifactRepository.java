package com.marketinghub.repository.jpa.experimentstrategist;

import com.marketinghub.experimentstrategist.memory.ExperimentStrategistMemoryArtifact;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: consultar metadados dos artefatos S3 da memoria do Estrategista. */
public interface ExperimentStrategistMemoryArtifactRepository
    extends JpaRepository<ExperimentStrategistMemoryArtifact, Long> {
  /** Evita armazenar duas vezes o mesmo conteudo para uma memoria. */
  Optional<ExperimentStrategistMemoryArtifact> findByMemoryIdAndSha256(
      Long memoryId, String sha256);

  /** Lista artefatos vinculados a uma memoria. */
  List<ExperimentStrategistMemoryArtifact> findByMemoryIdOrderByCreatedAtDesc(Long memoryId);
}
