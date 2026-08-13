package com.marketinghub.repository.jpa.financialagent;

import com.marketinghub.financialagent.StudioCostLedgerEntry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir e consolidar o ledger de custos do Estúdio. */
public interface StudioCostLedgerEntryRepository
    extends JpaRepository<StudioCostLedgerEntry, Long> {
  /** Busca a tentativa pela identidade idempotente da origem. */
  Optional<StudioCostLedgerEntry> findBySourceTypeAndSourceId(String sourceType, String sourceId);

  /** Lista as tentativas atribuídas a um plano comercial. */
  List<StudioCostLedgerEntry> findByCommercialPlanIdOrderByCreatedAtAsc(Long commercialPlanId);

  /** Lista tentativas ainda sem atribuição a um planejamento comercial. */
  List<StudioCostLedgerEntry> findByCommercialPlanIdIsNullOrderByCreatedAtAsc();

  /** Lista somente as tentativas novas pertencentes ao ciclo financeiro informado. */
  List<StudioCostLedgerEntry> findByVideoProductionCycleIdOrderByCreatedAtAsc(Long cycleId);

  /** Lista consumos de uma família de provedor sem misturar provedores de nome parecido. */
  @Query(
      "select e from StudioCostLedgerEntry e where upper(e.provider) = upper(:provider) or upper(e.provider) like concat(upper(:provider), '%') order by e.createdAt asc")
  List<StudioCostLedgerEntry> findByProviderFamily(@Param("provider") String provider);

  /** Soma custos conhecidos, preferindo valor reportado pelo provedor. */
  @Query(
      "select coalesce(sum(coalesce(e.providerCostUsd, e.estimatedCostUsd)), 0) from StudioCostLedgerEntry e where e.commercialPlanId = :planId")
  BigDecimal totalCostUsdByPlanId(Long planId);

  /** Soma os custos conhecidos que ainda não foram atribuídos a um planejamento. */
  @Query(
      "select coalesce(sum(coalesce(e.providerCostUsd, e.estimatedCostUsd)), 0) from StudioCostLedgerEntry e where e.commercialPlanId is null")
  BigDecimal totalUnassignedCostUsd();

  /** Consolida custo e revisao comercial por provedor para um plano. */
  @Query(
      value =
          """
          select costs.provider as provider,
                 costs.totalAttempts as totalAttempts,
                 costs.knownCostAttempts as knownCostAttempts,
                 costs.knownCostUsd as knownCostUsd,
                 coalesce(reviews.reviewedAssets, 0) as reviewedAssets,
                 coalesce(reviews.approvedAssets, 0) as approvedAssets,
                 coalesce(reviews.pendingReviewAssets, 0) as pendingReviewAssets
            from (
                  select l.provider,
                         count(*) as totalAttempts,
                         count(case when coalesce(l.provider_cost_usd, l.estimated_cost_usd) is not null then 1 end) as knownCostAttempts,
                         coalesce(sum(coalesce(l.provider_cost_usd, l.estimated_cost_usd)), 0) as knownCostUsd
                    from studio_cost_ledger_entry l
                   where l.commercial_plan_id = :planId
                   group by l.provider
                 ) costs
            left join (
                  select l.provider,
                         count(distinct case when v.review_status in ('APPROVED', 'REJECTED') then v.id end) as reviewedAssets,
                         count(distinct case when v.review_status = 'APPROVED' then v.id end) as approvedAssets,
                         count(distinct case when v.review_status = 'PENDING' then v.id end) as pendingReviewAssets
                    from studio_cost_ledger_entry l
                    join experiment_video_asset v
                      on (l.source_type = 'SALES_VIDEO_JOB' and v.sales_video_job_id = cast(l.source_id as unsigned))
                      or (l.source_type = 'MEDIA_ASSET' and v.asset_id = cast(l.source_id as unsigned))
                   where l.commercial_plan_id = :planId
                   group by l.provider
                 ) reviews on reviews.provider = costs.provider
           order by costs.provider
          """,
      nativeQuery = true)
  List<StudioProviderEfficiencyProjection> providerEfficiencyByPlanId(@Param("planId") Long planId);
}
