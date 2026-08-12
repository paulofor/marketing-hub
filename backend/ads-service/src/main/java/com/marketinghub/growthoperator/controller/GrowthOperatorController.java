package com.marketinghub.growthoperator.controller;

import com.marketinghub.growthoperator.service.GrowthOperatorService;
import com.marketinghub.growthoperator.service.action.GrowthOperatorExperimentActionRequest;
import com.marketinghub.growthoperator.service.action.ResolveGrowthOperatorTaskRequest;
import com.marketinghub.growthoperator.service.result.CompleteGrowthOperatorRequest;
import com.marketinghub.growthoperator.service.result.FailGrowthOperatorRequest;
import com.marketinghub.growthoperator.service.start.StartGrowthOperatorRequest;
import com.marketinghub.growthoperator.service.view.GrowthOperatorExecutionResponse;
import com.marketinghub.growthoperator.service.view.GrowthOperatorMcpToolResponse;
import com.marketinghub.growthoperator.service.view.GrowthOperatorTaskResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor o contrato unico do Operador de Crescimento v1. */
@RestController
@RequestMapping("/api/growth-operator/v1")
public class GrowthOperatorController {
  private final GrowthOperatorService service;

  public GrowthOperatorController(GrowthOperatorService service) {
    this.service = service;
  }

  /** Solicita um diagnostico somente leitura para uma semana do planejamento. */
  @PostMapping("/commercial-plans/{planId}/executions")
  public GrowthOperatorExecutionResponse start(
      @PathVariable Long planId, @RequestBody StartGrowthOperatorRequest request) {
    return service.start(planId, request);
  }

  /** Lista diagnosticos vinculados ao planejamento. */
  @GetMapping("/commercial-plans/{planId}/executions")
  public List<GrowthOperatorExecutionResponse> list(@PathVariable Long planId) {
    return service.list(planId);
  }

  /** Lista pendencias abertas e concluidas do planejamento. */
  @GetMapping("/commercial-plans/{planId}/tasks")
  public List<GrowthOperatorTaskResponse> listTasks(@PathVariable Long planId) {
    return service.listTasks(planId);
  }

  /** Registra a evidencia humana que conclui uma pendencia. */
  @PostMapping("/commercial-plans/{planId}/tasks/{taskId}/resolve")
  public GrowthOperatorTaskResponse resolveTask(
      @PathVariable Long planId,
      @PathVariable Long taskId,
      @RequestBody ResolveGrowthOperatorTaskRequest request) {
    return service.resolveTask(planId, taskId, request);
  }

  /** Lista as ferramentas MCP que o Operador pode consultar. */
  @GetMapping("/mcp-tools")
  public List<GrowthOperatorMcpToolResponse> listMcpTools() {
    return service.listMcpTools();
  }

  /** Entrega inteligencia detalhada e anonimizada de sessoes para consulta direta do agente. */
  @GetMapping("/internal/commercial-plans/{planId}/session-intelligence")
  public Map<String, Object> sessionIntelligence(
      @PathVariable Long planId, @RequestParam(defaultValue = "2000") int eventLimit) {
    return service.sessionIntelligence(planId, eventLimit);
  }

  /** Entrega estrategia, custos, progressao e aprendizados dos videos ao agente. */
  @GetMapping("/internal/commercial-plans/{planId}/video-strategy-intelligence")
  public Map<String, Object> videoStrategyIntelligence(@PathVariable Long planId) {
    return service.videoStrategyIntelligence(planId);
  }

  /** Solicita pausa preventiva submetida aos gates deterministas do backend. */
  @PostMapping("/internal/commercial-plans/{planId}/experiment/pause")
  public Map<String, Object> requestPreventivePause(
      @PathVariable Long planId, @RequestBody GrowthOperatorExperimentActionRequest request) {
    return service.requestPreventivePause(planId, request);
  }

  /** Registra uma solicitacao de retomada sem reativar o experimento. */
  @PostMapping("/internal/commercial-plans/{planId}/experiment/resume-request")
  public Map<String, Object> requestExperimentResume(
      @PathVariable Long planId, @RequestBody GrowthOperatorExperimentActionRequest request) {
    return service.requestExperimentResume(planId, request);
  }

  /** Reserva a proxima pendencia para o worker executor. */
  @PostMapping("/internal/executions/pending/claim")
  public GrowthOperatorExecutionResponse claimPending() {
    return service.claimPending();
  }

  /** Solicita ao backend que decida se a cadencia permite criar o proximo ciclo. */
  @PostMapping("/internal/commercial-plans/{planId}/executions/ensure")
  public GrowthOperatorExecutionResponse ensureAutomaticCycle(@PathVariable Long planId) {
    return service.ensureAutomaticCycle(planId);
  }

  /** Garante que nenhum plano comercial aberto fique sem uma próxima análise persistida. */
  @PostMapping("/internal/commercial-plans/executions/ensure-active")
  public List<GrowthOperatorExecutionResponse> ensureActivePlanCycles() {
    return service.ensureActivePlanCycles();
  }

  /** Recebe um diagnostico estruturado sem aplicar a recomendacao. */
  @PostMapping("/internal/executions/{id}/complete")
  public GrowthOperatorExecutionResponse complete(
      @PathVariable Long id, @RequestBody CompleteGrowthOperatorRequest request) {
    return service.complete(id, request);
  }

  /** Recebe uma falha tecnica do worker. */
  @PostMapping("/internal/executions/{id}/fail")
  public GrowthOperatorExecutionResponse fail(
      @PathVariable Long id, @RequestBody FailGrowthOperatorRequest request) {
    return service.fail(id, request);
  }
}
