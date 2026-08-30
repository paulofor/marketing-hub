package com.marketinghub.productdiscovery.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.businessprocess.independent.service.IndependentBusinessProcessExecutionHandler;
import com.marketinghub.businessprocess.independent.service.IndependentBusinessProcessStartedExecution;
import com.marketinghub.businessprocess.independent.service.catalog.IndependentBusinessProcessInputFieldResponse;
import com.marketinghub.businessprocess.independent.service.catalog.IndependentBusinessProcessInputOptionResponse;
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
        selectField(
            "researchMode",
            "O que Argos deve fazer?",
            true,
            "DISCOVER_MARKETS",
            "Descobrir parte de público, canal e fontes; validar aprofunda um mercado já escolhido.",
            List.of(
                new IndependentBusinessProcessInputOptionResponse(
                    "DISCOVER_MARKETS", "Descobrir mercados candidatos"),
                new IndependentBusinessProcessInputOptionResponse(
                    "VALIDATE_MARKET", "Validar um mercado informado"))),
        selectField(
            "marketType",
            "Tipo de comprador",
            true,
            "B2C",
            "Declare o comprador para Argos aplicar os gates corretos sem inferir pelo texto.",
            List.of(
                new IndependentBusinessProcessInputOptionResponse("B2C", "Pessoa física (B2C)"),
                new IndependentBusinessProcessInputOptionResponse("B2B", "Empresa (B2B)"),
                new IndependentBusinessProcessInputOptionResponse(
                    "UNSPECIFIED", "Ainda não definido"))),
        field(
            "theme",
            "Público, universo ou mercado de partida",
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
            "Instagram",
            "Ex.: TikTok, Instagram, Google ou WhatsApp."),
        field(
            "referenceSources",
            "Fontes editoriais de referência",
            "TEXTAREA",
            false,
            5000,
            null,
            "Uma URL pública por linha, como revista, comunidade ou publicação que represente o público."),
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
                text(input, "objective"),
                researchMode(input),
                marketType(input),
                text(input, "referenceSources")));
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

  /** Constrói um campo seletivo cujas opções também serão validadas pelo backend. */
  private IndependentBusinessProcessInputFieldResponse selectField(
      String key,
      String label,
      boolean required,
      String defaultValue,
      String helpText,
      List<IndependentBusinessProcessInputOptionResponse> options) {
    return new IndependentBusinessProcessInputFieldResponse(
        key, label, "SELECT", required, 32, defaultValue, helpText, options);
  }

  /** Lê texto já normalizado pelo serviço genérico sem fabricar valor ausente. */
  private String text(JsonNode input, String key) {
    JsonNode value = input.get(key);
    return value == null || value.isNull() ? null : value.asText();
  }

  /** Preserva validação dirigida para chamadas antigas que não passaram pela tela dinâmica. */
  private ProductDiscoveryResearchMode researchMode(JsonNode input) {
    String value = text(input, "researchMode");
    return value == null
        ? ProductDiscoveryResearchMode.VALIDATE_MARKET
        : ProductDiscoveryResearchMode.valueOf(value);
  }

  /** Evita inferir consumidor em integrações antigas sem declaração explícita. */
  private ProductDiscoveryMarketType marketType(JsonNode input) {
    String value = text(input, "marketType");
    return value == null
        ? ProductDiscoveryMarketType.UNSPECIFIED
        : ProductDiscoveryMarketType.valueOf(value);
  }
}
