package com.marketinghub.businessprocesschain.learningcycle.v1.decision;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agent.Agent;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycleEvent;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.createCycle.CreateLearningCycleRequest;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.LearningCycleResponse;
import com.marketinghub.experiment.*;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

/**
 * Responsabilidade: impedir gasto, migração histórica e duplicidade na preparação de sucessores.
 */
class LearningCycleSuccessorPreparationTest {
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final LearningSalesCycleEventRepository events =
      mock(LearningSalesCycleEventRepository.class);
  private final LearningCycleDecisionProposalRepository proposals =
      mock(LearningCycleDecisionProposalRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final AgentRepository agents = mock(AgentRepository.class);
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final BusinessProcessChainDefinitionRepository chains =
      mock(BusinessProcessChainDefinitionRepository.class);
  private final LearningCycleService service = mock(LearningCycleService.class);
  private final ObjectMapper mapper = new ObjectMapper();
  private final LearningCycleSuccessorPreparation preparation =
      new LearningCycleSuccessorPreparation(
          cycles,
          events,
          proposals,
          products,
          agents,
          experiments,
          chains,
          service,
          new LearningCycleJson(mapper));

  private final com.marketinghub.businessprocess.automation.v1.service.ProcessRunService
      processRuns =
          mock(com.marketinghub.businessprocess.automation.v1.service.ProcessRunService.class);
  private final LearningCycleWorkResolver work = mock(LearningCycleWorkResolver.class);

  /** Conecta a continuidade sem executar agentes reais durante a homologação. */
  @org.junit.jupiter.api.BeforeEach
  void configureContinuation() {
    org.springframework.test.util.ReflectionTestUtils.setField(
        preparation, "processRuns", processRuns);
    org.springframework.test.util.ReflectionTestUtils.setField(preparation, "workResolver", work);
  }

  /**
   * Monta duas identidades independentes com contrato histórico e resultado de agente persistido.
   */
  private LearningSalesCycle fixture(long productId) throws Exception {
    var product = Product.builder().id(productId).automaticExecutionEnabled(true).build();
    when(products.findLockedById(productId)).thenReturn(Optional.of(product));
    when(agents.findByAgentKey("experiment-strategist"))
        .thenReturn(Optional.of(Agent.builder().automaticExecutionEnabled(true).build()));
    var cycle = new LearningSalesCycle();
    cycle.setId(productId + 100);
    cycle.setProductId(productId);
    cycle.setExperimentId(productId + 200);
    cycle.setStage("DECISION");
    cycle.setStatus("OPEN");
    cycle.setRevision(4);
    cycle.setChainCode("pde");
    cycle.setProductVersion("v1");
    cycle.setBriefJson(
        "{\"audience\":\"Profissionais\",\"offer\":\"Kit R$67\",\"successCriterion\":\"Contribuição\",\"sampleTarget\":100,\"minimumNetSales\":5,\"acquisition\":\"Limite histórico R$125\"}");
    when(cycles.findLocked(productId, cycle.getId())).thenReturn(Optional.of(cycle));
    var proposal = new LearningCycleDecisionProposal();
    proposal.setId(900L);
    proposal.setCycleId(cycle.getId());
    proposal.setCycleRevision(4);
    proposal.setStatus("READY");
    proposal.setProposalJson(valid().toString());
    when(proposals.findFirstByCycleIdAndCycleRevisionOrderByAttemptDesc(cycle.getId(), 4))
        .thenReturn(Optional.of(proposal));
    var source =
        Experiment.builder()
            .id(cycle.getExperimentId())
            .product(product)
            .name("Experimento predecessor")
            .status(ExperimentStatus.INVALIDATED)
            .platform(ExperimentPlatform.FACEBOOK)
            .unitPrice(new BigDecimal("67"))
            .dailyBudget(new BigDecimal("20"))
            .mediaSpendLimit(new BigDecimal("125"))
            .commercialCheckoutUrl("https://checkout.invalid/old")
            .followUpActionUrl("https://page.invalid/old")
            .creativeApproved(true)
            .totalCost(new BigDecimal("384.83"))
            .build();
    when(experiments.findById(source.getId())).thenReturn(Optional.of(source));
    when(experiments.saveAndFlush(any()))
        .thenAnswer(
            call -> {
              Experiment e = call.getArgument(0);
              e.setId(productId + 300);
              return e;
            });
    var chain = new BusinessProcessChainDefinition();
    chain.setId(26L);
    when(chains.findFirstByChainCodeAndStatusOrderByVersionNumberDesc("pde", "PUBLISHED"))
        .thenReturn(Optional.of(chain));
    var response = mock(LearningCycleResponse.class);
    when(response.id()).thenReturn(productId + 400);
    when(response.revision()).thenReturn(0L);
    when(response.experimentId()).thenReturn(productId + 300);
    when(response.chainDefinitionId()).thenReturn(26L);
    var prepared = new LearningSalesCycle();
    prepared.setId(productId + 400);
    when(cycles.findById(productId + 400)).thenReturn(Optional.of(prepared));
    when(work.resolve(prepared))
        .thenReturn(
            new com.marketinghub
                .businessprocesschain
                .learningcycle
                .v1
                .service
                .getCycles
                .LearningCycleProcessContext
                .Work(
                116L,
                2,
                "Planejamento",
                "marketStrategy",
                1,
                "Estratégia",
                "Atena",
                "NOT_STARTED",
                "Pronto",
                "/local"));
    when(service.createPreparation(eq(productId), any())).thenReturn(response);
    when(service.carryPreparedLearning(eq(productId), eq(productId + 400), any()))
        .thenReturn(response);
    return cycle;
  }

  /** Declara um parecer elegível sem fingir aprovação humana ou resultado comercial. */
  private ObjectNode valid() throws Exception {
    return (ObjectNode)
        mapper.readTree(
            """
        {"contractVersion":"LEARNING_CYCLE_DECISION_PROPOSAL_V2","action":"ADJUST",
         "learning":"Pouca exposição; nenhuma venda","rootCause":"Hipótese ainda não comprovada",
         "nextHypothesis":"Tornar o valor claro","evidenceLimits":"Amostra insuficiente",
         "marketReview":{"recommendedScope":"KEEP_FOCUS","requiresNewCycle":true}}
        """);
  }

  /** Reproduz Capella e outro produto sem copiar mídia, ativos, custos, janela ou autorizações. */
  @ParameterizedTest
  @ValueSource(longs = {7, 83})
  void preparesSameFlowForDifferentIdentities(long productId) throws Exception {
    var cycle = fixture(productId);
    preparation.prepare(productId, cycle.getId());
    var saved = ArgumentCaptor.forClass(Experiment.class);
    verify(experiments).saveAndFlush(saved.capture());
    var successor = saved.getValue();
    assertThat(successor.getStatus()).isEqualTo(ExperimentStatus.PLANNED);
    assertThat(successor.getDailyBudget()).isZero();
    assertThat(successor.getMediaSpendLimit()).isZero();
    assertThat(successor.getStartDate()).isNull();
    assertThat(successor.getEndDate()).isNull();
    assertThat(successor.getFacebookReleaseRequestedAt()).isNull();
    assertThat(successor.getCommercialCheckoutUrl()).isNull();
    assertThat(successor.getFollowUpActionUrl()).isNull();
    assertThat(successor.isCreativeApproved()).isFalse();
    assertThat(successor.getTotalCost()).isNull();
    assertThat(successor.getSourceExperiment()).isNull();
    var request = ArgumentCaptor.forClass(CreateLearningCycleRequest.class);
    verify(service).createPreparation(eq(productId), request.capture());
    assertThat(request.getValue().previousCycleId()).isEqualTo(cycle.getId());
    assertThat(request.getValue().budgetLimitBrl()).isZero();
    assertThat(request.getValue().windowEnd()).isNull();
    assertThat(request.getValue().acquisition()).doesNotContain("125");
    var decision =
        ArgumentCaptor.forClass(
            com.marketinghub.businessprocesschain.learningcycle.v1.service.command
                .LearningCycleCommand.class);
    verify(service).recordPreparationDecision(eq(productId), eq(cycle.getId()), decision.capture());
    assertThat(decision.getValue().evidence().path("humanApproved").asBoolean()).isFalse();
    assertThat(decision.getValue().evidence().path("preparationPolicy").asText())
        .isEqualTo(LearningCyclePreparationPolicy.CONTRACT);
    verifyNoInteractions(processRuns);
    assertThat(cycle.getStatus())
        .isEqualTo("OPEN"); // O double não executa o comando; não alteramos a entidade diretamente.
  }

  /** Replay devolve o sucessor existente sem novo cadastro, decisão ou inferência. */
  @Test
  void returnsExistingSuccessor() throws Exception {
    var cycle = fixture(7);
    var next = new LearningSalesCycle();
    next.setId(407L);
    when(cycles.findByPreviousCycleId(cycle.getId())).thenReturn(Optional.of(next));
    var response = mock(LearningCycleResponse.class);
    when(response.id()).thenReturn(407L);
    when(service.get(7L, 407L)).thenReturn(response);
    assertThat(preparation.prepare(7L, cycle.getId())).isSameAs(response);
    verify(experiments, never()).saveAndFlush(any());
    verify(service, never()).recordPreparationDecision(any(), any(), any());
  }

  /** STOP do produto não pode ser convertido em preparação automática. */
  @Test
  void respectsProductStop() throws Exception {
    var cycle = fixture(7);
    products.findLockedById(7L).orElseThrow().setAutomaticExecutionEnabled(false);
    assertThatThrownBy(() -> preparation.prepare(7L, cycle.getId())).hasMessageContaining("STOP");
    verifyNoInteractions(service);
  }

  /** Reutiliza a decisão final aprovada sem repetir aprovação ou alterar o registro anterior. */
  @Test
  void recoversAlreadyApprovedDecisionWithFinalEdits() throws Exception {
    var cycle = fixture(7);
    var proposal =
        proposals
            .findFirstByCycleIdAndCycleRevisionOrderByAttemptDesc(cycle.getId(), 4)
            .orElseThrow();
    cycle.setStatus("ADJUSTED");
    cycle.setRevision(5);
    proposal.setStatus("APPROVED");
    proposal.setApprovedEventId(800L);
    when(proposals.findFirstByCycleIdOrderByIdDesc(cycle.getId()))
        .thenReturn(Optional.of(proposal));
    var event = new LearningSalesCycleEvent();
    event.setCycleId(cycle.getId());
    event.setRevision(5);
    event.setAction("ADJUST");
    event.setEvidenceJson(
        "{\"decisionProposalId\":900,\"humanApproved\":true,\"nextHypothesis\":\"Hipótese final editada e aprovada\"}");
    when(events.findById(800L)).thenReturn(Optional.of(event));
    preparation.prepare(7L, cycle.getId());
    verify(processRuns)
        .start(
            eq(7L),
            eq(116L),
            eq(
                new com.marketinghub.businessprocess.automation.v1.service.commands
                    .ProcessRunCommand(26L, 407L, "experiment:307")));
    verify(service, never()).recordPreparationDecision(any(), any(), any());
    var request = ArgumentCaptor.forClass(CreateLearningCycleRequest.class);
    verify(service).createPreparation(eq(7L), request.capture());
    assertThat(request.getValue().hypothesis()).isEqualTo("Hipótese final editada e aprovada");
    assertThat(proposal.getStatus()).isEqualTo("APPROVED");
    assertThat(proposal.getApprovedEventId()).isEqualTo(800L);
    assertThat(cycle.getRevision()).isEqualTo(5);
  }

  /** Preserva inconclusivo aprovado e prepara o próximo contexto em produtos independentes. */
  @ParameterizedTest
  @ValueSource(longs = {4, 83})
  void recoversApprovedInconclusiveWithoutReopeningHistory(long productId) throws Exception {
    var cycle = fixture(productId);
    var proposal = approvedInconclusive(cycle);
    assertThat(preparation.available(cycle, proposal)).isTrue();
    preparation.prepare(productId, cycle.getId());
    verify(service, never()).recordPreparationDecision(any(), any(), any());
    var request = ArgumentCaptor.forClass(CreateLearningCycleRequest.class);
    verify(service).createPreparation(eq(productId), request.capture());
    assertThat(request.getValue().hypothesis()).isEqualTo("Novo teste aprovado no mesmo foco");
    assertThat(request.getValue().budgetLimitBrl()).isZero();
    assertThat(request.getValue().windowStart()).isNull();
    assertThat(request.getValue().windowEnd()).isNull();
    assertThat(cycle.getStatus()).isEqualTo("INCONCLUSIVE");
    assertThat(cycle.getRevision()).isEqualTo(5);
    assertThat(cycle.getReturnProcessId()).isNull();
    assertThat(proposal.getApprovedEventId()).isEqualTo(800L);
    verify(processRuns).start(eq(productId), eq(116L), any());
  }

  /** A recuperação administrativa de Mira e outro produto preserva aprovação sem iniciar IA. */
  @ParameterizedTest
  @CsvSource({"10,ADJUSTED,ADJUST", "83,INCONCLUSIVE,INCONCLUSIVE"})
  void preparesApprovedHistoryWithoutStartingAgents(long productId, String status, String action)
      throws Exception {
    var cycle = fixture(productId);
    var proposal = approvedInconclusive(cycle);
    cycle.setStatus(status);
    events.findById(800L).orElseThrow().setAction(action);
    String originalProposal = proposal.getProposalJson();
    preparation.prepareOnly(productId, cycle.getId());
    var request = ArgumentCaptor.forClass(CreateLearningCycleRequest.class);
    verify(service).createPreparation(eq(productId), request.capture());
    assertThat(request.getValue().previousCycleId()).isEqualTo(cycle.getId());
    assertThat(request.getValue().hypothesis()).isEqualTo("Novo teste aprovado no mesmo foco");
    assertThat(request.getValue().budgetLimitBrl()).isZero();
    assertThat(request.getValue().windowStart()).isNull();
    assertThat(request.getValue().windowEnd()).isNull();
    verify(service, never()).recordPreparationDecision(any(), any(), any());
    verifyNoInteractions(processRuns);
    assertThat(proposal.getProposalJson()).isEqualTo(originalProposal);
    assertThat(proposal.getApprovedEventId()).isEqualTo(800L);
    assertThat(cycle.getStatus()).isEqualTo(status);
    assertThat(cycle.getRevision()).isEqualTo(5);
  }

  /** Monta recibo humano correspondente ao inconclusivo, com edição final da hipótese. */
  private LearningCycleDecisionProposal approvedInconclusive(LearningSalesCycle cycle)
      throws Exception {
    var proposal =
        proposals
            .findFirstByCycleIdAndCycleRevisionOrderByAttemptDesc(cycle.getId(), 4)
            .orElseThrow();
    cycle.setStatus("INCONCLUSIVE");
    cycle.setRevision(5);
    proposal.setStatus("APPROVED");
    proposal.setApprovedEventId(800L);
    proposal.setProposalJson(valid().put("action", "INCONCLUSIVE").toString());
    when(proposals.findFirstByCycleIdOrderByIdDesc(cycle.getId()))
        .thenReturn(Optional.of(proposal));
    var event = new LearningSalesCycleEvent();
    event.setCycleId(cycle.getId());
    event.setRevision(5);
    event.setAction("INCONCLUSIVE");
    event.setEvidenceJson(
        "{\"decisionProposalId\":900,\"humanApproved\":true,\"nextHypothesis\":\"Novo teste aprovado no mesmo foco\"}");
    when(events.findById(800L)).thenReturn(Optional.of(event));
    return proposal;
  }

  /** Edição final de mercado, hipótese vazia ou aprovação ausente não libera o sucessor. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "STOP",
        "ADJACENT_SEGMENTS",
        "EMPTY_HYPOTHESIS",
        "NOT_HUMAN",
        "OTHER_CYCLE",
        "STALE_REVISION"
      })
  void rejectsUnsafeInconclusiveReceipts(String scenario) throws Exception {
    var cycle = fixture(4);
    var proposal = approvedInconclusive(cycle);
    var event = events.findById(800L).orElseThrow();
    var finalEvidence = (ObjectNode) mapper.readTree(event.getEvidenceJson());
    switch (scenario) {
      case "STOP" -> event.setAction("STOP");
      case "ADJACENT_SEGMENTS" ->
          finalEvidence.set(
              "marketReview",
              mapper.readTree(
                  "{\"recommendedScope\":\"ADJACENT_SEGMENTS\",\"requiresNewCycle\":true}"));
      case "EMPTY_HYPOTHESIS" -> finalEvidence.put("nextHypothesis", "");
      case "NOT_HUMAN" -> finalEvidence.put("humanApproved", false);
      case "OTHER_CYCLE" -> event.setCycleId(cycle.getId() + 1);
      case "STALE_REVISION" -> cycle.setRevision(6);
      default -> throw new IllegalArgumentException("Cenário desconhecido");
    }
    event.setEvidenceJson(finalEvidence.toString());
    assertThat(preparation.available(cycle, proposal)).isFalse();
    assertThatThrownBy(() -> preparation.prepare(4L, cycle.getId()))
        .hasMessageContaining("compatível");
    verify(experiments, never()).saveAndFlush(any());
    verifyNoInteractions(service, processRuns);
  }

  /** Recusa aprovação sem recibo canônico, mantendo a integridade do histórico encerrado. */
  @Test
  void refusesApprovedProposalWithoutMatchingReceipt() throws Exception {
    var cycle = fixture(7);
    cycle.setStatus("ADJUSTED");
    cycle.setRevision(5);
    var proposal =
        proposals
            .findFirstByCycleIdAndCycleRevisionOrderByAttemptDesc(cycle.getId(), 4)
            .orElseThrow();
    proposal.setStatus("APPROVED");
    proposal.setApprovedEventId(800L);
    when(proposals.findFirstByCycleIdOrderByIdDesc(cycle.getId()))
        .thenReturn(Optional.of(proposal));
    assertThat(preparation.available(cycle, proposal)).isFalse();
    assertThatThrownBy(() -> preparation.prepare(7L, cycle.getId()))
        .hasMessageContaining("vigente");
    verifyNoInteractions(service);
  }

  /** Proposta substituída, incompleta ou outro mercado conserva a decisão para revisão. */
  @ParameterizedTest
  @ValueSource(strings = {"ADJACENT_SEGMENTS", "BROAD_PROBLEM", "INSUFFICIENT_EVIDENCE"})
  void rejectsMarketRedirection(String scope) throws Exception {
    var cycle = fixture(7);
    var value = valid();
    ((ObjectNode) value.get("marketReview")).put("recommendedScope", scope);
    proposals
        .findFirstByCycleIdAndCycleRevisionOrderByAttemptDesc(cycle.getId(), 4)
        .orElseThrow()
        .setProposalJson(value.toString());
    assertThatThrownBy(() -> preparation.prepare(7L, cycle.getId()))
        .hasMessageContaining("compatível");
    verifyNoInteractions(service);
    verify(experiments, never()).saveAndFlush(any());
  }

  /** Autorização ou escala nunca se tornam comandos preparatórios por conter a mesma hipótese. */
  @ParameterizedTest
  @ValueSource(strings = {"SCALE", "CONTINUE", "STOP", "INCONCLUSIVE", "FIX_MEASUREMENT"})
  void rejectsOtherActions(String action) throws Exception {
    var value = valid().put("action", action);
    assertThat(LearningCyclePreparationPolicy.eligible(value)).isFalse();
  }

  /** Parecer ainda ausente ou recusado continua legível sem tentar interpretar resposta nula. */
  @ParameterizedTest
  @ValueSource(strings = {"QUEUED", "RUNNING", "FAILED", "STALE"})
  void unavailableResultDoesNotBreakReport(String status) throws Exception {
    var cycle = fixture(7);
    var proposal =
        proposals
            .findFirstByCycleIdAndCycleRevisionOrderByAttemptDesc(cycle.getId(), 4)
            .orElseThrow();
    proposal.setStatus(status);
    proposal.setProposalJson(null);
    assertThat(preparation.available(cycle, proposal)).isFalse();
  }
}
