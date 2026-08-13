package com.marketinghub.agentmonitor;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor o monitor administrativo de trabalho dos agentes. */
@RestController
@RequestMapping("/api/agents/work-monitor")
public class AgentWorkMonitorController {
  private final AgentWorkMonitorService service;

  /** Configura o serviço canônico de monitoramento. */
  public AgentWorkMonitorController(AgentWorkMonitorService service) {
    this.service = service;
  }

  /** Lista trabalho, dificuldade, decisão externa e prontidão técnica de todos os agentes. */
  @GetMapping
  public List<AgentWorkMonitorResponse> list() {
    return service.list();
  }

  /** Inicia uma reconexão Codex auditada para o executor selecionado. */
  @PostMapping("/{agentId}/codex-auth/reconnections")
  public CodexAuthReconnectResponse reconnect(
      @PathVariable Long agentId, HttpServletRequest request) {
    String operator =
        request.getRemoteUser() == null ? "marketing-hub-admin" : request.getRemoteUser();
    return service.requestReconnect(agentId, operator);
  }

  /** Retorna a verdade persistida da reconexão mais recente. */
  @GetMapping("/{agentId}/codex-auth/reconnections/current")
  public ResponseEntity<CodexAuthReconnectResponse> currentReconnect(@PathVariable Long agentId) {
    CodexAuthReconnectResponse response = service.currentReconnect(agentId);
    return response == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
  }

  /** Solicita atualização ou reinício do executor por comando auditável. */
  @PostMapping("/{agentId}/executor-operations/{operationType}")
  public AgentExecutorAdminOperationResponse executorOperation(
      @PathVariable Long agentId, @PathVariable String operationType, HttpServletRequest request) {
    String operator =
        request.getRemoteUser() == null ? "marketing-hub-admin" : request.getRemoteUser();
    return service.requestExecutorOperation(agentId, operationType, operator);
  }

  /** Retorna o último comando de atualização ou reinício do executor. */
  @GetMapping("/{agentId}/executor-operations/current")
  public ResponseEntity<AgentExecutorAdminOperationResponse> currentExecutorOperation(
      @PathVariable Long agentId) {
    AgentExecutorAdminOperationResponse response = service.currentExecutorOperation(agentId);
    return response == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
  }
}
