package com.marketinghub.agenttask;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: expor as mesas de trabalho dos agentes para a interface administrativa. */
@RestController
@RequestMapping("/api/agent-tasks")
public class AgentTaskController {
  private final AgentTaskService service;

  /** Configura o serviço canônico da caixa de entrada. */
  public AgentTaskController(AgentTaskService service) {
    this.service = service;
  }

  /** Lista a caixa de entrada de um único agente. */
  @GetMapping("/agents/{agentKey}")
  public List<AgentTaskResponse> inbox(@PathVariable String agentKey) {
    return service.inbox(agentKey);
  }

  /** Lista todo trabalho que ainda exige atuação de algum agente. */
  @GetMapping("/active")
  public List<AgentTaskResponse> activeTasks() {
    return service.activeTasks();
  }

  /** Permite que uma pessoa abra uma solicitação pela tela. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AgentTaskResponse create(@Valid @RequestBody CreateAgentTaskRequest request) {
    return service.createByHuman(request);
  }

  /** Permite evoluir o estado operacional da tarefa. */
  @PatchMapping("/{taskId}/status")
  public AgentTaskResponse updateStatus(
      @PathVariable Long taskId, @Valid @RequestBody UpdateAgentTaskStatusRequest request) {
    return service.updateStatus(taskId, request);
  }
}
