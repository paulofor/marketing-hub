package com.marketinghub.experiment.repository;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.niche.MarketNiche;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

/**
 * Repository for experiments.
 */
public interface ExperimentRepository extends JpaRepository<Experiment, Long> {
    List<Experiment> findByNicheId(Long nicheId);
    boolean existsByNicheAndName(MarketNiche niche, String name);
    List<Experiment> findByStatus(ExperimentStatus status);
    List<Experiment> findByStatusAndPlatform(ExperimentStatus status, ExperimentPlatform platform);
    List<Experiment> findByStatusAndPlatformAndAudienceApprovedTrueAndCreativeApprovedTrue(
            ExperimentStatus status, ExperimentPlatform platform);
    long countBySalesFunnelId(UUID salesFunnelId);

    /**
     * Retrieves experiments configured to generate creatives.
     *
     * <p>Filters are handled in the query so we only fetch the records we
     * actually need.</p>
     */
    @Query("""
            select e from Experiment e
            join fetch e.hypothesisRef
            where e.creativesToGenerate is not null
              and e.creativesToGenerate > 0
            """)
    List<Experiment> findAllToGenerateCreatives();

    @Query("""
            select distinct e from Experiment e
            join fetch e.niche n
            join fetch e.hypothesisRef h
            where e.audienceApproved = true
              and e.platform = :platform
              and e.status in :statuses
            """)
    List<Experiment> findAllReadyForAdSets(@Param("platform") ExperimentPlatform platform,
                                           @Param("statuses") List<ExperimentStatus> statuses);
}
