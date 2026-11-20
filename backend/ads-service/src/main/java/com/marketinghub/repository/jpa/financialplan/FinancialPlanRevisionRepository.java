package com.marketinghub.repository.jpa.financialplan;

import com.marketinghub.financialplan.v1.FinancialPlanRevision;
import com.marketinghub.financialplan.v1.FinancialPlanRevision.Environment;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir revisões segregadas e serializar solicitações de análise. */
public interface FinancialPlanRevisionRepository
    extends JpaRepository<FinancialPlanRevision, Long> {
  /** Lista somente as revisões do proprietário e ambiente solicitados. */
  List<FinancialPlanRevision> findByScopeKindAndScopeIdAndEnvironmentOrderByRevisionNumberDesc(
      String kind, Long scopeId, Environment environment);

  /** Seleciona no banco somente a revisão mais recente do mesmo proprietário, ambiente e plano. */
  Optional<FinancialPlanRevision>
      findFirstByScopeKindAndScopeIdAndEnvironmentAndCommercialPlanIdOrderByRevisionNumberDesc(
          String kind, Long scopeId, Environment environment, Long commercialPlanId);

  /** Identifica mudança intermediária para impedir que o retorno ao tipo reative parecer antigo. */
  @Query(
      "select count(r) > 0 from FinancialPlanRevision r where r.scopeKind = 'PRODUCT' and r.scopeId"
          + " = :productId and r.environment = :environment and r.revisionNumber > :revision and"
          + " (r.productTypeId <> :typeId or r.productTypeId is null)")
  boolean hasSubsequentTypeChange(
      @Param("productId") Long productId,
      @Param("environment") Environment environment,
      @Param("revision") int revision,
      @Param("typeId") Long typeId);

  /** Reserva uma revisão para impedir múltiplas avaliações pagas concorrentes. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM FinancialPlanRevision p WHERE p.id=:id")
  Optional<FinancialPlanRevision> findLockedById(@Param("id") Long id);
}
