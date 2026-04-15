package com.marketinghub.oprm.repository;

import com.marketinghub.oprm.OprmArtifact;
import com.marketinghub.oprm.OprmArtifactStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OprmArtifactRepository extends JpaRepository<OprmArtifact, Long> {
    Optional<OprmArtifact> findByIdempotencyKey(String idempotencyKey);

    List<OprmArtifact> findByCorrelationIdOrderByCreatedAtDesc(String correlationId);

    List<OprmArtifact> findByOccupationSeedRefAndArtifactStatusOrderByCreatedAtDesc(String occupationSeedRef,
                                                                                     OprmArtifactStatus artifactStatus);

    List<OprmArtifact> findByOccupationSeedRefOrderByCreatedAtDesc(String occupationSeedRef);
}
