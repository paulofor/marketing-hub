package com.marketinghub.funnel;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Repository for {@link SalesFunnel}. */
public interface SalesFunnelRepository extends JpaRepository<SalesFunnel, UUID> {
    @EntityGraph(attributePaths = "steps")
    Optional<SalesFunnel> findWithStepsById(UUID id);
}
