package com.marketinghub.productdiscovery.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.businessprocess.independent.service.IndependentBusinessProcessExecutionHandler;
import com.marketinghub.businessprocess.independent.service.IndependentBusinessProcessStartedExecution;
import com.marketinghub.businessprocess.independent.service.catalog.IndependentBusinessProcessInputFieldResponse;
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
            "Tema ou pergunta de mercado",
            "TEXT",
            true,
            191,
            null,
            "Descreva a dor, público ou oportunidade que precisa de evidência factual."),
        field(
            "targetAudience",
            "Público-alvo",
            "TEXT",
            false,
            191,
            null,
            "Quem enfrenta a situação pesquisada."),
        field(
            "acquisitionChannel",
            "Canal provável de aquisição",
            "TEXT",
            false,
            120,
            null,
            "Ex.: TikTok, Instagram, Google ou WhatsApp."),
        field(
            "objective",
            "Objetivo comercial da pesquisa",
            "TEXTAREA",
            false,
            5000,
            null,
            "Decisão que as evidências deverão sustentar."),
        field(
            "commercialConstraints",
            "Restrições comerciais",
            "TEXTAREA",
            false,
            5000,
            null,
            "Limites de promessa, formato, ticket ou entrega."),
        field(
            "forbiddenCategories",
            "Categorias proibidas",
            "TEXTAREA",
            false,
            5000,
            null,
            "Temas que Argos não deve considerar."),
        field("country", "País", "TEXT", true, 16, "BR", "Código do país pesquisado."),
        field(
            "language", "Idioma", "TEXT", true, 16, "pt-BR", "Idioma das fontes e do relatório."));
  }

  /** Cria o ciclo técnico e mantém o endpoint pending do worker como único ponto de consumo. */
  @Override
  public IndependentBusinessProcessStartedExecution start(JsonNode input) {
    ProductDiscoveryCycleResponse cycle =
        productDiscoveryService.createCycle(
            new CreateProductDiscoveryCycleRequest(
                text(input, "theme"),
                text(input, "targetAudience"),
                text(input, "country"),
                text(input, "language"),
                text(input, "acquisitionChannel"),
                text(input, "commercialConstraints"),
                text(input, "forbiddenCategories"),
                text(input, "objective")));
    return new IndependentBusinessProcessStartedExecution(
        "product-discovery-cycle:" + cycle.id(), cycle.theme());
  }

  /** Constrói de forma uniforme a descrição de um campo operacional. */
  private IndependentBusinessProcessInputFieldResponse field(
      String key,
      String label,
      String controlType,
      boolean required,
      Integer maxLength,
      String defaultValue,
      String helpText) {
    return new IndependentBusinessProcessInputFieldResponse(
        key, label, controlType, required, maxLength, defaultValue, helpText);
  }

  /** Lê texto já normalizado pelo serviço genérico sem fabricar valor ausente. */
  private String text(JsonNode input, String key) {
    JsonNode value = input.get(key);
    return value == null || value.isNull() ? null : value.asText();
  }
}
