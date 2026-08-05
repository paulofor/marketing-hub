package com.marketinghub.experimentstrategist.controller;

import com.marketinghub.experimentstrategist.memory.ExperimentStrategistMemoryService;
import com.marketinghub.experimentstrategist.memory.ExperimentStrategistMemoryService.ArtifactRequest;
import com.marketinghub.experimentstrategist.memory.ExperimentStrategistMemoryService.ArtifactResponse;
import com.marketinghub.experimentstrategist.memory.ExperimentStrategistMemoryService.CreateMemoryRequest;
import com.marketinghub.experimentstrategist.memory.ExperimentStrategistMemoryService.MemoryResponse;
import com.marketinghub.experimentstrategist.service.ExperimentStrategistContextService;
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

  /** Configura o serviço canônico de contexto estratégico. */
  public ExperimentStrategistController(
      ExperimentStrategistContextService service, ExperimentStrategistMemoryService memoryService) {
    this.service = service;
    this.memoryService = memoryService;
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
