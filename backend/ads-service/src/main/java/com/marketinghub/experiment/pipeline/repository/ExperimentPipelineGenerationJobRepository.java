package com.marketinghub.experiment.pipeline.repository;

import com.marketinghub.experiment.pipeline.ExperimentPipelineGenerationJob;
import com.marketinghub.experiment.pipeline.ExperimentPipelineGenerationJobStatus;
import com.marketinghub.experiment.pipeline.ExperimentPipelineSection;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperimentPipelineGenerationJobRepository extends JpaRepository<ExperimentPipelineGenerationJob, UUID> {
    List<ExperimentPipelineGenerationJob> findByExperimentIdAndSectionAndStatusInOrderByCreatedAtDesc(
            Long experimentId,
            ExperimentPipelineSection section,
            Collection<ExperimentPipelineGenerationJobStatus> statuses);

    List<ExperimentPipelineGenerationJob> findByStatusOrderByCreatedAtAsc(ExperimentPipelineGenerationJobStatus status,
                                                                          Pageable pageable);

    List<ExperimentPipelineGenerationJob> findByExperimentIdOrderByCreatedAtDesc(Long experimentId, Pageable pageable);

    Page<ExperimentPipelineGenerationJob> findByExperimentId(Long experimentId, Pageable pageable);

    Page<ExperimentPipelineGenerationJob> findByExperimentIdAndSection(Long experimentId,
                                                                       ExperimentPipelineSection section,
                                                                       Pageable pageable);
}
