package com.marketinghub.businessprocess.execution.service.humanactivity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorReadiness;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution.ProductProcessActivityExecutionRequest;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: comprovar confirmação, ordem e auditoria das decisões humanas padronizadas. */
class StandardHumanProductProcessActivityExecutorTest {
  private static final Instant NOW = Instant.parse("2026-08-30T15:00:00Z");
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final ProductProcessActivityPredecessorService predecessors =
      mock(ProductProcessActivityPredecessorService.class);
  private final StandardHumanProductProcessActivityExecutor executor =
      new StandardHumanProductProcessActivityExecutor(
          instances, predecessors, new ObjectMapper(), List.of(), Clock.fixed(NOW, ZoneOffset.UTC));

  /** Registra aprovação com responsável, justificativa, evidência e token específico. */
  @Test
  void recordsApprovedHumanDecision() throws Exception {
    BusinessProcessDefinition process = process();
    BusinessProcessActivityDefinition activity = activity(process);
    when(predecessors.readiness(process, activity, "experiment:89"))
        .thenReturn(new ProductProcessActivityPredecessorReadiness(true, "Predecessoras prontas."));
    when(instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            590L, "experiment:89"))
        .thenReturn(Optional.empty());

    HumanProductProcessActivityExecutionResult result =
        executor.execute(
            process,
            activity,
            Product.builder().id(9L).build(),
            "experiment:89",
            new ProductProcessActivityExecutionRequest(
                "APPROVE",
                "Paulo Operador",
                "Os gates e o teto foram revisados e estão adequados.",
                "experiment-run:12",
                "CONFIRM:pde-commercial-homologation-activation:authorization"));

    ArgumentCaptor<BusinessProcessActivityInstance> reserved =
        ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    ArgumentCaptor<BusinessProcessActivityInstance> saved =
        ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    verify(instances).saveAndFlush(reserved.capture());
    verify(instances).save(saved.capture());
    assertThat(result.operationalState()).isEqualTo("COMPLETED");
    assertThat(result.objectiveAchieved()).isTrue();
    assertThat(saved.getValue().getStatus()).isEqualTo("COMPLETED");
    assertThat(saved.getValue().getOccurrenceNumber()).isEqualTo(1);
    assertThat(saved.getValue().getExitedAt()).isEqualTo(NOW);
    assertThat(reserved.getValue().getCostCoverage()).isEqualTo("COMPLETE");
    assertThat(reserved.getValue().getEvidenceQuality()).isEqualTo("DIRECT");
    var evidence = new ObjectMapper().readTree(saved.getValue().getObjectiveEvidenceJson());
    assertThat(evidence.path("operatorName").asText()).isEqualTo("Paulo Operador");
    assertThat(evidence.path("evidenceReference").asText()).isEqualTo("experiment-run:12");
  }

  /** Permite revisar e aceitar sem redigitar a auditoria já resolvida pelo backend. */
  @Test
  void recordsReviewAndAcceptDecisionWithBackendAuditContext() throws Exception {
    BusinessProcessDefinition process = process();
    BusinessProcessActivityDefinition activity = activity(process);
    HumanProductProcessActivityHandler handler = mock(HumanProductProcessActivityHandler.class);
    StandardHumanProductProcessActivityExecutor reviewExecutor =
        new StandardHumanProductProcessActivityExecutor(
            instances,
            predecessors,
            new ObjectMapper(),
            List.of(handler),
            Clock.fixed(NOW, ZoneOffset.UTC));
    when(handler.supports(process, activity)).thenReturn(true);
    when(handler.readiness(process, activity, Product.builder().id(9L).build(), "experiment:89"))
        .thenReturn(
            new HumanProductProcessActivityReadiness(
                true,
                "Tudo pronto.",
                "Li, entendi e autorizo",
                "Revise e aceite.",
                "Revise e autorize",
                "O experimento está pronto, com amostra de 15 contatos e teto de R$ 540,00.",
                "CONFIRM:pde-commercial-homologation-activation:authorization",
                "EXPERIMENT_ACTIVATION",
                89L,
                List.of(),
                HumanProductProcessActivityReadiness.REVIEW_AND_ACCEPT,
                "experiment:89; experiment-run:9/run-number:2; commercial-plan:4"));
    when(predecessors.readiness(process, activity, "experiment:89"))
        .thenReturn(new ProductProcessActivityPredecessorReadiness(true, "Predecessoras prontas."));
    when(instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            590L, "experiment:89"))
        .thenReturn(Optional.empty());

    HumanProductProcessActivityExecutionResult result =
        reviewExecutor.execute(
            process,
            activity,
            Product.builder().id(9L).build(),
            "experiment:89",
            new ProductProcessActivityExecutionRequest(
                "APPROVE",
                null,
                null,
                null,
                "CONFIRM:pde-commercial-homologation-activation:authorization"));

    ArgumentCaptor<BusinessProcessActivityInstance> saved =
        ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    verify(instances).save(saved.capture());
    var evidence = new ObjectMapper().readTree(saved.getValue().getObjectiveEvidenceJson());
    assertThat(result.objectiveAchieved()).isTrue();
    assertThat(evidence.path("operatorName").asText())
        .isEqualTo("Operador humano pelo Marketing Hub");
    assertThat(evidence.path("evidenceReference").asText())
        .isEqualTo("experiment:89; experiment-run:9/run-number:2; commercial-plan:4");
    assertThat(evidence.path("decisionMode").asText()).isEqualTo("REVIEW_AND_ACCEPT");
  }

  /** Exige somente o motivo quando o operador decide não autorizar no modo simplificado. */
  @Test
  void requiresReasonToRejectReviewAndAcceptDecision() {
    BusinessProcessDefinition process = process();
    BusinessProcessActivityDefinition activity = activity(process);
    HumanProductProcessActivityHandler handler = mock(HumanProductProcessActivityHandler.class);
    StandardHumanProductProcessActivityExecutor reviewExecutor =
        new StandardHumanProductProcessActivityExecutor(
            instances,
            predecessors,
            new ObjectMapper(),
            List.of(handler),
            Clock.fixed(NOW, ZoneOffset.UTC));
    when(handler.supports(process, activity)).thenReturn(true);
    when(handler.readiness(process, activity, Product.builder().id(9L).build(), "experiment:89"))
        .thenReturn(reviewAndAcceptReadiness());
    when(predecessors.readiness(process, activity, "experiment:89"))
        .thenReturn(new ProductProcessActivityPredecessorReadiness(true, "Predecessoras prontas."));

    assertThatThrownBy(
            () ->
                reviewExecutor.execute(
                    process,
                    activity,
                    Product.builder().id(9L).build(),
                    "experiment:89",
                    new ProductProcessActivityExecutionRequest(
                        "REJECT",
                        null,
                        null,
                        null,
                        "CONFIRM:pde-commercial-homologation-activation:authorization")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("motivo");
    verify(instances, never()).save(any());
  }

  /** Preserva a decisão bloqueada anterior ao registrar uma nova ocorrência aprovada. */
  @Test
  void preservesBlockedDecisionWhenRetrying() {
    BusinessProcessDefinition process = process();
    BusinessProcessActivityDefinition activity = activity(process);
    BusinessProcessActivityInstance blocked = new BusinessProcessActivityInstance();
    blocked.setOccurrenceNumber(1);
    blocked.setStatus("BLOCKED");
    when(predecessors.readiness(process, activity, "experiment:89"))
        .thenReturn(new ProductProcessActivityPredecessorReadiness(true, "Predecessoras prontas."));
    when(instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            590L, "experiment:89"))
        .thenReturn(Optional.of(blocked));

    executor.execute(
        process,
        activity,
        Product.builder().id(9L).build(),
        "experiment:89",
        decision("APPROVE", "Nova evidência confirmou a decisão comercial."));

    ArgumentCaptor<BusinessProcessActivityInstance> saved =
        ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    verify(instances).saveAndFlush(any(BusinessProcessActivityInstance.class));
    verify(instances).save(saved.capture());
    assertThat(saved.getValue()).isNotSameAs(blocked);
    assertThat(saved.getValue().getOccurrenceNumber()).isEqualTo(2);
    assertThat(blocked.getStatus()).isEqualTo("BLOCKED");
  }

  /** Bloqueia a ação quando a confirmação pertence a outra atividade. */
  @Test
  void rejectsMismatchedConfirmation() {
    BusinessProcessDefinition process = process();
    BusinessProcessActivityDefinition activity = activity(process);
    when(predecessors.readiness(process, activity, "experiment:89"))
        .thenReturn(new ProductProcessActivityPredecessorReadiness(true, "Predecessoras prontas."));

    assertThatThrownBy(
            () ->
                executor.execute(
                    process,
                    activity,
                    Product.builder().id(9L).build(),
                    "experiment:89",
                    new ProductProcessActivityExecutionRequest(
                        "APPROVE",
                        "Paulo Operador",
                        "Evidências suficientes para a decisão solicitada.",
                        "experiment-run:12",
                        "CONFIRM:outro-processo:outra-atividade")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("confirmação não corresponde");
    verify(instances, never()).save(any());
  }

  /** Impede decisão humana antes de a atividade predecessora atingir o objetivo. */
  @Test
  void blocksDecisionBeforePredecessorCompletion() {
    BusinessProcessDefinition process = process();
    BusinessProcessActivityDefinition activity = activity(process);
    when(predecessors.readiness(process, activity, "experiment:89"))
        .thenReturn(
            new ProductProcessActivityPredecessorReadiness(
                false, "Conclua primeiro a atividade Executar preflight técnico."));

    HumanProductProcessActivityReadiness readiness =
        executor.readiness(process, activity, Product.builder().id(9L).build(), "experiment:89");

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.reason()).contains("preflight");
    assertThat(readiness.requirements())
        .singleElement()
        .satisfies(
            requirement -> {
              assertThat(requirement.code()).isEqualTo("PREDECESSORS_COMPLETED");
              assertThat(requirement.satisfied()).isFalse();
            });
  }

  /** Rejeita uma segunda decisão quando a ocorrência atual já foi concluída. */
  @Test
  void rejectsDecisionAlreadyCompletedInCurrentCycle() {
    BusinessProcessDefinition process = process();
    BusinessProcessActivityDefinition activity = activity(process);
    BusinessProcessActivityInstance completed = new BusinessProcessActivityInstance();
    completed.setOccurrenceNumber(1);
    completed.setStatus("COMPLETED");
    completed.setObjectiveAchieved(true);
    when(predecessors.readiness(process, activity, "experiment:89"))
        .thenReturn(new ProductProcessActivityPredecessorReadiness(true, "Predecessoras prontas."));
    when(instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            590L, "experiment:89"))
        .thenReturn(Optional.of(completed));

    assertThatThrownBy(
            () ->
                executor.execute(
                    process,
                    activity,
                    Product.builder().id(9L).build(),
                    "experiment:89",
                    decision("APPROVE", "Evidências comerciais revisadas e aprovadas.")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("ativa ou concluída");
    verify(instances, never()).saveAndFlush(any());
  }

  /** Cria uma decisão válida para os cenários de retentativa. */
  private ProductProcessActivityExecutionRequest decision(String decision, String justification) {
    return new ProductProcessActivityExecutionRequest(
        decision,
        "Paulo Operador",
        justification,
        "experiment-run:12",
        "CONFIRM:pde-commercial-homologation-activation:authorization");
  }

  /** Monta a prontidão simplificada usada nos cenários de recusa segura. */
  private HumanProductProcessActivityReadiness reviewAndAcceptReadiness() {
    return new HumanProductProcessActivityReadiness(
        true,
        "Tudo pronto.",
        "Li, entendi e autorizo",
        "Revise e aceite.",
        "Revise e autorize",
        "O experimento está pronto, com amostra de 15 contatos e teto de R$ 540,00.",
        "CONFIRM:pde-commercial-homologation-activation:authorization",
        "EXPERIMENT_ACTIVATION",
        89L,
        List.of(),
        HumanProductProcessActivityReadiness.REVIEW_AND_ACCEPT,
        "experiment:89; experiment-run:9/run-number:2; commercial-plan:4");
  }

  /** Monta o processo publicado usado pela confirmação específica. */
  private BusinessProcessDefinition process() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(56L);
    process.setProcessCode("pde-commercial-homologation-activation");
    process.setStatus("PUBLISHED");
    return process;
  }

  /** Monta a atividade humana com identidade persistida. */
  private BusinessProcessActivityDefinition activity(BusinessProcessDefinition process) {
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setId(590L);
    activity.setProcessDefinition(process);
    activity.setActivityId("authorization");
    activity.setOwnerName("Operador humano");
    return activity;
  }
}
