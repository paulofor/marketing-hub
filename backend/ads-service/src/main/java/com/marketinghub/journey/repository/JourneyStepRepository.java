package com.marketinghub.journey.repository;

import com.marketinghub.journey.model.JourneyStep;
import com.marketinghub.journey.model.JourneyTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for journey steps.
 */
public interface JourneyStepRepository extends JpaRepository<JourneyStep, Long> {
    List<JourneyStep> findByTemplateOrderByPositionAsc(JourneyTemplate template);
}
