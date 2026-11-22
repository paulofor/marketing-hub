package com.marketinghub.agentcostwaiver.v1.controller;

import com.marketinghub.agentcostwaiver.v1.service.AgentCostWaiverService;
import com.marketinghub.agentcostwaiver.v1.service.decisions.CostWaiver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: expor decisões explícitas de dispensa financeira pelo operador. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/agent-cost-waivers/v1/products/{productId}/cycles/{cycleId}")
public class AgentCostWaiverController {
  private final AgentCostWaiverService service;

  /** Consulta tarefas e recibos do produto no contexto validado. */
  @GetMapping
  @io.swagger.v3.oas.annotations.Operation(summary = "Consultar dispensas de custos legados")
  public CostWaiver.View read(
      @PathVariable Long productId, @PathVariable Long cycleId, @RequestParam Long chainId) {
    return service.view(productId, cycleId, chainId);
  }

  /** Registra seleção confirmada sem retomar processos ou iniciar agentes. */
  @PostMapping
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Dispensar conciliação de tarefas selecionadas")
  public CostWaiver.View decide(
      @PathVariable Long productId,
      @PathVariable Long cycleId,
      @Valid @RequestBody CostWaiver.Request request) {
    return service.decide(productId, cycleId, request);
  }
}
