package com.marketinghub.experiment.repository;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.niche.MarketNiche;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

/**
 * Repository for experiments.
 */
public interface ExperimentRepository extends JpaRepository<Experiment, Long> {
    List<Experiment> findByNicheId(Long nicheId);
    boolean existsByNicheAndName(MarketNiche niche, String name);
    List<Experiment> findByStatus(com.marketinghub.experiment.ExperimentStatus status);
    long countBySalesFunnelId(UUID salesFunnelId);

    /**
     * Retrieves experiments configured to generate creatives.
     *
     * <p>Filters are handled in the query so we only fetch the records we
     * actually need.</p>
     */
    @Query("""
            select e from Experiment e
            where e.creativesToGenerate is not null
              and e.creativesToGenerate > 0
            """)
    List<Experiment> findAllToGenerateCreatives();
}
