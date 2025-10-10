package com.marketinghub.journey.repository;

import com.marketinghub.journey.model.Journey;
import com.marketinghub.journey.model.JourneyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Journey} instances.
 */
public interface JourneyRepository extends JpaRepository<Journey, Long> {
    Page<Journey> findByTemplateId(Long templateId, Pageable pageable);

    Page<Journey> findByStatus(JourneyStatus status, Pageable pageable);

    Page<Journey> findByTemplateIdAndStatus(Long templateId, JourneyStatus status, Pageable pageable);

    long countByStatus(JourneyStatus status);

    List<Journey> findByExperimentId(Long experimentId);

    Optional<Journey> findFirstByExperimentIdOrderByCreatedAtDesc(Long experimentId);
}
