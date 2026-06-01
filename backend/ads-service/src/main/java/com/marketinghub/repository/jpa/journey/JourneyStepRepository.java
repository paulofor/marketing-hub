package com.marketinghub.repository.jpa.journey;

import com.marketinghub.journey.model.JourneyStep;
import com.marketinghub.journey.model.JourneyTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for journey steps.
 */
public interface JourneyStepRepository extends JpaRepository<JourneyStep, Long> {
    List<JourneyStep> findByTemplateOrderByPositionAsc(JourneyTemplate template);

    Optional<JourneyStep> findFirstByTemplateAndPositionGreaterThanOrderByPositionAsc(JourneyTemplate template,
                                                                                    Integer position);
}
