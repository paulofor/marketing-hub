package com.marketinghub.businessprocess.automation.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.repository.jpa.salesvideo.*;
import com.marketinghub.salesvideo.*;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: impedir espera enganosa e reaproveitamento de bloqueio de outra peça. */
class ProcessRunVideoGuidanceTest {
  private final VideoProductionCycleRepository productions =
      mock(VideoProductionCycleRepository.class);
  private final VideoProviderPreflightRepository preflights =
      mock(VideoProviderPreflightRepository.class);
  private final ProcessRunVideoGuidance guidance =
      new ProcessRunVideoGuidance(productions, preflights);
  private final LearningSalesCycle cycle = new LearningSalesCycle();
  private final VideoProductionCycle production = new VideoProductionCycle();
  private final VideoProviderPreflight preflight = new VideoProviderPreflight();
  private final Instant changedAt = Instant.parse("2026-09-12T08:00:00Z");

  /** Monta somente dados sintéticos correlacionados com o contrato da v12. */
  @BeforeEach
  void setup() {
    cycle.setProductId(4L);
    cycle.setExperimentId(92L);
    cycle.setProductVersion("musa-v12");
    cycle.setVersionChangedAt(changedAt);
    cycle.setStage("CAMPAIGN_VIDEO");
    production.setId(12L);
    production.setVideoProjectId(4L);
    production.setStatus("PROVIDER_PREFLIGHT_ONLY_BLOCKED");
    preflight.setId(12L);
    preflight.setStatus("BLOCKED");
    preflight.setFailureCode("PROVIDER_ROUTER_CONFIG_MISSING");
    when(productions.findLatestForLearningCycle(
            4L, 92L, "musa-v12", "CAMPAIGN_QUALIFICATION", changedAt))
        .thenReturn(Optional.of(production));
    when(preflights.findByVideoProductionCycleId(12L)).thenReturn(Optional.of(preflight));
  }

  /** Usa o bloqueio oficial, sem incluir payload bruto ou repetir tarefa paga. */
  @Test
  void exposesMissingConfigurationWithProjectAndEvidence() {
    var action = guidance.resolve(cycle);
    assertThat(action.code()).isEqualTo("RESOLVE_VIDEO_PREFLIGHT");
    assertThat(action.title()).contains("anúncio", "bloqueada");
    assertThat(action.reason()).contains("configuração", "não está acessível");
    assertThat(action.actionUrl()).isEqualTo("/audio-video-studio/projects/4");
    assertThat(action.evidenceReference()).contains("/cycles/12/provider-preflight/12");
    verify(productions, never()).save(any());
    verify(preflights, never()).save(any());
  }

  /** Diferencia uma falha de integração de outras restrições do preflight. */
  @Test
  void otherBlockersKeepTheirAuditAccessible() {
    preflight.setFailureCode("PROVIDER_AUTH_ERROR");
    assertThat(guidance.resolve(cycle).reason()).contains("motivo registrado no projeto");
  }

  /** A etapa da demonstração nunca reutiliza o impedimento do anúncio. */
  @Test
  void entryVideoUsesItsOwnRole() {
    cycle.setStage("PDE_ENTRY_VIDEO");
    assertThat(guidance.resolve(cycle)).isNull();
    verify(productions)
        .findLatestForLearningCycle(4L, 92L, "musa-v12", "PDE_HERO_CONVERSION", changedAt);
    verifyNoInteractions(preflights);
  }

  /** A interrupção de Apolo independe do snapshot vencido e mantém o job como evidência. */
  @Test
  void productionFailureIsVisibleWithoutReusingPreflightFailure() {
    cycle.setStage("PDE_ENTRY_VIDEO");
    production.setVideoProjectId(5L);
    production.setStatus("APOLLO_BLOCKED");
    production.setLastFailedJobId(21240L);
    production.setLastApolloFailureCode("APOLLO_STORYBOARD_BLOCKED");
    production.setLastApolloFailureDetail("Payload bruto privado não deve aparecer no card");
    preflight.setStatus("EXPIRED");
    when(productions.findLatestForLearningCycle(
            4L, 92L, "musa-v12", "PDE_HERO_CONVERSION", changedAt))
        .thenReturn(Optional.of(production));
    var action = guidance.resolve(cycle);
    assertThat(action.code()).isEqualTo("RESOLVE_VIDEO_PRODUCTION");
    assertThat(action.title()).contains("demonstração", "interrompida");
    assertThat(action.reason()).contains("planejamento", "corrigir").doesNotContain("Payload");
    assertThat(action.actionUrl()).isEqualTo("/audio-video-studio/projects/5");
    assertThat(action.evidenceReference()).endsWith("/cycles/12/jobs/21240");
    assertThat(action.afterAction()).contains("reaproveite", "Plutus", "aprovação humana");
    verifyNoInteractions(preflights);
    verify(productions, never()).save(any());
  }

  /** Bloqueio anterior ao job usa somente a evidência de ciclo existente. */
  @Test
  void productionBlockBeforeJobDoesNotInventAnExecution() {
    production.setStatus("APOLLO_BLOCKED");
    var action = guidance.resolve(cycle);
    assertThat(action.evidenceReference()).endsWith("/cycles/12");
    assertThat(action.reason()).contains("falha registrada");
    verifyNoInteractions(preflights);
  }

  /** O monitor financeiro define o responsável sem oferecer recarga ou consumo automáticos. */
  @Test
  void financialProductionBlockKeepsItsOwnerAndKnownJob() {
    production.setStatus("APOLLO_BLOCKED");
    production.setBudgetMonitorStatus("BLOCKED");
    production.setSalesVideoJobId(900L);
    var action = guidance.resolve(cycle);
    assertThat(action.responsible()).contains("financeiro", "Plutus");
    assertThat(action.reason()).contains("saldo", "consumo", "teto");
    assertThat(action.evidenceReference()).endsWith("/jobs/900");
    verifyNoInteractions(preflights);
  }

  /** Parecer financeiro reprovado não pode parecer produção em andamento. */
  @Test
  void rejectedFinancialReviewIsVisibleBeforeAnyJob() {
    production.setStatus("FINANCIAL_BLOCKED");
    var action = guidance.resolve(cycle);
    assertThat(action.responsible()).contains("Plutus");
    assertThat(action.reason()).contains("controle financeiro");
    assertThat(action.evidenceReference()).endsWith("/cycles/12");
    verifyNoInteractions(preflights);
  }

  /** Falha da integração de IA preserva seu diagnóstico sem culpabilizar o storyboard. */
  @Test
  void planningIntegrationFailureHasActionableGuidance() {
    production.setStatus("APOLLO_BLOCKED");
    production.setLastApolloFailureCode("APOLLO_PLANNING_ACCOUNT_BLOCKED");
    assertThat(guidance.resolve(cycle).reason()).contains("resposta auditada", "quota");
    verifyNoInteractions(preflights);
  }

  /** A tentativa nova em curso ou com vídeo prevalece sobre o bloqueio histórico. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "PENDING_PROVIDER_PREFLIGHT_ONLY",
        "PENDING_FINANCIAL_REVIEW",
        "APOLLO_QUEUED",
        "QUEUED_FOR_APOLLO",
        "REUSING_APOLLO_MATERIAL",
        "VIDEO_READY_FOR_REVIEW"
      })
  void doesNotCallActiveOrFinishedWorkAHumanBlocker(String status) {
    production.setStatus(status);
    production.setLastFailedJobId(21240L);
    production.setLastApolloFailureCode("APOLLO_STORYBOARD_BLOCKED");
    assertThat(guidance.resolve(cycle)).isNull();
    verifyNoInteractions(preflights);
  }

  /** Consulta encerrada exige outro comando mesmo quando seu snapshot já venceu. */
  @ParameterizedTest
  @ValueSource(strings = {"READY", "EXPIRED"})
  void completedDryRunExplainsProductionRequestWithoutAuthorizingSpend(String status) {
    production.setStatus("PROVIDER_PREFLIGHT_ONLY_COMPLETED");
    preflight.setStatus(status);
    preflight.setFailureCode(null);
    var action = guidance.resolve(cycle);
    assertThat(action).isNotNull();
    assertThat(action.code()).isEqualTo("REQUEST_VIDEO_PRODUCTION");
    assertThat(action.title()).contains("anúncio", "produção");
    assertThat(action.reason()).contains("consulta", "Plutus", "não iniciou");
    assertThat(action.actionUrl()).isEqualTo("/audio-video-studio/projects/4");
    assertThat(action.afterAction()).contains("novo preflight", "aprovações");
    assertThat(action.evidenceReference()).endsWith("/cycles/12/provider-preflight/12");
    verify(productions, never()).save(any());
    verify(preflights, never()).save(any());
  }

  /** Histórico inconsistente não pode sugerir duplicação de uma tarefa ou de um job. */
  @Test
  void completedDryRunWithExecutionDoesNotSuggestAnotherRequest() {
    production.setStatus("PROVIDER_PREFLIGHT_ONLY_COMPLETED");
    preflight.setStatus("READY");
    production.setAgentTaskId(900L);
    assertThat(guidance.resolve(cycle)).isNull();
    production.setAgentTaskId(null);
    production.setSalesVideoJobId(901L);
    assertThat(guidance.resolve(cycle)).isNull();
  }

  /** Falta de auditoria ou preflight liberado não é prova de bloqueio. */
  @Test
  void missingOrReadyPreflightDoesNotInventFailure() {
    when(preflights.findByVideoProductionCycleId(12L)).thenReturn(Optional.empty());
    assertThat(guidance.resolve(cycle)).isNull();
    when(preflights.findByVideoProductionCycleId(12L)).thenReturn(Optional.of(preflight));
    preflight.setStatus("READY");
    assertThat(guidance.resolve(cycle)).isNull();
  }

  /** Sem marco de versão não há identidade temporal suficiente para usar a tentativa. */
  @Test
  void missingVersionTimestampDoesNotUseHistory() {
    cycle.setVersionChangedAt(null);
    assertThat(guidance.resolve(cycle)).isNull();
    verifyNoInteractions(productions, preflights);
  }
}
