package com.marketinghub.repository.jpa.creative.convergence;

import com.marketinghub.creative.convergence.v1.CreativeConvergenceTask;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: consultar e persistir tarefas verificáveis de convergência. */
public interface CreativeConvergenceTaskRepository
    extends JpaRepository<CreativeConvergenceTask, Long> {
  /** Lista as tarefas do ciclo para relatório e prevenção de repetição. */
  List<CreativeConvergenceTask> findByCycleIdOrderByIdAsc(Long cycleId);

  /** Informa se a mesma falha já apareceu no ciclo. */
  boolean existsByCycleIdAndFingerprint(Long cycleId, String fingerprint);
}
