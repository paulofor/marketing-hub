package com.marketinghub.salesvideo.autonomy.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskResponse;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.CreateAgentTaskByAgentRequest;
import com.marketinghub.financialagent.service.FinancialAgentService;
import com.marketinghub.financialagent.service.StudioCostLedgerService;
import com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.VideoProductionCycle;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.salesvideo.dto.RequestVideoRenderRequest;
import com.marketinghub.salesvideo.dto.SalesVideoJobDto;
import com.marketinghub.salesvideo.service.SalesVideoService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: proteger o gate financeiro entre Plutus e Apolo. */
@ExtendWith(MockitoExtension.class)
class VideoProductionCycleServiceTest {
  @Mock private VideoProductionCycleRepository repository;
  @Mock private VideoProjectRepository projectRepository;
  @Mock private AgentTaskService taskService;
  @Mock private SalesVideoService salesVideoService;
  @Mock private FinancialAgentService financialAgentService;
  @Mock private StudioCostLedgerService studioCostLedgerService;
  private VideoProductionCycleService service;
  private final AtomicLong ids = new AtomicLong(10);

  /** Prepara persistência simulada sem consumir qualquer provider real. */
  @BeforeEach
  void setUp() {
    service =
        new VideoProductionCycleService(
            repository,
            projectRepository,
            taskService,
            salesVideoService,
            financialAgentService,
            studioCostLedgerService,
            new ObjectMapper());
    lenient()
        .when(studioCostLedgerService.cycleLedger(any()))
        .thenReturn(java.util.Map.of("segregated", true));
    lenient()
        .when(financialAgentService.intelligence(any()))
        .thenReturn(java.util.Map.of("coverage", "COMPLETE"));
    lenient()
        .when(repository.save(any(VideoProductionCycle.class)))
        .thenAnswer(
            invocation -> {
              VideoProductionCycle cycle = invocation.getArgument(0);
              if (cycle.getId() == null) cycle.setId(ids.incrementAndGet());
              return cycle;
            });
  }

  /** Comprova que a abertura cria uma tarefa para Plutus e não cria render. */
  @Test
  void shouldOpenFinancialGateBeforeAnyProviderJob() {
    when(projectRepository.findById(7L)).thenReturn(Optional.of(project()));
    when(taskService.createGateByAgent(any(), any()))
        .thenReturn(
            new AgentTaskResponse(
                99L,
                4L,
                "financial-agent",
                "Plutus",
                "AGENT",
                8L,
                "videomaker",
                "Apolo",
                "Avaliar",
                "Ciclo",
                "HIGH",
                "PENDING",
                "cycle",
                "GATE_DECISION",
                "VIDEO_BUDGET_APPROVAL",
                "PENDING",
                null,
                null,
                Instant.now(),
                Instant.now()));

    var result =
        service.create(
            new VideoProductionCycleContracts.CreateRequest(
                7L, new BigDecimal("12.50"), "Validar gancho", "Retencao superior", "usuario@mkt"));

    assertThat(result.status()).isEqualTo("PENDING_FINANCIAL_REVIEW");
    assertThat(result.learningObjective()).isEqualTo("Validar gancho");
    assertThat(result.successCriterion()).isEqualTo("Retencao superior");
    assertThat(result.financialSnapshot()).contains("incrementalLedger", "segregated");
    assertThat(result.salesVideoJobId()).isNull();
    ArgumentCaptor<CreateAgentTaskByAgentRequest> task =
        ArgumentCaptor.forClass(CreateAgentTaskByAgentRequest.class);
    verify(taskService)
        .createGateByAgent(
            task.capture(), org.mockito.ArgumentMatchers.eq("VIDEO_BUDGET_APPROVAL"));
    assertThat(task.getValue().requestedByAgentKey()).isEqualTo("videomaker");
    assertThat(task.getValue().assignedAgentKey()).isEqualTo("financial-agent");
    verify(salesVideoService, never()).requestRender(any(), any());
  }

  /** Comprova que um projeto legado usa custos não atribuídos sem falhar nem inventar plano. */
  @Test
  void shouldOpenLegacyProjectWithUnassignedFinancialSnapshot() {
    VideoProject legacy = project();
    legacy.setCommercialPlanId(null);
    when(projectRepository.findById(7L)).thenReturn(Optional.of(legacy));
    when(financialAgentService.unassignedStudioIntelligence(76L))
        .thenReturn(java.util.Map.of("coverage", "PARTIAL"));
    when(taskService.createGateByAgent(any(), any()))
        .thenReturn(
            new AgentTaskResponse(
                100L,
                3L,
                "financial-agent",
                "Plutus",
                "AGENT",
                8L,
                "videomaker",
                "Apolo",
                "Avaliar",
                "Ciclo",
                "HIGH",
                "PENDING",
                "cycle",
                "GATE_DECISION",
                "VIDEO_BUDGET_APPROVAL",
                "PENDING",
                null,
                null,
                Instant.now(),
                Instant.now()));

    var result =
        service.create(
            new VideoProductionCycleContracts.CreateRequest(
                7L,
                new BigDecimal("40.00"),
                "Validar prova",
                "Prova compreensivel",
                "usuario@mkt"));

    assertThat(result.commercialPlanId()).isNull();
    assertThat(result.financialSnapshot()).contains("PARTIAL");
    verify(financialAgentService).unassignedStudioIntelligence(76L);
    verify(salesVideoService, never()).requestRender(any(), any());
  }

  /** Comprova que somente a identidade técnica de Plutus decide o gate. */
  @Test
  void shouldRejectDecisionFromAnotherAgent() {
    assertThatThrownBy(
            () ->
                service.decide(
                    11L,
                    new VideoProductionCycleContracts.FinancialDecisionRequest(
                        "APPROVED", "parecer", "videomaker")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("403");
    verify(salesVideoService, never()).requestRender(any(), any());
  }

  /** Comprova que uma reprovação financeira termina sem gerar vídeo. */
  @Test
  void shouldBlockRejectedCycleWithoutProviderJob() {
    VideoProductionCycle cycle = cycle();
    when(repository.findById(11L)).thenReturn(Optional.of(cycle));

    var result =
        service.decide(
            11L,
            new VideoProductionCycleContracts.FinancialDecisionRequest(
                "REJECTED", "Custo acima do limite aprovado.", "financial-agent"));

    assertThat(result.status()).isEqualTo("FINANCIAL_BLOCKED");
    assertThat(result.knownCostUsd()).isEqualByComparingTo(BigDecimal.ZERO);
    verify(salesVideoService, never()).requestRender(any(), any());
  }

  /** Comprova que Apolo não volta à Luma mesmo quando um plano legado ainda a menciona. */
  @Test
  void shouldQueueApprovedCycleWithSeedanceInsteadOfLegacyLuma() {
    VideoProductionCycle cycle = cycle();
    VideoProject project = project();
    project.setTargetDurationSeconds(60);
    project.setProviderPlan("LUMA_RAY_3_2 como principal no plano legado.");
    SalesVideoJobDto job = new SalesVideoJobDto();
    job.setId(321L);
    when(repository.findById(11L)).thenReturn(Optional.of(cycle));
    when(projectRepository.findById(7L)).thenReturn(Optional.of(project));
    when(salesVideoService.requestRender(any(), any())).thenReturn(job);

    var result =
        service.decide(
            11L,
            new VideoProductionCycleContracts.FinancialDecisionRequest(
                "APPROVED", "Teto e ledger incremental válidos.", "financial-agent"));

    ArgumentCaptor<RequestVideoRenderRequest> render =
        ArgumentCaptor.forClass(RequestVideoRenderRequest.class);
    verify(salesVideoService).requestRender(org.mockito.ArgumentMatchers.eq(13L), render.capture());
    assertThat(render.getValue().getProviderName()).isEqualTo("RUNWAY_SEEDANCE_2_5");
    assertThat(render.getValue().getTargetDurationSeconds()).isEqualTo(60);
    assertThat(result.status()).isEqualTo("QUEUED_FOR_APOLLO");
    assertThat(result.salesVideoJobId()).isEqualTo(321L);
  }

  /** Cria o projeto mínimo de teste com perfil operacional. */
  private VideoProject project() {
    return VideoProject.builder()
        .id(7L)
        .productId(76L)
        .commercialPlanId(5L)
        .experimentId(88L)
        .salesVideoProfileId(13L)
        .title("MUSA v7")
        .targetDurationSeconds(15)
        .build();
  }

  /** Cria um ciclo pendente sem qualquer consumo. */
  private VideoProductionCycle cycle() {
    VideoProductionCycle cycle = new VideoProductionCycle();
    cycle.setId(11L);
    cycle.setVideoProjectId(7L);
    cycle.setProductId(76L);
    cycle.setCommercialPlanId(5L);
    cycle.setStatus("PENDING_FINANCIAL_REVIEW");
    cycle.setBudgetLimitUsd(new BigDecimal("12.50"));
    cycle.setKnownCostUsd(BigDecimal.ZERO);
    cycle.setCreatedAt(Instant.now());
    cycle.setUpdatedAt(Instant.now());
    return cycle;
  }
}
