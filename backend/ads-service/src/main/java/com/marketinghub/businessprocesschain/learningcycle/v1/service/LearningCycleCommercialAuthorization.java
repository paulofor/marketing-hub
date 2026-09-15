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
    apply(cycle, experiment, authorizedAt, null);
  }

  /** Sugere distribuição do teto pelos dias restantes, sem registrar autorização. */
  public static java.math.BigDecimal suggestDaily(LearningSalesCycle cycle, Instant now) {
    if (cycle.getBudgetLimitBrl() == null
        || cycle.getWindowStart() == null
        || cycle.getWindowEnd() == null
        || !now.isBefore(cycle.getWindowEnd())) return null;
    Instant start = now.isAfter(cycle.getWindowStart()) ? now : cycle.getWindowStart();
    long days =
        ChronoUnit.DAYS.between(
                start.atZone(COMMERCIAL_ZONE).toLocalDate(),
                cycle.getWindowEnd().minusNanos(1).atZone(COMMERCIAL_ZONE).toLocalDate())
            + 1;
    return days > 0
        ? cycle.getBudgetLimitBrl().divide(java.math.BigDecimal.valueOf(days), 2, RoundingMode.DOWN)
        : null;
  }

  /**
   * Persiste os limites escolhidos pelo operador, mantendo a janela e o teto total independentes.
   */
  public void apply(
      LearningSalesCycle cycle,
      Experiment experiment,
      Instant authorizedAt,
      java.math.BigDecimal requestedDaily) {
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
    if (requestedDaily != null) dailyBudget = requestedDaily;
    require(
        dailyBudget.compareTo(cycle.getBudgetLimitBrl()) <= 0,
        "O diário não pode ultrapassar o total.");
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
