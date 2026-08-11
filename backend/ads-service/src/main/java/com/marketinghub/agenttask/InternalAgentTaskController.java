package com.marketinghub.agenttask;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: receber delegações auditáveis originadas pelos agentes do Marketing Hub. */
@RestController
@RequestMapping("/api/internal/agent-tasks/v1")
public class InternalAgentTaskController {
  private final AgentTaskService service;

  /** Configura o serviço compartilhado de delegação. */
  public InternalAgentTaskController(AgentTaskService service) {
    this.service = service;
  }

  /** Registra uma tarefa criada por um agente para outro agente. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AgentTaskResponse create(@Valid @RequestBody CreateAgentTaskByAgentRequest request) {
    return service.createByAgent(request);
  }

  /** Permite que qualquer agente cadastrado abra um gate na mesa do responsável. */
  @PostMapping("/gates")
  @ResponseStatus(HttpStatus.CREATED)
  public AgentTaskResponse createGate(@Valid @RequestBody CreateAgentGateByAgentRequest request) {
    return service.createGateByAgent(request.asTaskRequest(), request.gateCode());
  }

  /** Registra a decisão protegida de um gate na própria mesa do agente responsável. */
  @PostMapping("/{taskId}/gate-decision")
  public AgentTaskResponse decideGate(
      @PathVariable Long taskId, @Valid @RequestBody DecideAgentGateRequest request) {
    return service.decideGate(taskId, request);
  }
}
