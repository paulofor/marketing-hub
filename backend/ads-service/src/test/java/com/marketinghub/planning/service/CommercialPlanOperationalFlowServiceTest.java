package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanStatus;
import com.marketinghub.planning.dto.CommercialPlanAgentActivityDto;
import com.marketinghub.planning.dto.CommercialPlanAgentActivityDto.Entry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: validar a simplificação do plano sem remover gates comerciais essenciais. */
class CommercialPlanOperationalFlowServiceTest {

  /** Um bloqueio explícito governa a orientação mesmo depois de gasto ou homologação antiga. */
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void showsPersistedCorrectionWithoutAuthorizingPublication(boolean homologated) {
    CommercialPlanAgentActivityService activities = mock(CommercialPlanAgentActivityService.class);
    var service = new CommercialPlanOperationalFlowService(activities);
    CommercialPlan plan =
        CommercialPlan.builder()
            .id(3L)
            .status(CommercialPlanStatus.BLOCKED)
            .mainOffer("MUSA, sete dias, R$ 67")
            .targetAudience("Mulheres adultas")
            .currentBlocker("#91 interrompido; reconciliar dados e comprovar utilidade.")
            .nextAction("1. Reconciliar #91.\n2. Dédalo: microação.\n3. Íris: entrada mobile.")
            .mainMetric("Vendas líquidas entregues e contribuição por canal")
            .successCriteria(
                "Homologação atual, observação consentida e autorização antes de novo gasto")
            .actualExperimentsPublished(1)
            .actualTotalCost(new BigDecimal("27.35"))
            .build();
    Entry previous =
        new Entry(
            "JOURNEY_HOMOLOGATION",
            "communication-director",
            "Íris",
            "Homologação anterior",
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
    when(activities.activity(plan))
        .thenReturn(activity(homologated ? List.of(previous) : List.of()));

    var result = service.view(plan);

    assertThat(result.currentStage()).isEqualTo("CORRECT_CURRENT_PLAN");
    assertThat(result.status()).isEqualTo("BLOQUEADO");
    assertThat(result.nextAction()).isEqualTo(plan.getNextAction());
    assertThat(result.blocker()).isEqualTo(plan.getCurrentBlocker());
    assertThat(result.expectedMetric()).isEqualTo(plan.getMainMetric());
    assertThat(result.decisionCriterion()).isEqualTo(plan.getSuccessCriteria());
    assertThat(result.stages())
        .singleElement()
        .satisfies(
            stage -> {
              assertThat(stage.code()).isEqualTo("CORRECT_CURRENT_PLAN");
              assertThat(stage.status()).isEqualTo("ATUAL");
            });
    assertThat(plan.getStatus()).isEqualTo(CommercialPlanStatus.BLOCKED);
  }

  /** Não converte falta de descrição do bloqueio em permissão de publicação. */
  @Test
  void keepsIncompleteBlockedPlanBlockedWithActionableGuidance() {
    CommercialPlanAgentActivityService activities = mock(CommercialPlanAgentActivityService.class);
    var service = new CommercialPlanOperationalFlowService(activities);
    CommercialPlan plan =
        CommercialPlan.builder().id(3L).status(CommercialPlanStatus.BLOCKED).build();
    when(activities.activity(plan)).thenReturn(activity(List.of()));

    var result = service.view(plan);

    assertThat(result.status()).isEqualTo("BLOQUEADO");
    assertThat(result.nextAction()).contains("Registrar causa, evidência");
    assertThat(result.blocker()).contains("causa ainda precisa ser registrada");
  }

  /** Mantém publicação bloqueada enquanto Íris não concluir a homologação da jornada. */
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
    Entry iris =
        new Entry(
            "JOURNEY_HOMOLOGATION",
            "communication-director",
            "Íris",
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
    when(activities.activity(plan)).thenReturn(activity(List.of(iris)));

    var result = service.view(plan);

    assertThat(result.currentStage()).isEqualTo("PUBLISH_TEST");
    assertThat(result.status()).isEqualTo("APROVADO");
    assertThat(result.nextAction()).contains("aprovação humana");
    assertThat(result.specialistDecisions())
        .extracting(decision -> decision.specialist())
        .contains("Dédalo", "Íris");
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
