package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanAgentActivityDto;
import com.marketinghub.planning.dto.CommercialPlanAgentActivityDto.Entry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a simplificação do plano sem remover gates comerciais essenciais. */
class CommercialPlanOperationalFlowServiceTest {

  /** Mantém publicação bloqueada enquanto Dédalo não comprovar a homologação da jornada. */
  @Test
  void blocksPublicationUntilJourneyIsHomologated() {
    CommercialPlanAgentActivityService activities = mock(CommercialPlanAgentActivityService.class);
    CommercialPlanOperationalFlowService service =
        new CommercialPlanOperationalFlowService(activities);
    Experiment experiment = new Experiment();
    experiment.setId(9L);
    CommercialPlan plan =
        CommercialPlan.builder()
            .id(7L)
            .mainOffer("Agenda Cheia")
            .targetAudience("Prestadores de serviço")
            .experiment(experiment)
            .build();
    when(activities.activity(plan)).thenReturn(activity(List.of()));

    var result = service.view(plan);

    assertThat(result.currentStage()).isEqualTo("HOMOLOGATE_JOURNEY");
    assertThat(result.status()).isEqualTo("BLOQUEADO");
    assertThat(result.nextAction()).contains("homologação");
  }

  /** Libera somente a etapa de publicação quando a homologação foi concluída. */
  @Test
  void advancesToHumanGovernedPublicationAfterHomologation() {
    CommercialPlanAgentActivityService activities = mock(CommercialPlanAgentActivityService.class);
    CommercialPlanOperationalFlowService service =
        new CommercialPlanOperationalFlowService(activities);
    Experiment experiment = new Experiment();
    experiment.setId(9L);
    CommercialPlan plan =
        CommercialPlan.builder()
            .id(7L)
            .mainOffer("Agenda Cheia")
            .targetAudience("Prestadores de serviço")
            .experiment(experiment)
            .build();
    Entry dedalo =
        new Entry(
            "JOURNEY_HOMOLOGATION",
            "landing-generator",
            "Dédalo",
            "Homologação",
            "COMPLETED",
            null,
            null,
            null,
            false,
            null,
            "test",
            null,
            null,
            null,
            Instant.now());
    when(activities.activity(plan)).thenReturn(activity(List.of(dedalo)));

    var result = service.view(plan);

    assertThat(result.currentStage()).isEqualTo("PUBLISH_TEST");
    assertThat(result.status()).isEqualTo("APROVADO");
    assertThat(result.nextAction()).contains("aprovação humana");
  }

  /** Cria uma atividade mínima para os cenários do fluxo. */
  private CommercialPlanAgentActivityDto activity(List<Entry> entries) {
    return new CommercialPlanAgentActivityDto(
        7L,
        1,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        0,
        0,
        entries);
  }
}
