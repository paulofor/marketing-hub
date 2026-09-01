package com.marketinghub.repository.jpa.salesvideo;

import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoRetryReason;
import com.marketinghub.salesvideo.SalesVideoStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositório dos jobs do módulo de vídeos. */
public interface SalesVideoJobRepository
    extends JpaRepository<SalesVideoJob, Long>, JpaSpecificationExecutor<SalesVideoJob> {

  /** Lista jobs de um perfil de vídeo do mais recente para o mais antigo. */
  List<SalesVideoJob> findByProfileIdOrderByRequestedAtDesc(Long profileId);

  /** Lista jobs de todos os perfis de vídeo de um produto do mais recente para o mais antigo. */
  List<SalesVideoJob> findByProfileProductIdAndTenantIdOrderByRequestedAtDesc(
      Long productId, String tenantId);

  /** Lista todos os jobs de vídeo de um tenant do mais recente para o mais antigo. */
  List<SalesVideoJob> findByTenantIdOrderByRequestedAtDesc(String tenantId);

  /** Busca o job mais recente de um perfil de vídeo. */
  Optional<SalesVideoJob> findFirstByProfileIdOrderByRequestedAtDesc(Long profileId);

  /** Verifica se algum job usa o asset informado em qualquer papel de mídia. */
  @Query(
      "select case when count(j) > 0 then true else false end from SalesVideoJob j "
          + "where (j.asset.id = :assetId or j.posterAsset.id = :assetId or j.vttAsset.id = :assetId)")
  boolean existsByAnyAssetReference(@Param("assetId") Long assetId);

  /**
   * Lista um lote limitado de falhas elegíveis para avaliação de retry automático, sem carregar o
   * histórico completo em memória.
   */
  @Query(
      "select j from SalesVideoJob j where j.status = :status "
          + "and j.finishedAt < :finishedAt "
          + "and j.failureDetail like '%retryable=true%' "
          + "and (j.retryReason is null or j.retryReason <> :excludedRetryReason) "
          + "and not exists (select child.id from SalesVideoJob child "
          + "where child.retryOfJob.id = j.id) "
          + "order by j.finishedAt asc, j.id asc")
  List<SalesVideoJob> findAutomaticRetryCandidates(
      @Param("status") SalesVideoStatus status,
      @Param("finishedAt") Instant finishedAt,
      @Param("excludedRetryReason") SalesVideoRetryReason excludedRetryReason,
      Pageable pageable);

  /** Lista jobs pendentes antigos ainda não iniciados. */
  List<SalesVideoJob> findByStatusAndRequestedAtBeforeAndStartedAtIsNull(
      SalesVideoStatus status, Instant requestedAt);

  /** Verifica se um job já originou outro job de retry. */
  boolean existsByRetryOfJob_Id(Long jobId);

  /** Recupera o encaminhamento já criado para impedir pós-produção duplicada. */
  Optional<SalesVideoJob> findFirstByRetryOfJob_IdOrderByRequestedAtDesc(Long jobId);

  /** Localiza recusas recentes de crédito emitidas por uma família de provedor. */
  @Query(
      "select j from SalesVideoJob j where upper(j.providerName) like concat('%', upper(:provider), '%') and (lower(j.failureDetail) like '%credit%' or lower(j.failureDetail) like '%saldo%') order by j.finishedAt desc, j.updatedAt desc")
  List<SalesVideoJob> findRecentCreditFailures(
      @Param("provider") String provider, Pageable pageable);
}
