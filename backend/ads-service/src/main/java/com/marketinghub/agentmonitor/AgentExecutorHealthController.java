package com.marketinghub.agentmonitor;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: receber o health-check interno executado pelos workers dos agentes. */
@RestController
@RequestMapping("/api/internal/agents/executor-health")
public class AgentExecutorHealthController {
  private final AgentExecutorHealthService service;

  /** Configura o serviço que valida e persiste as provas operacionais. */
  public AgentExecutorHealthController(AgentExecutorHealthService service) {
    this.service = service;
  }

  /** Registra uma operação segura que já comprova o acesso do executor ao backend. */
  @PostMapping
  public AgentExecutorHealthResponse report(
      @Valid @RequestBody AgentExecutorHealthReportRequest request) {
    return service.report(request);
  }
}
