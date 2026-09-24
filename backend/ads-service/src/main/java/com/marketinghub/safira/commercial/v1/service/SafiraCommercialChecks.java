package com.marketinghub.safira.commercial.v1.service;

import static com.marketinghub.safira.commercial.v1.service.SafiraCommercialContext.require;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.service.ExperimentReadinessService;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.planning.CommercialPlanStatus;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: validar jornada pública e economia de Safira antes de revisões pagas ou
 * consolidação.
 */
@Component
@Slf4j
public class SafiraCommercialChecks {
  public static final List<String> PREPARATION = List.of("journey", "economics");
  private final ExperimentReadinessService readiness;

  /** Resolve os gates gerais do experimento somente quando a etapa os exige. */
  public SafiraCommercialChecks(@Lazy ExperimentReadinessService readiness) {
    this.readiness = readiness;
  }

  /** Confirma o conjunto de provas próprio da atividade sem autorizar mídia ou publicação. */
  public void check(String activity, SafiraCommercialContext.Scope scope, JsonNode snapshot) {
    require(
        scope.experiment().getUnitPrice() != null && scope.experiment().getUnitPrice().signum() > 0,
        "Defina um preço positivo para a oferta Safira.");
    require(
        scope.product().getCurrentPriceBrl() != null
            && scope.experiment().getUnitPrice().compareTo(scope.product().getCurrentPriceBrl())
                == 0,
        "O preço do produto diverge do experimento; alinhe a oferta antes de preparar.");
    switch (activity) {
      case "journey" -> journey(scope, snapshot);
      case "economics" -> economics(scope, snapshot);
      default ->
          throw new IllegalArgumentException(
              "Atividade de preparação Safira desconhecida: " + activity);
    }
  }

  /**
   * Exige experiência comercial, cinco pontos e controles do canal aprovado no mesmo experimento.
   */
  private void journey(SafiraCommercialContext.Scope scope, JsonNode snapshot) {
    var experiment = scope.experiment();
    boolean direct = experiment.getPlatform() == ExperimentPlatform.DIRECT_ONE_TO_ONE;
    if (!direct) {
      require(
          experiment.getInstagramAccount() != null,
          "Selecione a identidade pública do Instagram que representará esta oferta.");
    }
    require(
        !snapshot.path("singlePain").asText().isBlank()
            && !snapshot.path("funnelPromise").asText().isBlank()
            && !snapshot.path("primaryCta").asText().isBlank(),
        "Complete dor, promessa e CTA do experimento Safira.");
    require(
        !snapshot.path("desireTerritoryCode").asText().isBlank()
            && snapshot.path("desireTerritory").isObject()
            && !snapshot.path("desireTerritory").isEmpty(),
        "Escolha um desejo reconhecido e preserve sua fonte no experimento.");
    require(
        !snapshot.path("freeReward").asText().isBlank(),
        "Defina a demonstração inicial de valor antes do compromisso pago.");
    require(
        scope.slot() != null,
        "Publique a experiência comercial no slot vinculado ao próprio experimento.");
    require(
        List.of(PdeProductionSlotStatus.READY, PdeProductionSlotStatus.ACTIVE)
            .contains(scope.slot().getStatus()),
        "O slot Safira precisa estar READY ou ACTIVE.");
    require(
        "OK".equals(scope.slot().getValidationStatus()),
        "Valide a URL pública da experiência Safira.");
    require(
        http(snapshot.path("publicUrl").asText())
            && !snapshot.path("backendUrl").asText().isBlank(),
        "A experiência precisa de URL HTTPS e backend responsável por acesso e eventos.");
    require(
        snapshot.path("commercialJourneyIntegrated").asBoolean(false),
        "Conclua o Processo 4 em modo comercial para este experimento; a preparação privada não substitui essa prova.");
    require(
        !snapshot.path("experienceHash").asText().isBlank()
            && snapshot.path("publishedExperience").isObject()
            && !snapshot.path("publishedExperience").isEmpty(),
        "A experiência publicada precisa ter contrato e SHA-256 verificáveis.");
    require(
        http(snapshot.path("checkoutUrl").asText())
            && snapshot.path("checkoutPriceBrl").isNumber()
            && snapshot.path("checkoutPriceBrl").decimalValue().compareTo(experiment.getUnitPrice())
                == 0,
        "O checkout canônico precisa usar HTTPS e o mesmo preço da oferta.");
    for (String field :
        List.of(
            "deliverable",
            "deliveryMode",
            "checkoutMonetization",
            "riskReversal",
            "valueUnit",
            "valueEvidenceMetric"))
      require(
          !snapshot.path(field).asText().isBlank(),
          "Complete o contrato comercial Safira: " + field + ".");
    require(
        snapshot.path("creatives").isArray() && !snapshot.path("creatives").isEmpty(),
        "Aprove ao menos um criativo final fiel à experiência, ao preço e à entrega.");
    gate(scope, "CREATIVE_APPROVED");
    if (!direct) {
      require(
          snapshot.path("savedAudience").isArray() && !snapshot.path("savedAudience").isEmpty(),
          "Salve o público oficial de Atena no mesmo experimento.");
    }
    gate(scope, "TARGETING_READY");
    require(
        scope.commercialPlan() != null
            && scope.commercialPlan().getStatus() != CommercialPlanStatus.CANCELLED,
        "Vincule um plano comercial vigente ao experimento Safira.");
    if (direct) {
      require(
          scope.commercialPlan().getMainChannel() != null
              && scope
                  .commercialPlan()
                  .getMainChannel()
                  .matches("(?s)^DIRECT_ONE_TO_ONE(?:\\s.*)?$"),
          "O plano Safira deve declarar DIRECT_ONE_TO_ONE e preservar consentimento individual.");
      require(
          !snapshot.path("commercialPlan").path("targetAudience").asText().isBlank()
              && experiment.getSampleSize() != null
              && experiment.getSampleSize() > 0,
          "Defina público elegível e amostra do piloto individual consentido.");
      require(
          (experiment.getDailyBudget() == null || experiment.getDailyBudget().signum() == 0)
              && (experiment.getMediaSpendLimit() == null
                  || experiment.getMediaSpendLimit().signum() == 0),
          "O piloto individual consentido deve permanecer sem orçamento de mídia.");
    } else {
      require(
          containsInstagram(scope.commercialPlan().getMainChannel()),
          "O plano Safira deve declarar literalmente Instagram Ads como canal principal.");
    }
    require(
        !snapshot.path("commercialPlan").path("mainOffer").asText().isBlank()
            && !snapshot.path("commercialPlan").path("successCriteria").asText().isBlank()
            && !snapshot.path("commercialPlan").path("stopCriteria").asText().isBlank(),
        "Complete oferta, sucesso e parada no plano comercial.");
  }

  /** Exige custos completos e decisão de Plutus para os três cenários da mesma versão. */
  private void economics(SafiraCommercialContext.Scope scope, JsonNode snapshot) {
    JsonNode plan = snapshot.path("financialPlan");
    require(
        plan.isObject(), "Cadastre o plano financeiro LIVE da versão e do plano comercial Safira.");
    require(
        !plan.path("stale").asBoolean(true), "Atualize o plano financeiro vencido ou alterado.");
    require(
        "PROJECTED_VIABLE".equals(plan.path("evaluation").path("status").asText()),
        "A projeção determinística precisa comprovar viabilidade com todos os custos.");
    require(
        plan.path("assumptions").path("priceBrl").isNumber()
            && plan.path("assumptions")
                    .path("priceBrl")
                    .decimalValue()
                    .compareTo(scope.experiment().getUnitPrice())
                == 0,
        "O preço da projeção financeira diverge da oferta Safira.");
    require(
        "COMPLETED".equals(plan.path("analysis").path("status").asText()),
        "Solicite e conclua o parecer de Plutus para a revisão Safira.");
    JsonNode result = plan.path("analysis").path("result");
    require(
        "APPROVE".equals(result.path("decision").asText()),
        "Plutus não aprovou a hipótese econômica desta revisão.");
    require(
        List.of("COMPLETE_AGGREGATE", "COMPLETE_DETAILED")
            .contains(result.path("costCoverageAssessment").path("status").asText()),
        "Plutus não comprovou IA, infraestrutura, entrega, suporte, mídia, taxas e reembolsos.");
    JsonNode scenarios = result.path("scenarios");
    require(
        scenarios.isArray() && scenarios.size() == 3,
        "O parecer de Plutus precisa preservar cenários conservador, esperado e intensivo.");
    JsonNode base = null;
    for (JsonNode scenario : scenarios)
      if ("BASE".equals(scenario.path("name").asText())) base = scenario;
    require(
        base != null
            && base.path("profitBrl").isNumber()
            && base.path("profitBrl").decimalValue().signum() > 0
            && base.path("averagePriceBrl").isNumber()
            && base.path("averagePriceBrl")
                    .decimalValue()
                    .compareTo(scope.experiment().getUnitPrice())
                == 0,
        "O cenário esperado de Plutus não demonstra lucro positivo com o preço atual.");
  }

  /** Revalida todas as fontes determinísticas antes dos agentes e da consolidação. */
  public void all(SafiraCommercialContext.Scope scope, JsonNode snapshot) {
    PREPARATION.forEach(activity -> check(activity, scope, snapshot));
  }

  /** Consulta um requisito canônico sem aprovar orçamento ou publicação por consequência. */
  private void gate(SafiraCommercialContext.Scope scope, String code) {
    var requirement =
        readiness.summarize(scope.experiment().getId()).runningGateRequirements().stream()
            .filter(item -> code.equals(item.code()))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "O experimento Safira não expôs o requisito obrigatório " + code + "."));
    require(requirement.ready(), requirement.detail() + " " + requirement.recommendation());
  }

  /** Confirma o canal decidido sem aceitar rótulos genéricos de Meta ou Instagram. */
  private boolean containsInstagram(String value) {
    return value != null
        && value.toLowerCase(java.util.Locale.ROOT).contains("instagram")
        && value.toLowerCase(java.util.Locale.ROOT).contains("ads");
  }

  /** Recusa destinos sem protocolo público navegável. */
  private boolean http(String value) {
    try {
      URI uri = URI.create(value);
      return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
    } catch (IllegalArgumentException ex) {
      log.warn("Safira: URL pública inválida durante a conferência da jornada", ex);
      return false;
    }
  }
}
