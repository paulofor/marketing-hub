package com.marketinghub.agentlearning.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agentmemory.service.AgentMemoryService;
import com.marketinghub.agentmemory.service.registerFeedback.RegisterMemoryFeedbackRequest;
import com.marketinghub.repository.jpa.agentlearning.GovernedAgentLearningExperimentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: comparar estratégias de agentes em replay local e governar sua promoção. */
@Service
public class GovernedAgentLearningService {
  private static final int MINIMUM_REPLAY_CASES = 10;
  private static final int MINIMUM_HOLDOUT_CASES = 5;
  private static final Set<String> GOVERNED_AGENTS =
      Set.of("landing-generator", "meta-ad-approver", "apollo");
  private final GovernedAgentLearningExperimentRepository repository;
  private final AgentMemoryService memoryService;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  /** Inicializa a governança com persistência, memória oficial e relógio UTC. */
  @Autowired
  public GovernedAgentLearningService(
      GovernedAgentLearningExperimentRepository repository,
      AgentMemoryService memoryService,
      ObjectMapper objectMapper) {
    this(repository, memoryService, objectMapper, Clock.systemUTC());
  }

  /** Inicializa a governança com relógio controlável para testes. */
  GovernedAgentLearningService(
      GovernedAgentLearningExperimentRepository repository,
      AgentMemoryService memoryService,
      ObjectMapper objectMapper,
      Clock clock) {
    this.repository = repository;
    this.memoryService = memoryService;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  /** Congela baseline, candidata, replay e holdout antes de qualquer execução comparativa. */
  @Transactional
  public LearningExperimentResponse create(CreateLearningExperimentRequest request) {
    validateAgent(request.agentKey());
    validateFrozenSet(request.frozenReplaySetJson(), MINIMUM_REPLAY_CASES, "replay");
    validateFrozenSet(request.holdoutReplaySetJson(), MINIMUM_HOLDOUT_CASES, "holdout");
    GovernedAgentLearningExperiment value = new GovernedAgentLearningExperiment();
    value.setAgentKey(request.agentKey());
    value.setScopeType(request.scopeType());
    value.setScopeId(request.scopeId());
    value.setMemoryId(request.memoryId());
    value.setCandidateVersion(request.candidateVersion());
    value.setBaselineVersion(request.baselineVersion());
    value.setFrozenReplaySetJson(request.frozenReplaySetJson());
    value.setHoldoutReplaySetJson(request.holdoutReplaySetJson());
    value.setMinimumGain(request.minimumGain());
    value.setMaximumCostIncreaseRatio(request.maximumCostIncreaseRatio());
    value.setStatus("FROZEN");
    value.setCreatedAt(clock.instant());
    return response(repository.save(value));
  }

  /** Avalia o holdout e fecha o gate sem permitir promoção pelo próprio executor do agente. */
  @Transactional
  public LearningExperimentResponse evaluate(Long id, EvaluateLearningExperimentRequest request) {
    GovernedAgentLearningExperiment value = find(id);
    if (!"FROZEN".equals(value.getStatus())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Experimento já avaliado");
    }
    int frozenReplayCases = arraySize(value.getFrozenReplaySetJson(), "replay");
    int frozenHoldoutCases = arraySize(value.getHoldoutReplaySetJson(), "holdout");
    if (request.replayCaseCount() != frozenReplayCases
        || request.holdoutCaseCount() != frozenHoldoutCases) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Contagens de resultado divergem dos conjuntos congelados");
    }
    if (request.externalProviderCalled()
        || request.spendingAuthorized()
        || request.publicationPerformed()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Replay sombra não permite provider externo, autorização de gasto ou publicação");
    }
    BigDecimal gain = request.candidateHoldoutScore().subtract(request.baselineHoldoutScore());
    BigDecimal costRatio = costRatio(request.baselineCost(), request.candidateCost());
    boolean enoughCases =
        request.replayCaseCount() >= MINIMUM_REPLAY_CASES
            && request.holdoutCaseCount() >= MINIMUM_HOLDOUT_CASES;
    boolean eligible =
        enoughCases
            && request.regressionPassed()
            && request.localValidationPassed()
            && gain.compareTo(value.getMinimumGain()) >= 0
            && costRatio.compareTo(value.getMaximumCostIncreaseRatio()) <= 0;
    value.setBaselineResultJson(request.baselineResultJson());
    value.setCandidateResultJson(request.candidateResultJson());
    value.setRegressionPassed(request.regressionPassed());
    value.setLocalValidationPassed(request.localValidationPassed());
    value.setStatus(eligible ? "READY_FOR_PROMOTION" : "REJECTED");
    value.setDecisionEvidence(
        "holdoutGain="
            + gain
            + "; costIncreaseRatio="
            + costRatio
            + "; replayCases="
            + request.replayCaseCount()
            + "; holdoutCases="
            + request.holdoutCaseCount()
            + "; regressionPassed="
            + request.regressionPassed()
            + "; localValidationPassed="
            + request.localValidationPassed()
            + "; externalEffects=false");
    value.setEvaluatedAt(clock.instant());
    return response(repository.save(value));
  }

  /** Promove somente uma candidata aprovada localmente e confirma sua memória por fonte externa. */
  @Transactional
  public LearningExperimentResponse promote(Long id) {
    GovernedAgentLearningExperiment value = find(id);
    if (!"READY_FOR_PROMOTION".equals(value.getStatus())
        || !value.isRegressionPassed()
        || !value.isLocalValidationPassed()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Estratégia sem evidência local suficiente para promoção");
    }
    memoryService.feedback(
        value.getAgentKey(),
        value.getMemoryId(),
        new RegisterMemoryFeedbackRequest(
            "CONFIRMED",
            "Promoção governada após replay, holdout e regressão locais: "
                + value.getDecisionEvidence(),
            "agent-learning-experiment/" + value.getId()));
    value.setStatus("PROMOTED");
    value.setPromotedAt(clock.instant());
    return response(repository.save(value));
  }

  /** Lista somente estratégias promovidas para o agente e escopo congelados. */
  @Transactional(readOnly = true)
  public List<LearningExperimentResponse> promoted(
      String agentKey, String scopeType, String scopeId) {
    validateAgent(agentKey);
    return repository
        .findByAgentKeyAndScopeTypeAndScopeIdAndStatusOrderByIdDesc(
            agentKey, scopeType, scopeId, "PROMOTED")
        .stream()
        .map(this::response)
        .toList();
  }

  /** Lista os experimentos auditáveis do agente sem permitir mutação pelo painel. */
  @Transactional(readOnly = true)
  public List<LearningExperimentResponse> list(String agentKey) {
    validateAgent(agentKey);
    return repository.findByAgentKeyOrderByIdDesc(agentKey).stream().map(this::response).toList();
  }

  /** Calcula aumento relativo de custo e bloqueia baseline zero com candidata onerosa. */
  private BigDecimal costRatio(BigDecimal baseline, BigDecimal candidate) {
    if (baseline.signum() == 0) {
      return candidate.signum() == 0 ? BigDecimal.ZERO : new BigDecimal("999999");
    }
    return candidate
        .subtract(baseline)
        .divide(baseline, 4, RoundingMode.HALF_UP)
        .max(BigDecimal.ZERO);
  }

  /** Restringe a primeira versão aos agentes com avaliador de replay homologado. */
  private void validateAgent(String agentKey) {
    if (!GOVERNED_AGENTS.contains(agentKey)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agente fora da governança v1");
    }
  }

  /** Valida que o conjunto é um array JSON com amostra mínima antes de congelá-lo. */
  private void validateFrozenSet(String json, int minimum, String name) {
    if (arraySize(json, name) < minimum) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Conjunto " + name + " abaixo da amostra mínima");
    }
  }

  /** Lê o tamanho real do conjunto sem confiar em contagem enviada após a execução. */
  private int arraySize(String json, String name) {
    try {
      JsonNode node = objectMapper.readTree(json);
      if (!node.isArray()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Conjunto " + name + " deve ser um array JSON");
      }
      return node.size();
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Conjunto " + name + " contém JSON inválido", ex);
    }
  }

  /** Localiza o experimento ou responde ausência de forma canônica. */
  private GovernedAgentLearningExperiment find(Long id) {
    return repository
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experimento não encontrado"));
  }

  /** Converte a entidade na visão auditável permitida aos consumidores. */
  private LearningExperimentResponse response(GovernedAgentLearningExperiment value) {
    return new LearningExperimentResponse(
        value.getId(),
        value.getAgentKey(),
        value.getScopeType(),
        value.getScopeId(),
        value.getCandidateVersion(),
        value.getBaselineVersion(),
        value.getStatus(),
        value.getMemoryId(),
        value.getBaselineResultJson(),
        value.getCandidateResultJson(),
        value.getDecisionEvidence(),
        value.getMinimumGain(),
        value.getMaximumCostIncreaseRatio(),
        value.isRegressionPassed(),
        value.isLocalValidationPassed(),
        value.getCreatedAt(),
        value.getEvaluatedAt(),
        value.getPromotedAt());
  }
}
