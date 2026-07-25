package com.marketinghub.repository.jpa.experiment.video;

import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório dos vídeos comerciais vinculados a experimentos.
 */
public interface ExperimentVideoAssetRepository extends JpaRepository<ExperimentVideoAsset, Long> {
    /** Lista todos os vídeos de experimento para a biblioteca operacional. */
    @EntityGraph(attributePaths = {"experiment", "salesVideoProfile", "salesVideoJob", "asset", "landingVideoSlot"})
    List<ExperimentVideoAsset> findAllByOrderByCreatedAtDesc();

    /** Lista os vídeos de um experimento com os vínculos necessários para resposta da API. */
    @EntityGraph(attributePaths = {"experiment", "salesVideoProfile", "salesVideoJob", "asset", "landingVideoSlot"})
    List<ExperimentVideoAsset> findByExperimentIdOrderByCreatedAtDesc(Long experimentId);

    /** Busca o ativo de experimento vinculado ao job de vídeo canônico. */
    @EntityGraph(attributePaths = {"experiment", "salesVideoProfile", "salesVideoJob", "asset", "landingVideoSlot"})
    List<ExperimentVideoAsset> findBySalesVideoJobId(Long salesVideoJobId);

    /** Lista os ativos de experimento vinculados ao perfil de vídeo canônico. */
    @EntityGraph(attributePaths = {"experiment", "salesVideoProfile", "salesVideoJob", "asset", "landingVideoSlot"})
    List<ExperimentVideoAsset> findBySalesVideoProfileIdOrderByCreatedAtDesc(Long salesVideoProfileId);

    /** Lista os ativos comerciais que usam o asset de mídia informado. */
    @EntityGraph(attributePaths = {"experiment", "salesVideoProfile", "salesVideoJob", "asset", "landingVideoSlot"})
    List<ExperimentVideoAsset> findByAssetId(Long assetId);

    /** Lista dados mínimos de reputação de provider para consumo por módulos externos ao domínio de experimento. */
    @Query(value = """
            select v.provider as provider,
                   v.status as status,
                   v.review_status as reviewStatus
            from experiment_video_asset v
            where v.sales_video_profile_id = :salesVideoProfileId
            order by v.created_at desc
            """, nativeQuery = true)
    List<ExperimentVideoAssetProviderReviewProjection> findProviderReviewsBySalesVideoProfileId(
            @Param("salesVideoProfileId") Long salesVideoProfileId);

    /** Verifica se existem vídeos obrigatórios que ainda bloqueiam a publicação. */
    @Query("""
            select case when count(v) > 0 then true else false end
            from ExperimentVideoAsset v
            where v.experiment.id = :experimentId
              and v.requiredForRelease = true
              and (v.status <> :readyStatus or v.reviewStatus <> :approvedStatus)
            """)
    boolean existsRequiredReleaseBlocker(@Param("experimentId") Long experimentId,
                                         @Param("readyStatus") ExperimentVideoStatus readyStatus,
                                         @Param("approvedStatus") ExperimentVideoReviewStatus approvedStatus);

    /** Verifica se o experimento possui ao menos um vídeo pronto e aprovado para uso comercial. */
    boolean existsByExperimentIdAndStatusAndReviewStatus(Long experimentId,
                                                         ExperimentVideoStatus status,
                                                         ExperimentVideoReviewStatus reviewStatus);

    /** Lista vídeos prontos de experimento com contexto comercial para revisão humana. */
    @EntityGraph(attributePaths = {"experiment", "experiment.niche", "experiment.hypothesisRef",
            "experiment.hypothesisRef.marketNiche", "salesVideoProfile", "salesVideoJob", "asset", "landingVideoSlot"})
    @Query("""
            select v
              from ExperimentVideoAsset v
             where v.status = :readyStatus
               and (
                    (v.assetUrl is not null and trim(v.assetUrl) <> '')
                 or v.asset is not null
               )
               and v.hasAudio = true
             order by v.id desc
            """)
    List<ExperimentVideoAsset> findReadyExperimentVideosForReview(
            @Param("readyStatus") ExperimentVideoStatus readyStatus);

    /** Lista vídeos prontos de experimento em um status de revisão específico. */
    @EntityGraph(attributePaths = {"experiment", "experiment.niche", "experiment.hypothesisRef",
            "experiment.hypothesisRef.marketNiche", "salesVideoProfile", "salesVideoJob", "asset", "landingVideoSlot"})
    @Query("""
            select v
              from ExperimentVideoAsset v
             where v.status = :readyStatus
               and v.reviewStatus = :reviewStatus
               and (
                    (v.assetUrl is not null and trim(v.assetUrl) <> '')
                 or v.asset is not null
               )
               and v.hasAudio = true
             order by v.id desc
            """)
    List<ExperimentVideoAsset> findReadyExperimentVideosForReviewByReviewStatus(
            @Param("readyStatus") ExperimentVideoStatus readyStatus,
            @Param("reviewStatus") ExperimentVideoReviewStatus reviewStatus);
}
