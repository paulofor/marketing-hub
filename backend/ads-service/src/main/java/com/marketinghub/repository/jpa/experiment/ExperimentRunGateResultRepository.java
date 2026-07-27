package com.marketinghub.repository.jpa.experiment;

import com.marketinghub.experiment.run.ExperimentRunGateResult;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pelos resultados atuais de gates de runs de experimento. */
public interface ExperimentRunGateResultRepository
    extends JpaRepository<ExperimentRunGateResult, Long> {
  /** Lista os gates de um run agrupáveis para leitura do frontend. */
  List<ExperimentRunGateResult> findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(
      Long experimentRunId);

  /** Remove resultados anteriores para reavaliar o preflight de forma idempotente. */
  void deleteByExperimentRunId(Long experimentRunId);
}
