package com.marketinghub.journey.repository;

import com.marketinghub.journey.model.Journey;
import com.marketinghub.journey.model.JourneyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link Journey} instances.
 */
public interface JourneyRepository extends JpaRepository<Journey, Long> {
    Page<Journey> findByTemplateId(Long templateId, Pageable pageable);

    Page<Journey> findByStatus(JourneyStatus status, Pageable pageable);

    Page<Journey> findByTemplateIdAndStatus(Long templateId, JourneyStatus status, Pageable pageable);
}
