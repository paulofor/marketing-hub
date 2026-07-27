package com.marketinghub.repository.jpa.funnel;

import com.marketinghub.funnel.SalesFunnel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link SalesFunnel}. */
public interface SalesFunnelRepository extends JpaRepository<SalesFunnel, UUID> {
  @EntityGraph(attributePaths = "steps")
  Optional<SalesFunnel> findWithStepsById(UUID id);

  Optional<SalesFunnel> findByNameIgnoreCase(String name);

  Optional<SalesFunnel> findFirstByNameIgnoreCaseOrderByCreatedAtDesc(String name);
}
