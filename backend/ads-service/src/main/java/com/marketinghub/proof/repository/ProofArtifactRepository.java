package com.marketinghub.proof.repository;

import com.marketinghub.proof.ProofArtifact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository exposing proof artifacts.
 */
public interface ProofArtifactRepository extends JpaRepository<ProofArtifact, Long> {
    List<ProofArtifact> findByHypothesisIdOrderByCreatedAtDesc(UUID hypothesisId);
    List<ProofArtifact> findByExperimentIdOrderByCreatedAtDesc(Long experimentId);
}
