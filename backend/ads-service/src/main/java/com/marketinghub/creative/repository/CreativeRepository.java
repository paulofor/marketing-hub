package com.marketinghub.creative.repository;

import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for creatives.
 */
public interface CreativeRepository extends JpaRepository<Creative, Long> {
    List<Creative> findByExperimentId(Long experimentId);

    boolean existsByExperimentIdAndStatus(Long experimentId, CreativeStatus status);
}
