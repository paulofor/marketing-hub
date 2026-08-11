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
}
