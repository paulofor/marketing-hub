package com.marketinghub.geralanding;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GeraLandingStageExecutionRepository extends JpaRepository<GeraLandingStageExecution, GeraLandingStageExecutionId> {
    Optional<GeraLandingStageExecution> findTopByIdJobOrderByExecutionRequestedAtDesc(String idJob);

    List<GeraLandingStageExecution> findTop20ByStatusOrderByExecutionRequestedAtAsc(String status);
}
