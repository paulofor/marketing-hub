package com.marketinghub.repository.jpa.experiment;

import com.marketinghub.experiment.AdSet;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for ad sets.
 */
public interface AdSetRepository extends JpaRepository<AdSet, Long> {
    long countByExperimentId(Long experimentId);
}
