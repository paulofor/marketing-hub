package com.marketinghub.businessprocesschain.learningcycle.v1.decision;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.audit.DecisionProposalAudit;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.result.DecisionProposalResult;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.command.LearningCycleCommand;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.*;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.learningcycle.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: proteger proposta, fonte, autoria, segregação e aprovação comercial de Atena.
 */
class LearningCycleDecisionServiceTest {
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final LearningCycleDecisionProposalRepository proposals =
      mock(LearningCycleDecisionProposalRepository.class);
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final BusinessProcessActivityDefinitionRepository activities =
      mock(BusinessProcessActivityDefinitionRepository.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final AgentRepository agents = mock(AgentRepository.class);
  private final LearningCycleService cycleService = mock(LearningCycleService.class);
  private final LearningCycleJson json = new LearningCycleJson(mapper);
  private final LearningCycleDecisionService service =
      new LearningCycleDecisionService(
          cycles, proposals, processes, activities, instances, agents, cycleService, json);
  private final LearningCycleDecisionApproval approval =
      new LearningCycleDecisionApproval(proposals, instances);
  private final AtomicReference<LearningCycleDecisionProposal> saved = new AtomicReference<>();
  private LearningSalesCycle cycle;
  private BusinessProcessActivityInstance instance;
  private Agent agent;

  /** Monta somente fatos segregados e uma conciliação atribuída ao ciclo sob teste. */
  @BeforeEach
  void setup() throws Exception {
    cycle = new LearningSalesCycle();
    cycle.setId(1L);
    cycle.setProductId(4L);
    cycle.setExperimentId(91L);
    cycle.setRevision(1L);
    cycle.setStage("DECISION");
    cycle.setStatus("OPEN");
    cycle.setProductVersion("v7");
    cycle.setChainDefinitionId(13L);
    cycle.setProcessDefinitionId(72L);
    cycle.setCreationJson("{\"operatorName\":\"Pessoa responsável\"}");
    agent = Agent.builder().id(4L).nickname("Atena").automaticExecutionEnabled(true).build();
    when(agents.findByAgentKey("experiment-strategist")).thenReturn(Optional.of(agent));
    when(cycles.findDecisionPending(any())).thenReturn(List.of(1L));
    when(cycles.findById(1L)).thenReturn(Optional.of(cycle));
    when(cycles.findLockedById(1L)).thenReturn(Optional.of(cycle));
    when(cycles.findLocked(4L, 1L)).thenReturn(Optional.of(cycle));
    when(proposals.findFirstByCycleIdAndCycleRevisionOrderByAttemptDesc(1L, 1L))
        .thenAnswer(call -> Optional.ofNullable(saved.get()));
    when(proposals.saveAndFlush(any()))
        .thenAnswer(
            call -> {
              var value = (LearningCycleDecisionProposal) call.getArgument(0);
              value.setId(5L);
              saved.set(value);
              return value;
            });
    when(proposals.findById(5L)).thenAnswer(call -> Optional.ofNullable(saved.get()));
    when(proposals.findCycleId(5L)).thenReturn(Optional.of(1L));
    when(proposals.findByCycleIdOrderByIdDesc(1L)).thenAnswer(call -> List.of(saved.get()));
    var process = new BusinessProcessDefinition();
    process.setId(76L);
    when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            anyString(), eq("PUBLISHED")))
        .thenReturn(Optional.of(process));
    var activity = new BusinessProcessActivityDefinition();
    activity.setId(700L);
    activity.setDefinitionJson("{\"responsibleAgentKeys\":[\"experiment-strategist\"]}");
    when(activities.findByProcessDefinitionIdAndActivityId(76L, "DECISION"))
        .thenReturn(Optional.of(activity));
    when(instances.saveAndFlush(any()))
        .thenAnswer(
            call -> {
              instance = call.getArgument(0);
              instance.setId(70L);
              return instance;
            });
    when(instances.findById(70L)).thenAnswer(call -> Optional.of(instance));
    var response =
        mapper.readValue(
            """
        {"id":1,"productId":4,"experimentId":91,"revision":1,"stage":"DECISION","status":"OPEN","brief":{"hypothesis":"Hipótese registrada"},"inheritedLearning":{},"events":[{"id":2,"revision":1,"action":"MEASURE","evidenceReference":"internal://measure/2","evidence":{"automatic":true,"dataValid":true,"testDataExcluded":true,"sessions":4,"netSales":0}}],"commands":[{"action":"ADJUST","available":true},{"action":"SCALE","available":false}]}
        """,
            LearningCycleResponse.class);
    when(cycleService.list(4L)).thenReturn(List.of(response));
    when(cycleService.catalog(13L, 4L))
        .thenReturn(
            mapper.readValue(
                """
        {"returnTargets":[{"processDefinitionId":3,"activityId":"rework","owner":"Dédalo"}]}
        """,
                LearningCycleCatalog.class));
  }

  /** Navegar não cria proposta nem consome modelo. */
  @Test
  void readDoesNotCreateWork() {
    assertThat(service.get(4L, 1L).status()).isEqualTo("WAITING");
    verify(proposals, never()).saveAndFlush(any());
    verifyNoInteractions(cycleService);
  }

  /** STOP impede reserva e a segunda consulta não duplica a lease já entregue. */
  @Test
  void respectsPlayAndExclusiveReservation() {
    agent.setAutomaticExecutionEnabled(false);
    assertThat(service.pending()).isEmpty();
    agent.setAutomaticExecutionEnabled(true);
    assertThat(service.pending()).hasSize(1);
    assertThat(service.pending()).isEmpty();
    assertThat(saved.get().getActivityDefinitionId()).isEqualTo(700L);
    assertThat(instance.getSourceReference()).isEqualTo("learning-cycle:1:decision:1");
    assertThat(cycle.getStage()).isEqualTo("DECISION");
  }

  /** Contexto inclui conciliação atual e definição histórica sem transformá-la em aprovação. */
  @Test
  void freezesAttributionAndHistoricalIdentity() {
    var job = service.pending().getFirst();
    assertThat(job.context().path("experimentId").asLong()).isEqualTo(91);
    assertThat(job.context().path("cycleProcessDefinitionId").asLong()).isEqualTo(72);
    assertThat(job.context().path("decisionProcessDefinitionId").asLong()).isEqualTo(76);
    assertThat(job.context().path("measurementEventId").asLong()).isEqualTo(2);
    assertThat(job.context().path("events").get(0).path("evidence").path("sessions").asInt())
        .isEqualTo(4);
  }

  /** Resposta válida preenche a revisão sem autorizar retorno, encerramento ou publicação. */
  @Test
  void validDraftWaitsForHumanAndPreservesOriginal() {
    var result = complete(valid());
    assertThat(result.status()).isEqualTo("READY");
    assertThat(result.proposal().path("evidenceReference").asText())
        .isEqualTo("internal://measure/2");
    assertThat(cycle.getStatus()).isEqualTo("OPEN");
    assertThat(instance.isObjectiveAchieved()).isFalse();
    assertThat(instance.getStatus()).isEqualTo("PENDING");
    assertThat(saved.get().getRawResponse()).doesNotContain("evidenceReference");
    verify(cycleService, never()).command(any(), any(), any());
  }

  /** Proposta estrangeira ou fonte inventada não pode entrar no formulário. */
  @Test
  void rejectsForeignEvidenceAndTarget() {
    var bad = valid();
    bad.putArray("evidenceEventIds").add(999);
    assertThat(complete(bad).status()).isEqualTo("FAILED");
    assertThat(saved.get().getRawResponse()).contains("999");
  }

  /** Exige que o retorno exista na própria composição do BPM. */
  @Test
  void rejectsForeignReturn() {
    var bad = valid();
    bad.put("returnProcessId", 999L);
    assertThat(complete(bad).status()).isEqualTo("FAILED");
  }

  /** Não transforma o parecer em atalho para escalar uma amostra insuficiente. */
  @Test
  void rejectsBlockedCommercialAction() {
    var bad = valid();
    bad.put("action", "SCALE");
    assertThat(complete(bad).status()).isEqualTo("FAILED");
  }

  /** Proposta deve comparar três opções, deixando clara a escolha recomendada. */
  @Test
  void requiresThreeAlternatives() {
    var bad = valid();
    bad.putArray("alternatives").add(mapper.createObjectNode());
    assertThat(complete(bad).status()).isEqualTo("FAILED");
  }

  /** Callback sem request prévio registra falha com a resposta bruta consultável. */
  @Test
  void refusesUnauditedModelResult() {
    var job = service.pending().getFirst();
    assertThat(
            service
                .result(
                    5L,
                    new DecisionProposalResult(
                        job.leaseToken(), json.write(valid()), null, 1L, 1L, null))
                .status())
        .isEqualTo("FAILED");
    assertThat(service.audit(4L, 1L).getFirst().get("rawResponse")).isNotNull();
  }

  /** Request congelado não pode mudar e tier diferente de Flex exige justificativa. */
  @Test
  void requiresTierReasonAndImmutableRequest() {
    var job = service.pending().getFirst();
    assertThatThrownBy(
            () ->
                service.recordAudit(
                    5L,
                    new DecisionProposalAudit(
                        job.leaseToken(),
                        "prompt",
                        mapper.createObjectNode(),
                        "model",
                        "default",
                        null)))
        .isInstanceOf(ResponseStatusException.class);
    auditRequest(job.leaseToken());
    assertThatThrownBy(
            () ->
                service.recordAudit(
                    5L,
                    new DecisionProposalAudit(
                        job.leaseToken(),
                        "outro",
                        mapper.createObjectNode(),
                        "model",
                        "flex",
                        null)))
        .isInstanceOf(ResponseStatusException.class);
  }

  /** Resultado repetido é idempotente e resposta divergente não substitui o original. */
  @Test
  void resultReplayCannotReplaceDraft() {
    complete(valid());
    var p = saved.get();
    var request =
        new DecisionProposalResult(p.getLeaseToken(), json.write(valid()), null, 10L, 20L, null);
    assertThat(service.result(5L, request).status()).isEqualTo("READY");
    assertThatThrownBy(
            () ->
                service.result(
                    5L, new DecisionProposalResult(p.getLeaseToken(), "{}", null, 10L, 20L, null)))
        .isInstanceOf(ResponseStatusException.class);
  }

  /** Revisão alterada durante a execução invalida a resposta e deixa a causa persistida. */
  @Test
  void blocksStaleModelResponse() {
    var job = service.pending().getFirst();
    auditRequest(job.leaseToken());
    cycle.setRevision(2);
    service.result(
        5L,
        new DecisionProposalResult(job.leaseToken(), json.write(valid()), null, 10L, 20L, null));
    assertThat(saved.get().getStatus()).isEqualTo("FAILED");
    assertThat(saved.get().getError()).contains("ciclo mudou");
  }

  /** A API não expõe a proposta de outro produto. */
  @Test
  void isolatesProducts() {
    assertThatThrownBy(() -> service.get(7L, 1L)).isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> service.audit(7L, 1L)).isInstanceOf(ResponseStatusException.class);
  }

  /** Nenhuma decisão comercial dispensa a confirmação explícita e a proposta vigente. */
  @Test
  void requiresExplicitHumanApproval() {
    complete(valid());
    assertThatThrownBy(() -> approval.validate(cycle, command(mapper.createObjectNode())))
        .isInstanceOf(ResponseStatusException.class);
    var evidence =
        mapper.createObjectNode().put("decisionProposalId", 999L).put("humanApproved", true);
    assertThatThrownBy(() -> approval.validate(cycle, command(evidence)))
        .isInstanceOf(ResponseStatusException.class);
    evidence.put("decisionProposalId", 5L);
    assertThat(approval.validate(cycle, command(evidence))).isSameAs(saved.get());
    approval.record(saved.get(), 10L, "{\"summary\":\"Edição humana\"}", Instant.now());
    assertThat(saved.get().getApprovedEventId()).isEqualTo(10L);
    assertThat(saved.get().getProposalJson()).contains("Hipótese de melhoria");
    assertThat(instance.isObjectiveAchieved()).isTrue();
  }

  /** Monta uma proposta simulada com explicitação de incerteza e três alternativas. */
  private ObjectNode valid() {
    var node = mapper.createObjectNode();
    node.put("contractVersion", LearningCycleDecisionService.CONTRACT);
    node.put("action", "ADJUST");
    for (String key :
        List.of(
            "summary",
            "rootCause",
            "learning",
            "nextHypothesis",
            "evidenceLimits",
            "correctionPlan",
            "scaleHypothesis"))
      node.put(key, "Hipótese de melhoria; amostra pequena não comprova causalidade.");
    node.put("returnProcessId", 3);
    node.put("returnActivityId", "rework");
    node.putArray("evidenceEventIds").add(2);
    node.put("selectedAlternative", 0);
    var alternatives = node.putArray("alternatives");
    for (int i = 0; i < 3; i++) {
      var alternative = alternatives.addObject();
      for (String key : List.of("option", "benefit", "risk", "effort", "salesImpact"))
        alternative.put(key, "Alternativa " + i);
    }
    return node;
  }

  /** Registra o request antes de simular uma resposta do modelo. */
  private void auditRequest(String lease) {
    service.recordAudit(
        5L,
        new DecisionProposalAudit(
            lease, "prompt do ciclo", mapper.createObjectNode(), "model-fixture", "flex", null));
  }

  /** Consome a fila e entrega somente a resposta simulada para validação real. */
  private com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.get
          .DecisionProposalResponse
      complete(ObjectNode node) {
    var job = service.pending().getFirst();
    auditRequest(job.leaseToken());
    return service.result(
        5L, new DecisionProposalResult(job.leaseToken(), json.write(node), null, 10L, 20L, null));
  }

  /** Declara o comando humano editado sem confundi-lo com a autora Atena. */
  private LearningCycleCommand command(ObjectNode evidence) {
    return new LearningCycleCommand(
        UUID.randomUUID(),
        cycle.getRevision(),
        LearningCycleCommand.Action.ADJUST,
        "Pessoa revisora",
        "Edição humana",
        "Fonte oficial",
        evidence);
  }
}
