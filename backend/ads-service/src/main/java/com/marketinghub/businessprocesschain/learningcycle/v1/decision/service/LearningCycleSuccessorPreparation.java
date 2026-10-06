package com.marketinghub.businessprocesschain.learningcycle.v1.decision.service;

import static com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleRules.require;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.LearningCycleDecisionProposal;
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
   * Serializa a preparação elegível e enfileira o planejamento quando há decisão humana aprovada.
   */
  @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
  public LearningCycleResponse prepare(Long productId, Long cycleId) {
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
    boolean approved = "ADJUSTED".equals(cycle.getStatus());
    require(
        ("OPEN".equals(cycle.getStatus()) || approved) && "DECISION".equals(cycle.getStage()),
        "A preparação exige decisão corrente ou ajuste aprovado, sem reabrir operação encerrada.");
    var proposal =
        (approved
                ? proposals.findFirstByCycleIdOrderByIdDesc(cycleId)
                : proposals.findFirstByCycleIdAndCycleRevisionOrderByAttemptDesc(
                    cycleId, cycle.getRevision()))
            .orElseThrow();
    require(
        available(cycle, proposal),
        "Aguarde o parecer ou a decisão de ajuste vigente e compatível.");
    var decision = (ObjectNode) json.read(proposal.getProposalJson());
    boolean humanApproved =
        approved && approvedEvidence(cycle, proposal).path("humanApproved").asBoolean(false);
    if (approved) {
      var evidence = approvedEvidence(cycle, proposal);
      for (String field :
          List.of(
              "rootCause",
              "learning",
              "nextHypothesis",
              "returnProcessId",
              "returnActivityId",
              "marketReview")) if (evidence.has(field)) decision.set(field, evidence.get(field));
      decision.put("sourceDecisionEventId", proposal.getApprovedEventId());
    }
    require(
        LearningCyclePreparationPolicy.eligible(decision),
        "A proposta requer decisão sobre mercado ou operação; a preparação automática é restrita a ajuste no mesmo foco.");
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
    var successor = experiments.saveAndFlush(plannedExperiment(source, cycleId, decision));
    var prepared =
        service.createPreparation(
            productId,
            new CreateLearningCycleRequest(
                key(cycleId, "successor"),
                chain.getId(),
                successor.getId(),
                cycleId,
                false,
                cycle.getProductVersion(),
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
                "internal://learning-cycles/" + cycleId + "/decision-proposals/" + proposal.getId(),
                learning));
    if (humanApproved) {
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
        "Ciclo: sucessor preparado policy={} productId={} predecessor={} proposalId={} cycleId={} experimentId={} spendAuthorized=false",
        LearningCyclePreparationPolicy.CONTRACT,
        productId,
        cycleId,
        proposal.getId(),
        result.id(),
        successor.getId());
    return result;
  }

  /**
   * Expõe recuperação de parecer corrente ou ajuste aprovado, sem duplicar um sucessor existente.
   */
  @Transactional(readOnly = true)
  public boolean available(LearningSalesCycle cycle, LearningCycleDecisionProposal proposal) {
    if (proposal == null
        || proposal.getProposalJson() == null
        || !("READY".equals(proposal.getStatus()) || "APPROVED".equals(proposal.getStatus()))
        || !LearningCyclePreparationPolicy.eligible(json.read(proposal.getProposalJson()))
        || cycles.findByPreviousCycleId(cycle.getId()).isPresent()) return false;
    return ("OPEN".equals(cycle.getStatus())
            && "DECISION".equals(cycle.getStage())
            && "READY".equals(proposal.getStatus())
            && cycle.getRevision() == proposal.getCycleRevision())
        || approvedEvidence(cycle, proposal) != null;
  }

  /** Recupera somente o recibo final vinculado à proposta e à revisão encerrada para ajuste. */
  private JsonNode approvedEvidence(
      LearningSalesCycle cycle, LearningCycleDecisionProposal proposal) {
    if (!"ADJUSTED".equals(cycle.getStatus())
        || !"APPROVED".equals(proposal.getStatus())
        || proposal.getApprovedEventId() == null
        || cycle.getRevision() != proposal.getCycleRevision() + 1) return null;
    return events
        .findById(proposal.getApprovedEventId())
        .filter(
            event ->
                cycle.getId().equals(event.getCycleId())
                    && event.getRevision() == cycle.getRevision()
                    && "ADJUST".equals(event.getAction()))
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
