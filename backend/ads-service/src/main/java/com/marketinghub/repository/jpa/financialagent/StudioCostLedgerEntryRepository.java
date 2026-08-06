package com.marketinghub.repository.jpa.financialagent;

import com.marketinghub.financialagent.StudioCostLedgerEntry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Responsabilidade: persistir e consolidar o ledger de custos do Estúdio. */
public interface StudioCostLedgerEntryRepository
    extends JpaRepository<StudioCostLedgerEntry, Long> {
  /** Busca a tentativa pela identidade idempotente da origem. */
  Optional<StudioCostLedgerEntry> findBySourceTypeAndSourceId(String sourceType, String sourceId);

  /** Lista as tentativas atribuídas a um plano comercial. */
  List<StudioCostLedgerEntry> findByCommercialPlanIdOrderByCreatedAtAsc(Long commercialPlanId);

  /** Lista tentativas ainda sem atribuição a um planejamento comercial. */
  List<StudioCostLedgerEntry> findByCommercialPlanIdIsNullOrderByCreatedAtAsc();

  /** Soma custos conhecidos, preferindo valor reportado pelo provedor. */
  @Query(
      "select coalesce(sum(coalesce(e.providerCostUsd, e.estimatedCostUsd)), 0) from StudioCostLedgerEntry e where e.commercialPlanId = :planId")
  BigDecimal totalCostUsdByPlanId(Long planId);

  /** Soma os custos conhecidos que ainda não foram atribuídos a um planejamento. */
  @Query(
      "select coalesce(sum(coalesce(e.providerCostUsd, e.estimatedCostUsd)), 0) from StudioCostLedgerEntry e where e.commercialPlanId is null")
  BigDecimal totalUnassignedCostUsd();
}
