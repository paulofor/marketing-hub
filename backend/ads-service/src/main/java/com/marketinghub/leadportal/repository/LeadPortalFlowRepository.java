package com.marketinghub.leadportal.repository;

import com.marketinghub.leadportal.LeadPortalFlow;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for accessing {@link LeadPortalFlow} entities.
 */
public interface LeadPortalFlowRepository extends JpaRepository<LeadPortalFlow, Long> {
    @Override
    @EntityGraph(attributePaths = "questions")
    List<LeadPortalFlow> findAll();

    @EntityGraph(attributePaths = "questions")
    List<LeadPortalFlow> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = "questions")
    List<LeadPortalFlow> findAllByExperimentIdOrderByCreatedAtDesc(Long experimentId);

    Optional<LeadPortalFlow> findBySlug(String slug);
}
