package com.marketinghub.experiment.repository;

import com.marketinghub.experiment.MetricPreset;
import org.springframework.data.repository.CrudRepository;

/**
 * Repository for {@link MetricPreset} entities.
 */
public interface MetricPresetRepository extends CrudRepository<MetricPreset, String> {
}
