package com.marketinghub.financialplan.v1.service;

import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.financialplan.v1.service.saveplan.PlanAssumptions;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: impedir que o aceite de descoberta ultrapasse sua identidade e seus limites.
 */
@Component
@RequiredArgsConstructor
public class FinancialDiscoveryPolicy {
  private final LearningSalesCycleRepository cycles;
  private final ExperimentRepository experiments;
  private final CommercialPlanRepository plans;

  /** Confirma fontes oficiais antes de salvar a exceção ou consumir a análise independente. */
  public void validate(String scope, Long productId, Long planId, PlanAssumptions assumptions) {
    var d = assumptions.discoveryInvestment();
    if (d == null) return;
    require("PRODUCT".equals(scope), "Modelos de tipo não podem autorizar descoberta.");
    var cycle =
        cycles
            .findById(d.learningCycleId())
            .orElseThrow(() -> conflict("Ciclo de descoberta não encontrado."));
    var experiment =
        experiments
            .findById(d.experimentId())
            .orElseThrow(() -> conflict("Experimento de descoberta não encontrado."));
    require(
        Objects.equals(cycle.getProductId(), productId)
            && Objects.equals(cycle.getExperimentId(), experiment.getId())
            && experiment.getProduct() != null
            && Objects.equals(experiment.getProduct().getId(), productId)
            && "OPEN".equals(cycle.getStatus())
            && experiment.getStatus() == ExperimentStatus.PLANNED,
        "O aceite exige o próprio produto, ciclo aberto e experimento ainda planejado.");
    require(
        planId != null
            && plans.findByExperimentReference(experiment.getId()).stream()
                .anyMatch(p -> Objects.equals(p.getId(), planId)),
        "Vincule o experimento ao mesmo plano comercial antes do aceite de descoberta.");
    require(
        d.mediaBudgetBrl() != null
            && d.mediaBudgetBrl().signum() > 0
            && d.durationDays() != null
            && d.durationDays() >= 1
            && d.durationDays() <= 30
            && Boolean.TRUE.equals(d.acceptBelowMinimumMargin()),
        "Confirme o risco de descoberta com teto total e duração válidos.");
    require(
        experiment.getMediaSpendLimit() == null
            || experiment.getMediaSpendLimit().compareTo(d.mediaBudgetBrl()) <= 0,
        "O teto do experimento excede o aceite de descoberta.");
    require(
        cycle.getBudgetLimitBrl() == null
            || cycle.getBudgetLimitBrl().compareTo(d.mediaBudgetBrl()) <= 0,
        "O teto do ciclo excede o aceite de descoberta.");
    if (experiment.getStartDate() != null && experiment.getEndDate() != null)
      require(
          !experiment.getEndDate().isBefore(experiment.getStartDate())
              && java.time.temporal.ChronoUnit.DAYS.between(
                          experiment.getStartDate(), experiment.getEndDate())
                      + 1
                  <= d.durationDays(),
          "A janela do experimento excede a duração aceita para descoberta.");
    if (cycle.getWindowStart() != null && cycle.getWindowEnd() != null)
      require(
          cycle.getWindowEnd().isAfter(cycle.getWindowStart())
              && java.time.Duration.between(cycle.getWindowStart(), cycle.getWindowEnd())
                      .compareTo(java.time.Duration.ofDays(d.durationDays()))
                  <= 0,
          "A janela do ciclo excede a duração aceita para descoberta.");
  }

  /** Recusa o uso fora do escopo sem executar chamadas externas ou alterar dados. */
  private static void require(boolean condition, String message) {
    if (!condition) throw conflict(message);
  }

  /** Expõe conflito acionável preservando a transação e os dados anteriores. */
  private static ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}
