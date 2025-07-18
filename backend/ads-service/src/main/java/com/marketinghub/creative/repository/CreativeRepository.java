package com.marketinghub.creative.repository;

import com.marketinghub.creative.Creative;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for creatives.
 */
public interface CreativeRepository extends JpaRepository<Creative, Long> {
    List<Creative> findByExperimentId(Long experimentId);
}
