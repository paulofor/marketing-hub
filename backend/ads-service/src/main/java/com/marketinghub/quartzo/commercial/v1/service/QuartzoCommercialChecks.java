package com.marketinghub.quartzo.commercial.v1.service;

import static com.marketinghub.quartzo.commercial.v1.service.QuartzoCommercialContext.require;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.experiment.service.ExperimentCampaignDestinationPolicy;
import com.marketinghub.experiment.service.ExperimentReadinessService;
import java.net.URI;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: validar insumos por etapa antes de consumir agentes ou consolidar preparação.
 */
@Component
public class QuartzoCommercialChecks {
  public static final List<String> PREPARATION =
      List.of("entry", "creative", "checkout", "targeting", "economics");
  private final ExperimentCampaignDestinationPolicy destinations;
  private final ExperimentReadinessService readiness;

  /** Resolve a prontidão de campanha sob demanda, preservando a inicialização do backend. */
  public QuartzoCommercialChecks(
      ExperimentCampaignDestinationPolicy destinations,
      @Lazy ExperimentReadinessService readiness) {
    this.destinations = destinations;
    this.readiness = readiness;
  }

  /** Confirma as fontes próprias de cada trabalho, sem confundir inspeção com autorização. */
  public void check(String activity, QuartzoCommercialContext.Scope scope, JsonNode snapshot) {
    var experiment = scope.experiment();
    require(
        experiment.getUnitPrice() != null && experiment.getUnitPrice().signum() > 0,
        "Defina um preço positivo para a oferta do experimento.");
    require(
        scope.product().getCurrentPriceBrl() != null
            && experiment.getUnitPrice().compareTo(scope.product().getCurrentPriceBrl()) == 0,
        "O preço do produto diverge do experimento; revise a oferta antes de preparar.");
    switch (activity) {
      case "entry" -> {
        require(
            destinations.hasCompleteCommercialContract(experiment),
            "Complete dor, promessa, oferta e CTA no experimento.");
        require(
            scope.publication() != null
                && destinations.hasCompletedGeraSalesPagePipeline(experiment.getId()),
            "Conclua a página de venda no GeraSalesPage e sua publicação auditada.");
        require(
            http(snapshot.path("destinationUrl").asText()),
            "A página auditada não possui URL pública válida.");
        require(
            blank(experiment.getFollowUpActionUrl())
                || destinations.hasAdDestinationPointingToSalesPage(
                    experiment, scope.publication()),
            "O destino do experimento diverge da página auditada; revise o vínculo pela tela.");
        require(
            destinations.hasRequiredSalesPageAnalyticsCollectors(scope.publication()),
            "Republique a página com os coletores oficiais; QA deve ficar fora da amostra comercial.");
        require(
            snapshot.path("productProof").isArray() && !snapshot.path("productProof").isEmpty(),
            "Aprove uma prova real do kit na Biblioteca Audiovisual antes da divulgação.");
      }
      case "creative" -> {
        require(
            snapshot.path("creatives").isArray() && !snapshot.path("creatives").isEmpty(),
            "Aprove ao menos um criativo final fiel ao kit, ao preço e à página.");
        gate(scope, "CREATIVE_APPROVED");
      }
      case "checkout" -> {
        require(
            http(snapshot.path("checkoutUrl").asText()),
            "A publicação auditada não possui checkout válido.");
        require(
            blank(experiment.getCommercialCheckoutUrl())
                || experiment
                    .getCommercialCheckoutUrl()
                    .equals(snapshot.path("checkoutUrl").asText()),
            "O checkout do experimento diverge da página auditada.");
        for (String field :
            List.of("deliverable", "deliveryMode", "checkoutMonetization", "riskReversal"))
          require(
              !snapshot.path(field).asText().isBlank(),
              "Complete o contrato do kit: " + field + ".");
        require(
            snapshot.path("validationContract").path("delivery").isObject(),
            "Registre a forma de entrega e personalização no contrato de validação do produto.");
      }
      case "targeting" -> gate(scope, "TARGETING_READY");
      case "economics" -> {
        var plan = snapshot.path("financialPlan");
        require(
            plan.isObject(),
            "Cadastre o plano financeiro LIVE desta versão e do plano comercial do experimento.");
        require(
            !plan.path("stale").asBoolean(true),
            "Atualize o plano financeiro vencido ou alterado.");
        require(
            "PROJECTED_VIABLE".equals(plan.path("evaluation").path("status").asText()),
            "Corrija custos ausentes ou margem inviável no plano financeiro.");
        require(
            plan.path("assumptions").path("priceBrl").isNumber()
                && plan.path("assumptions")
                        .path("priceBrl")
                        .decimalValue()
                        .compareTo(experiment.getUnitPrice())
                    == 0,
            "O preço da projeção financeira diverge da oferta.");
        require(
            "COMPLETED".equals(plan.path("analysis").path("status").asText()),
            "Solicite e conclua o parecer de Plutus sobre esta revisão pela tela Plano financeiro.");
        var scenarios = plan.path("analysis").path("result").path("scenarios");
        require(
            scenarios.isArray() && scenarios.size() == 3,
            "O parecer de Plutus precisa preservar os três cenários auditáveis.");
        JsonNode base = null;
        for (var scenario : scenarios)
          if ("BASE".equals(scenario.path("name").asText())) base = scenario;
        require(
            base != null
                && base.path("profitBrl").isNumber()
                && base.path("profitBrl").decimalValue().signum() > 0
                && base.path("averagePriceBrl").isNumber()
                && base.path("averagePriceBrl").decimalValue().compareTo(experiment.getUnitPrice())
                    == 0,
            "O cenário base de Plutus não demonstra resultado positivo com o preço atual.");
      }
      default ->
          throw new IllegalArgumentException(
              "Atividade de preparação Quartzo desconhecida: " + activity);
    }
  }

  /** Revalida todas as fontes antes da avaliação independente e da consolidação final. */
  public void all(QuartzoCommercialContext.Scope scope, JsonNode snapshot) {
    PREPARATION.forEach(activity -> check(activity, scope, snapshot));
  }

  /** Usa o requisito canônico sem aprovar outros gates, orçamento ou período por consequência. */
  private void gate(QuartzoCommercialContext.Scope scope, String code) {
    var gate =
        readiness.summarize(scope.experiment().getId()).runningGateRequirements().stream()
            .filter(item -> code.equals(item.code()))
            .findFirst()
            .orElseThrow();
    require(gate.ready(), gate.detail() + " " + gate.recommendation());
  }

  /** Recusa destinos incompletos ou esquemas não navegáveis, sem realizar requisição externa. */
  private boolean http(String value) {
    try {
      var uri = URI.create(value);
      return List.of("http", "https").contains(uri.getScheme()) && uri.getHost() != null;
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }

  /** Reconhece vínculo ausente sem substituir um destino explicitamente cadastrado. */
  private boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
