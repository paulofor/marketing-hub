package com.marketinghub.businessprocesschain.learningcycle.v1.decision.service;

import static com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleRules.require;

import com.fasterxml.jackson.databind.node.ObjectNode;
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
  private final LearningCycleDecisionProposalRepository proposals;
  private final ProductRepository products;
  private final AgentRepository agents;
  private final ExperimentRepository experiments;
  private final BusinessProcessChainDefinitionRepository chains;
  private final LearningCycleService service;
  private final LearningCycleJson json;

  /** Serializa produto e ciclo, reaproveita sucessor e só materializa proposta vigente elegível. */
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
    require(
        "OPEN".equals(cycle.getStatus()) && "DECISION".equals(cycle.getStage()),
        "A preparação exige a decisão corrente, sem reabrir ciclos encerrados.");
    var proposal =
        proposals
            .findFirstByCycleIdAndCycleRevisionOrderByAttemptDesc(cycleId, cycle.getRevision())
            .orElseThrow();
    require("READY".equals(proposal.getStatus()), "Aguarde o parecer válido de Atena.");
    var decision = (ObjectNode) json.read(proposal.getProposalJson());
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
