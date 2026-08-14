package com.marketinghub.agentlearning.v1;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: expor o fluxo auditável de replay e promoção governada dos agentes. */
@RestController
@RequestMapping("/api/internal/agent-learning/v1")
public class GovernedAgentLearningController {
  private final GovernedAgentLearningService service;
  private final ApolloLearningOrchestrationService apolloService;
  private final ApolloSkillCandidateService skillService;

  /** Inicializa o controller com a governança central. */
  public GovernedAgentLearningController(
      GovernedAgentLearningService service,
      ApolloLearningOrchestrationService apolloService,
      ApolloSkillCandidateService skillService) {
    this.service = service;
    this.apolloService = apolloService;
    this.skillService = skillService;
  }

  /** Recebe um replay real de Apolo e acumula a amostra sem liberar geração paga. */
  @PostMapping("/agents/apollo/observations")
  public ApolloLearningObservationResponse observeApollo(
      @Valid @RequestBody ApolloLearningObservationRequest request) {
    return apolloService.observe(request);
  }

  /** Congela uma candidata e os casos de replay antes dos testes. */
  @PostMapping("/experiments")
  @ResponseStatus(HttpStatus.CREATED)
  public LearningExperimentResponse create(
      @Valid @RequestBody CreateLearningExperimentRequest request) {
    return service.create(request);
  }

  /** Registra resultados locais comparáveis e calcula elegibilidade. */
  @PostMapping("/experiments/{id}/evaluation")
  public LearningExperimentResponse evaluate(
      @PathVariable Long id, @Valid @RequestBody EvaluateLearningExperimentRequest request) {
    return service.evaluate(id, request);
  }

  /** Executa a promoção explícita após todos os gates locais. */
  @PostMapping("/experiments/{id}/promotion")
  public LearningExperimentResponse promote(@PathVariable Long id) {
    return service.promote(id);
  }

  /** Lista o playbook promovido que pode orientar o agente em um escopo. */
  @GetMapping("/agents/{agentKey}/promoted")
  public List<LearningExperimentResponse> promoted(
      @PathVariable String agentKey, @RequestParam String scopeType, @RequestParam String scopeId) {
    return service.promoted(agentKey, scopeType, scopeId);
  }

  /** Lista o histórico completo do agente para acompanhamento no Estúdio. */
  @GetMapping("/agents/{agentKey}/experiments")
  public List<LearningExperimentResponse> list(@PathVariable String agentKey) {
    return service.list(agentKey);
  }

  /** Lista skills candidatas e promovidas do piloto de Apolo. */
  @GetMapping("/agents/apollo/skills")
  public List<SkillCandidateResponse> skills() {
    return skillService.list();
  }

  /** Promove explicitamente uma skill que passou replay e segurança. */
  @PostMapping("/agents/apollo/skills/{id}/promotion")
  public SkillCandidateResponse promoteSkill(@PathVariable Long id) {
    return skillService.promote(id);
  }

  /** Registra o resultado real para detectar regressão pós-promoção. */
  @PostMapping("/agents/apollo/skills/{id}/monitoring")
  public SkillCandidateResponse monitorSkill(
      @PathVariable Long id, @Valid @RequestBody SkillMonitoringRequest request) {
    return skillService.monitor(id, request);
  }

  /** Reverte explicitamente uma skill promovida. */
  @PostMapping("/agents/apollo/skills/{id}/rollback")
  public SkillCandidateResponse rollbackSkill(@PathVariable Long id, @RequestParam String reason) {
    return skillService.rollback(id, reason);
  }
}
