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
    @EntityGraph(attributePaths = {"questions", "questions.options", "experiment", "marketNiche", "simpleFormStyle"})
    List<LeadPortalFlow> findAll();

    @EntityGraph(attributePaths = {"questions", "questions.options", "experiment", "marketNiche", "simpleFormStyle"})
    List<LeadPortalFlow> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"questions", "questions.options", "experiment", "marketNiche", "simpleFormStyle"})
    List<LeadPortalFlow> findAllByExperimentIdOrderByCreatedAtDesc(Long experimentId);

    @EntityGraph(attributePaths = {"questions", "questions.options", "experiment", "marketNiche", "simpleFormStyle"})
    List<LeadPortalFlow> findAllByMarketNicheIdOrderByCreatedAtDesc(Long marketNicheId);

    @EntityGraph(attributePaths = {"questions", "questions.options", "experiment", "marketNiche", "simpleFormStyle"})
    List<LeadPortalFlow> findAllByApprovedTrue();

    @EntityGraph(attributePaths = {"questions", "questions.options", "experiment", "marketNiche", "simpleFormStyle"})
    Optional<LeadPortalFlow> findBySlug(String slug);
}
