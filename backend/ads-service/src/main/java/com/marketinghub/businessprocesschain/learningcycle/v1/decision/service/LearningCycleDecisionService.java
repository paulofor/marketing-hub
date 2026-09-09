package com.marketinghub.businessprocesschain.learningcycle.v1.decision.service;

import static com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleRules.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.LearningCycleDecisionProposal;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.audit.DecisionProposalAudit;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.get.DecisionProposalResponse;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.pending.PendingDecisionProposal;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.result.DecisionProposalResult;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.retry.RetryDecisionProposal;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleJson;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleService;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: governar propostas auditáveis de Atena dentro da decisão do ciclo comercial.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LearningCycleDecisionService {
  public static final String AGENT = "experiment-strategist";
  public static final String CONTRACT = "LEARNING_CYCLE_DECISION_PROPOSAL_V1";
  private final LearningSalesCycleRepository cycles;
  private final LearningCycleDecisionProposalRepository proposals;
  private final BusinessProcessDefinitionRepository processes;
  private final BusinessProcessActivityDefinitionRepository activities;
  private final BusinessProcessActivityInstanceRepository instances;
  private final AgentRepository agents;
  private final LearningCycleService cycleService;
  private final LearningCycleJson json;

  /** Reserva sob lock do ciclo e lê commits recentes, evitando snapshot antigo do MySQL 5.7. */
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public List<PendingDecisionProposal> pending() {
    if (!enabled()) return List.of();
    for (Long id : cycles.findDecisionPending(PageRequest.of(0, 10))) {
      var cycle = cycles.findLockedById(id).orElseThrow();
      if (!isDecision(cycle)) continue;
      var proposal = latest(cycle);
      if (proposal != null && !"QUEUED".equals(proposal.getStatus())) continue;
      if (proposal == null) proposal = create(cycle, 1);
      if ("FAILED".equals(proposal.getStatus())) continue;
      proposal.setStatus("RUNNING");
      proposal.setLeaseToken(UUID.randomUUID().toString());
      proposal.setStartedAt(now());
      proposals.saveAndFlush(proposal);
      state(proposal, "RUNNING", "Atena está preparando a proposta; aprovação humana pendente.");
      log.info(
          "Ciclo: proposta reservada agent={} proposalId={} cycleId={} experimentId={} revision={}",
          AGENT,
          proposal.getId(),
          id,
          cycle.getExperimentId(),
          cycle.getRevision());
      return List.of(
          new PendingDecisionProposal(
              proposal.getId(),
              proposal.getLeaseToken(),
              id,
              cycle.getRevision(),
              proposal.getActivityDefinitionId(),
              json.read(proposal.getContextJson())));
    }
    return List.of();
  }

  /** Expõe o andamento sem criar tarefa, consumir modelo ou sobrescrever a decisão do usuário. */
  @Transactional(readOnly = true)
  public DecisionProposalResponse get(Long productId, Long cycleId) {
    var cycle = owned(productId, cycleId);
    var proposal =
        isDecision(cycle)
            ? latest(cycle)
            : proposals.findFirstByCycleIdOrderByIdDesc(cycleId).orElse(null);
    return view(cycle, proposal);
  }

  /** Reexpõe entrada, auditoria e tentativas somente sob demanda e no produto correto. */
  @Transactional(readOnly = true)
  public List<Map<String, Object>> audit(Long productId, Long cycleId) {
    owned(productId, cycleId);
    return proposals.findByCycleIdOrderByIdDesc(cycleId).stream()
        .map(
            p -> {
              Map<String, Object> result = new LinkedHashMap<>();
              result.put("id", p.getId());
              result.put("status", p.getStatus());
              result.put("context", json.read(p.getContextJson()));
              result.put(
                  "executionAudit", p.getAuditJson() == null ? null : json.read(p.getAuditJson()));
              result.put("rawResponse", p.getRawResponse());
              result.put(
                  "usage",
                  p.getResultReceiptJson() == null
                      ? null
                      : publicReceipt(p.getResultReceiptJson()));
              result.put("error", p.getError());
              result.put("approvedEventId", p.getApprovedEventId());
              return result;
            })
        .toList();
  }

  /** Preserva o request antes do modelo e reconhece callbacks concorrentes já confirmados. */
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void recordAudit(Long id, DecisionProposalAudit request) {
    var proposal = claimed(id, request.leaseToken());
    require(
        "RUNNING".equals(proposal.getStatus()) && !expired(proposal),
        "A execução já foi encerrada ou venceu.");
    require(request.schema().isObject(), "Schema estruturado obrigatório.");
    require(
        "flex".equals(request.serviceTier())
            || ("default".equals(request.serviceTier())
                && request.serviceTierReason() != null
                && !request.serviceTierReason().isBlank()),
        "Flex é obrigatório, salvo justificativa funcional explícita do harness.");
    ObjectNode body = (ObjectNode) json.read(json.write(request));
    body.remove("leaseToken");
    String input = json.write(body);
    require(
        proposal.getAuditJson() == null || json.read(proposal.getAuditJson()).equals(body),
        "O request congelado desta execução não pode mudar.");
    proposal.setAuditJson(input);
    proposals.save(proposal);
  }

  /** Serializa respostas pelo ciclo, preservando a primeira confirmada sem aplicar sua decisão. */
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public DecisionProposalResponse result(Long id, DecisionProposalResult request) {
    var proposal = claimed(id, request.leaseToken());
    var cycle =
        owned(
            json.read(proposal.getContextJson()).path("productId").asLong(), proposal.getCycleId());
    String receipt = json.write(request);
    if (!"RUNNING".equals(proposal.getStatus())) {
      require(
          proposal.getResultReceiptJson() != null
              && json.read(proposal.getResultReceiptJson()).equals(json.read(receipt)),
          "Callback divergente de uma execução já encerrada.");
      return view(cycle, proposal);
    }
    proposal.setResultReceiptJson(receipt);
    proposal.setRawResponse(request.rawResponse());
    proposal.setFinishedAt(now());
    try {
      require(!expired(proposal), "A execução venceu; a resposta atrasada não pode ser aprovada.");
      require(
          isDecision(cycle) && cycle.getRevision() == proposal.getCycleRevision(),
          "O ciclo mudou durante a análise; a proposta não pode ser aprovada.");
      require(
          proposal.getAuditJson() != null,
          "O request do modelo não foi registrado antes da resposta.");
      require(request.error() == null || request.error().isBlank(), request.error());
      require(
          request.rawResponse() != null && !request.rawResponse().isBlank(),
          "O modelo não entregou uma proposta.");
      JsonNode result = json.read(request.rawResponse());
      validate(proposal, cycle, result);
      proposal.setProposalJson(json.write(result));
      proposal.setStatus("READY");
      state(
          proposal,
          "PENDING",
          "Proposta de Atena concluída; aguardando edição e aprovação humana.");
    } catch (RuntimeException ex) {
      log.error(
          "Ciclo: resposta de Atena bloqueada proposalId={} cycleId={} revision={}",
          id,
          cycle.getId(),
          cycle.getRevision(),
          ex);
      proposal.setStatus("FAILED");
      proposal.setError(
          ex instanceof ResponseStatusException reason
              ? reason.getReason()
              : "Resposta inválida de Atena; consulte a auditoria da execução.");
      state(proposal, "BLOCKED", proposal.getError());
    }
    if (request.costUsd() != null) {
      var instance = instances.findById(proposal.getActivityInstanceId()).orElseThrow();
      instance.setKnownCostUsd(request.costUsd());
      instance.setCostCoverage("COMPLETE");
      instances.save(instance);
    }
    proposals.saveAndFlush(proposal);
    log.info(
        "Ciclo: proposta recebida proposalId={} cycleId={} status={}",
        id,
        cycle.getId(),
        proposal.getStatus());
    return view(cycle, proposal);
  }

  /** Abre nova tentativa após falha, reconhecendo o commit de uma retentativa concorrente. */
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public DecisionProposalResponse retry(
      Long productId, Long cycleId, RetryDecisionProposal request) {
    var cycle = locked(productId, cycleId);
    require(
        isDecision(cycle) && cycle.getRevision() == request.expectedRevision(),
        "O ciclo mudou; atualize a tela.");
    var previous = latest(cycle);
    require(previous != null, "Nenhuma execução anterior encontrada.");
    if (previous.getId().equals(request.previousProposalId())) {
      require(
          "FAILED".equals(previous.getStatus()) || expired(previous),
          "A proposta não está bloqueada nem vencida.");
      if (expired(previous)) {
        previous.setStatus("FAILED");
        previous.setError("Execução vencida sem callback; nova tentativa solicitada na atividade.");
        previous.setFinishedAt(now());
        proposals.save(previous);
        state(previous, "BLOCKED", previous.getError());
      }
      previous = create(cycle, previous.getAttempt() + 1);
    } else {
      var requested = proposals.findById(request.previousProposalId()).orElseThrow();
      require(
          requested.getCycleId().equals(cycleId)
              && requested.getCycleRevision() == cycle.getRevision()
              && previous.getAttempt() == requested.getAttempt() + 1,
          "Tentativa desatualizada ou de outro ciclo.");
    }
    return view(cycle, previous);
  }

  /** Congela contexto e vincula a ocorrência à atividade assistida publicada na Cadeia de Valor. */
  private LearningCycleDecisionProposal create(LearningSalesCycle cycle, int attempt) {
    var process =
        processes
            .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(PROCESS_CODE, "PUBLISHED")
            .orElseThrow();
    var activity =
        activities
            .findByProcessDefinitionIdAndActivityId(process.getId(), "DECISION")
            .orElseThrow();
    require(
        json.read(activity.getDefinitionJson())
            .path("responsibleAgentKeys")
            .toString()
            .contains("\"" + AGENT + "\""),
        "A atividade de decisão precisa estar associada a Atena no BPM publicado.");
    var instance = new BusinessProcessActivityInstance();
    instance.setActivityDefinition(activity);
    instance.setSourceReference(
        "learning-cycle:" + cycle.getId() + ":decision:" + cycle.getRevision());
    instance.setOccurrenceNumber(attempt);
    instance.setStatus("PENDING");
    instance.setEnteredAt(now());
    instance.setCreatedAt(now());
    instance.setUpdatedAt(now());
    instance.setCostCoverage("UNKNOWN");
    instance.setEvidenceQuality("NOT_RECORDED");
    instance.setObjectiveAchieved(false);
    instances.saveAndFlush(instance);
    var proposal = new LearningCycleDecisionProposal();
    proposal.setCycleId(cycle.getId());
    proposal.setCycleRevision(cycle.getRevision());
    proposal.setAttempt(attempt);
    proposal.setActivityDefinitionId(activity.getId());
    proposal.setActivityInstanceId(instance.getId());
    proposal.setStatus("QUEUED");
    proposal.setCreatedAt(now());
    proposal.setContextJson(
        json.write(Map.of("productId", cycle.getProductId(), "cycleId", cycle.getId())));
    try {
      proposal.setContextJson(context(cycle, process.getId()));
    } catch (RuntimeException ex) {
      log.error(
          "Ciclo: entrada indisponível para Atena cycleId={} experimentId={}",
          cycle.getId(),
          cycle.getExperimentId(),
          ex);
      proposal.setStatus("FAILED");
      proposal.setError(
          ex instanceof ResponseStatusException reason
              ? reason.getReason()
              : "Não foi possível carregar as evidências oficiais do ciclo.");
      proposal.setFinishedAt(now());
    }
    proposals.saveAndFlush(proposal);
    if ("FAILED".equals(proposal.getStatus())) state(proposal, "BLOCKED", proposal.getError());
    return proposal;
  }

  /** Reúne apenas fatos persistidos, limitações e destinos oficiais da cadeia do próprio ciclo. */
  private String context(LearningSalesCycle cycle, Long decisionProcessId) {
    var current =
        cycleService.list(cycle.getProductId()).stream()
            .filter(c -> c.id().equals(cycle.getId()))
            .findFirst()
            .orElseThrow();
    var measurement =
        current.events().stream()
            .filter(e -> "MEASURE".equals(e.action()))
            .reduce((a, b) -> b)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.CONFLICT, "Conciliação automática ausente."));
    require(
        measurement.evidence().path("automatic").asBoolean()
            && measurement.evidence().path("dataValid").asBoolean()
            && measurement.evidence().path("testDataExcluded").asBoolean(),
        "Concilie as fontes oficiais e segregue testes antes de solicitar a proposta.");
    Map<String, Object> context = new LinkedHashMap<>();
    context.put("contractVersion", CONTRACT);
    context.put("productId", cycle.getProductId());
    context.put("experimentId", cycle.getExperimentId());
    context.put("cycleId", cycle.getId());
    context.put("cycleRevision", cycle.getRevision());
    context.put("cycleProcessDefinitionId", cycle.getProcessDefinitionId());
    context.put("decisionProcessDefinitionId", decisionProcessId);
    context.put("productVersion", cycle.getProductVersion());
    context.put("brief", current.brief());
    context.put("inheritedLearning", current.inheritedLearning());
    context.put("events", current.events());
    context.put("measurementEventId", measurement.id());
    context.put("evidenceReference", measurement.evidenceReference());
    context.put("commands", current.commands());
    context.put(
        "returnTargets",
        cycleService.catalog(cycle.getChainDefinitionId(), cycle.getProductId()).returnTargets());
    context.put("operatorName", json.read(cycle.getCreationJson()).path("operatorName").asText(""));
    return json.write(context);
  }

  /** Valida estrutura, alternativa, fontes e destino antes de liberar o formulário para revisão. */
  private void validate(
      LearningCycleDecisionProposal proposal, LearningSalesCycle cycle, JsonNode result) {
    var context = json.read(proposal.getContextJson());
    require(
        result.isObject() && CONTRACT.equals(result.path("contractVersion").asText()),
        "Contrato de proposta inválido.");
    for (String field :
        List.of(
            "action",
            "summary",
            "rootCause",
            "learning",
            "nextHypothesis",
            "evidenceLimits",
            "correctionPlan",
            "scaleHypothesis")) {
      require(
          result.path(field).isTextual()
              && !result.path(field).asText().isBlank()
              && result.path(field).asText().length() <= 4000,
          "Campo ausente ou inválido na proposta: " + field);
    }
    Set<String> allowedFields =
        Set.of(
            "contractVersion",
            "action",
            "summary",
            "rootCause",
            "learning",
            "nextHypothesis",
            "evidenceLimits",
            "correctionPlan",
            "scaleHypothesis",
            "returnProcessId",
            "returnActivityId",
            "evidenceEventIds",
            "selectedAlternative",
            "alternatives");
    var names = result.fieldNames();
    while (names.hasNext())
      require(allowedFields.contains(names.next()), "A proposta contém campo fora do contrato.");
    require(result.size() == allowedFields.size(), "A proposta está incompleta.");
    String action = result.path("action").asText();
    require(
        Set.of("ADJUST", "CONTINUE", "FIX_MEASUREMENT", "SCALE", "STOP", "INCONCLUSIVE")
            .contains(action),
        "Decisão não permitida para Atena.");
    require(
        java.util.stream.StreamSupport.stream(context.path("commands").spliterator(), false)
            .anyMatch(
                c -> action.equals(c.path("action").asText()) && c.path("available").asBoolean()),
        "Atena sugeriu uma ação bloqueada pelas evidências.");
    var alternatives = result.path("alternatives");
    require(
        alternatives.isArray() && alternatives.size() == 3,
        "Atena deve comparar exatamente três alternativas.");
    for (JsonNode alternative : alternatives)
      for (String key : List.of("option", "benefit", "risk", "effort", "salesImpact"))
        require(
            alternative.path(key).isTextual()
                && !alternative.path(key).asText().isBlank()
                && alternative.path(key).asText().length() <= 2000,
            "Alternativa incompleta: " + key);
    require(
        result.path("selectedAlternative").isIntegralNumber()
            && result.path("selectedAlternative").asInt(-1) >= 0
            && result.path("selectedAlternative").asInt(3) < 3,
        "Alternativa escolhida inválida.");
    var ids = result.path("evidenceEventIds");
    require(ids.isArray() && !ids.isEmpty(), "Fontes do ciclo obrigatórias.");
    Set<Long> availableIds = new HashSet<>();
    context.path("events").forEach(e -> availableIds.add(e.path("id").asLong()));
    boolean includesMeasurement = false;
    for (var id : ids) {
      require(
          id.isIntegralNumber() && availableIds.contains(id.asLong()),
          "Fonte inexistente ou de outro ciclo.");
      includesMeasurement |= id.asLong() == context.path("measurementEventId").asLong();
    }
    require(includesMeasurement, "A proposta deve citar a conciliação oficial deste ciclo.");
    if ("ADJUST".equals(action)) {
      require(
          result.path("returnProcessId").isIntegralNumber()
              && result.path("returnActivityId").isTextual(),
          "Destino do retorno obrigatório.");
      require(
          java.util.stream.StreamSupport.stream(context.path("returnTargets").spliterator(), false)
              .anyMatch(
                  t ->
                      t.path("processDefinitionId").equals(result.path("returnProcessId"))
                          && t.path("activityId").equals(result.path("returnActivityId"))),
          "Destino fora do BPM da cadeia do ciclo.");
    }
    if (!"ADJUST".equals(action))
      require(
          result.path("returnProcessId").isNull() && result.path("returnActivityId").isNull(),
          "Esta decisão não deve declarar um retorno de ajuste.");
    ((ObjectNode) result).put("evidenceReference", context.path("evidenceReference").asText());
  }

  /** Serializa o relatório compacto, mantendo prompts e respostas extensas fora do polling. */
  private DecisionProposalResponse view(
      LearningSalesCycle cycle, LearningCycleDecisionProposal proposal) {
    var agent = agents.findByAgentKey(AGENT).orElse(null);
    String status =
        proposal == null ? "WAITING" : expired(proposal) ? "EXPIRED" : proposal.getStatus();
    if (proposal != null && isDecision(cycle) && proposal.getCycleRevision() != cycle.getRevision())
      status = "STALE";
    return new DecisionProposalResponse(
        proposal == null ? null : proposal.getId(),
        cycle.getId(),
        proposal == null ? cycle.getRevision() : proposal.getCycleRevision(),
        status,
        AGENT,
        "Atena",
        agent == null ? null : agent.getId(),
        agent != null && Boolean.TRUE.equals(agent.getAutomaticExecutionEnabled()),
        proposal == null ? null : proposal.getActivityDefinitionId(),
        proposal == null || proposal.getProposalJson() == null
            ? null
            : json.read(proposal.getProposalJson()),
        json.read(cycle.getCreationJson()).path("operatorName").asText(""),
        proposal == null ? null : proposal.getError(),
        proposal == null ? null : proposal.getCreatedAt(),
        proposal == null ? null : proposal.getFinishedAt(),
        proposal == null ? null : proposal.getApprovedAt(),
        proposal == null ? null : proposal.getApprovedEventId());
  }

  /** Bloqueia ciclo antes da proposta para manter a ordem única de locks também na aprovação. */
  private LearningCycleDecisionProposal claimed(Long id, String lease) {
    var cycleId = proposals.findCycleId(id).orElseThrow();
    cycles.findLockedById(cycleId).orElseThrow();
    var proposal = proposals.findById(id).orElseThrow();
    require(
        lease != null && lease.equals(proposal.getLeaseToken()),
        "Lease inválida para esta execução.");
    return proposal;
  }

  /** Atualiza a ocorrência assistida sem marcar objetivo alcançado antes da aprovação humana. */
  private void state(LearningCycleDecisionProposal proposal, String status, String description) {
    var instance = instances.findById(proposal.getActivityInstanceId()).orElseThrow();
    instance.setStatus(status);
    instance.setBlockedReason("BLOCKED".equals(status) ? description : null);
    instance.setObjectiveEvidenceJson(
        json.write(
            Map.of(
                "proposalId",
                proposal.getId(),
                "cycleId",
                proposal.getCycleId(),
                "agentKey",
                AGENT,
                "description",
                description)));
    instance.setEvidenceQuality("AGENT_PROPOSAL");
    instance.setUpdatedAt(now());
    instances.save(instance);
  }

  /** Retira o identificador reservado da lease dos dados de auditoria administrativos. */
  private JsonNode publicReceipt(String receipt) {
    var node = (ObjectNode) json.read(receipt);
    node.remove("leaseToken");
    return node;
  }

  /** Obtém a última tentativa da revisão sem misturar uma decisão anterior. */
  private LearningCycleDecisionProposal latest(LearningSalesCycle cycle) {
    return proposals
        .findFirstByCycleIdAndCycleRevisionOrderByAttemptDesc(cycle.getId(), cycle.getRevision())
        .orElse(null);
  }

  /** Confere a identidade do produto antes de expor dados do ciclo. */
  private LearningSalesCycle owned(Long productId, Long cycleId) {
    return cycles
        .findById(cycleId)
        .filter(c -> c.getProductId().equals(productId))
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Ciclo não encontrado neste produto."));
  }

  /** Serializa alterações do mesmo ciclo. */
  private LearningSalesCycle locked(Long productId, Long cycleId) {
    return cycles
        .findLocked(productId, cycleId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Ciclo não encontrado neste produto."));
  }

  /** Consulta o comando administrativo PLAY sem assumir controle de agendamento. */
  private boolean enabled() {
    return agents
        .findByAgentKey(AGENT)
        .map(a -> Boolean.TRUE.equals(a.getAutomaticExecutionEnabled()))
        .orElse(false);
  }

  /** Distingue uma decisão pendente de um ciclo já encerrado. */
  private boolean isDecision(LearningSalesCycle cycle) {
    return "OPEN".equals(cycle.getStatus()) && "DECISION".equals(cycle.getStage());
  }

  /** Expõe abandono de uma lease para recuperação explícita sem repetir o modelo no backend. */
  private boolean expired(LearningCycleDecisionProposal proposal) {
    return "RUNNING".equals(proposal.getStatus())
        && proposal.getStartedAt() != null
        && proposal.getStartedAt().isBefore(now().minus(60, ChronoUnit.MINUTES));
  }

  /** Normaliza instantes à precisão auditável do MySQL 5.7. */
  private Instant now() {
    return Instant.now().truncatedTo(ChronoUnit.MICROS);
  }
}
