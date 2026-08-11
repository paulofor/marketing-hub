package com.marketinghub.financialagent.controller;

import com.marketinghub.financialagent.service.CompleteFinancialAgentRequest;
import com.marketinghub.financialagent.service.FailFinancialAgentRequest;
import com.marketinghub.financialagent.service.FinancialAgentExecutionResponse;
import com.marketinghub.financialagent.service.FinancialAgentService;
import com.marketinghub.financialagent.service.ProviderCreditPurchaseService;
import com.marketinghub.financialagent.service.StartRevenueProjectionRequest;
import com.marketinghub.financialagent.service.registerProviderCreditPurchase.ProviderCreditPurchaseResponse;
import com.marketinghub.financialagent.service.registerProviderCreditPurchase.RegisterProviderCreditPurchaseRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor o contrato canonico do Agente Financeiro v1. */
@RestController
@RequestMapping("/api/financial-agent/v1")
public class FinancialAgentController {
  private final FinancialAgentService service;
  private final ProviderCreditPurchaseService providerCreditPurchaseService;

  /** Inicializa o controller com conciliação e recargas de provedores. */
  public FinancialAgentController(
      FinancialAgentService service, ProviderCreditPurchaseService providerCreditPurchaseService) {
    this.service = service;
    this.providerCreditPurchaseService = providerCreditPurchaseService;
  }

  /** Registra uma compra pré-paga sem tratá-la como custo já consumido. */
  @PostMapping("/providers/{provider}/credit-purchases")
  public ProviderCreditPurchaseResponse registerProviderCreditPurchase(
      @PathVariable String provider,
      @Valid @RequestBody RegisterProviderCreditPurchaseRequest request) {
    return providerCreditPurchaseService.register(provider, request);
  }

  /** Lista as recargas auditáveis do provedor. */
  @GetMapping("/providers/{provider}/credit-purchases")
  public List<ProviderCreditPurchaseResponse> listProviderCreditPurchases(
      @PathVariable String provider) {
    return providerCreditPurchaseService.list(provider);
  }

  /** Solicita uma nova conciliacao financeira somente leitura. */
  @PostMapping("/commercial-plans/{planId}/executions")
  public FinancialAgentExecutionResponse start(@PathVariable Long planId) {
    return service.start(planId);
  }

  /** Lista os relatorios financeiros do planejamento. */
  @GetMapping("/commercial-plans/{planId}/executions")
  public List<FinancialAgentExecutionResponse> list(@PathVariable Long planId) {
    return service.list(planId);
  }

  /** Solicita a Plutus uma projeção de receita sem autorizar investimento. */
  @PostMapping("/commercial-plans/{planId}/revenue-projections")
  public FinancialAgentExecutionResponse startRevenueProjection(
      @PathVariable Long planId, @Valid @RequestBody StartRevenueProjectionRequest request) {
    return service.startRevenueProjection(planId, request);
  }

  /** Lista projeções auditáveis separadas dos valores realizados. */
  @GetMapping("/commercial-plans/{planId}/revenue-projections")
  public List<FinancialAgentExecutionResponse> listRevenueProjections(@PathVariable Long planId) {
    return service.listRevenueProjections(planId);
  }

  /** Entrega os valores e a cobertura das fontes para consulta somente leitura. */
  @GetMapping("/internal/commercial-plans/{planId}/intelligence")
  public Map<String, Object> intelligence(@PathVariable Long planId) {
    return service.intelligence(planId);
  }

  /** Garante no maximo um relatorio automatico por dia. */
  @PostMapping("/internal/commercial-plans/{planId}/executions/ensure-daily")
  public FinancialAgentExecutionResponse ensureDaily(@PathVariable Long planId) {
    return service.ensureDaily(planId);
  }

  /** Reserva a conciliacao pendente mais antiga para o worker. */
  @PostMapping("/internal/executions/pending/claim")
  public FinancialAgentExecutionResponse claimPending() {
    return service.claimPending();
  }

  /** Entrega ao MCP exclusivo o snapshot congelado da conciliacao reservada. */
  @GetMapping("/internal/executions/{id}")
  public FinancialAgentExecutionResponse getExecution(@PathVariable Long id) {
    return service.getExecution(id);
  }

  /** Recebe um relatorio financeiro sem aplicar mutacoes. */
  @PostMapping("/internal/executions/{id}/complete")
  public FinancialAgentExecutionResponse complete(
      @PathVariable Long id, @RequestBody CompleteFinancialAgentRequest request) {
    return service.complete(id, request);
  }

  /** Recebe uma falha tecnica do executor financeiro. */
  @PostMapping("/internal/executions/{id}/fail")
  public FinancialAgentExecutionResponse fail(
      @PathVariable Long id, @RequestBody FailFinancialAgentRequest request) {
    return service.fail(id, request);
  }
}
