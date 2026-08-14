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

  /** Inicializa o controller com a governança central. */
  public GovernedAgentLearningController(
      GovernedAgentLearningService service, ApolloLearningOrchestrationService apolloService) {
    this.service = service;
    this.apolloService = apolloService;
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
}
