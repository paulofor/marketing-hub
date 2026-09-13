package com.marketinghub.repository.jpa.salesvideo;

import com.marketinghub.salesvideo.VideoProductionCycle;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: acessar ciclos governados de produção de vídeo. */
public interface VideoProductionCycleRepository extends JpaRepository<VideoProductionCycle, Long> {
  /** Lista ciclos de um projeto do mais recente para o mais antigo. */
  List<VideoProductionCycle> findByVideoProjectIdOrderByCreatedAtDesc(Long videoProjectId);

  /** Lista ciclos aguardando decisão financeira. */
  List<VideoProductionCycle> findByStatusOrderByCreatedAtAsc(String status);

  /** Lista ciclos liberados a Apolo para reconciliar jobs terminais antes do consumo. */
  List<VideoProductionCycle> findByStatusAndFinancialDecisionOrderByCreatedAtAsc(
      String status, String financialDecision);

  /** Busca o ciclo de vídeo atualizado mais recentemente. */
  Optional<VideoProductionCycle> findTopByOrderByUpdatedAtDesc();

  /** Lista os ciclos financeiros e criativos pertencentes ao plano comercial. */
  List<VideoProductionCycle> findByCommercialPlanIdOrderByUpdatedAtDesc(Long commercialPlanId);

  /** Lê a última tentativa da peça, produto, experimento e versão vigentes do ciclo. */
  @Query(
      value =
          """
      SELECT c.* FROM video_production_cycle c
      JOIN video_project p ON p.id = c.video_project_id
      WHERE c.product_id = :productId AND p.product_id = :productId
        AND c.experiment_id = :experimentId AND p.experiment_id = :experimentId
        AND p.campaign_key = :productVersion AND p.strategy_role = :strategyRole
        AND c.created_at >= :versionChangedAt
      ORDER BY c.created_at DESC, c.id DESC LIMIT 1
      """,
      nativeQuery = true)
  Optional<VideoProductionCycle> findLatestForLearningCycle(
      @Param("productId") Long productId,
      @Param("experimentId") Long experimentId,
      @Param("productVersion") String productVersion,
      @Param("strategyRole") String strategyRole,
      @Param("versionChangedAt") Instant versionChangedAt);
}
