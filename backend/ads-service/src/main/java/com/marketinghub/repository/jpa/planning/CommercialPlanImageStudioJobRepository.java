package com.marketinghub.repository.jpa.planning;

import com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioJob;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir e consultar a fila do Estúdio de Imagens de Têmis. */
public interface CommercialPlanImageStudioJobRepository
    extends JpaRepository<CommercialPlanImageStudioJob, Long> {
  /** Lista jobs sem carregar request, response ou auditoria bruta armazenados em LONGTEXT. */
  @Query(
      "select new com.marketinghub.repository.jpa.planning.CommercialPlanImageStudioJobSummary("
          + "j.id, j.commercialPlan.id, source.id, result.id, j.operation, j.status, j.label, "
          + "j.prompt, j.purposesJson, j.size, j.quality, j.model, j.costUsd, j.error, "
          + "j.startedAt, j.finishedAt, j.createdAt) "
          + "from CommercialPlanImageStudioJob j "
          + "left join j.sourceVisualAsset source left join j.resultVisualAsset result "
          + "where j.commercialPlan.id = :planId order by j.createdAt desc")
  List<CommercialPlanImageStudioJobSummary> findSummariesByCommercialPlanId(
      @Param("planId") Long planId);

  /** Localiza reenvio idêntico ainda válido para impedir geração e custo duplicados. */
  @Query(
      "select new com.marketinghub.repository.jpa.planning.CommercialPlanImageStudioJobSummary("
          + "j.id, j.commercialPlan.id, source.id, result.id, j.operation, j.status, j.label, "
          + "j.prompt, j.purposesJson, j.size, j.quality, j.model, j.costUsd, j.error, "
          + "j.startedAt, j.finishedAt, j.createdAt) "
          + "from CommercialPlanImageStudioJob j "
          + "left join j.sourceVisualAsset source left join j.resultVisualAsset result "
          + "where j.commercialPlan.id = :planId"
          + " and ((:sourceId is null and j.sourceVisualAsset is null)"
          + " or j.sourceVisualAsset.id = :sourceId)"
          + " and j.operation = :operation and j.label = :label and j.prompt = :prompt"
          + " and j.status <> :failed order by j.createdAt desc")
  List<CommercialPlanImageStudioJobSummary> findEquivalentSummaries(
      @Param("planId") Long planId,
      @Param("sourceId") Long sourceId,
      @Param("operation")
          com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioOperation operation,
      @Param("label") String label,
      @Param("prompt") String prompt,
      @Param("failed") CommercialPlanImageStudioStatus failed);

  /** Lista a fila de produção na ordem canônica. */
  List<CommercialPlanImageStudioJob> findByStatusOrderByCreatedAtAsc(
      CommercialPlanImageStudioStatus status);

  /** Reserva pendências e recupera produções cuja lease expirou. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select j from CommercialPlanImageStudioJob j where j.status = :pending"
          + " or (j.status = :processing and j.startedAt < :cutoff) order by j.createdAt asc")
  List<CommercialPlanImageStudioJob> findClaimable(
      @Param("pending") CommercialPlanImageStudioStatus pending,
      @Param("processing") CommercialPlanImageStudioStatus processing,
      @Param("cutoff") Instant cutoff);

  /** Informa se o asset foi materializado por uma execução governada do estúdio. */
  boolean existsByResultVisualAssetId(Long assetId);

  /** Localiza o job produtor pelo entregável resultante. */
  Optional<CommercialPlanImageStudioJob> findByResultVisualAssetId(Long assetId);
}
