package com.marketinghub.journey.repository;

import com.marketinghub.journey.model.JourneyTemplate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for {@link JourneyTemplate} aggregates.
 */
public interface JourneyTemplateRepository extends JpaRepository<JourneyTemplate, Long> {
    @EntityGraph(attributePaths = {"steps"})
    Optional<JourneyTemplate> findWithStepsById(Long id);
}
