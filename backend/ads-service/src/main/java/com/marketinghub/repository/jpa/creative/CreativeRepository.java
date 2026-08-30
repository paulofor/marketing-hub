package com.marketinghub.repository.jpa.creative;

import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.creative.CreativeImprovementStatus;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.repository.jpa.agentlearning.TemisVisualLearningCreativeHistory;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: consultar e persistir criativos vinculados a experimentos. */
public interface CreativeRepository extends JpaRepository<Creative, Long> {
  /** Lista os criativos vinculados ao experimento informado. */
  List<Creative> findByExperimentId(Long experimentId);

  /** Lista apenas a versão mais recente de cada linhagem criativa do experimento. */
  @Query(
      """
            select c from Creative c
             where c.experiment.id = :experimentId
               and not exists (
                    select newer.id from Creative newer where newer.sourceCreative.id = c.id
               )
             order by c.id
            """)
  List<Creative> findLatestLineageCreativesByExperimentId(@Param("experimentId") Long experimentId);

  /** Busca o criativo mais recente do experimento para o gate coordenado. */
  Optional<Creative> findFirstByExperimentIdOrderByIdDesc(Long experimentId);

  /** Busca somente revisões comerciais abertas para o monitor operacional de Têmis. */
  @Query(
      """
            select c from Creative c
             where c.experiment.id = :experimentId
               and c.agentReviewStatus in (
                      com.marketinghub.creative.CreativeAgentReviewStatus.PENDING,
                      com.marketinghub.creative.CreativeAgentReviewStatus.PROCESSING,
                      com.marketinghub.creative.CreativeAgentReviewStatus.ADJUST,
                      com.marketinghub.creative.CreativeAgentReviewStatus.REJECTED,
                      com.marketinghub.creative.CreativeAgentReviewStatus.FAILED)
             order by c.id desc
            """)
  List<Creative> findTemisOpenReviews(@Param("experimentId") Long experimentId);

  /** Busca materializações visuais abertas para o monitor operacional de Íris. */
  @Query(
      """
            select c from Creative c
             where c.agentImprovementStatus in (
                      com.marketinghub.creative.CreativeImprovementStatus.PENDING,
                      com.marketinghub.creative.CreativeImprovementStatus.PROCESSING,
                      com.marketinghub.creative.CreativeImprovementStatus.FAILED,
                      com.marketinghub.creative.CreativeImprovementStatus.LIMIT_REACHED)
             order by c.id desc
            """)
  List<Creative> findIrisOpenMaterializations();

  /** Verifica se existe criativo do experimento no status informado. */
  boolean existsByExperimentIdAndStatus(Long experimentId, CreativeStatus status);

  /** Verifica se existe criativo aprovado com mídia publicável no experimento. */
  @Query(
      """
            select case when count(c) > 0 then true else false end
              from Creative c
             where c.experiment.id = :experimentId
               and c.status = :status
               and (c.agentReviewStatus = com.marketinghub.creative.CreativeAgentReviewStatus.APPROVED or c.agentReviewStatus is null)
               and (
                    (upper(coalesce(c.format, 'IMAGE')) <> 'VIDEO' and c.imageUrl is not null and trim(c.imageUrl) <> '')
                 or (upper(c.format) = 'VIDEO' and (
                        (c.videoId is not null and trim(c.videoId) <> '')
                     or (c.videoUrl is not null and trim(c.videoUrl) <> '')
                    ))
               )
            """)
  boolean existsByExperimentIdAndStatusAndUsableMedia(
      @Param("experimentId") Long experimentId, @Param("status") CreativeStatus status);

  /** Mantém compatibilidade com chamadas antigas, agora considerando imagem ou vídeo publicável. */
  @Query(
      """
            select case when count(c) > 0 then true else false end
              from Creative c
             where c.experiment.id = :experimentId
               and c.status = :status
               and (c.agentReviewStatus = com.marketinghub.creative.CreativeAgentReviewStatus.APPROVED or c.agentReviewStatus is null)
               and (
                    (upper(coalesce(c.format, 'IMAGE')) <> 'VIDEO' and c.imageUrl is not null and trim(c.imageUrl) <> '')
                 or (upper(c.format) = 'VIDEO' and (
                        (c.videoId is not null and trim(c.videoId) <> '')
                     or (c.videoUrl is not null and trim(c.videoUrl) <> '')
                    ))
               )
            """)
  boolean existsByExperimentIdAndStatusAndUsableImage(
      @Param("experimentId") Long experimentId, @Param("status") CreativeStatus status);

  /** Busca um criativo carregando também o experimento vinculado. */
  @Query("select c from Creative c join fetch c.experiment where c.id = :id")
  Optional<Creative> findByIdWithExperiment(@Param("id") Long id);

  /** Lista anúncios novos que aguardam o gate multimodal do agente especialista. */
  @Query(
      """
            select c from Creative c
              join fetch c.experiment e
              left join fetch e.hypothesisRef h
              left join fetch e.niche n
             where c.agentReviewStatus = :status
             order by c.id
            """)
  List<Creative> findAgentReviewQueue(@Param("status") CreativeAgentReviewStatus status);

  /** Bloqueia leases de revisão vencidos para que somente um consumidor os recupere. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
            select c from Creative c
              join fetch c.experiment e
             where c.agentReviewStatus = :status
               and (c.agentReviewStartedAt is null or c.agentReviewStartedAt < :cutoff)
             order by c.id
            """)
  List<Creative> findExpiredAgentReviewLeases(
      @Param("status") CreativeAgentReviewStatus status, @Param("cutoff") Instant cutoff);

  /** Lista correções decididas pelo agente que aguardam geração de uma nova versão. */
  @Query(
      """
            select c from Creative c
              join fetch c.experiment e
             where c.agentImprovementStatus = :status
             order by c.id
            """)
  List<Creative> findAgentImprovementQueue(@Param("status") CreativeImprovementStatus status);

  /** Lista criativos de vídeo com o contexto comercial necessário para revisão. */
  @Query(
      """
            select c
              from Creative c
              join fetch c.experiment e
              join fetch e.niche n
              left join fetch e.hypothesisRef h
              left join fetch h.marketNiche hn
             where upper(coalesce(c.format, '')) = 'VIDEO'
               and (
                    (c.videoId is not null and trim(c.videoId) <> '')
                 or (c.videoUrl is not null and trim(c.videoUrl) <> '')
               )
             order by c.id desc
            """)
  List<Creative> findVideoCreativesForReview();

  /** Lista criativos de vídeo em um status específico com contexto comercial para revisão. */
  @Query(
      """
            select c
              from Creative c
              join fetch c.experiment e
              join fetch e.niche n
              left join fetch e.hypothesisRef h
              left join fetch h.marketNiche hn
             where upper(coalesce(c.format, '')) = 'VIDEO'
               and c.status = :status
               and (
                    (c.videoId is not null and trim(c.videoId) <> '')
                 or (c.videoUrl is not null and trim(c.videoUrl) <> '')
               )
             order by c.id desc
            """)
  List<Creative> findVideoCreativesForReviewByStatus(@Param("status") CreativeStatus status);

  /** Conta todos os criativos vinculados ao experimento informado. */
  long countByExperimentId(Long experimentId);

  /** Conta os criativos do experimento que estão no status informado. */
  long countByExperimentIdAndStatus(Long experimentId, CreativeStatus status);

  /** Conta criativos aprovados com mídia publicável no experimento informado. */
  @Query(
      """
            select count(c)
              from Creative c
             where c.experiment.id = :experimentId
               and c.status = :status
               and (c.agentReviewStatus = com.marketinghub.creative.CreativeAgentReviewStatus.APPROVED or c.agentReviewStatus is null)
               and (
                    (upper(coalesce(c.format, 'IMAGE')) <> 'VIDEO' and c.imageUrl is not null and trim(c.imageUrl) <> '')
                 or (upper(c.format) = 'VIDEO' and (
                        (c.videoId is not null and trim(c.videoId) <> '')
                     or (c.videoUrl is not null and trim(c.videoUrl) <> '')
                    ))
               )
            """)
  long countByExperimentIdAndStatusAndUsableMedia(
      @Param("experimentId") Long experimentId, @Param("status") CreativeStatus status);

  /** Mantém compatibilidade com chamadas antigas, agora contando imagem ou vídeo publicável. */
  @Query(
      """
            select count(c)
              from Creative c
             where c.experiment.id = :experimentId
               and c.status = :status
               and (c.agentReviewStatus = com.marketinghub.creative.CreativeAgentReviewStatus.APPROVED or c.agentReviewStatus is null)
               and (
                    (upper(coalesce(c.format, 'IMAGE')) <> 'VIDEO' and c.imageUrl is not null and trim(c.imageUrl) <> '')
                 or (upper(c.format) = 'VIDEO' and (
                        (c.videoId is not null and trim(c.videoId) <> '')
                     or (c.videoUrl is not null and trim(c.videoUrl) <> '')
                    ))
               )
            """)
  long countByExperimentIdAndStatusAndUsableImage(
      @Param("experimentId") Long experimentId, @Param("status") CreativeStatus status);

  /** Lista pareceres históricos sem carregar relações e campos alheios ao aprendizado visual. */
  @Query(
      "select new com.marketinghub.repository.jpa.agentlearning.TemisVisualLearningCreativeHistory("
          + "c.id, c.experiment.id, c.versionNumber, c.format, c.costUsd, c.agentReviewStatus, "
          + "c.agentReviewJson, c.agentReviewRequestJson, c.agentReviewResponseJson, "
          + "c.agentImprovementJson) from Creative c "
          + "where c.experiment.id = :experimentId and c.agentReviewStatus is not null "
          + "order by c.id")
  List<TemisVisualLearningCreativeHistory> findVisualLearningHistoryByExperimentId(
      @Param("experimentId") Long experimentId);
}
