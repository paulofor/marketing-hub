package com.marketinghub.agentlearning.v1;

import com.marketinghub.repository.jpa.agentlearning.GovernedAgentSkillCandidateRepository;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: governar criação, segurança, promoção, monitoramento e rollback das skills de
 * Apolo.
 */
@Service
public class ApolloSkillCandidateService {
  private static final Set<String> FORBIDDEN =
      Set.of(
          "autorizar gasto",
          "publicar automaticamente",
          "ignorar plutus",
          "remover qa",
          "chamar provider pago",
          "ampliar autoridade",
          "expor credencial");
  private static final int MINIMUM_MONITORED_CASES = 5;
  private final GovernedAgentSkillCandidateRepository repository;
  private final Clock clock;

  /** Inicializa o ciclo da skill com persistência e relógio UTC. */
  @Autowired
  public ApolloSkillCandidateService(GovernedAgentSkillCandidateRepository repository) {
    this(repository, Clock.systemUTC());
  }

  /** Inicializa o ciclo com relógio controlável para testes. */
  ApolloSkillCandidateService(GovernedAgentSkillCandidateRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  /** Cria idempotentemente a skill a partir do experimento congelado e registra sua procedência. */
  @Transactional
  public SkillCandidateResponse createForExperiment(
      LearningExperimentResponse experiment,
      String content,
      String diffSummary,
      String provenanceJson) {
    return repository
        .findByExperimentId(experiment.id())
        .map(this::response)
        .orElseGet(
            () -> {
              GovernedAgentSkillCandidate value = new GovernedAgentSkillCandidate();
              value.setExperimentId(experiment.id());
              value.setAgentKey("apollo");
              value.setSkillKey("MUSA_COMMERCIAL_STORYBOARD");
              value.setBaselineVersion(experiment.baselineVersion());
              value.setCandidateVersion(experiment.candidateVersion());
              value.setContent(content);
              value.setDiffSummary(diffSummary);
              value.setProvenanceJson(provenanceJson);
              String violation = safetyViolation(content + " " + diffSummary);
              value.setSafetyDecision(violation == null ? "APPROVED" : "REJECTED");
              value.setSafetyEvidence(
                  violation == null
                      ? "SAFE_EVOLVE_V1: autoridade, gasto, publicação, credenciais e QA preservados"
                      : "SAFE_EVOLVE_V1: instrução proibida detectada: " + violation);
              value.setStatus(
                  violation == null && "READY_FOR_PROMOTION".equals(experiment.status())
                      ? "READY_FOR_PROMOTION"
                      : "REJECTED");
              value.setCreatedAt(clock.instant());
              value.setUpdatedAt(clock.instant());
              return response(repository.save(value));
            });
  }

  /** Promove explicitamente uma skill aprovada pelo replay e pelo crítico independente. */
  @Transactional
  public SkillCandidateResponse promote(Long id) {
    GovernedAgentSkillCandidate value = find(id);
    if (!"READY_FOR_PROMOTION".equals(value.getStatus())
        || !"APPROVED".equals(value.getSafetyDecision())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Skill sem gates suficientes para promoção");
    }
    value.setStatus("PROMOTED_MONITORING");
    value.setPromotedAt(clock.instant());
    value.setUpdatedAt(clock.instant());
    return response(repository.save(value));
  }

  /**
   * Registra resultado posterior e executa rollback automático em incidente, custo ou regressão.
   */
  @Transactional
  public SkillCandidateResponse monitor(Long id, SkillMonitoringRequest request) {
    GovernedAgentSkillCandidate value = find(id);
    if (!Set.of("PROMOTED_MONITORING", "PROMOTED").contains(value.getStatus())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Skill não está em monitoramento");
    }
    value.setMonitoredCases(value.getMonitoredCases() + 1);
    if (request.approved()) value.setApprovedCases(value.getApprovedCases() + 1);
    if (request.safetyIncident() || !request.costLimitRespected()) {
      rollback(value, request.evidence());
    } else if (value.getMonitoredCases() >= MINIMUM_MONITORED_CASES) {
      double approvalRate = (double) value.getApprovedCases() / value.getMonitoredCases();
      if (approvalRate < 0.60d) rollback(value, "Regressão pós-promoção: " + request.evidence());
      else value.setStatus("PROMOTED");
    }
    value.setUpdatedAt(clock.instant());
    return response(repository.save(value));
  }

  /** Executa rollback explícito preservando a versão e toda a evidência. */
  @Transactional
  public SkillCandidateResponse rollback(Long id, String reason) {
    GovernedAgentSkillCandidate value = find(id);
    if (!Set.of("PROMOTED_MONITORING", "PROMOTED").contains(value.getStatus())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Somente skill promovida aceita rollback");
    }
    rollback(value, reason);
    value.setUpdatedAt(clock.instant());
    return response(repository.save(value));
  }

  /** Lista o histórico auditável do piloto de Apolo. */
  @Transactional(readOnly = true)
  public List<SkillCandidateResponse> list() {
    return repository.findByAgentKeyOrderByIdDesc("apollo").stream().map(this::response).toList();
  }

  /** Identifica instruções que ampliariam autoridade persistente. */
  private String safetyViolation(String value) {
    String normalized = value.toLowerCase(Locale.ROOT);
    return FORBIDDEN.stream().filter(normalized::contains).findFirst().orElse(null);
  }

  /** Marca rollback e preserva a causa operacional. */
  private void rollback(GovernedAgentSkillCandidate value, String reason) {
    value.setStatus("ROLLED_BACK");
    value.setRolledBackAt(clock.instant());
    value.setRollbackReason(reason);
  }

  /** Localiza a skill ou responde ausência canônica. */
  private GovernedAgentSkillCandidate find(Long id) {
    return repository
        .findById(id)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Skill candidata não encontrada"));
  }

  /** Converte a entidade na visão auditável. */
  private SkillCandidateResponse response(GovernedAgentSkillCandidate v) {
    return new SkillCandidateResponse(
        v.getId(),
        v.getExperimentId(),
        v.getAgentKey(),
        v.getSkillKey(),
        v.getBaselineVersion(),
        v.getCandidateVersion(),
        v.getContent(),
        v.getDiffSummary(),
        v.getProvenanceJson(),
        v.getSafetyDecision(),
        v.getSafetyEvidence(),
        v.getStatus(),
        v.getMonitoredCases(),
        v.getApprovedCases(),
        v.getPromotedAt(),
        v.getRolledBackAt(),
        v.getRollbackReason(),
        v.getCreatedAt(),
        v.getUpdatedAt());
  }
}
