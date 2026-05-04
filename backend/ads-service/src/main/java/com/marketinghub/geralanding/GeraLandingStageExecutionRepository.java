package com.marketinghub.geralanding;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GeraLandingStageExecutionRepository extends JpaRepository<GeraLandingStageExecution, GeraLandingStageExecutionId> {
    Optional<GeraLandingStageExecution> findTopByIdJobOrderByExecutionRequestedAtDesc(UUID idJob);
}
