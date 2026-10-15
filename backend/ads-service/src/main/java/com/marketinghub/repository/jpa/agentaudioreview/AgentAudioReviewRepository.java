package com.marketinghub.repository.jpa.agentaudioreview;

import com.marketinghub.agenttask.audioreview.v1.AgentAudioReview;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

/** Consulta recibos auditivos exclusivamente pela tarefa e mídia de origem. */
public interface AgentAudioReviewRepository extends JpaRepository<AgentAudioReview, Long> {
  /** Recupera a chamada única sem repetir inferência após falha de transporte. */
  Optional<AgentAudioReview> findByTaskIdAndAssetId(Long taskId, Long assetId);

  /** Lista recibos da tarefa para o relatório e a comprovação do aceite. */
  List<AgentAudioReview> findByTaskIdOrderByIdAsc(Long taskId);
}
