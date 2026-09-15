package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleRules.require;

import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Responsabilidade: materializar no experimento os limites de mídia confirmados no ciclo. */
@Component
@RequiredArgsConstructor
public class LearningCycleCommercialAuthorization {
  private static final ZoneId COMMERCIAL_ZONE = ZoneId.of("America/Sao_Paulo");
  private final ExperimentRepository experiments;

  /**
   * Persiste teto, período restante e orçamento diário seguro no mesmo comando que recebe a
   * autorização humana.
   */
  public void apply(LearningSalesCycle cycle, Experiment experiment, Instant authorizedAt) {
    if (experiment.getPlatform() != ExperimentPlatform.FACEBOOK) return;
    require(
        cycle.getBudgetLimitBrl() != null && cycle.getBudgetLimitBrl().signum() > 0,
        "O ciclo precisa possuir teto financeiro positivo antes da autorização.");
    require(
        cycle.getWindowStart() != null
            && cycle.getWindowEnd() != null
            && authorizedAt.isBefore(cycle.getWindowEnd()),
        "A janela terminou; planeje outro experimento.");

    Instant effectiveStart =
        authorizedAt.isAfter(cycle.getWindowStart()) ? authorizedAt : cycle.getWindowStart();
    LocalDate startDate = effectiveStart.atZone(COMMERCIAL_ZONE).toLocalDate();
    LocalDate endDate = cycle.getWindowEnd().minusNanos(1).atZone(COMMERCIAL_ZONE).toLocalDate();
    require(!startDate.isAfter(endDate), "A janela não possui mais nenhum dia comercial útil.");
    long inclusiveDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
    var dailyBudget =
        cycle
            .getBudgetLimitBrl()
            .divide(java.math.BigDecimal.valueOf(inclusiveDays), 2, RoundingMode.DOWN);
    require(
        dailyBudget.signum() > 0,
        "O teto do ciclo é insuficiente para distribuir a mídia durante a janela restante.");

    experiment.setMediaSpendLimit(cycle.getBudgetLimitBrl());
    experiment.setDailyBudget(dailyBudget);
    experiment.setStartDate(startDate);
    experiment.setEndDate(endDate);
    experiments.save(experiment);
  }
}
