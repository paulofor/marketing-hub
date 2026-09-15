package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar a materialização atômica do teto comercial autorizado no ciclo. */
class LearningCycleCommercialAuthorizationTest {
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final LearningCycleCommercialAuthorization authorization =
      new LearningCycleCommercialAuthorization(experiments);

  /** Distribui somente o saldo de dias da janela e nunca ultrapassa o teto confirmado. */
  @Test
  void appliesRemainingFacebookWindowAndExactTotalLimit() {
    LearningSalesCycle cycle = new LearningSalesCycle();
    cycle.setBudgetLimitBrl(new BigDecimal("100.00"));
    cycle.setWindowStart(Instant.parse("2026-09-10T03:00:00Z"));
    cycle.setWindowEnd(Instant.parse("2026-09-17T02:59:59Z"));
    Experiment experiment =
        Experiment.builder().id(92L).platform(ExperimentPlatform.FACEBOOK).build();

    authorization.apply(cycle, experiment, Instant.parse("2026-09-15T12:00:00Z"));

    assertThat(experiment.getMediaSpendLimit()).isEqualByComparingTo("100.00");
    assertThat(experiment.getDailyBudget()).isEqualByComparingTo("50.00");
    assertThat(experiment.getStartDate()).isEqualTo(LocalDate.parse("2026-09-15"));
    assertThat(experiment.getEndDate()).isEqualTo(LocalDate.parse("2026-09-16"));
    verify(experiments).save(experiment);
  }

  /** Mantém canal sem mídia fora do contrato de orçamento do Facebook. */
  @Test
  void keepsDirectChannelWithoutMediaBudget() {
    LearningSalesCycle cycle = new LearningSalesCycle();
    Experiment experiment =
        Experiment.builder().id(92L).platform(ExperimentPlatform.DIRECT_ONE_TO_ONE).build();

    authorization.apply(cycle, experiment, Instant.parse("2026-09-15T12:00:00Z"));

    assertThat(experiment.getMediaSpendLimit()).isNull();
    assertThat(experiment.getDailyBudget()).isNull();
  }

  /** Sugere e grava valores diferentes do rateio sem alterar o total ou a janela. */
  @Test
  void preservesChosenDailyAndSuggestsWithoutWriting() {
    var cycle = new LearningSalesCycle();
    cycle.setBudgetLimitBrl(new BigDecimal("120.00"));
    cycle.setWindowStart(Instant.parse("2026-09-10T03:00:00Z"));
    cycle.setWindowEnd(Instant.parse("2026-09-17T02:59:59Z"));
    var now = Instant.parse("2026-09-15T12:00:00Z");
    assertThat(LearningCycleCommercialAuthorization.suggestDaily(cycle, now))
        .isEqualByComparingTo("60.00");
    org.mockito.Mockito.verifyNoInteractions(experiments);
    var experiment = Experiment.builder().id(193L).platform(ExperimentPlatform.FACEBOOK).build();
    authorization.apply(cycle, experiment, now, new BigDecimal("30.25"));
    assertThat(experiment.getDailyBudget()).isEqualByComparingTo("30.25");
    assertThat(experiment.getMediaSpendLimit()).isEqualByComparingTo("120.00");
    assertThat(LearningCycleCommercialAuthorization.suggestDaily(cycle, cycle.getWindowEnd()))
        .isNull();
  }

  /** Rejeita montantes inválidos e preserva o gate de janela e versão homologada. */
  @Test
  void validatesEditedLimitsAndKeepsVersionAndWindowGates() {
    var cycle = new LearningSalesCycle();
    cycle.setBudgetLimitBrl(new BigDecimal("100"));
    cycle.setProductVersion("outra-versao");
    cycle.setWindowStart(Instant.parse("2026-09-10T03:00:00Z"));
    cycle.setWindowEnd(Instant.parse("2026-09-17T02:59:59Z"));
    var now = Instant.parse("2026-09-15T12:00:00Z");
    var data = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
    data.put("confirmed", true)
        .put("productVersion", "outra-versao")
        .put("budgetLimitBrl", new BigDecimal("120.00"))
        .put("dailyBudgetBrl", new BigDecimal("30.25"));
    var evidence = new LearningCycleEvidence(null, null, null, null, null);
    evidence.authorization(cycle, data, now);
    assertThat(cycle.getBudgetLimitBrl()).isEqualByComparingTo("100");
    for (String invalid : java.util.List.of("-1", "0", "120.01", "1.001", "1000000000000")) {
      data.put("dailyBudgetBrl", new BigDecimal(invalid));
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () -> evidence.authorization(cycle, data, now))
          .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
    data.put("dailyBudgetBrl", new BigDecimal("30.25"));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> evidence.authorization(cycle, data, cycle.getWindowEnd()))
        .hasMessageContaining("janela terminou");
    data.put("productVersion", "versao-errada");
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> evidence.authorization(cycle, data, now))
        .hasMessageContaining("versão homologada");
  }
}
