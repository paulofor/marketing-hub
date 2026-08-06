package com.marketinghub.codextelemetry;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: expor callbacks internos e consulta da telemetria Codex. */
@RestController
@RequestMapping("/api/codex-agent-telemetry/v1")
public class CodexAgentExecutionTelemetryController {
  private final CodexAgentExecutionTelemetryService service;

  /** Configura o serviço canônico. */
  public CodexAgentExecutionTelemetryController(CodexAgentExecutionTelemetryService service) {
    this.service = service;
  }

  /** Recebe o heartbeat auditável de um worker. */
  @PostMapping("/internal/{agentType}/executions/{executionId}/heartbeat")
  public CodexAgentExecutionTelemetryService.Response heartbeat(
      @PathVariable String agentType,
      @PathVariable Long executionId,
      @RequestBody CodexAgentExecutionTelemetryService.HeartbeatRequest request) {
    return service.heartbeat(agentType, executionId, request);
  }

  /** Recebe o encerramento auditável de um worker. */
  @PostMapping("/internal/{agentType}/executions/{executionId}/finish")
  public CodexAgentExecutionTelemetryService.Response finish(
      @PathVariable String agentType,
      @PathVariable Long executionId,
      @RequestBody CodexAgentExecutionTelemetryService.FinishRequest request) {
    return service.finish(agentType, executionId, request);
  }

  /** Consulta o progresso sem depender dos logs técnicos. */
  @GetMapping("/{agentType}/executions/{executionId}")
  public ResponseEntity<CodexAgentExecutionTelemetryService.Response> get(
      @PathVariable String agentType, @PathVariable Long executionId) {
    return ResponseEntity.ofNullable(service.get(agentType, executionId));
  }
}
