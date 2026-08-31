package com.marketinghub.productdiscovery.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.businessprocess.independent.service.IndependentBusinessProcessExecutionHandler;
import com.marketinghub.businessprocess.independent.service.IndependentBusinessProcessStartedExecution;
import com.marketinghub.businessprocess.independent.service.catalog.IndependentBusinessProcessInputFieldResponse;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryMarketType;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryResearchMode;
import java.util.List;
import org.springframework.stereotype.Component;

/** Responsabilidade: iniciar descoberta factual independente pelo ciclo canônico de Argos. */
@Component
public class ProductDiscoveryIndependentBusinessProcessExecutionHandler
    implements IndependentBusinessProcessExecutionHandler {
  private static final String PROCESS_CODE = "pde-opportunity-discovery";
  private final ProductDiscoveryService productDiscoveryService;

  /** Configura o adaptador com o serviço que cria ciclo, fila e auditoria BPM atomicamente. */
  public ProductDiscoveryIndependentBusinessProcessExecutionHandler(
      ProductDiscoveryService productDiscoveryService) {
    this.productDiscoveryService = productDiscoveryService;
  }

  /** Identifica o processo de descoberta de oportunidades PDE. */
  @Override
  public String processCode() {
    return PROCESS_CODE;
  }

  /** Declara o briefing mínimo e os limites aceitos pelo ciclo de descoberta. */
  @Override
  public List<IndependentBusinessProcessInputFieldResponse> inputFields() {
    return List.of(
        field(
            "theme",
            "Tema amplo",
            true,
            191,
            "Ex.: guarda-roupa cápsula para mulheres 40+ com foco em bem-estar e beleza madura."));
  }

  /** Cria o ciclo técnico e mantém o endpoint pending do worker como único ponto de consumo. */
  @Override
  public IndependentBusinessProcessStartedExecution start(JsonNode input) {
    ProductDiscoveryCycleResponse cycle =
        productDiscoveryService.createCycle(
            new CreateProductDiscoveryCycleRequest(
                text(input, "theme"),
                null,
                "BR",
                "pt-BR",
                "Instagram",
                "Não publicar, gastar ou tratar anúncios e intenção como vendas.",
                null,
                "Descobrir de duas a três oportunidades PDE factuais para priorização comercial.",
                ProductDiscoveryResearchMode.DISCOVER_MARKETS,
                ProductDiscoveryMarketType.B2C,
                null));
    return new IndependentBusinessProcessStartedExecution(
        "product-discovery-cycle:" + cycle.id(), cycle.theme());
  }

  /** Constrói de forma uniforme a descrição de um campo operacional. */
  private IndependentBusinessProcessInputFieldResponse field(
      String key, String label, boolean required, Integer maxLength, String helpText) {
    return new IndependentBusinessProcessInputFieldResponse(
        key, label, "TEXTAREA", required, maxLength, null, helpText);
  }

  /** Lê texto já normalizado pelo serviço genérico sem fabricar valor ausente. */
  private String text(JsonNode input, String key) {
    JsonNode value = input.get(key);
    return value == null || value.isNull() ? null : value.asText();
  }
}
