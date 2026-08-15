package com.marketinghub.agentmemory.controller;

import com.marketinghub.agentmemory.service.AgentLearningDashboardService;
import com.marketinghub.agentmemory.service.AgentMemoryService;
import com.marketinghub.agentmemory.service.dashboard.AgentLearningDashboardResponse;
import com.marketinghub.agentmemory.service.registerFeedback.RegisterMemoryFeedbackRequest;
import com.marketinghub.agentmemory.service.retrieveMemory.MemoryResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: expor a supervisão humana do aprendizado persistente dos agentes. */
@RestController
@RequestMapping("/api/agent-learning-dashboard/v1")
public class AgentLearningDashboardController {
  private final AgentLearningDashboardService dashboardService;
  private final AgentMemoryService memoryService;

  /** Inicializa o controller com consulta e governança de memória. */
  public AgentLearningDashboardController(
      AgentLearningDashboardService dashboardService, AgentMemoryService memoryService) {
    this.dashboardService = dashboardService;
    this.memoryService = memoryService;
  }

  /** Entrega a visão consolidada sem transformar correlação em resultado comercial. */
  @GetMapping
  public AgentLearningDashboardResponse dashboard() {
    return dashboardService.dashboard();
  }

  /** Registra decisão humana auditável sem permitir autopromoção pelo agente. */
  @PostMapping("/agents/{agentKey}/memories/{memoryId}/feedback")
  public MemoryResponse feedback(
      @PathVariable String agentKey,
      @PathVariable Long memoryId,
      @Valid @RequestBody RegisterMemoryFeedbackRequest request) {
    return memoryService.feedback(agentKey, memoryId, request);
  }
}
