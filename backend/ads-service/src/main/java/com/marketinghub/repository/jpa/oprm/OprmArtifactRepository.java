package com.marketinghub.repository.jpa.oprm;

import com.marketinghub.oprm.OprmArtifact;
import com.marketinghub.oprm.OprmArtifactStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de OprmArtifact.
 */
public interface OprmArtifactRepository extends JpaRepository<OprmArtifact, Long> {
    Optional<OprmArtifact> findByIdempotencyKey(String idempotencyKey);

    List<OprmArtifact> findAllByOrderByCreatedAtDesc();

    List<OprmArtifact> findByCorrelationIdOrderByCreatedAtDesc(String correlationId);

    List<OprmArtifact> findByOccupationSeedRefAndArtifactStatusOrderByCreatedAtDesc(String occupationSeedRef,
                                                                                     OprmArtifactStatus artifactStatus);

    List<OprmArtifact> findByOccupationSeedRefOrderByCreatedAtDesc(String occupationSeedRef);

    java.util.Optional<OprmArtifact> findFirstByOccupationSeedRefAndArtifactTypeOrderByCreatedAtDesc(String occupationSeedRef,
                                                                                                      String artifactType);
}
