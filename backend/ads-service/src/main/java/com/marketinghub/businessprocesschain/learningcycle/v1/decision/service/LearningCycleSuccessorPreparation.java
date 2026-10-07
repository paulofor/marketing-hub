package com.marketinghub.businessprocesschain.learningcycle.v1.decision.service;

import static com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleRules.require;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.LearningCycleDecisionProposal;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.prepareAdjustment.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.command.LearningCycleCommand;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.createCycle.CreateLearningCycleRequest;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.LearningCycleResponse;
import com.marketinghub.experiment.*;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Responsabilidade: preparar atomicamente o sucessor sem mídia, modelo ou aprovação fictícia. */
@Service
@RequiredArgsConstructor
@Slf4j
public class LearningCycleSuccessorPreparation {
  public static final String OPERATOR = "Marketing Hub · preparação automática";
  private final LearningSalesCycleRepository cycles;
  private final LearningSalesCycleEventRepository events;
  private final LearningCycleDecisionProposalRepository proposals;
  private final ProductRepository products;
  private final AgentRepository agents;
  private final ExperimentRepository experiments;
  private final BusinessProcessChainDefinitionRepository chains;
  private final LearningCycleService service;
  private final LearningCycleJson json;

  @org.springframework.beans.factory.annotation.Autowired
  @org.springframework.context.annotation.Lazy
  private com.marketinghub.businessprocess.automation.v1.service.ProcessRunService processRuns;

  @org.springframework.beans.factory.annotation.Autowired
  private LearningCycleWorkResolver workResolver;

  /**
   * Serializa a preparação ou o inconclusivo aprovado e enfileira o planejamento no novo contexto.
   */
  @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
  public LearningCycleResponse prepare(Long productId, Long cycleId) {
    return prepare(productId, cycleId, true);
  }

  /** Prepara pela tela sem iniciar execução, mesmo quando a decisão histórica foi aprovada. */
  @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
  public LearningCycleResponse prepareOnly(Long productId, Long cycleId) {
    return prepare(productId, cycleId, false);
  }

  /** Expõe apenas ajuste privado encerrado com recibo válido, sem criar tarefa pela leitura. */
  @Transactional(readOnly = true)
  public AdjustmentPreparationAvailability adjustmentAvailability(Long productId, Long cycleId) {
    try {
      var cycle =
          cycles.findById(cycleId).filter(c -> productId.equals(c.getProductId())).orElse(null);
      boolean ready =
          cycle != null
              && cycles.findByPreviousCycleId(cycleId).isEmpty()
              && closedPrivateAdjustment(cycle) != null
              && products
                  .findById(productId)
                  .map(p -> Boolean.TRUE.equals(p.getAutomaticExecutionEnabled()))
                  .orElse(false);
      return new AdjustmentPreparationAvailability(
          ready,
          ready
              ? "O ajuste registrado pode preparar uma versão sucessora sem janela, agentes ou gasto. A nova implementação e as revisões independentes continuam obrigatórias."
              : "Somente ajuste pré-mercado encerrado, sem sucessor e com decisão registrada permite esta preparação.");
    } catch (RuntimeException ex) {
      log.error(
          "Falha ao verificar preparação do ajuste privado productId={} cycleId={}",
          productId,
          cycleId,
          ex);
      return new AdjustmentPreparationAvailability(
          false,
          "Não foi possível conferir a decisão registrada; nenhuma preparação foi iniciada.");
    }
  }

  /** Prepara uma candidata nova a partir do ajuste pré-mercado registrado, sem nova inferência. */
  @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
  public LearningCycleResponse prepareAdjustmentOnly(
      Long productId, Long cycleId, PrepareAdjustmentSuccessorRequest request) {
    var product =
        products
            .findLockedById(productId)
            .orElseThrow(
                () ->
                    new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Produto não encontrado."));
    var cycle =
        cycles
            .findLocked(productId, cycleId)
            .orElseThrow(
                () ->
                    new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Ciclo não encontrado neste produto."));
    var existing = cycles.findByPreviousCycleId(cycleId);
    if (existing.isPresent()) {
      require(
          request.productVersion().equals(existing.get().getProductVersion()),
          "O sucessor existente pertence a outra versão; preserve sua história.");
      return service.get(productId, existing.get().getId());
    }
    require(
        Boolean.TRUE.equals(product.getAutomaticExecutionEnabled()),
        "Produto em STOP; preparação preservada.");
    require(
        request.expectedRevision() == cycle.getRevision(),
        "O ciclo mudou; atualize a tela antes de preparar.");
    require(
        !cycle.getProductVersion().equals(request.productVersion()),
        "A correção exige uma versão nova; a candidata rejeitada permanece no histórico.");
    var decision = closedPrivateAdjustment(cycle);
    require(
        decision != null,
        "A preparação exige ajuste privado encerrado com decisão persistida e sem exposição comercial.");
    var source = experiments.findById(cycle.getExperimentId()).orElseThrow();
    var chain =
        chains
            .findFirstByChainCodeAndStatusOrderByVersionNumberDesc(
                cycle.getChainCode(), "PUBLISHED")
            .orElseThrow();
    decision.put("preparationPolicy", LearningCyclePreparationPolicy.CONTRACT);
    decision.put("humanApproved", false);
    decision.put("externalSpendAuthorized", false);
    var result =
        createPreparedSuccessor(
            productId,
            cycle,
            source,
            chain.getId(),
            json.read(cycle.getBriefJson()),
            decision,
            request.productVersion(),
            "internal://learning-cycles/" + cycleId + "/private-adjustment");
    log.info(
        "Ciclo: sucessor do ajuste privado preparado productId={} predecessor={} cycleId={} experimentId={} version={} executionStarted=false",
        productId,
        cycleId,
        result.id(),
        result.experimentId(),
        request.productVersion());
    return result;
  }

  /** Confere a última decisão do ajuste ainda sem mercado, preservando o próprio recibo. */
  private ObjectNode closedPrivateAdjustment(LearningSalesCycle cycle) {
    if (!"ADJUSTED".equals(cycle.getStatus())
        || cycle.isBaseline()
        || !java.util.Set.of("ADJUSTMENT", "VALIDATION").contains(cycle.getStage())) return null;
    var source = experiments.findById(cycle.getExperimentId()).orElse(null);
    if (source == null
        || source.getProduct() == null
        || !cycle.getProductId().equals(source.getProduct().getId())
        || source.getStatus() != ExperimentStatus.PLANNED
        || source.getFacebookReleaseRequestedAt() != null) return null;
    var receipt =
        events.findByCycleIdOrderByRevisionAsc(cycle.getId()).stream()
            .filter(
                event ->
                    cycle.getId().equals(event.getCycleId())
                        && event.getRevision() == cycle.getRevision()
                        && "ADJUST".equals(event.getAction()))
            .findFirst()
            .orElse(null);
    if (receipt == null) return null;
    var decision = json.read(receipt.getEvidenceJson());
    if (!decision.isObject()
        || java.util.List.of("rootCause", "learning", "nextHypothesis").stream()
            .anyMatch(key -> decision.path(key).asText().isBlank())) return null;
    ObjectNode result = decision.deepCopy();
    result.put("sourceDecisionEventId", receipt.getId());
    result.put(
        "evidenceLimits",
        "Ajuste pré-mercado: preservar o parecer interno; sem evidência humana, venda ou contribuição comercial.");
    return result;
  }

  /** Compartilha a preparação atômica e separa o cadastro da execução dos agentes. */
  private LearningCycleResponse prepare(Long productId, Long cycleId, boolean startExecution) {
    var product =
        products
            .findLockedById(productId)
            .orElseThrow(
                () ->
                    new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Produto não encontrado."));
    var cycle =
        cycles
            .findLocked(productId, cycleId)
            .orElseThrow(
                () ->
                    new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Ciclo não encontrado neste produto."));
    var existing = cycles.findByPreviousCycleId(cycleId);
    if (existing.isPresent()) return service.get(productId, existing.get().getId());
    require(
        Boolean.TRUE.equals(product.getAutomaticExecutionEnabled()),
        "Produto em STOP; preparação preservada.");
    require(
        agents
            .findByAgentKey(LearningCycleDecisionService.AGENT)
            .map(a -> Boolean.TRUE.equals(a.getAutomaticExecutionEnabled()))
            .orElse(false),
        "Atena em STOP; preparação preservada.");
    boolean approved = List.of("ADJUSTED", "INCONCLUSIVE").contains(cycle.getStatus());
    require(
        ("OPEN".equals(cycle.getStatus()) || approved) && "DECISION".equals(cycle.getStage()),
        "A preparação exige decisão corrente ou parecer aprovado compatível, sem reabrir operação encerrada.");
    var proposal =
        (approved
                ? proposals.findFirstByCycleIdOrderByIdDesc(cycleId)
                : proposals.findFirstByCycleIdAndCycleRevisionOrderByAttemptDesc(
                    cycleId, cycle.getRevision()))
            .orElseThrow();
    require(
        available(cycle, proposal), "Aguarde o parecer ou a decisão final vigente e compatível.");
    var decision =
        approved
            ? approvedDecision(cycle, proposal)
            : (ObjectNode) json.read(proposal.getProposalJson());
    boolean humanApproved = approved && decision.path("humanApproved").asBoolean(false);
    var source = experiments.findById(cycle.getExperimentId()).orElseThrow();
    require(
        source.getProduct() != null && productId.equals(source.getProduct().getId()),
        "Experimento pertence a outro produto.");
    require(
        source.getStatus() != ExperimentStatus.RUNNING, "O experimento ainda está em operação.");
    var chain =
        chains
            .findFirstByChainCodeAndStatusOrderByVersionNumberDesc(
                cycle.getChainCode(), "PUBLISHED")
            .orElseThrow();
    var brief = json.read(cycle.getBriefJson());
    // A proposta original permanece imutável; somente a evidência da política ganha metadados.
    decision.put("decisionProposalId", proposal.getId());
    decision.put("preparationPolicy", LearningCyclePreparationPolicy.CONTRACT);
    decision.put("humanApproved", false);
    decision.put("externalSpendAuthorized", false);
    if (!approved)
      service.recordPreparationDecision(
          productId,
          cycleId,
          new LearningCycleCommand(
              key(cycleId, "decision"),
              cycle.getRevision(),
              LearningCycleCommand.Action.ADJUST,
              OPERATOR,
              "Preparação automática do sucessor a partir da proposta #"
                  + proposal.getId()
                  + "; sem gasto ou publicação.",
              "internal://learning-cycles/" + cycleId + "/decision-proposals/" + proposal.getId(),
              decision));
    proposal.setError(null);
    var result =
        createPreparedSuccessor(
            productId,
            cycle,
            source,
            chain.getId(),
            brief,
            decision,
            cycle.getProductVersion(),
            "internal://learning-cycles/" + cycleId + "/decision-proposals/" + proposal.getId());
    if (humanApproved && startExecution) {
      var preparedCycle = cycles.findById(result.id()).orElseThrow();
      var next = workResolver.resolve(preparedCycle);
      require(next != null, "O sucessor aprovado precisa de um processo preparatório disponível.");
      processRuns.start(
          productId,
          next.processDefinitionId(),
          new com.marketinghub.businessprocess.automation.v1.service.commands.ProcessRunCommand(
              result.chainDefinitionId(), result.id(), "experiment:" + result.experimentId()));
    }
    log.info(
        "Ciclo: sucessor preparado policy={} productId={} predecessor={} proposalId={} cycleId={} experimentId={} executionStarted={} mediaSpendAuthorized=false",
        LearningCyclePreparationPolicy.CONTRACT,
        productId,
        cycleId,
        proposal.getId(),
        result.id(),
        result.experimentId(),
        humanApproved && startExecution);
    return result;
  }

  /** Compartilha o cadastro seguro sem copiar mídia, janela ou permissões do predecessor. */
  private LearningCycleResponse createPreparedSuccessor(
      Long productId,
      LearningSalesCycle cycle,
      Experiment source,
      Long chainId,
      JsonNode brief,
      ObjectNode decision,
      String version,
      String evidenceReference) {
    Long cycleId = cycle.getId();
    var successor = experiments.saveAndFlush(plannedExperiment(source, cycleId, decision));
    var prepared =
        service.createPreparation(
            productId,
            new CreateLearningCycleRequest(
                key(cycleId, "successor"),
                chainId,
                successor.getId(),
                cycleId,
                false,
                version,
                decision.path("nextHypothesis").asText(),
                decision.path("nextHypothesis").asText(),
                brief.path("successCriterion").asText(),
                brief.path("audience").asText(),
                brief.path("offer").asText(),
                "Canal de referência: "
                    + source.getPlatform()
                    + "; mídia zero, sem janela ou autorização herdada.",
                BigDecimal.ZERO,
                null,
                null,
                brief.path("sampleTarget").asInt(1),
                brief.path("minimumNetSales").asInt(1),
                OPERATOR));
    var learning = decision.deepCopy();
    learning.put("competingExplanation", decision.path("evidenceLimits").asText());
    var result =
        service.carryPreparedLearning(
            productId,
            prepared.id(),
            new LearningCycleCommand(
                key(cycleId, "learning"),
                prepared.revision(),
                LearningCycleCommand.Action.COMPLETE,
                OPERATOR,
                "Aprendizado preservado; planejamento encaminhado ao processo responsável sem executar tarefas pagas.",
                evidenceReference,
                learning));
    return result;
  }

  /** Expõe recuperação de parecer corrente ou encerramento aprovado, sem duplicar o sucessor. */
  @Transactional(readOnly = true)
  public boolean available(LearningSalesCycle cycle, LearningCycleDecisionProposal proposal) {
    if (proposal == null
        || proposal.getProposalJson() == null
        || !("READY".equals(proposal.getStatus()) || "APPROVED".equals(proposal.getStatus()))
        || cycles.findByPreviousCycleId(cycle.getId()).isPresent()) return false;
    if ("OPEN".equals(cycle.getStatus()))
      return "DECISION".equals(cycle.getStage())
          && "READY".equals(proposal.getStatus())
          && cycle.getRevision() == proposal.getCycleRevision()
          && LearningCyclePreparationPolicy.eligible(json.read(proposal.getProposalJson()));
    var decision = approvedDecision(cycle, proposal);
    return decision != null && LearningCyclePreparationPolicy.eligibleApproved(decision);
  }

  /** Combina o parecer com a edição final, preservando a ação e a autoria do encerramento. */
  private ObjectNode approvedDecision(
      LearningSalesCycle cycle, LearningCycleDecisionProposal proposal) {
    var evidence = approvedEvidence(cycle, proposal);
    if (evidence == null) return null;
    var decision = (ObjectNode) json.read(proposal.getProposalJson());
    for (String field :
        List.of(
            "rootCause",
            "learning",
            "nextHypothesis",
            "returnProcessId",
            "returnActivityId",
            "marketReview",
            "evidenceLimits")) if (evidence.has(field)) decision.set(field, evidence.get(field));
    decision.put("action", "ADJUSTED".equals(cycle.getStatus()) ? "ADJUST" : "INCONCLUSIVE");
    decision.put("humanApproved", evidence.path("humanApproved").asBoolean(false));
    decision.put("sourceDecisionEventId", proposal.getApprovedEventId());
    return decision;
  }

  /**
   * Recupera apenas recibo da mesma proposta, ação, ciclo e revisão final, sem reabrir histórico.
   */
  private JsonNode approvedEvidence(
      LearningSalesCycle cycle, LearningCycleDecisionProposal proposal) {
    if (!List.of("ADJUSTED", "INCONCLUSIVE").contains(cycle.getStatus())
        || !"DECISION".equals(cycle.getStage())
        || !"APPROVED".equals(proposal.getStatus())
        || proposal.getApprovedEventId() == null
        || cycle.getRevision() != proposal.getCycleRevision() + 1) return null;
    return events
        .findById(proposal.getApprovedEventId())
        .filter(
            event ->
                cycle.getId().equals(event.getCycleId())
                    && event.getRevision() == cycle.getRevision()
                    && ("ADJUSTED".equals(cycle.getStatus())
                        ? "ADJUST".equals(event.getAction())
                        : "INCONCLUSIVE".equals(event.getAction())))
        .map(event -> json.read(event.getEvidenceJson()))
        .filter(evidence -> evidence.path("decisionProposalId").asLong(-1) == proposal.getId())
        .orElse(null);
  }

  /** Copia somente o contexto da oferta; campanha, artefatos, janela e permissões nascem vazios. */
  private Experiment plannedExperiment(Experiment source, Long cycleId, ObjectNode proposal) {
    return Experiment.builder()
        .name(
            "Planejamento do ciclo "
                + cycleId
                + " · "
                + source.getName().substring(0, Math.min(190, source.getName().length())))
        .product(source.getProduct())
        .niche(source.getNiche())
        .hypothesisRef(source.getHypothesisRef())
        .experimentType(source.getExperimentType())
        .productAiSubtype(source.getProductAiSubtype())
        .campaignObjective(source.getCampaignObjective())
        .platform(source.getPlatform())
        .facebookPage(source.getFacebookPage())
        .instagramAccount(source.getInstagramAccount())
        .unitPrice(source.getUnitPrice())
        .desireTerritoryCode(source.getDesireTerritoryCode())
        .desireTerritorySnapshotJson(source.getDesireTerritorySnapshotJson())
        .hypothesis(source.getHypothesis())
        .learnedLessons(proposal.path("learning").asText())
        .status(ExperimentStatus.PLANNED)
        .dailyBudget(BigDecimal.ZERO)
        .mediaSpendLimit(BigDecimal.ZERO)
        .zeroResultSpendLimit(BigDecimal.ZERO)
        .zeroPurchaseSpendLimit(BigDecimal.ZERO)
        .creativeApproved(false)
        .build();
  }

  /** Deriva chaves estáveis para que replays não dupliquem decisões, ciclos ou aprendizados. */
  private UUID key(Long cycleId, String operation) {
    return UUID.nameUUIDFromBytes(
        (LearningCyclePreparationPolicy.CONTRACT + ":" + cycleId + ":" + operation)
            .getBytes(StandardCharsets.UTF_8));
  }
}
