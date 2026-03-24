package com.marketinghub.experiment.repository;

import com.marketinghub.experiment.CreativeVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for creative variants.
 */
public interface CreativeVariantRepository extends JpaRepository<CreativeVariant, Long> {
    List<CreativeVariant> findByExperimentId(Long experimentId);
}
