package com.marketinghub.funnel;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository for {@link FunnelStep}. */
public interface FunnelStepRepository extends JpaRepository<FunnelStep, UUID> {
    @EntityGraph(attributePaths = "funnel")
    Optional<FunnelStep> findWithFunnelById(UUID id);

    List<FunnelStep> findByFunnelId(UUID funnelId);
}
