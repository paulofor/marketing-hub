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
    @EntityGraph(attributePaths = {"questions", "experiment", "marketNiche", "simpleFormStyle"})
    List<LeadPortalFlow> findAll();

    @EntityGraph(attributePaths = {"questions", "experiment", "marketNiche", "simpleFormStyle"})
    List<LeadPortalFlow> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"questions", "experiment", "marketNiche", "simpleFormStyle"})
    List<LeadPortalFlow> findAllByExperimentIdOrderByCreatedAtDesc(Long experimentId);

    @EntityGraph(attributePaths = {"questions", "experiment", "marketNiche", "simpleFormStyle"})
    List<LeadPortalFlow> findAllByMarketNicheIdOrderByCreatedAtDesc(Long marketNicheId);

    Optional<LeadPortalFlow> findBySlug(String slug);
}
