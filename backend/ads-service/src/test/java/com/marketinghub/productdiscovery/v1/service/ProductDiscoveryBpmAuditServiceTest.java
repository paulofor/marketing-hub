package com.marketinghub.productdiscovery.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskExecutionAuditRequest;
import com.marketinghub.agenttask.AgentTaskResponse;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.CompleteAgentTaskRequest;
import com.marketinghub.agenttask.CreateAgentTaskRequest;
import com.marketinghub.agenttask.FailAgentTaskRequest;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunity;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunityDecision;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: comprovar a correlação auditável entre ciclos PDE e atividades do BPM. */
@ExtendWith(MockitoExtension.class)
class ProductDiscoveryBpmAuditServiceTest {

  @Mock private BusinessProcessDefinitionRepository processRepository;

  @Mock private AgentTaskService agentTaskService;

  private ProductDiscoveryBpmAuditService service;

  /** Prepara a versão publicada usada por todos os cenários da integração. */
  @BeforeEach
  void setUp() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(49L);
    process.setProcessCode("pde-opportunity-discovery");
    process.setVersionNumber(6);
    process.setStatus("PUBLISHED");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"marketEvidence\",\"type\":\"TASK\","
            + "\"label\":\"Reunir evidências factuais\",\"owner\":\"Argos\","
            + "\"responsibleAgentKeys\":[\"market-radar\"],"
            + "\"responsibilityDomain\":\"MARKET_EVIDENCE\"}]}");
    when(processRepository.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            "pde-opportunity-discovery", "PUBLISHED"))
        .thenReturn(Optional.of(process));
    service =
        new ProductDiscoveryBpmAuditService(
            processRepository, agentTaskService, new ObjectMapper());
  }

  /** Deve abrir a atividade vigente com referência estável e sem criar resultado antecipado. */
  @Test
  void opensCurrentProcessActivityForCycle() {
    ProductDiscoveryCycle cycle = cycle(ProductDiscoveryCycleStatus.READY_FOR_RESEARCH);
    when(agentTaskService.createByHumanIfAbsentAcrossProcessVersions(any(), any()))
        .thenReturn(taskResponse());

    AgentTaskResponse response = service.open(cycle);

    ArgumentCaptor<CreateAgentTaskRequest> request =
        ArgumentCaptor.forClass(CreateAgentTaskRequest.class);
    verify(agentTaskService)
        .createByHumanIfAbsentAcrossProcessVersions(
            request.capture(),
            org.mockito.ArgumentMatchers.eq(List.of("marketEvidence", "inspiration", "evidence")));
    assertThat(response.id()).isEqualTo(901L);
    assertThat(request.getValue().assignedAgentKey()).isEqualTo("market-radar");
    assertThat(request.getValue().sourceReference()).isEqualTo("product-discovery-cycle:37");
    assertThat(request.getValue().processDefinitionId()).isEqualTo(49L);
    assertThat(request.getValue().processActivityId()).isEqualTo("marketEvidence");
  }

  /** Deve registrar reserva, prompt e consumo do plano na mesma tarefa do ciclo. */
  @Test
  void startsAndAuditsDirectedPlanOnLinkedTask() {
    ProductDiscoveryCycle cycle = cycle(ProductDiscoveryCycleStatus.RESEARCHING);
    when(agentTaskService.createByHumanIfAbsentAcrossProcessVersions(any(), any()))
        .thenReturn(taskResponse());
    ProductDiscoveryResearchPlanRequest request =
        new ProductDiscoveryResearchPlanRequest(
            "lease-37",
            "{\"questions\":[\"dor\"]}",
            "{\"questions\":[\"dor\"]}",
            "gpt-5.6-sol",
            "CODEX",
            "Núcleo de Argos.\n\nPesquise o mercado.",
            "Núcleo de Argos.",
            "Pesquise o mercado.",
            "high",
            100L,
            20L,
            10L);

    service.start(cycle);
    service.recordPlan(cycle, request);

    verify(agentTaskService, times(2)).claimLinkedProcessTask("market-radar", 901L);
    verify(agentTaskService)
        .recordClaimedProcessTaskExecutionAudit(
            "market-radar",
            901L,
            "gpt-5.6-sol",
            "high",
            "Núcleo de Argos.\n\nPesquise o mercado.",
            "Núcleo de Argos.",
            "Pesquise o mercado.",
            100L,
            20L,
            10L,
            true);
  }

  /** Deve concluir com decisão e oportunidades estruturadas, preservando as evidências do plano. */
  @Test
  void completesLinkedTaskWithStructuredCycleEvidence() throws Exception {
    ProductDiscoveryCycle cycle = cycle(ProductDiscoveryCycleStatus.COMPLETED);
    cycle.setDecisionSummary("Pesquisar mais antes de investir.");
    cycle.setResearchPlanModel("gpt-5.6-sol");
    cycle.setResearchPlanJson("{\"questions\":[\"Qual dor é urgente?\"]}");
    cycle.setResearchPlanRawResponse("{\"questions\":[\"Qual dor é urgente?\"]}");
    ProductDiscoveryOpportunity opportunity = new ProductDiscoveryOpportunity();
    opportunity.setName("Auditoria de saída em 10 minutos");
    opportunity.setDecision(ProductDiscoveryOpportunityDecision.RESEARCH_MORE);
    opportunity.setScore(new BigDecimal("76.00"));
    when(agentTaskService.createByHumanIfAbsentAcrossProcessVersions(any(), any()))
        .thenReturn(taskResponse());

    service.complete(cycle, List.of(opportunity));

    ArgumentCaptor<CompleteAgentTaskRequest> request =
        ArgumentCaptor.forClass(CompleteAgentTaskRequest.class);
    verify(agentTaskService)
        .completeClaimedProcessTask(
            org.mockito.ArgumentMatchers.eq("market-radar"),
            org.mockito.ArgumentMatchers.eq(901L),
            request.capture());
    var result = new ObjectMapper().readTree(request.getValue().resultJson());
    var evidence = new ObjectMapper().readTree(request.getValue().evidenceJson());
    assertThat(result.path("cycleId").asLong()).isEqualTo(37L);
    assertThat(result.path("opportunities").get(0).path("score").decimalValue())
        .isEqualByComparingTo("76.00");
    assertThat(evidence.path("researchPlan").path("questions").get(0).asText())
        .isEqualTo("Qual dor é urgente?");
    assertThat(evidence.path("researchPlan").isTextual()).isFalse();
  }

  /** Deve bloquear a tarefa BPM com a mesma causa técnica persistida no ciclo. */
  @Test
  void failsLinkedTaskWithOriginalCycleError() throws Exception {
    ProductDiscoveryCycle cycle = cycle(ProductDiscoveryCycleStatus.FAILED);
    cycle.setErrorMessage("Backend rejeitou a conclusão com status 422.");
    when(agentTaskService.createByHumanIfAbsentAcrossProcessVersions(any(), any()))
        .thenReturn(taskResponse());

    AgentTaskExecutionAuditRequest executionAudit =
        new AgentTaskExecutionAuditRequest(
            "MODEL",
            "gpt-5.6-sol",
            "high",
            "Núcleo de Argos.\n\nPesquise o mercado.",
            "Núcleo de Argos.",
            "Pesquise o mercado.",
            List.of());

    service.fail(cycle, executionAudit);

    ArgumentCaptor<FailAgentTaskRequest> request =
        ArgumentCaptor.forClass(FailAgentTaskRequest.class);
    verify(agentTaskService)
        .failClaimedProcessTask(
            org.mockito.ArgumentMatchers.eq("market-radar"),
            org.mockito.ArgumentMatchers.eq(901L),
            request.capture());
    assertThat(request.getValue().error())
        .isEqualTo("Backend rejeitou a conclusão com status 422.");
    assertThat(request.getValue().executionAudit()).isEqualTo(executionAudit);
    assertThat(
            new ObjectMapper().readTree(request.getValue().resultJson()).path("cycleId").asLong())
        .isEqualTo(37L);
  }

  /** Cria um ciclo persistido mínimo com a identidade operacional usada no histórico. */
  private ProductDiscoveryCycle cycle(ProductDiscoveryCycleStatus status) {
    ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
    cycle.setId(37L);
    cycle.setTheme("Auditoria de saída de imóvel");
    cycle.setObjective("Descobrir oportunidade PDE comparável ao Rigel.");
    cycle.setStatus(status);
    cycle.setStageCode("research");
    cycle.setExecutionAttempt(1);
    return cycle;
  }

  /** Cria a resposta mínima da tarefa correlacionada devolvida pela mesa dos agentes. */
  private AgentTaskResponse taskResponse() {
    Instant now = Instant.parse("2026-08-27T23:09:00Z");
    return new AgentTaskResponse(
        901L,
        8L,
        "market-radar",
        "Argos",
        "HUMAN",
        null,
        null,
        "Marketing Hub",
        "Pesquisar oportunidade PDE #37",
        "Qualificar fontes.",
        "HIGH",
        "PENDING",
        "product-discovery-cycle:37",
        "WORK",
        null,
        null,
        null,
        null,
        now,
        now);
  }
}
