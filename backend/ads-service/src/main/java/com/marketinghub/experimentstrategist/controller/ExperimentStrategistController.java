package com.marketinghub.experimentstrategist.controller;

import com.marketinghub.experimentstrategist.memory.ExperimentStrategistMemoryService;
import com.marketinghub.experimentstrategist.memory.ExperimentStrategistMemoryService.ArtifactRequest;
import com.marketinghub.experimentstrategist.memory.ExperimentStrategistMemoryService.ArtifactResponse;
import com.marketinghub.experimentstrategist.memory.ExperimentStrategistMemoryService.CreateMemoryRequest;
import com.marketinghub.experimentstrategist.memory.ExperimentStrategistMemoryService.MemoryResponse;
import com.marketinghub.experimentstrategist.service.ExperimentStrategistContextService;
import com.marketinghub.experimentstrategist.service.ExperimentStrategistExecutionService;
import com.marketinghub.experimentstrategist.service.ExperimentStrategistExecutionService.CompleteRequest;
import com.marketinghub.experimentstrategist.service.ExperimentStrategistExecutionService.ExecutionResponse;
import com.marketinghub.experimentstrategist.service.ExperimentStrategistExecutionService.FailRequest;
import com.marketinghub.experimentstrategist.service.ExperimentStrategistExecutionService.StartRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor contexto e registros auditaveis da memoria do Estrategista v1. */
@RestController
@RequestMapping("/api/experiment-strategist/v1")
public class ExperimentStrategistController {
  private final ExperimentStrategistContextService service;
  private final ExperimentStrategistMemoryService memoryService;
  private final ExperimentStrategistExecutionService executionService;

  /** Configura o serviço canônico de contexto estratégico. */
  public ExperimentStrategistController(
      ExperimentStrategistContextService service,
      ExperimentStrategistMemoryService memoryService,
      ExperimentStrategistExecutionService executionService) {
    this.service = service;
    this.memoryService = memoryService;
    this.executionService = executionService;
  }

  /** Solicita uma pesquisa estrategica somente leitura. */
  @PostMapping("/commercial-plans/{planId}/executions")
  public ExecutionResponse start(@PathVariable Long planId, @RequestBody StartRequest request) {
    return executionService.start(planId, request);
  }

  /** Lista as pesquisas e os pareceres do planejamento. */
  @GetMapping("/commercial-plans/{planId}/executions")
  public List<ExecutionResponse> listExecutions(@PathVariable Long planId) {
    return executionService.list(planId);
  }

  /** Reserva a proxima pesquisa para o worker executor. */
  @PostMapping("/internal/executions/pending/claim")
  public ExecutionResponse claimPending() {
    return executionService.claim();
  }

  /** Recebe um parecer estruturado sem executar sua recomendacao. */
  @PostMapping("/internal/executions/{id}/complete")
  public ExecutionResponse complete(@PathVariable Long id, @RequestBody CompleteRequest request) {
    return executionService.complete(id, request);
  }

  /** Recebe a causa detalhada de uma falha tecnica. */
  @PostMapping("/internal/executions/{id}/fail")
  public ExecutionResponse fail(@PathVariable Long id, @RequestBody FailRequest request) {
    return executionService.fail(id, request);
  }

  /** Consolida as evidências internas permitidas para uma pesquisa estratégica. */
  @GetMapping("/commercial-plans/{planId}/research-context")
  public Map<String, Object> researchContext(@PathVariable Long planId) {
    return service.researchContext(planId);
  }

  /** Registra hipotese ou aprendizado estruturado produzido por fluxo autorizado. */
  @PostMapping("/memory")
  public MemoryResponse createMemory(@RequestBody CreateMemoryRequest request) {
    return memoryService.create(request);
  }

  /** Lista memorias vigentes disponibilizadas ao Estrategista somente leitura. */
  @GetMapping("/commercial-plans/{planId}/memory")
  public List<MemoryResponse> listMemory(@PathVariable Long planId) {
    return memoryService.activeForPlan(planId);
  }

  /** Armazena artefato textual depois de anonimiza-lo no backend. */
  @PostMapping("/memory/{memoryId}/artifacts")
  public ArtifactResponse storeArtifact(
      @PathVariable Long memoryId, @RequestBody ArtifactRequest request) {
    return memoryService.storeArtifact(memoryId, request);
  }
}
