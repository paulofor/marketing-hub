package com.marketinghub.geralanding;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GeraLandingStageExecutionRepository extends JpaRepository<GeraLandingStageExecution, byte[]> {
    Optional<GeraLandingStageExecution> findTopByIdJobOrderByExecutionRequestedAtDesc(byte[] idJob);

    Optional<GeraLandingStageExecution> findTopByExperimentIdAndIdJobOrderByExecutionRequestedAtDesc(Long experimentId,
                                                                                                       byte[] idJob);

    Optional<GeraLandingStageExecution> findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(Long experimentId,
                                                                                                           String stageCode);

    List<GeraLandingStageExecution> findTop20ByStatusOrderByExecutionRequestedAtAsc(String status);

    List<GeraLandingStageExecution>
    findTop20ByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(Long experimentId, String stageCode);

    List<GeraLandingStageExecution>
    findTop20ByExperimentIdAndStageCodeAndStatusNotOrderByExecutionRequestedAtDesc(
            Long experimentId,
            String stageCode,
            String status);
}
