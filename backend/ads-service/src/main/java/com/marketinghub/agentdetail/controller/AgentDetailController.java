package com.marketinghub.agentdetail.controller;

import com.marketinghub.agentdetail.service.AgentDetailService;
import com.marketinghub.agentdetail.service.getDetail.AgentDetailResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor a visão administrativa consolidada de um agente. */
@RestController
@RequestMapping("/api/agents/{agentId}/details")
public class AgentDetailController {
  private final AgentDetailService service;

  /** Configura o serviço canônico do detalhe do agente. */
  public AgentDetailController(AgentDetailService service) {
    this.service = service;
  }

  /** Entrega todos os dados específicos do contrato atual do agente. */
  @GetMapping
  public AgentDetailResponse getDetail(@PathVariable Long agentId) {
    return service.getDetail(agentId);
  }
}
