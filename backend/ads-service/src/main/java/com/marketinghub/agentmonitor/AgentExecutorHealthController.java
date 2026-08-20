package com.marketinghub.agentmonitor;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: receber o health-check interno executado pelos workers dos agentes. */
@RestController
@RequestMapping("/api/internal/agents/executor-health")
public class AgentExecutorHealthController {
  private final AgentExecutorHealthService service;
  private final AgentAutomaticExecutionControlService automaticExecution;

  /** Configura o serviço que valida e persiste as provas operacionais. */
  public AgentExecutorHealthController(
      AgentExecutorHealthService service,
      AgentAutomaticExecutionControlService automaticExecution) {
    this.service = service;
    this.automaticExecution = automaticExecution;
  }

  /** Registra uma operação segura que já comprova o acesso do executor ao backend. */
  @PostMapping
  public AgentExecutorHealthResponse report(
      @Valid @RequestBody AgentExecutorHealthReportRequest request) {
    return service.report(request);
  }

  /** Informa ao executor se ele pode iniciar um novo trabalho automático. */
  @GetMapping("/{agentKey}/automatic-execution")
  public AgentAutomaticExecutionControlResponse automaticExecution(@PathVariable String agentKey) {
    return automaticExecution.current(agentKey);
  }

  /** Reserva uma solicitação pendente para o executor autenticador. */
  @GetMapping("/{agentKey}/codex-auth/reconnections/pending")
  public org.springframework.http.ResponseEntity<CodexAuthReconnectResponse> pending(
      @PathVariable String agentKey) {
    CodexAuthReconnectResponse response = service.claimReconnect(agentKey);
    return response == null
        ? org.springframework.http.ResponseEntity.noContent().build()
        : org.springframework.http.ResponseEntity.ok(response);
  }

  /** Recebe URL e código temporários emitidos pelo App Server. */
  @PostMapping("/codex-auth/reconnections/{id}/device-code")
  public CodexAuthReconnectResponse deviceCode(
      @PathVariable Long id, @Valid @RequestBody CodexAuthDeviceCodeRequest request) {
    return service.deviceCode(id, request);
  }

  /** Recebe o resultado da validação segura account/read. */
  @PostMapping("/codex-auth/reconnections/{id}/completion")
  public CodexAuthReconnectResponse completion(
      @PathVariable Long id, @RequestBody CodexAuthCompletionRequest request) {
    return service.complete(id, request);
  }

  /** Reserva o próximo comando para o controlador de implantação instalado no host. */
  @GetMapping("/admin-operations/pending")
  public org.springframework.http.ResponseEntity<AgentExecutorAdminOperationResponse>
      pendingAdminOperation() {
    AgentExecutorAdminOperationResponse response = service.claimOperation();
    return response == null
        ? org.springframework.http.ResponseEntity.noContent().build()
        : org.springframework.http.ResponseEntity.ok(response);
  }

  /** Recebe o resultado do controlador sem substituir a prova do health-check. */
  @PostMapping("/admin-operations/{id}/completion")
  public AgentExecutorAdminOperationResponse completeAdminOperation(
      @PathVariable Long id, @RequestBody AgentExecutorAdminCompletionRequest request) {
    return service.completeOperation(id, request);
  }
}
