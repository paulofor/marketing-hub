package com.marketinghub.experiment.history;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir e ordenar o histórico factual de cada experimento. */
public interface ExperimentHistoryEventRepository
    extends JpaRepository<ExperimentHistoryEvent, Long> {
  /** Lista os fatos mais recentes de um experimento sem misturar dados de outros testes. */
  List<ExperimentHistoryEvent> findByExperimentIdOrderByOccurredAtDescIdDesc(Long experimentId);
}
