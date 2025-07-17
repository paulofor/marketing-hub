package com.marketinghub.experiment.repository;

import com.marketinghub.experiment.Experiment;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for experiments.
 */
public interface ExperimentRepository extends JpaRepository<Experiment, Long> {}
