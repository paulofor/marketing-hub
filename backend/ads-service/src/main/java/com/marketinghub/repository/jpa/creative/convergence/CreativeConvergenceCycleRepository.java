package com.marketinghub.repository.jpa.creative.convergence;

import com.marketinghub.creative.convergence.v1.ConvergenceCycleStatus;
import com.marketinghub.creative.convergence.v1.CreativeConvergenceCycle;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: consultar e persistir ciclos de convergência comercial. */
public interface CreativeConvergenceCycleRepository
    extends JpaRepository<CreativeConvergenceCycle, Long> {
  /** Localiza o ciclo ativo mais recente da linhagem do criativo. */
  Optional<CreativeConvergenceCycle> findFirstByRootCreativeIdAndStatusOrderByIdDesc(
      Long rootCreativeId, ConvergenceCycleStatus status);

  /** Localiza o ciclo mais recente da linhagem independentemente do estado final. */
  Optional<CreativeConvergenceCycle> findFirstByRootCreativeIdOrderByIdDesc(Long rootCreativeId);
}
