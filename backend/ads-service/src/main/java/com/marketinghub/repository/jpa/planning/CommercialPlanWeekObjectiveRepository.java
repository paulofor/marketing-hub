package com.marketinghub.repository.jpa.planning;

import com.marketinghub.planning.CommercialPlanWeekObjective;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir objetivos semanais do planejamento comercial. */
public interface CommercialPlanWeekObjectiveRepository
    extends JpaRepository<CommercialPlanWeekObjective, Long> {
  /** Lista objetivos de uma semana do plano na ordem de exibicao. */
  List<CommercialPlanWeekObjective> findByPlanIdAndWeekNumberOrderBySequenceOrderAsc(
      Long planId, Integer weekNumber);

  /** Remove objetivos de uma semana para substituir pela versao editada. */
  void deleteByPlanIdAndWeekNumber(Long planId, Integer weekNumber);

  /** Busca um compromisso garantindo a segregação pelo plano comercial. */
  Optional<CommercialPlanWeekObjective> findByIdAndPlanId(Long id, Long planId);
}
