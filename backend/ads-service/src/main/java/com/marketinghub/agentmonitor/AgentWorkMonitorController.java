package com.marketinghub.agentmonitor;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
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
}
