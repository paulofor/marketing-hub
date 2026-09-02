package com.marketinghub.repository.jpa.productdiscovery;

import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositório dos ciclos de descoberta de produtos PDE. */
public interface ProductDiscoveryCycleRepository
    extends JpaRepository<ProductDiscoveryCycle, Long> {

  /** Bloqueia o ciclo durante comandos humanos idempotentes que alteram sua execução. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT cycle FROM ProductDiscoveryCycle cycle WHERE cycle.id = :cycleId")
  Optional<ProductDiscoveryCycle> findByIdForUpdate(@Param("cycleId") Long cycleId);

  /** Lista os ciclos mais recentes para a tela administrativa. */
  List<ProductDiscoveryCycle> findTop50ByOrderByUpdatedAtDesc();

  /** Lista ciclos pendentes para consumo pelo worker. */
  List<ProductDiscoveryCycle> findTop5ByStatusInOrderByUpdatedAtAsc(
      Collection<ProductDiscoveryCycleStatus> statuses);

  /** Consolida em lote apenas estado, candidatas prontas e produtos para a lista independente. */
  @Query(
      value =
          """
          SELECT cycle.id AS cycleId,
                 cycle.status AS cycleStatus,
                 (SELECT COUNT(*)
                    FROM product_discovery_opportunity opportunity
                   WHERE opportunity.cycle_id = cycle.id
                     AND opportunity.maturity_status = 'DOSSIER_READY') AS readyOpportunityCount,
                 (SELECT COUNT(*)
                    FROM opportunity_dossier dossier
                   WHERE dossier.product_discovery_cycle_id = cycle.id
                     AND dossier.created_product_id IS NOT NULL) AS productCount
            FROM product_discovery_cycle cycle
           WHERE cycle.id IN (:cycleIds)
          """,
      nativeQuery = true)
  List<ProductDiscoveryIndependentStatusProjection> findIndependentStatusSnapshotsByIds(
      @Param("cycleIds") Collection<Long> cycleIds);

  /** Reserva ciclos novos ou recupera execuções cujo lease expirou sem callback terminal. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT cycle FROM ProductDiscoveryCycle cycle
      WHERE cycle.status = :ready
         OR (cycle.status = :researching
             AND ((cycle.leaseExpiresAt IS NOT NULL AND cycle.leaseExpiresAt <= :now)
                  OR (cycle.leaseExpiresAt IS NULL AND cycle.updatedAt <= :legacyCutoff)))
      ORDER BY cycle.updatedAt ASC
      """)
  List<ProductDiscoveryCycle> findClaimableForUpdate(
      @Param("ready") ProductDiscoveryCycleStatus ready,
      @Param("researching") ProductDiscoveryCycleStatus researching,
      @Param("now") Instant now,
      @Param("legacyCutoff") Instant legacyCutoff,
      Pageable pageable);
}
