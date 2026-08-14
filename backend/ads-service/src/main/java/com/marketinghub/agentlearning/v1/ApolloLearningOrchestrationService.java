package com.marketinghub.agentlearning.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agentmemory.service.AgentMemoryService;
import com.marketinghub.agentmemory.service.registerMemory.RegisterMemoryRequest;
import com.marketinghub.agentmemory.service.retrieveMemory.MemoryResponse;
import com.marketinghub.repository.jpa.agentlearning.ApolloLearningObservationRepository;
import com.marketinghub.repository.jpa.agentlearning.GovernedAgentLearningExperimentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: transformar replays reais de Apolo em aprendizado governado sem efeitos
 * externos.
 */
@Service
public class ApolloLearningOrchestrationService {
  private static final int REPLAY_CASES = 10;
  private static final int HOLDOUT_CASES = 5;
  private static final int REQUIRED_CASES = REPLAY_CASES + HOLDOUT_CASES;
  private static final String QA_REVIEWER = "BACKEND_DETERMINISTIC_QA_V1";
  private final ApolloLearningObservationRepository observations;
  private final GovernedAgentLearningExperimentRepository experiments;
  private final AgentMemoryService memoryService;
  private final GovernedAgentLearningService learningService;
  private final ApolloSkillCandidateService skillService;
  private final ObjectMapper objectMapper;
  private final Clock clock = Clock.systemUTC();

  /** Inicializa a orquestração com observações, memória, experimento e serialização oficiais. */
  public ApolloLearningOrchestrationService(
      ApolloLearningObservationRepository observations,
      GovernedAgentLearningExperimentRepository experiments,
      AgentMemoryService memoryService,
      GovernedAgentLearningService learningService,
      ApolloSkillCandidateService skillService,
      ObjectMapper objectMapper) {
    this.observations = observations;
    this.experiments = experiments;
    this.memoryService = memoryService;
    this.learningService = learningService;
    this.skillService = skillService;
    this.objectMapper = objectMapper;
  }

  /** Registra uma comparação idempotente e fecha automaticamente a primeira amostra completa. */
  @Transactional
  public ApolloLearningObservationResponse observe(ApolloLearningObservationRequest request) {
    blockExternalEffects(request);
    ApolloLearningObservation observation =
        observations
            .findByJobId(request.jobId())
            .orElseGet(() -> observations.save(toObservation(request)));
    List<ApolloLearningObservation> sample =
        observations.findByScopeIdAndBaselineVersionAndCandidateVersionOrderByIdAsc(
            request.scopeId(), request.baselineVersion(), request.candidateVersion());
    var existing =
        experiments.findByAgentKeyAndCandidateVersion("apollo", request.candidateVersion());
    if (existing.isPresent()) {
      return new ApolloLearningObservationResponse(
          observation.getId(),
          sample.size(),
          REQUIRED_CASES,
          existing.get().getId(),
          existing.get().getStatus());
    }
    if (sample.size() < REQUIRED_CASES) {
      return new ApolloLearningObservationResponse(
          observation.getId(), sample.size(), REQUIRED_CASES, null, "COLLECTING_REPLAY_CASES");
    }
    List<ApolloLearningObservation> frozen = sample.subList(0, REQUIRED_CASES);
    MemoryResponse memory =
        memoryService.register(
            "apollo",
            new RegisterMemoryRequest(
                null,
                "VIDEO_STORYBOARD",
                request.scopeId(),
                "DIRECAO_AUDIOVISUAL",
                "Candidata "
                    + request.candidateVersion()
                    + " superou a baseline "
                    + request.baselineVersion()
                    + " no replay sombra; aguarda promoção independente.",
                "Amostra congelada de 10 replays e 5 holdouts, QA=" + QA_REVIEWER,
                "apollo-learning/" + request.candidateVersion(),
                "job/" + request.jobId(),
                new BigDecimal("0.60"),
                null));
    String replayJson = json(frozen.subList(0, REPLAY_CASES));
    String holdoutJson = json(frozen.subList(REPLAY_CASES, REQUIRED_CASES));
    LearningExperimentResponse created =
        learningService.create(
            new CreateLearningExperimentRequest(
                "apollo",
                "VIDEO_STORYBOARD",
                request.scopeId(),
                memory.id(),
                request.candidateVersion(),
                request.baselineVersion(),
                replayJson,
                holdoutJson,
                new BigDecimal("1.00"),
                BigDecimal.ZERO));
    List<ApolloLearningObservation> holdout = frozen.subList(REPLAY_CASES, REQUIRED_CASES);
    boolean qaPassed = frozen.stream().allMatch(ApolloLearningObservation::isQaPassed);
    LearningExperimentResponse evaluated =
        learningService.evaluate(
            created.id(),
            new EvaluateLearningExperimentRequest(
                averageScore(holdout, false),
                averageScore(holdout, true),
                averageCost(holdout, false),
                averageCost(holdout, true),
                REPLAY_CASES,
                HOLDOUT_CASES,
                qaPassed,
                qaPassed,
                false,
                false,
                false,
                resultJson(frozen, false),
                resultJson(frozen, true)));
    skillService.createForExperiment(
        evaluated,
        skillContent(request.candidateVersion()),
        "Transforma padrões do replay em regras de roteiro, diversidade visual, demonstração do mecanismo e controle de retrabalho.",
        provenanceJson(frozen, request));
    return new ApolloLearningObservationResponse(
        observation.getId(), sample.size(), REQUIRED_CASES, evaluated.id(), evaluated.status());
  }

  /** Produz a skill operacional restrita ao planejamento criativo, sem ampliar autoridade. */
  private String skillContent(String candidateVersion) {
    return "# Skill "
        + candidateVersion
        + "\n"
        + "- Remover linguagem interna e repetições do roteiro.\n"
        + "- Demonstrar visualmente dor, mecanismo, transformação e CTA.\n"
        + "- Reaproveitar ativos aprovados antes de solicitar nova geração.\n"
        + "- Submeter storyboard ao QA independente e ao teto aprovado por Plutus.\n"
        + "- Nunca liberar provider, gasto ou publicação por decisão desta skill.";
  }

  /** Registra as trajetórias exatas que originaram a candidata. */
  private String provenanceJson(
      List<ApolloLearningObservation> frozen, ApolloLearningObservationRequest request) {
    return write(
        java.util.Map.of(
            "source", "APOLLO_REAL_STORYBOARD_TRAJECTORIES",
            "baselineVersion", request.baselineVersion(),
            "candidateVersion", request.candidateVersion(),
            "qaReviewer", QA_REVIEWER,
            "jobIds", frozen.stream().map(ApolloLearningObservation::getJobId).toList()));
  }

  /** Bloqueia qualquer relato que tente usar o replay para gastar, publicar ou chamar provider. */
  private void blockExternalEffects(ApolloLearningObservationRequest request) {
    if (request.providerCalled()
        || request.spendingAuthorized()
        || request.publicationPerformed()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Replay de Apolo deve permanecer sem efeitos externos");
    }
  }

  /** Materializa a observação e aplica o QA determinístico independente do executor. */
  private ApolloLearningObservation toObservation(ApolloLearningObservationRequest request) {
    ApolloLearningObservation value = new ApolloLearningObservation();
    value.setJobId(request.jobId());
    value.setScopeId(request.scopeId());
    value.setBaselineVersion(request.baselineVersion());
    value.setCandidateVersion(request.candidateVersion());
    value.setBaselineScore(request.baselineScore());
    value.setCandidateScore(request.candidateScore());
    value.setBaselineCost(request.baselineCost());
    value.setCandidateCost(request.candidateCost());
    value.setComparisonJson(request.comparisonJson());
    value.setQaReviewer(QA_REVIEWER);
    value.setQaPassed(independentQa(request));
    value.setCreatedAt(clock.instant());
    return value;
  }

  /** Revalida o contrato bruto no backend sem confiar nas notas declaradas por Apolo. */
  private boolean independentQa(ApolloLearningObservationRequest request) {
    try {
      var root = objectMapper.readTree(request.comparisonJson());
      var comparison = root.path("comparison");
      var current = comparison.path("current");
      var candidate = comparison.path("candidate");
      boolean valuesMatch =
          current.path("qualityScore").decimalValue().compareTo(request.baselineScore()) == 0
              && candidate.path("qualityScore").decimalValue().compareTo(request.candidateScore())
                  == 0
              && current.path("expectedCostUsd").decimalValue().compareTo(request.baselineCost())
                  == 0
              && candidate.path("expectedCostUsd").decimalValue().compareTo(request.candidateCost())
                  == 0;
      return root.path("shadowMode").asBoolean(false)
          && !root.path("providerCalled").asBoolean(true)
          && !root.path("spendingAuthorized").asBoolean(true)
          && comparison.path("shadowMode").asBoolean(false)
          && candidate.path("gateApproved").asBoolean(false)
          && candidate.path("qualityScore").asInt() >= 70
          && candidate.path("qualityScore").asInt() > current.path("qualityScore").asInt()
          && candidate
                  .path("expectedCostUsd")
                  .decimalValue()
                  .compareTo(current.path("expectedCostUsd").decimalValue())
              <= 0
          && valuesMatch;
    } catch (Exception ex) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Comparação bruta de Apolo é inválida", ex);
    }
  }

  /** Calcula a média de nota da amostra congelada. */
  private BigDecimal averageScore(List<ApolloLearningObservation> values, boolean candidate) {
    return average(
        values.stream()
            .map(value -> candidate ? value.getCandidateScore() : value.getBaselineScore())
            .toList());
  }

  /** Calcula a média de custo da amostra congelada. */
  private BigDecimal averageCost(List<ApolloLearningObservation> values, boolean candidate) {
    return average(
        values.stream()
            .map(value -> candidate ? value.getCandidateCost() : value.getBaselineCost())
            .toList());
  }

  /** Calcula média decimal estável para decisão auditável. */
  private BigDecimal average(List<BigDecimal> values) {
    return values.stream()
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
  }

  /** Serializa os casos completos usados na amostra congelada. */
  private String json(List<ApolloLearningObservation> values) {
    return write(
        values.stream()
            .map(
                value -> {
                  try {
                    return objectMapper.readTree(value.getComparisonJson());
                  } catch (JsonProcessingException ex) {
                    throw new IllegalStateException(
                        "Caso congelado de Apolo contém JSON inválido", ex);
                  }
                })
            .toList());
  }

  /** Consolida os resultados de baseline ou candidata sem recomputar no frontend. */
  private String resultJson(List<ApolloLearningObservation> values, boolean candidate) {
    return write(
        java.util.Map.of(
            "reviewer",
            QA_REVIEWER,
            "cases",
            values.size(),
            "score",
            averageScore(values, candidate),
            "cost",
            averageCost(values, candidate)));
  }

  /** Serializa auditoria e preserva a causa completa em caso de falha interna. */
  private String write(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Falha ao serializar replay governado de Apolo", ex);
    }
  }
}
