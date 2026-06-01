package com.marketinghub.repository.jpa.journey;

import com.marketinghub.journey.model.JourneyTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

/**
 * Repository for {@link JourneyTemplate} aggregates.
 */
public interface JourneyTemplateRepository extends JpaRepository<JourneyTemplate, Long> {
    @Override
    Page<JourneyTemplate> findAll(Pageable pageable);

    @Query(
            value = "select jt.id from JourneyTemplate jt",
            countQuery = "select count(jt) from JourneyTemplate jt"
    )
    Page<Long> findPageIds(Pageable pageable);

    @EntityGraph(attributePaths = {"steps", "steps.metadata"})
    @Query("select distinct jt from JourneyTemplate jt where jt.id in :ids")
    List<JourneyTemplate> findAllWithStepsByIdIn(@Param("ids") List<Long> ids);

    @EntityGraph(attributePaths = {"steps", "steps.metadata"})
    Optional<JourneyTemplate> findWithStepsById(Long id);
}
