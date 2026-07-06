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
    /** Lista os vídeos de um experimento com os vínculos necessários para resposta da API. */
    @EntityGraph(attributePaths = {"experiment", "salesVideoProfile", "salesVideoJob", "asset", "landingVideoSlot"})
    List<ExperimentVideoAsset> findByExperimentIdOrderByCreatedAtDesc(Long experimentId);

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
}
