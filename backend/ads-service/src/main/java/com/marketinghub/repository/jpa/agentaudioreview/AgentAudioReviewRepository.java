package com.marketinghub.repository.jpa.agentaudioreview;

import com.marketinghub.agenttask.audioreview.v1.AgentAudioReview;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Consulta recibos auditivos pela tarefa ou pela identidade completa de uma escuta reutilizável.
 */
public interface AgentAudioReviewRepository extends JpaRepository<AgentAudioReview, Long> {
  /** Recupera a chamada única sem repetir inferência após falha de transporte. */
  Optional<AgentAudioReview> findByTaskIdAndAssetId(Long taskId, Long assetId);

  /** Lista recibos da tarefa para o relatório e a comprovação do aceite. */
  List<AgentAudioReview> findByTaskIdOrderByIdAsc(Long taskId);

  /** Reutiliza somente resposta concluída e custeada para a mesma mídia, contrato e contexto. */
  Optional<AgentAudioReview>
      findFirstByProductIdAndExperimentIdAndAssetIdAndVideoSha256AndAudioSha256AndRequestHashAndStatusAndEstimatedUsdIsNotNullOrderByIdDesc(
          Long productId,
          Long experimentId,
          Long assetId,
          String videoSha256,
          String audioSha256,
          String requestHash,
          String status);
}
