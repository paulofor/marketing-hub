package com.marketinghub.experiment.repository;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.niche.MarketNiche;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Repository for experiments.
 */
public interface ExperimentRepository extends JpaRepository<Experiment, Long> {
    List<Experiment> findByNicheId(Long nicheId);
    boolean existsByNicheAndName(MarketNiche niche, String name);
}
