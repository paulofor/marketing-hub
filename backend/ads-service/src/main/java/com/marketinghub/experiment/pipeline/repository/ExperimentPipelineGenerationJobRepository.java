package com.marketinghub.experiment.pipeline.repository;

import com.marketinghub.experiment.pipeline.ExperimentPipelineGenerationJob;
import com.marketinghub.experiment.pipeline.ExperimentPipelineGenerationJobStatus;
import com.marketinghub.experiment.pipeline.ExperimentPipelineSection;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            select j
            from ExperimentPipelineGenerationJob j
            where j.experiment.id = :experimentId
              and j.status = com.marketinghub.experiment.pipeline.ExperimentPipelineGenerationJobStatus.COMPLETED
              and (:section is null or j.section = :section)
              and j.createdAt = (
                  select max(j2.createdAt)
                  from ExperimentPipelineGenerationJob j2
                  where j2.experiment.id = j.experiment.id
                    and j2.section = j.section
                    and j2.status = com.marketinghub.experiment.pipeline.ExperimentPipelineGenerationJobStatus.COMPLETED
              )
            """)
    List<ExperimentPipelineGenerationJob> findLatestCompletedPerSectionByExperimentId(
            @Param("experimentId") Long experimentId,
            @Param("section") ExperimentPipelineSection section);

    @Query("select coalesce(sum(j.costUsd), 0) from ExperimentPipelineGenerationJob j where j.experiment.id = :experimentId")
    BigDecimal sumCostUsdByExperimentId(Long experimentId);
}
