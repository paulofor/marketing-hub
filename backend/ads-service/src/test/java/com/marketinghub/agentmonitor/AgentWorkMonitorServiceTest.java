package com.marketinghub.agentmonitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.codextelemetry.CodexAgentExecutionTelemetry;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.creative.CreativeImprovementStatus;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.opportunitydossier.OpportunityAgentReview;
import com.marketinghub.opportunitydossier.OpportunityDossier;
import com.marketinghub.opportunitydossier.OpportunityReviewExecutionStatus;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.codextelemetry.CodexAgentExecutionTelemetryRepository;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.repository.jpa.opportunitydossier.OpportunityAgentReviewRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository;
import com.marketinghub.salesvideo.VideoProductionCycle;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a leitura comum de paralelismo, bloqueios e decisões dos agentes. */
class AgentWorkMonitorServiceTest {
  /** Mantém o health saudável separado da falha canônica do parecer de Atena. */
  @Test
  void shouldExposeReadyExecutorWithBlockedOpportunityReview() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    GeraLandingStageExecutionRepository landings = mock(GeraLandingStageExecutionRepository.class);
    VideoProductionCycleRepository cycles = mock(VideoProductionCycleRepository.class);
    CodexAgentExecutionTelemetryRepository telemetry =
        mock(CodexAgentExecutionTelemetryRepository.class);
    CreativeRepository creatives = mock(CreativeRepository.class);
    AgentExecutorHealthService health = mock(AgentExecutorHealthService.class);
    OpportunityAgentReviewRepository reviews = mock(OpportunityAgentReviewRepository.class);
    Agent atena =
        Agent.builder()
            .id(10L)
            .agentKey("experiment-strategist")
            .nickname("Atena")
            .name("Estrategista")
            .currentVersion(4)
            .build();
    OpportunityAgentReview review =
        OpportunityAgentReview.builder()
            .id(7L)
            .dossier(OpportunityDossier.builder().id(6L).build())
            .agentKey("ATENA")
            .executionStatus(OpportunityReviewExecutionStatus.FAILED)
            .errorMessage("HTTP 500 ao consultar pendências")
            .updatedAt(Instant.parse("2026-08-13T16:48:00Z"))
            .build();
    when(agents.findAllByOrderByNicknameAsc()).thenReturn(List.of(atena));
    when(reviews.findTopByAgentKeyOrderByUpdatedAtDescIdDesc("ATENA"))
        .thenReturn(Optional.of(review));
    when(health.current(atena))
        .thenReturn(
            new AgentExecutorHealthResponse(
                "READY",
                4,
                4,
                true,
                true,
                true,
                "build",
                "Executor pronto.",
                review.getUpdatedAt()));

    AgentWorkMonitorResponse result =
        new AgentWorkMonitorService(
                agents, tasks, landings, cycles, telemetry, creatives, health, reviews)
            .list()
            .getFirst();

    assertThat(result.workStatus()).isEqualTo("BLOCKED");
    assertThat(result.currentWork()).isEqualTo("Parecer de Atena no dossiê #6");
    assertThat(result.executionId()).isEqualTo(7L);
    assertThat(result.difficulty()).isEqualTo("HTTP 500 ao consultar pendências");
    assertThat(result.executorHealth().status()).isEqualTo("READY");
  }

  /** Alerta Atena quando a decisão estratégica permanece pendente sem início. */
  @Test
  void shouldExposePendingWithoutStartForAtena() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    GeraLandingStageExecutionRepository landings = mock(GeraLandingStageExecutionRepository.class);
    VideoProductionCycleRepository cycles = mock(VideoProductionCycleRepository.class);
    CodexAgentExecutionTelemetryRepository telemetry =
        mock(CodexAgentExecutionTelemetryRepository.class);
    CreativeRepository creatives = mock(CreativeRepository.class);
    AgentExecutorHealthService health = mock(AgentExecutorHealthService.class);
    OpportunityAgentReviewRepository reviews = mock(OpportunityAgentReviewRepository.class);
    List<Agent> reviewers = List.of(reviewer(1L, "experiment-strategist", "Atena"));
    when(agents.findAllByOrderByNicknameAsc()).thenReturn(reviewers);
    for (Agent reviewer : reviewers) {
      OpportunityAgentReview review =
          OpportunityAgentReview.builder()
              .id(reviewer.getId() + 10)
              .dossier(OpportunityDossier.builder().id(6L).build())
              .agentKey("ATENA")
              .executionStatus(OpportunityReviewExecutionStatus.PENDING)
              .requestedAt(Instant.now().minusSeconds(240))
              .updatedAt(Instant.now().minusSeconds(240))
              .build();
      when(reviews.findTopByAgentKeyOrderByUpdatedAtDescIdDesc(review.getAgentKey()))
          .thenReturn(Optional.of(review));
      when(health.current(reviewer))
          .thenReturn(
              new AgentExecutorHealthResponse(
                  "READY", 1, 1, true, true, true, "build", "Executor pronto.", Instant.now()));
    }

    List<AgentWorkMonitorResponse> result =
        new AgentWorkMonitorService(
                agents, tasks, landings, cycles, telemetry, creatives, health, reviews)
            .list();

    assertThat(result).extracting(AgentWorkMonitorResponse::workStatus).containsOnly("BLOCKED");
    assertThat(result)
        .extracting(AgentWorkMonitorResponse::combinedStatus)
        .containsOnly("READY — parecer bloqueado");
    assertThat(result)
        .extracting(AgentWorkMonitorResponse::difficulty)
        .allMatch(message -> message.contains("pendente sem início"));
  }

  /** Cria um especialista mínimo para cenários transversais do monitor. */
  private Agent reviewer(Long id, String key, String nickname) {
    return Agent.builder()
        .id(id)
        .agentKey(key)
        .nickname(nickname)
        .name("Especialista")
        .currentVersion(1)
        .build();
  }

  /** Impede que bloqueio antigo de Argos prevaleça sobre a pesquisa mais recente concluída. */
  @Test
  void shouldIgnoreObsoleteBlockedTaskWhenLatestTaskIsCompleted() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    GeraLandingStageExecutionRepository landings = mock(GeraLandingStageExecutionRepository.class);
    VideoProductionCycleRepository cycles = mock(VideoProductionCycleRepository.class);
    CodexAgentExecutionTelemetryRepository telemetry =
        mock(CodexAgentExecutionTelemetryRepository.class);
    CreativeRepository creatives = mock(CreativeRepository.class);
    Agent argos =
        Agent.builder().id(5L).agentKey("market-radar").nickname("Argos").name("Radar").build();
    AgentTask completed = new AgentTask();
    completed.setId(20L);
    completed.setStatus("COMPLETED");
    AgentTask obsolete = new AgentTask();
    obsolete.setId(10L);
    obsolete.setStatus("BLOCKED");
    when(agents.findAllByOrderByNicknameAsc()).thenReturn(List.of(argos));
    when(tasks.findByAssignedAgentAgentKeyOrderByCreatedAtDescIdDesc("market-radar"))
        .thenReturn(List.of(completed, obsolete));

    AgentWorkMonitorResponse result =
        new AgentWorkMonitorService(agents, tasks, landings, cycles, telemetry, creatives)
            .list()
            .getFirst();

    assertThat(result.workStatus()).isEqualTo("IDLE");
    assertThat(result.currentWork()).isEqualTo("Sem trabalho ativo");
  }

  /** Comprova que Plutus decide enquanto Apolo aguarda o mesmo ciclo sem duplicar estado. */
  @Test
  void shouldExposeFinancialDecisionAndApolloWaitingFromSameCycle() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    GeraLandingStageExecutionRepository landings = mock(GeraLandingStageExecutionRepository.class);
    VideoProductionCycleRepository cycles = mock(VideoProductionCycleRepository.class);
    CodexAgentExecutionTelemetryRepository telemetry =
        mock(CodexAgentExecutionTelemetryRepository.class);
    CreativeRepository creatives = mock(CreativeRepository.class);
    Agent apolo =
        Agent.builder().id(8L).agentKey("videomaker").nickname("Apolo").name("Videomaker").build();
    Agent plutus =
        Agent.builder()
            .id(3L)
            .agentKey("financial-agent")
            .nickname("Plutus")
            .name("Financeiro")
            .build();
    VideoProductionCycle cycle = new VideoProductionCycle();
    cycle.setId(21L);
    cycle.setStatus("PENDING_FINANCIAL_REVIEW");
    cycle.setBudgetLimitUsd(new BigDecimal("40.00"));
    cycle.setKnownCostUsd(BigDecimal.ZERO);
    cycle.setUpdatedAt(Instant.parse("2026-08-11T04:00:00Z"));
    when(agents.findAllByOrderByNicknameAsc()).thenReturn(List.of(apolo, plutus));
    when(cycles.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.of(cycle));
    AgentWorkMonitorService service =
        new AgentWorkMonitorService(agents, tasks, landings, cycles, telemetry, creatives);

    List<AgentWorkMonitorResponse> result = service.list();

    assertThat(result)
        .extracting(AgentWorkMonitorResponse::workStatus)
        .containsExactly("WAITING", "DECISION_REQUIRED");
    assertThat(result.get(1).externalDecisionRequired()).isTrue();
    assertThat(result.get(1).sourceReference()).isEqualTo("video-production-cycle:21");
  }

  /** Impede que um ciclo bloqueado de Apolo seja apresentado como geração de vídeo ativa. */
  @Test
  void shouldExposeApolloBlockedCycleAsBlocked() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    GeraLandingStageExecutionRepository landings = mock(GeraLandingStageExecutionRepository.class);
    VideoProductionCycleRepository cycles = mock(VideoProductionCycleRepository.class);
    CodexAgentExecutionTelemetryRepository telemetry =
        mock(CodexAgentExecutionTelemetryRepository.class);
    CreativeRepository creatives = mock(CreativeRepository.class);
    Agent apolo =
        Agent.builder().id(8L).agentKey("videomaker").nickname("Apolo").name("Videomaker").build();
    VideoProductionCycle cycle = new VideoProductionCycle();
    cycle.setId(6L);
    cycle.setStatus("APOLLO_BLOCKED");
    cycle.setBudgetLimitUsd(new BigDecimal("10.00"));
    cycle.setKnownCostUsd(BigDecimal.ZERO);
    cycle.setUpdatedAt(Instant.parse("2026-08-14T00:36:47Z"));
    when(agents.findAllByOrderByNicknameAsc()).thenReturn(List.of(apolo));
    when(cycles.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.of(cycle));
    AgentWorkMonitorService service =
        new AgentWorkMonitorService(agents, tasks, landings, cycles, telemetry, creatives);

    AgentWorkMonitorResponse result = service.list().getFirst();

    assertThat(result.workStatus()).isEqualTo("BLOCKED");
    assertThat(result.difficulty()).isEqualTo("Ciclo bloqueado.");
  }

  /** Impede que a falha canônica de Dédalo continue aparecendo como trabalho ativo. */
  @Test
  void shouldExposeDedaloFailureAsBlocked() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    GeraLandingStageExecutionRepository landings = mock(GeraLandingStageExecutionRepository.class);
    VideoProductionCycleRepository cycles = mock(VideoProductionCycleRepository.class);
    CodexAgentExecutionTelemetryRepository telemetry =
        mock(CodexAgentExecutionTelemetryRepository.class);
    CreativeRepository creatives = mock(CreativeRepository.class);
    Agent dedalo =
        Agent.builder()
            .id(7L)
            .agentKey("landing-generator")
            .nickname("Dédalo")
            .name("Landing")
            .build();
    GeraLandingStageExecution execution =
        GeraLandingStageExecution.builder()
            .experimentId(88L)
            .stageCode("landing-generation-agent-v1")
            .status("FALHA")
            .errorMessage("Timeout registrado")
            .completedAt(Instant.parse("2026-08-11T04:39:00Z"))
            .build();
    when(agents.findAllByOrderByNicknameAsc()).thenReturn(List.of(dedalo));
    when(landings.findTopByStageCodeOrderByExecutionRequestedAtDesc("landing-generation-agent-v1"))
        .thenReturn(Optional.of(execution));

    AgentWorkMonitorResponse result =
        new AgentWorkMonitorService(agents, tasks, landings, cycles, telemetry, creatives)
            .list()
            .getFirst();

    assertThat(result.workStatus()).isEqualTo("BLOCKED");
    assertThat(result.difficulty()).isEqualTo("Timeout registrado");
  }

  /** Impede que uma landing histórica esconda a atividade atual de produto atribuída a Dédalo. */
  @Test
  void shouldPrioritizeCurrentDedaloProductTaskOverHistoricalLanding() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    GeraLandingStageExecutionRepository landings = mock(GeraLandingStageExecutionRepository.class);
    VideoProductionCycleRepository cycles = mock(VideoProductionCycleRepository.class);
    CodexAgentExecutionTelemetryRepository telemetry =
        mock(CodexAgentExecutionTelemetryRepository.class);
    CreativeRepository creatives = mock(CreativeRepository.class);
    Agent dedalo =
        Agent.builder()
            .id(7L)
            .agentKey("landing-generator")
            .nickname("Dédalo")
            .name("Construtor do produto")
            .build();
    AgentTask productTask = new AgentTask();
    productTask.setId(271L);
    productTask.setStatus("IN_PROGRESS");
    productTask.setTitle("Materializar a experiência de degustação");
    productTask.setDescription("Construção funcional do valor antes da compra.");
    productTask.setSourceReference("product:88");
    productTask.setUpdatedAt(Instant.parse("2026-08-30T12:00:00Z"));
    GeraLandingStageExecution historicalLanding =
        GeraLandingStageExecution.builder()
            .experimentId(87L)
            .stageCode("landing-generation-agent-v1")
            .status("COMPLETED")
            .completedAt(Instant.parse("2026-08-29T12:00:00Z"))
            .build();
    when(agents.findAllByOrderByNicknameAsc()).thenReturn(List.of(dedalo));
    when(tasks.findByAssignedAgentAgentKeyOrderByCreatedAtDescIdDesc("landing-generator"))
        .thenReturn(List.of(productTask));
    when(landings.findTopByStageCodeOrderByExecutionRequestedAtDesc("landing-generation-agent-v1"))
        .thenReturn(Optional.of(historicalLanding));

    AgentWorkMonitorResponse result =
        new AgentWorkMonitorService(agents, tasks, landings, cycles, telemetry, creatives)
            .list()
            .getFirst();

    assertThat(result.workStatus()).isEqualTo("WORKING");
    assertThat(result.currentWork()).isEqualTo("Materializar a experiência de degustação");
    assertThat(result.taskId()).isEqualTo(271L);
    assertThat(result.sourceReference()).isEqualTo("product:88");
  }

  /** Traduz falha de autenticação em orientação operacional sem expor o stack trace. */
  @Test
  void shouldExposeAuthenticationFailureAsShortActionableBlocker() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    GeraLandingStageExecutionRepository landings = mock(GeraLandingStageExecutionRepository.class);
    VideoProductionCycleRepository cycles = mock(VideoProductionCycleRepository.class);
    CodexAgentExecutionTelemetryRepository telemetry =
        mock(CodexAgentExecutionTelemetryRepository.class);
    CreativeRepository creatives = mock(CreativeRepository.class);
    Agent dedalo =
        Agent.builder()
            .id(7L)
            .agentKey("landing-generator")
            .nickname("Dédalo")
            .name("Landing")
            .build();
    GeraLandingStageExecution execution =
        GeraLandingStageExecution.builder()
            .experimentId(88L)
            .stageCode("landing-generation-agent-v1")
            .status("FAILED")
            .errorMessage(
                "HTTP 401: refresh_token_reused\n"
                    + "java.lang.IllegalStateException: credencial recusada\n"
                    + "\tat com.marketinghub.worker.Executor.run(Executor.java:42)")
            .completedAt(Instant.parse("2026-08-12T14:00:00Z"))
            .build();
    when(agents.findAllByOrderByNicknameAsc()).thenReturn(List.of(dedalo));
    when(landings.findTopByStageCodeOrderByExecutionRequestedAtDesc("landing-generation-agent-v1"))
        .thenReturn(Optional.of(execution));

    AgentWorkMonitorResponse result =
        new AgentWorkMonitorService(agents, tasks, landings, cycles, telemetry, creatives)
            .list()
            .getFirst();

    assertThat(result.workStatus()).isEqualTo("BLOCKED");
    assertThat(result.difficulty())
        .isEqualTo(
            "Autenticação do executor inválida ou expirada. Reconecte a credencial usada por Dédalo e retome a tarefa.")
        .doesNotContain("refresh_token_reused", "java.lang", "Executor.java");
  }

  /** Resume falha técnica desconhecida na primeira linha para proteger a tela de stack traces. */
  @Test
  void shouldHideStackTraceFromUnknownOperationalFailure() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    GeraLandingStageExecutionRepository landings = mock(GeraLandingStageExecutionRepository.class);
    VideoProductionCycleRepository cycles = mock(VideoProductionCycleRepository.class);
    CodexAgentExecutionTelemetryRepository telemetry =
        mock(CodexAgentExecutionTelemetryRepository.class);
    CreativeRepository creatives = mock(CreativeRepository.class);
    Agent dedalo =
        Agent.builder()
            .id(7L)
            .agentKey("landing-generator")
            .nickname("Dédalo")
            .name("Landing")
            .build();
    GeraLandingStageExecution execution =
        GeraLandingStageExecution.builder()
            .experimentId(88L)
            .stageCode("landing-generation-agent-v1")
            .status("FAILED")
            .errorMessage(
                "Provider indisponível temporariamente\n\tat com.example.Client.call(Client.java:20)")
            .completedAt(Instant.parse("2026-08-12T14:00:00Z"))
            .build();
    when(agents.findAllByOrderByNicknameAsc()).thenReturn(List.of(dedalo));
    when(landings.findTopByStageCodeOrderByExecutionRequestedAtDesc("landing-generation-agent-v1"))
        .thenReturn(Optional.of(execution));

    AgentWorkMonitorResponse result =
        new AgentWorkMonitorService(agents, tasks, landings, cycles, telemetry, creatives)
            .list()
            .getFirst();

    assertThat(result.difficulty()).isEqualTo("Provider indisponível temporariamente");
  }

  /** Soma entrada e saída do dia e associa a telemetria à identidade canônica do agente. */
  @Test
  void shouldExposeDailyTokensReportedByAgentTelemetry() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    GeraLandingStageExecutionRepository landings = mock(GeraLandingStageExecutionRepository.class);
    VideoProductionCycleRepository cycles = mock(VideoProductionCycleRepository.class);
    CodexAgentExecutionTelemetryRepository telemetry =
        mock(CodexAgentExecutionTelemetryRepository.class);
    CreativeRepository creatives = mock(CreativeRepository.class);
    Agent temis =
        Agent.builder()
            .id(9L)
            .agentKey("meta-ad-approver")
            .nickname("Têmis")
            .name("Revisora de integridade comercial")
            .build();
    when(agents.findAllByOrderByNicknameAsc()).thenReturn(List.of(temis));
    when(telemetry.sumTokensByAgentTypeBetween(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.<Object[]>of(new Object[] {"META_AD_APPROVER", 12_345L}));

    AgentWorkMonitorResponse result =
        new AgentWorkMonitorService(agents, tasks, landings, cycles, telemetry, creatives)
            .list()
            .getFirst();

    assertThat(result.dailyTokens()).isEqualTo(12_345L);
    assertThat(result.dailyTokenDate()).isNotNull();
  }

  /** Expõe o heartbeat real de Dédalo para diferenciar trabalho ativo de estado nominal. */
  @Test
  void shouldExposeLatestExecutionActivityForAgent() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    GeraLandingStageExecutionRepository landings = mock(GeraLandingStageExecutionRepository.class);
    VideoProductionCycleRepository cycles = mock(VideoProductionCycleRepository.class);
    CodexAgentExecutionTelemetryRepository telemetry =
        mock(CodexAgentExecutionTelemetryRepository.class);
    CreativeRepository creatives = mock(CreativeRepository.class);
    Agent dedalo =
        Agent.builder()
            .id(7L)
            .agentKey("landing-generator")
            .nickname("Dédalo")
            .name("Landing")
            .build();
    GeraLandingStageExecution landing =
        GeraLandingStageExecution.builder()
            .experimentId(88L)
            .stageCode("landing-generation-agent-v1")
            .status("PROCESSANDO")
            .processingStartedAt(Instant.parse("2026-08-15T13:27:56Z"))
            .build();
    CodexAgentExecutionTelemetry signal = new CodexAgentExecutionTelemetry();
    signal.setAgentType("LANDING_GENERATOR");
    signal.setStatus("RUNNING");
    signal.setProcessAlive(true);
    signal.setEventCount(18L);
    signal.setOutputBytes(4096L);
    signal.setInputTokens(1200L);
    signal.setOutputTokens(350L);
    signal.setLastEventType("OUTPUT");
    signal.setStartedAt(Instant.parse("2026-08-15T13:27:56Z"));
    signal.setLastActivityAt(Instant.now());
    when(agents.findAllByOrderByNicknameAsc()).thenReturn(List.of(dedalo));
    when(landings.findTopByStageCodeOrderByExecutionRequestedAtDesc("landing-generation-agent-v1"))
        .thenReturn(Optional.of(landing));
    when(telemetry.findTopByAgentTypeOrderByUpdatedAtDescIdDesc("LANDING_GENERATOR"))
        .thenReturn(Optional.of(signal));

    AgentWorkMonitorResponse result =
        new AgentWorkMonitorService(agents, tasks, landings, cycles, telemetry, creatives)
            .list()
            .getFirst();

    assertThat(result.executionActivity()).isNotNull();
    assertThat(result.executionActivity().processAlive()).isTrue();
    assertThat(result.executionActivity().eventCount()).isEqualTo(18L);
    assertThat(result.executionActivity().outputBytes()).isEqualTo(4096L);
    assertThat(result.executionActivity().inputTokens()).isEqualTo(1200L);
    assertThat(result.executionActivity().outputTokens()).isEqualTo(350L);
    assertThat(result.executionActivity().stale()).isFalse();
  }

  /** Mostra a revisão real de Têmis em vez do bloqueio antigo da tarefa agregadora. */
  @Test
  void shouldExposeCurrentTemisReviewAndTaskIdentifiers() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    GeraLandingStageExecutionRepository landings = mock(GeraLandingStageExecutionRepository.class);
    VideoProductionCycleRepository cycles = mock(VideoProductionCycleRepository.class);
    CodexAgentExecutionTelemetryRepository telemetry =
        mock(CodexAgentExecutionTelemetryRepository.class);
    CreativeRepository creatives = mock(CreativeRepository.class);
    Agent temis =
        Agent.builder()
            .id(9L)
            .agentKey("meta-ad-approver")
            .nickname("Têmis")
            .name("Revisora de integridade comercial")
            .build();
    AgentTask task = new AgentTask();
    task.setId(14L);
    task.setAssignedAgent(temis);
    task.setTitle("Finalizar anúncios do experimento 88");
    task.setStatus("BLOCKED");
    task.setSourceReference("experiment:88");
    task.setUpdatedAt(Instant.parse("2026-08-12T10:00:00Z"));
    Creative creative =
        Creative.builder()
            .id(326L)
            .experiment(Experiment.builder().id(88L).build())
            .agentReviewStatus(CreativeAgentReviewStatus.PROCESSING)
            .agentImprovementStatus(CreativeImprovementStatus.COMPLETED)
            .build();
    when(agents.findAllByOrderByNicknameAsc()).thenReturn(List.of(temis));
    when(tasks.findByAssignedAgentAgentKeyOrderByCreatedAtDescIdDesc("meta-ad-approver"))
        .thenReturn(List.of(task));
    when(creatives.findTemisOpenReviews(88L)).thenReturn(List.of(creative));

    AgentWorkMonitorResponse result =
        new AgentWorkMonitorService(agents, tasks, landings, cycles, telemetry, creatives)
            .list()
            .getFirst();

    assertThat(result.workStatus()).isEqualTo("WORKING");
    assertThat(result.currentWork()).contains("Tarefa #14");
    assertThat(result.progressDetail()).contains("criativo #326");
    assertThat(result.taskId()).isEqualTo(14L);
    assertThat(result.executionId()).isEqualTo(326L);
    assertThat(result.difficulty()).isNull();
  }

  /** Atribui materialização visual aberta a Íris sem contaminar Dédalo ou Têmis. */
  @Test
  void shouldExposeCurrentVisualMaterializationForIris() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    GeraLandingStageExecutionRepository landings = mock(GeraLandingStageExecutionRepository.class);
    VideoProductionCycleRepository cycles = mock(VideoProductionCycleRepository.class);
    CodexAgentExecutionTelemetryRepository telemetry =
        mock(CodexAgentExecutionTelemetryRepository.class);
    CreativeRepository creatives = mock(CreativeRepository.class);
    Agent iris =
        Agent.builder()
            .id(9L)
            .agentKey("communication-director")
            .nickname("Íris")
            .name("Diretora de Comunicação")
            .build();
    Creative creative =
        Creative.builder()
            .id(326L)
            .experiment(Experiment.builder().id(88L).build())
            .agentReviewedAt(Instant.parse("2026-08-28T10:00:00Z"))
            .agentImprovementStatus(CreativeImprovementStatus.PROCESSING)
            .build();
    when(agents.findAllByOrderByNicknameAsc()).thenReturn(List.of(iris));
    when(creatives.findIrisOpenMaterializations()).thenReturn(List.of(creative));

    AgentWorkMonitorResponse result =
        new AgentWorkMonitorService(agents, tasks, landings, cycles, telemetry, creatives)
            .list()
            .getFirst();

    assertThat(result.agentKey()).isEqualTo("communication-director");
    assertThat(result.workStatus()).isEqualTo("WORKING");
    assertThat(result.currentWork()).contains("Materialização visual", "#326");
    assertThat(result.progressDetail()).contains("Produzindo");
    assertThat(result.sourceReference()).isEqualTo("experiment:88");
    assertThat(result.executionId()).isEqualTo(326L);
  }
}
