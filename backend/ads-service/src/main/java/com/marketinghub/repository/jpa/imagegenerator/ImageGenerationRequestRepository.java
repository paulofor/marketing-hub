package com.marketinghub.repository.jpa.imagegenerator;

import com.marketinghub.imagegenerator.ImageGenerationRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir registros de auditoria do gerador manual de imagens. */
public interface ImageGenerationRequestRepository
    extends JpaRepository<ImageGenerationRequest, Long> {

  /** Lista gerações concluídas do mesmo contexto comercial sem carregar o payload bruto. */
  @Query(
      """
      select r.jobId as jobId,
             r.batchJobId as batchJobId,
             r.model as model,
             r.serviceTier as serviceTier,
             r.outputFormat as outputFormat,
             r.prompt as prompt,
             r.finishedAt as finishedAt
      from ImageGenerationRequest r
      where r.productId = :productId
        and r.commercialPlanId = :commercialPlanId
        and ((:experimentId is null and r.experimentId is null) or r.experimentId = :experimentId)
        and r.status = 'COMPLETED'
      order by r.createdAt desc, r.id desc
      """)
  List<RecentCompletedProjection> findRecentCompleted(
      @Param("productId") Long productId,
      @Param("commercialPlanId") Long commercialPlanId,
      @Param("experimentId") Long experimentId,
      Pageable pageable);

  /** Responsabilidade: projetar somente os metadados leves necessários para o histórico. */
  interface RecentCompletedProjection {
    /** Retorna o identificador da geração individual. */
    String getJobId();

    /** Retorna o identificador do lote comparativo. */
    String getBatchJobId();

    /** Retorna o modelo que gerou a imagem. */
    String getModel();

    /** Retorna o nível de serviço usado na geração. */
    String getServiceTier();

    /** Retorna o formato persistido da imagem. */
    String getOutputFormat();

    /** Retorna o prompt original informado pelo usuário. */
    String getPrompt();

    /** Retorna o horário de conclusão da geração. */
    Instant getFinishedAt();
  }

  /** Recupera uma imagem somente quando ela pertence ao contexto comercial informado. */
  @Query(
      """
      select r from ImageGenerationRequest r
      where r.jobId = :jobId
        and r.productId = :productId
        and r.commercialPlanId = :commercialPlanId
        and ((:experimentId is null and r.experimentId is null) or r.experimentId = :experimentId)
        and r.status = 'COMPLETED'
      """)
  java.util.Optional<ImageGenerationRequest> findCompletedByContextAndJobId(
      @Param("productId") Long productId,
      @Param("commercialPlanId") Long commercialPlanId,
      @Param("experimentId") Long experimentId,
      @Param("jobId") String jobId);
}
