package com.marketinghub.safira.commercial.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.run.service.ExperimentHomologationLifecycle;
import com.marketinghub.experiment.service.ExperimentTargetingSelectionService;
import com.marketinghub.experiment.service.IntegratedPdeJourneyEvidenceService;
import com.marketinghub.financialplan.v1.FinancialPlanRevision.Environment;
import com.marketinghub.financialplan.v1.service.FinancialPlanService;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.service.PdeCommercialCheckoutContractResolver;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: congelar as fontes comerciais públicas de um Produto IA Safira sem promover a
 * validação privada a evidência humana ou de venda.
 */
@Component
@Slf4j
public class SafiraCommercialContext {
  public static final String CODE = "safira-commercial-preparation-v1";
  public static final String TYPE = "AI_PRODUCT";

  private final ExperimentRepository experiments;
  private final LearningSalesCycleRepository cycles;
  private final PdeProductionSlotRepository slots;
  private final IntegratedPdeJourneyEvidenceService integratedJourney;
  private final PdeCommercialCheckoutContractResolver checkoutResolver;
  private final CreativeRepository creatives;
  private final ExperimentTargetingSelectionService targeting;
  private final FinancialPlanService finances;
  private final CommercialPlanRepository plans;
  private final ObjectMapper json;

  /** Configura as fontes oficiais do experimento, da experiência, da oferta e da economia. */
  public SafiraCommercialContext(
      ExperimentRepository experiments,
      LearningSalesCycleRepository cycles,
      PdeProductionSlotRepository slots,
      IntegratedPdeJourneyEvidenceService integratedJourney,
      PdeCommercialCheckoutContractResolver checkoutResolver,
      CreativeRepository creatives,
      ExperimentTargetingSelectionService targeting,
      @Lazy FinancialPlanService finances,
      CommercialPlanRepository plans,
      ObjectMapper json) {
    this.experiments = experiments;
    this.cycles = cycles;
    this.slots = slots;
    this.integratedJourney = integratedJourney;
    this.checkoutResolver = checkoutResolver;
    this.creatives = creatives;
    this.targeting = targeting;
    this.finances = finances;
    this.plans = plans;
    this.json = json;
  }

  /** Transporta as identidades persistidas da candidata comercial exata. */
  public record Scope(
      Experiment experiment,
      Product product,
      String productVersion,
      Long cycleId,
      Long chainId,
      PdeProductionSlot slot,
      CommercialPlan commercialPlan) {}

  /** Reconhece exclusivamente o código oficial Safira, nunca nome ou mecanismo inferido. */
  public boolean applies(Product product) {
    return product != null
        && product.getProductTypeDefinition() != null
        && TYPE.equals(product.getProductTypeDefinition().getCode());
  }

  /** Resolve a identidade antes de consultar fontes atuais ou preservar comprovações históricas. */
  private Experiment experiment(String source, Long productId) {
    require(
        source != null && source.matches("experiment:[1-9][0-9]{0,17}"),
        "Crie e selecione o experimento comercial exato de Safira antes da preparação.");
    Experiment experiment =
        experiments
            .findById(Long.parseLong(source.substring(11)))
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "O experimento comercial Safira informado não foi encontrado."));
    Product product = experiment.getProduct();
    require(
        product != null && applies(product), "Este subprocesso exige o tipo cadastrado Safira.");
    require(
        productId == null || Objects.equals(productId, product.getId()),
        "O experimento Safira pertence a outro produto.");
    return experiment;
  }

  /** Consulta o ciclo pela referência oficial, mesmo quando a navegação omite seu identificador. */
  private LearningSalesCycle cycle(Experiment experiment) {
    LearningSalesCycle cycle = cycles.findByExperimentId(experiment.getId()).orElse(null);
    require(
        cycle == null || Objects.equals(cycle.getProductId(), experiment.getProduct().getId()),
        "O ciclo comercial pertence a outro produto.");
    return cycle;
  }

  /** Distingue arquivo histórico de candidata sem exigir que suas fontes continuem vigentes. */
  public String historicalBlockReason(String source, Long productId) {
    Experiment experiment = experiment(source, productId);
    return historicalBlockReason(experiment, cycle(experiment));
  }

  /** Preserva o encerramento do experimento ou ciclo sem renovar economia e pareceres. */
  private String historicalBlockReason(Experiment experiment, LearningSalesCycle cycle) {
    String reason = ExperimentHomologationLifecycle.blockReason(experiment);
    if (reason != null) return reason;
    if (cycle != null && !"OPEN".equals(cycle.getStatus())) {
      return "O ciclo #"
          + cycle.getId()
          + " está encerrado. Preserve as provas da preparação comercial; a continuidade exige "
          + "novo ciclo/experimento com limites próprios, sem repetir pareceres nesta referência.";
    }
    return null;
  }

  /** Confere identidade, encerramento, subtipo e estado seguro antes de qualquer nova atividade. */
  public Scope scope(String source, Long productId, boolean mutation) {
    Experiment experiment = experiment(source, productId);
    Product product = experiment.getProduct();
    LearningSalesCycle cycle = cycle(experiment);
    if (mutation) {
      String closed = historicalBlockReason(experiment, cycle);
      require(closed == null, closed);
    }
    require(
        experiment.getExperimentType() == ExperimentType.LOW_TICKET_PRODUCT,
        "O experimento Safira precisa usar o contrato de venda de Produto IA.");
    require(
        experiment.getProductAiSubtype() != null,
        "Prepare e registre o subtipo do Produto IA pela hipótese antes do experimento.");
    require(
        experiment.getPlatform() == ExperimentPlatform.FACEBOOK,
        "Safira exige aquisição paga no Instagram; convite individual, lista própria, contato direto e divulgação orgânica não são canais executáveis.");
    if (mutation) {
      require(
          List.of(ExperimentStatus.PLANNED, ExperimentStatus.USER_STOPPED, ExperimentStatus.PAUSED)
              .contains(experiment.getStatus()),
          "Prepare uma candidata planejada ou interrompida; não altere campanha em operação.");
    }
    if (mutation && cycle != null) {
      require(
          "OPEN".equals(cycle.getStatus())
              && List.of("AUTHORIZATION", "PUBLICATION").contains(cycle.getStage()),
          "O ciclo não está na etapa de preparação comercial.");
    }
    PdeProductionSlot slot =
        slots.findFirstBySourceExperimentIdOrderByUpdatedAtDesc(experiment.getId()).orElse(null);
    if (slot != null) {
      require(
          Objects.equals(slot.getSourceExperimentId(), experiment.getId())
              && Objects.equals(slot.getProductSlug(), product.getSlug()),
          "O slot público Safira pertence a outro produto ou experimento.");
      require(
          slot.getExperienceVersion() != null && !slot.getExperienceVersion().isBlank(),
          "O slot público Safira não identifica sua versão comercial.");
      require(
          cycle == null
              || cycle.getProductVersion() == null
              || cycle.getProductVersion().isBlank()
              || cycle.getProductVersion().equals(slot.getExperienceVersion()),
          "A versão pública Safira diverge da versão congelada no ciclo comercial.");
    }
    CommercialPlan plan =
        plans.findByExperimentReference(experiment.getId()).stream().findFirst().orElse(null);
    String version =
        slot != null
            ? slot.getExperienceVersion()
            : cycle != null ? cycle.getProductVersion() : product.getValidationDefinitionVersion();
    require(version != null && !version.isBlank(), "Registre a versão comercial do Produto IA.");
    return new Scope(
        experiment,
        product,
        version,
        cycle == null ? null : cycle.getId(),
        cycle == null ? null : cycle.getChainDefinitionId(),
        slot,
        plan);
  }

  /** Monta uma fotografia e valida cada versão contra sua fonte canônica correspondente. */
  public ObjectNode snapshot(String source) {
    Scope scope = scope(source, null, false);
    Experiment experiment = scope.experiment();
    Product product = scope.product();
    ObjectNode result = json.createObjectNode();
    result.put("contractVersion", "SAFIRA_COMMERCIAL_PREPARATION_V1");
    result.put("productTypeCode", TYPE);
    result.put("productId", product.getId());
    result.put("experimentId", experiment.getId());
    result.put("productVersion", scope.productVersion());
    result.putPOJO("cycleId", scope.cycleId());
    result.put("productAiSubtype", Objects.toString(experiment.getProductAiSubtype(), ""));
    result.put("platform", Objects.toString(experiment.getPlatform(), ""));
    result.put("sampleSize", experiment.getSampleSize());
    result.put("dailyBudgetBrl", experiment.getDailyBudget());
    result.put("mediaSpendLimitBrl", experiment.getMediaSpendLimit());
    result.put("campaignObjective", Objects.toString(experiment.getCampaignObjective(), ""));
    result.put("priceBrl", experiment.getUnitPrice());
    result.put("productPriceBrl", product.getCurrentPriceBrl());
    result.put("singlePain", experiment.getSinglePain());
    result.put("freeReward", experiment.getFreeReward());
    result.put("funnelPromise", experiment.getFunnelPromise());
    result.put("primaryCta", experiment.getPrimaryCta());
    result.put("desireTerritoryCode", experiment.getDesireTerritoryCode());
    result.set("desireTerritory", read(experiment.getDesireTerritorySnapshotJson()));
    result.set("desireAssociationMap", read(product.getDesireAssociationMapJson()));
    result.put("promise", product.getPromise());
    result.put("deliverable", product.getTripwire());
    result.put("deliveryMode", product.getDeliveryMode());
    result.put("checkoutMonetization", product.getCheckoutMonetization());
    result.put("riskReversal", product.getRiskReversal());
    result.put("valueUnit", product.getValueUnit());
    result.put("valueEvidenceMetric", product.getValueEvidenceMetric());
    result.set("productContract", read(product.getPdeExperienceJson()));
    result.set("validationContract", read(product.getValidationDefinitionJson()));
    result.put("privateValidationReusedAsProductReference", true);
    result.put("humanEvidenceClaimed", false);
    result.put("commercialEvidenceClaimed", false);

    PdeProductionSlot slot = scope.slot();
    if (slot != null) {
      result.put("slotId", slot.getId());
      result.put("slotCode", slot.getSlotCode());
      result.put("slotStatus", Objects.toString(slot.getStatus(), ""));
      result.put("slotValidationStatus", slot.getValidationStatus());
      result.put("publicUrl", slot.getPublicUrl());
      result.put("backendUrl", slot.getBackendUrl());
      JsonNode published = read(slot.getPublishedExperienceJson());
      result.set("publishedExperience", published);
      result.put(
          "experienceHash", fingerprintText(canonical(published).toString(), "experiência Safira"));
      checkoutResolver
          .resolve(product, published)
          .ifPresent(
              checkout -> {
                result.put("checkoutProvider", checkout.provider());
                result.put("checkoutReference", checkout.offerReference());
                result.put("checkoutUrl", checkout.checkoutUrl());
                result.put("checkoutPriceBrl", checkout.priceBrl());
              });
    }
    result.put("commercialJourneyIntegrated", integratedJourney.isReady(experiment));

    var allCreatives = creatives.findByExperimentId(experiment.getId());
    var superseded =
        allCreatives.stream()
            .map(Creative::getSourceCreative)
            .filter(Objects::nonNull)
            .map(Creative::getId)
            .toList();
    var approved = result.putArray("creatives");
    allCreatives.stream()
        .filter(
            creative ->
                creative.getStatus() == CreativeStatus.READY
                    && creative.getAgentReviewStatus() == CreativeAgentReviewStatus.APPROVED
                    && !superseded.contains(creative.getId()))
        .sorted(java.util.Comparator.comparing(Creative::getId))
        .forEach(
            creative ->
                approved
                    .addObject()
                    .put("id", creative.getId())
                    .put("version", creative.getVersionNumber())
                    .put("headline", creative.getHeadline())
                    .put("primaryText", creative.getPrimaryText())
                    .put("description", creative.getDescription())
                    .put("imageUrl", creative.getImageUrl())
                    .put("videoUrl", creative.getVideoUrl())
                    .put("destinationUrl", creative.getDestinationUrl())
                    .set("review", read(creative.getAgentReviewJson())));
    result.set("savedAudience", json.valueToTree(targeting.list(experiment.getId())));
    result.set("commercialPlan", commercialPlan(scope.commercialPlan()));

    var allowedPlans =
        plans.findByExperimentReference(experiment.getId()).stream()
            .map(CommercialPlan::getId)
            .toList();
    var financial =
        finances.list("PRODUCT", product.getId(), Environment.LIVE).stream()
            .filter(plan -> allowedPlans.contains(plan.commercialPlanId()))
            .filter(
                plan ->
                    Objects.equals(
                        product.getValidationDefinitionVersion(),
                        plan.assumptions().productVersion()))
            .findFirst()
            .orElse(null);
    result.set("financialPlan", financial == null ? json.nullNode() : json.valueToTree(financial));
    result.put("publicationAuthorized", false);
    result.put("mediaSpendAuthorized", false);
    result.put("salesProven", false);
    result.put("fingerprint", fingerprintText(canonical(result).toString(), "contrato Safira"));
    return result;
  }

  /** Projeta apenas os campos comerciais necessários sem serializar o grafo JPA do plano. */
  private JsonNode commercialPlan(CommercialPlan plan) {
    if (plan == null) return json.nullNode();
    ObjectNode result = json.createObjectNode();
    result.put("id", plan.getId());
    result.put("name", plan.getName());
    result.put("status", Objects.toString(plan.getStatus(), ""));
    result.put("mainChannel", plan.getMainChannel());
    result.put("mainOffer", plan.getMainOffer());
    result.put("targetAudience", plan.getTargetAudience());
    result.put("offerPriceBrl", plan.getOfferPriceBrl());
    result.put("maxBudgetBrl", plan.getMaxBudget());
    result.put("successCriteria", plan.getSuccessCriteria());
    result.put("stopCriteria", plan.getStopCriteria());
    return result;
  }

  /** Calcula a impressão da etapa para preservar conclusões independentes quando possível. */
  public static String activityFingerprint(String activity, JsonNode snapshot) {
    if ("ready".equals(activity) || activity == null || activity.isBlank())
      return snapshot.path("fingerprint").asText();
    ObjectNode scoped = JsonNodeFactory.instance.objectNode();
    copy(
        snapshot,
        scoped,
        List.of(
            "contractVersion",
            "productTypeCode",
            "productId",
            "experimentId",
            "productVersion",
            "cycleId",
            "productAiSubtype",
            "platform",
            "sampleSize",
            "dailyBudgetBrl",
            "mediaSpendLimitBrl",
            "priceBrl",
            "productPriceBrl"));
    if ("journey".equals(activity)) {
      copy(
          snapshot,
          scoped,
          List.of(
              "singlePain",
              "freeReward",
              "funnelPromise",
              "primaryCta",
              "desireTerritoryCode",
              "desireTerritory",
              "promise",
              "deliverable",
              "deliveryMode",
              "checkoutMonetization",
              "riskReversal",
              "valueUnit",
              "valueEvidenceMetric",
              "slotId",
              "slotCode",
              "slotStatus",
              "slotValidationStatus",
              "publicUrl",
              "backendUrl",
              "experienceHash",
              "checkoutProvider",
              "checkoutReference",
              "checkoutUrl",
              "checkoutPriceBrl",
              "commercialJourneyIntegrated",
              "creatives",
              "savedAudience",
              "commercialPlan"));
    } else if ("economics".equals(activity)) {
      copy(snapshot, scoped, List.of("commercialPlan", "financialPlan"));
    } else {
      return snapshot.path("fingerprint").asText();
    }
    return fingerprintText(canonical(scoped).toString(), "atividade Safira");
  }

  /** Copia ausência como nulo para que remoção de contrato também invalide a impressão. */
  private static void copy(JsonNode source, ObjectNode target, List<String> fields) {
    fields.forEach(
        field -> target.set(field, source.has(field) ? source.get(field) : NullNode.instance));
  }

  /** Normaliza ordem de propriedades e escala numérica antes de calcular SHA-256. */
  private static JsonNode canonical(JsonNode value) {
    if (value.isObject()) {
      ObjectNode normalized = JsonNodeFactory.instance.objectNode();
      var keys = new java.util.TreeSet<String>();
      value.fieldNames().forEachRemaining(keys::add);
      keys.forEach(key -> normalized.set(key, canonical(value.get(key))));
      return normalized;
    }
    if (value.isArray()) {
      var normalized = JsonNodeFactory.instance.arrayNode();
      value.forEach(item -> normalized.add(canonical(item)));
      return normalized;
    }
    if (value.isNumber())
      return com.fasterxml.jackson.databind.node.DecimalNode.valueOf(
          value.decimalValue().stripTrailingZeros());
    return value;
  }

  /** Identifica um documento imutável sem incluir data de leitura ou outro campo volátil. */
  private static String fingerprintText(String value, String subject) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      log.error("Safira: falha ao identificar {}", subject, ex);
      throw new IllegalStateException("Não foi possível identificar " + subject + ".", ex);
    }
  }

  /** Lê JSON persistido e mantém corrupção explícita em vez de inventar ausência. */
  public JsonNode read(String value) {
    try {
      return json.readTree(value == null || value.isBlank() ? "{}" : value);
    } catch (Exception ex) {
      log.error("Safira: contrato comercial persistido inválido", ex);
      throw new IllegalStateException("O contrato comercial Safira contém JSON inválido.", ex);
    }
  }

  /** Interrompe a preparação quando uma condição funcional não está comprovada. */
  public static void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException(message);
  }
}
