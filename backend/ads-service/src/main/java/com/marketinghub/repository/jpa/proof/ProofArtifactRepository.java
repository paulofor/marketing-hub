package com.marketinghub.repository.jpa.proof;

import com.marketinghub.proof.ProofArtifact;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository exposing proof artifacts. */
public interface ProofArtifactRepository extends JpaRepository<ProofArtifact, Long> {
  List<ProofArtifact> findByHypothesisIdOrderByCreatedAtDesc(UUID hypothesisId);

  List<ProofArtifact> findByExperimentIdOrderByCreatedAtDesc(Long experimentId);
}
