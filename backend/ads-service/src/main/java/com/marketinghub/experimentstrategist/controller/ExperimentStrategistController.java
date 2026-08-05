package com.marketinghub.experimentstrategist.controller;

import com.marketinghub.experimentstrategist.service.ExperimentStrategistContextService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Responsabilidade: expor exclusivamente o contexto de leitura do Estrategista de Experimentos v1.
 */
@RestController
@RequestMapping("/api/experiment-strategist/v1")
public class ExperimentStrategistController {
  private final ExperimentStrategistContextService service;

  /** Configura o serviço canônico de contexto estratégico. */
  public ExperimentStrategistController(ExperimentStrategistContextService service) {
    this.service = service;
  }

  /** Consolida as evidências internas permitidas para uma pesquisa estratégica. */
  @GetMapping("/commercial-plans/{planId}/research-context")
  public Map<String, Object> researchContext(@PathVariable Long planId) {
    return service.researchContext(planId);
  }
}
