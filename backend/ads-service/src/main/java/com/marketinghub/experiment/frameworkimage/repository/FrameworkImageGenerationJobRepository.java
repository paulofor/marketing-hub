package com.marketinghub.experiment.frameworkimage.repository;

import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJob;
import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJobStage;
import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJobStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FrameworkImageGenerationJobRepository extends JpaRepository<FrameworkImageGenerationJob, UUID> {
    List<FrameworkImageGenerationJob> findByStatusOrderByCreatedAtAsc(FrameworkImageGenerationJobStatus status,
                                                                      Pageable pageable);

    Optional<FrameworkImageGenerationJob> findFirstByExperimentIdAndPlanningItemKeyAndStatusInOrderByCreatedAtDesc(
            Long experimentId,
            String planningItemKey,
            Collection<FrameworkImageGenerationJobStatus> statuses);

    List<FrameworkImageGenerationJob> findByExperimentIdOrderByCreatedAtDesc(Long experimentId);

    List<FrameworkImageGenerationJob> findByStatusAndStageInAndAssetIdIsNotNullAndSourceUrlIsNotNullAndWebUrlIsNullOrderByUpdatedAtAsc(
            FrameworkImageGenerationJobStatus status,
            Collection<FrameworkImageGenerationJobStage> stages,
            Pageable pageable);

    Optional<FrameworkImageGenerationJob> findFirstByAssetIdOrderByCreatedAtDesc(Long assetId);

    List<FrameworkImageGenerationJob> findByStatusAndStartedAtBeforeOrderByStartedAtAsc(
            FrameworkImageGenerationJobStatus status,
            java.time.Instant startedAt,
            Pageable pageable);
}
