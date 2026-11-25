package com.marketinghub.consumerexpectation.v1.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.consumerexpectation.v1.service.ConsumerExpectationService;
import com.marketinghub.consumerexpectation.v1.service.learning.ExpectationLearningResponse;
import com.marketinghub.consumerexpectation.v1.service.plan.ExpectationPlanRequest;
import com.marketinghub.consumerexpectation.v1.service.plan.ExpectationPlanResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** Expõe planejamento consultivo e leitura de aprendizado sem criar execução ou autorização. */
@RestController
@RequestMapping("/api/consumer-expectations/v1")
public class ConsumerExpectationController {
  private final ConsumerExpectationService service;

  /** Conecta a interface ao único serviço responsável pela capacidade. */
  public ConsumerExpectationController(ConsumerExpectationService service) {
    this.service = service;
  }

  /** Entrega fontes científicas, hipóteses e limites com versão identificável. */
  @GetMapping("/protocol")
  @Operation(summary = "Consultar protocolo científico de expectativas e surpresa positiva")
  public JsonNode protocol() {
    return service.protocol();
  }

  /** Prepara desenho determinístico de três alternativas sem chamar modelo ou salvar aprovação. */
  @PostMapping("/plans/preview")
  @Operation(summary = "Preparar hipóteses consultivas sem geração paga ou publicação")
  public ExpectationPlanResponse preview(@Valid @RequestBody ExpectationPlanRequest request) {
    return service.preview(request);
  }

  /** Apresenta evidência conciliada e pendências do ciclo, preservando segregação por produto. */
  @GetMapping("/products/{productId}/cycles/{cycleId}/learning")
  @Operation(summary = "Ler aprendizado observacional do ciclo sem concluir causalidade")
  public ExpectationLearningResponse learning(
      @PathVariable Long productId, @PathVariable Long cycleId) {
    return service.learning(productId, cycleId);
  }
}
