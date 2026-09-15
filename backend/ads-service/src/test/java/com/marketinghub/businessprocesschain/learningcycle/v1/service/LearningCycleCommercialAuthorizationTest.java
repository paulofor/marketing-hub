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
}
