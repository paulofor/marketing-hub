package com.marketinghub.journey.repository;

import com.marketinghub.journey.model.JourneyTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Repository for {@link JourneyTemplate} aggregates.
 */
public interface JourneyTemplateRepository extends JpaRepository<JourneyTemplate, Long> {
    @Override
    @EntityGraph(attributePaths = {"steps", "steps.metadata"})
    @Query(
            value = "select distinct jt from JourneyTemplate jt",
            countQuery = "select count(jt) from JourneyTemplate jt"
    )
    Page<JourneyTemplate> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"steps", "steps.metadata"})
    Optional<JourneyTemplate> findWithStepsById(Long id);
}
