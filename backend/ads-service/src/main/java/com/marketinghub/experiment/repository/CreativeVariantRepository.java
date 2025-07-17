package com.marketinghub.experiment.repository;

import com.marketinghub.experiment.CreativeVariant;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for creative variants.
 */
public interface CreativeVariantRepository extends JpaRepository<CreativeVariant, Long> {}
