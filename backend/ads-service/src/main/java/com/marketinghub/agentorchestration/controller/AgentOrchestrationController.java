package com.marketinghub.agentorchestration.controller;

import com.marketinghub.agentorchestration.service.AgentOrchestrationService;
import com.marketinghub.agentorchestration.service.AgentOrchestrationService.OrchestrationResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor a reconciliacao auditavel dos agentes de crescimento. */
@RestController
@RequestMapping("/api/agent-orchestration/v1")
public class AgentOrchestrationController {
  private final AgentOrchestrationService service;

  /** Configura o servico canonico de orquestracao. */
  public AgentOrchestrationController(AgentOrchestrationService service) {
    this.service = service;
  }

  /** Reconcilia os pareceres persistidos sem executar acoes comerciais. */
  @PostMapping("/commercial-plans/{planId}/cases/synchronize")
  public OrchestrationResponse synchronize(@PathVariable Long planId) {
    return service.synchronize(planId);
  }

  /** Lista a coordenacao auditavel do planejamento. */
  @GetMapping("/commercial-plans/{planId}/cases")
  public List<OrchestrationResponse> list(@PathVariable Long planId) {
    return service.list(planId);
  }
}
