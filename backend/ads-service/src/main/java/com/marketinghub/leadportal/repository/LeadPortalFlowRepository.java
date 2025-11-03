package com.marketinghub.leadportal.repository;

import com.marketinghub.leadportal.LeadPortalFlow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for accessing {@link LeadPortalFlow} entities.
 */
public interface LeadPortalFlowRepository extends JpaRepository<LeadPortalFlow, Long> {
    List<LeadPortalFlow> findAllByOrderByNameAsc();

    Optional<LeadPortalFlow> findBySlug(String slug);
}
