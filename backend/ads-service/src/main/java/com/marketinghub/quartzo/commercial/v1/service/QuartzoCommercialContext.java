package com.marketinghub.quartzo.commercial.v1.service;

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
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.service.ExperimentCampaignDestinationPolicy;
import com.marketinghub.experiment.service.ExperimentTargetingSelectionService;
import com.marketinghub.financialplan.v1.FinancialPlanRevision.Environment;
import com.marketinghub.financialplan.v1.service.FinancialPlanService;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationAudit;
import com.marketinghub.planning.service.CommercialPlanLandingAssetService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Responsabilidade: reunir fontes comerciais do Quartzo sem exigir contratos ou slots de Opala. */
@Component
@Slf4j
public class QuartzoCommercialContext {
  public static final String CODE = "quartzo-commercial-preparation-v1";
  public static final String TYPE = "LOW_TICKET_DIGITAL_PRODUCT";
  private static final Object TRANSACTION_CACHE_RESOURCE = new Object();
  private final ExperimentRepository experiments;
  private final LearningSalesCycleRepository cycles;
  private final ExperimentCampaignDestinationPolicy destinations;
  private final CreativeRepository creatives;
  private final CommercialPlanLandingAssetService assets;
  private final ExperimentTargetingSelectionService targeting;
  private final FinancialPlanService finances;
  private final CommercialPlanRepository plans;
  private final ObjectMapper json;

  /** Configura as fontes oficiais; o plano financeiro é resolvido sem ciclo de inicialização. */
  public QuartzoCommercialContext(
      ExperimentRepository experiments,
      LearningSalesCycleRepository cycles,
      ExperimentCampaignDestinationPolicy destinations,
      CreativeRepository creatives,
      CommercialPlanLandingAssetService assets,
      ExperimentTargetingSelectionService targeting,
      @Lazy FinancialPlanService finances,
      CommercialPlanRepository plans,
      ObjectMapper json) {
    this.experiments = experiments;
    this.cycles = cycles;
    this.destinations = destinations;
    this.creatives = creatives;
    this.assets = assets;
    this.targeting = targeting;
    this.finances = finances;
    this.plans = plans;
    this.json = json;
  }

  /** Transporta identidade persistida e publicação auditada, inclusive antes do primeiro ciclo. */
  public record Scope(
      Experiment experiment,
      Product product,
      String productVersion,
      Long cycleId,
      Long chainId,
      GeraSalesPagePublicationAudit publication) {}

  /** Responsabilidade: manter a entidade do ciclo junto da projeção pública da referência. */
  private record ResolvedScope(Scope scope, LearningSalesCycle cycle) {}

  /** Responsabilidade: compartilhar fontes e fotografia somente durante a transação corrente. */
  private record TransactionContextCache(
      Map<String, ResolvedScope> scopes, Map<String, ObjectNode> snapshots) {}

  /** Reconhece exclusivamente o código oficial do tipo, nunca seu nome comercial. */
  public boolean applies(Product product) {
    return product != null
        && product.getProductTypeDefinition() != null
        && TYPE.equals(product.getProductTypeDefinition().getCode());
  }

  /** Confere a propriedade da referência e preserva o estado interrompido sem reativá-lo. */
  public Scope scope(String source, Long productId, boolean mutation) {
    require(
        source != null && source.matches("experiment:[1-9][0-9]{0,17}"),
        "Selecione o experimento exato do produto Quartzo antes da preparação.");
    ResolvedScope resolved =
        TransactionSynchronizationManager.isSynchronizationActive()
            ? transactionContextCache().scopes().computeIfAbsent(source, this::resolveScope)
            : resolveScope(source);
    require(
        productId == null || Objects.equals(productId, resolved.scope().product().getId()),
        "O experimento pertence a outro produto.");
    if (mutation) {
      require(
          List.of(ExperimentStatus.PLANNED, ExperimentStatus.USER_STOPPED, ExperimentStatus.PAUSED)
              .contains(resolved.scope().experiment().getStatus()),
          "Prepare uma candidata planejada ou interrompida; não altere a campanha em operação.");
      if (resolved.cycle() != null) {
        require(
            "OPEN".equals(resolved.cycle().getStatus())
                && List.of("AUTHORIZATION", "PUBLICATION").contains(resolved.cycle().getStage()),
            "O ciclo não está na etapa de preparação comercial.");
      }
    }
    return resolved.scope();
  }

  /** Resolve uma vez as fontes invariantes da referência dentro da mesma transação. */
  private ResolvedScope resolveScope(String source) {
    var experiment = experiments.findById(Long.parseLong(source.substring(11))).orElseThrow();
    var product = experiment.getProduct();
    require(applies(product), "Este subprocesso exige o tipo cadastrado Quartzo.");
    require(
        experiment.getExperimentType() == ExperimentType.LOW_TICKET_PRODUCT,
        "O experimento Quartzo precisa usar o contrato de venda low-ticket.");
    var cycle = cycles.findByExperimentId(experiment.getId()).orElse(null);
    require(
        cycle == null || Objects.equals(cycle.getProductId(), product.getId()),
        "O ciclo pertence a outro produto.");
    String version =
        cycle == null ? product.getValidationDefinitionVersion() : cycle.getProductVersion();
    require(version != null && !version.isBlank(), "Registre a versão do contrato do produto.");
    var publication = destinations.latestSalesPagePublication(experiment.getId()).orElse(null);
    require(
        publication == null || Objects.equals(publication.getExperimentId(), experiment.getId()),
        "A página auditada pertence a outro experimento.");
    return new ResolvedScope(
        new Scope(
            experiment,
            product,
            version,
            cycle == null ? null : cycle.getId(),
            cycle == null ? null : cycle.getChainDefinitionId(),
            publication),
        cycle);
  }

  /**
   * Congela fontes e ativos relevantes uma vez por transação; alteração material invalida as
   * revisões anteriores na próxima leitura.
   */
  public ObjectNode snapshot(String source) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) return buildSnapshot(source);
    var cache = transactionContextCache().snapshots();
    var cached = cache.get(source);
    if (cached == null) {
      cached = buildSnapshot(source);
      cache.put(source, cached);
    }
    return cached.deepCopy();
  }

  /** Monta a fotografia canônica quando ainda não existe leitura compartilhada na transação. */
  private ObjectNode buildSnapshot(String source) {
    var scope = scope(source, null, false);
    var experiment = scope.experiment();
    var product = scope.product();
    var result = json.createObjectNode();
    result.put("contractVersion", "QUARTZO_COMMERCIAL_PREPARATION_V1");
    result.put("productTypeCode", TYPE);
    result.put("productId", product.getId());
    result.put("experimentId", experiment.getId());
    result.put("productVersion", scope.productVersion());
    result.putPOJO("cycleId", scope.cycleId());
    result.put("priceBrl", experiment.getUnitPrice());
    result.put("productPriceBrl", product.getCurrentPriceBrl());
    result.put("singlePain", experiment.getSinglePain());
    result.put("freeReward", experiment.getFreeReward());
    result.put("funnelPromise", experiment.getFunnelPromise());
    result.put("primaryCta", experiment.getPrimaryCta());
    result.put("campaignObjective", Objects.toString(experiment.getCampaignObjective(), ""));
    result.put("platform", Objects.toString(experiment.getPlatform(), ""));
    result.put("promise", product.getPromise());
    result.put("deliverable", product.getTripwire());
    result.put("deliveryMode", product.getDeliveryMode());
    result.put("checkoutMonetization", product.getCheckoutMonetization());
    result.put("riskReversal", product.getRiskReversal());
    result.set("productContract", read(product.getPdeExperienceJson()));
    result.set("validationContract", read(product.getValidationDefinitionJson()));
    var publication = scope.publication();
    if (publication != null) {
      result.put("publicationId", publication.getId());
      result.put("publicationJobId", publication.getPublicationJobId());
      result.put("destinationUrl", publication.getSalesPageUrl());
      result.put("checkoutUrl", publication.getCheckoutUrl());
      result.put("pageHash", fingerprintText(Objects.toString(publication.getHtml(), "")));
    }
    result.set("productProof", json.valueToTree(assets.payloadForExperiment(experiment.getId())));
    result.put(
        "productProofInPage",
        publication != null
            && assets.hasRequiredApprovedAssetReferences(
                experiment.getId(), publication.getHtml()));
    var allCreatives = creatives.findByExperimentId(experiment.getId());
    var superseded =
        allCreatives.stream()
            .map(Creative::getSourceCreative)
            .filter(Objects::nonNull)
            .map(Creative::getId)
            .toList();
    var announcements = result.putArray("creatives");
    allCreatives.stream()
        .filter(
            c ->
                c.getStatus() == CreativeStatus.READY
                    && c.getAgentReviewStatus() == CreativeAgentReviewStatus.APPROVED
                    && !superseded.contains(c.getId()))
        .sorted(java.util.Comparator.comparing(Creative::getId))
        .forEach(
            c ->
                announcements
                    .addObject()
                    .put("id", c.getId())
                    .put("version", c.getVersionNumber())
                    .put("headline", c.getHeadline())
                    .put("primaryText", c.getPrimaryText())
                    .put("description", c.getDescription())
                    .put("imageUrl", c.getImageUrl())
                    .put("videoUrl", c.getVideoUrl())
                    .put("destinationUrl", c.getDestinationUrl())
                    .set("review", read(c.getAgentReviewJson())));
    result.set("savedAudience", json.valueToTree(targeting.list(experiment.getId())));
    var allowedPlans =
        plans.findByExperimentReference(experiment.getId()).stream()
            .map(com.marketinghub.planning.CommercialPlan::getId)
            .toList();
    var financial =
        finances.list("PRODUCT", product.getId(), Environment.LIVE).stream()
            .filter(p -> allowedPlans.contains(p.commercialPlanId()))
            .filter(p -> scope.productVersion().equals(p.assumptions().productVersion()))
            .findFirst()
            .orElse(null);
    result.set("financialPlan", financial == null ? json.nullNode() : json.valueToTree(financial));
    result.put("publicationAuthorized", false);
    result.put("mediaSpendAuthorized", false);
    result.put("salesProven", false);
    result.put("fingerprint", fingerprintText(canonical(result).toString()));
    return result;
  }

  /** Mantém a fotografia somente no ciclo da transação e a remove em suspensão ou conclusão. */
  private TransactionContextCache transactionContextCache() {
    var current = TransactionSynchronizationManager.getResource(TRANSACTION_CACHE_RESOURCE);
    if (current != null) return (TransactionContextCache) current;
    var cache = new TransactionContextCache(new HashMap<>(), new HashMap<>());
    TransactionSynchronizationManager.bindResource(TRANSACTION_CACHE_RESOURCE, cache);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          /** Libera o cache ao suspender a transação para não contaminar uma transação aninhada. */
          @Override
          public void suspend() {
            unbindIfCurrent(cache);
          }

          /** Restaura o cache quando a transação original volta à thread. */
          @Override
          public void resume() {
            if (!TransactionSynchronizationManager.hasResource(TRANSACTION_CACHE_RESOURCE))
              TransactionSynchronizationManager.bindResource(TRANSACTION_CACHE_RESOURCE, cache);
          }

          /** Descarta a fotografia ao terminar a leitura ou escrita corrente. */
          @Override
          public void afterCompletion(int status) {
            unbindIfCurrent(cache);
          }
        });
    return cache;
  }

  /** Remove somente o cache criado por esta transação, preservando qualquer contexto restaurado. */
  private void unbindIfCurrent(TransactionContextCache cache) {
    if (TransactionSynchronizationManager.getResource(TRANSACTION_CACHE_RESOURCE) == cache)
      TransactionSynchronizationManager.unbindResource(TRANSACTION_CACHE_RESOURCE);
  }

  /** Identifica somente as fontes que comprovam a atividade, preservando provas independentes. */
  public static String activityFingerprint(String activity, JsonNode snapshot) {
    if ("ready".equals(activity) || activity == null || activity.isBlank())
      return snapshot.path("fingerprint").asText();
    var scoped = JsonNodeFactory.instance.objectNode();
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
            "priceBrl",
            "productPriceBrl"));
    switch (activity) {
      case "entry" ->
          copy(
              snapshot,
              scoped,
              List.of(
                  "singlePain",
                  "freeReward",
                  "funnelPromise",
                  "primaryCta",
                  "campaignObjective",
                  "platform",
                  "promise",
                  "deliverable",
                  "productContract",
                  "publicationId",
                  "publicationJobId",
                  "destinationUrl",
                  "pageHash",
                  "productProof",
                  "productProofInPage"));
      case "creative" ->
          copy(
              snapshot,
              scoped,
              List.of("promise", "deliverable", "destinationUrl", "pageHash", "creatives"));
      case "checkout" ->
          copy(
              snapshot,
              scoped,
              List.of(
                  "checkoutUrl",
                  "deliverable",
                  "deliveryMode",
                  "checkoutMonetization",
                  "riskReversal",
                  "validationContract"));
      case "targeting" -> copy(snapshot, scoped, List.of("savedAudience"));
      case "economics" -> copy(snapshot, scoped, List.of("financialPlan"));
      default -> {
        return snapshot.path("fingerprint").asText();
      }
    }
    return fingerprintText(canonical(scoped).toString());
  }

  /** Copia ausências como nulo para que remoções materiais também alterem a identificação. */
  private static void copy(JsonNode source, ObjectNode target, List<String> fields) {
    fields.forEach(
        field -> target.set(field, source.has(field) ? source.get(field) : NullNode.instance));
  }

  /** Normaliza ordem de propriedades e escala numérica sem alterar o significado dos contratos. */
  private static JsonNode canonical(JsonNode value) {
    if (value.isObject()) {
      var normalized = JsonNodeFactory.instance.objectNode();
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

  /** Identifica um conjunto de fontes sem usar data de consulta nem outras informações voláteis. */
  private static String fingerprintText(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      log.error("Quartzo: falha ao identificar o contrato comercial", ex);
      throw new IllegalStateException("Não foi possível identificar os ativos comerciais.", ex);
    }
  }

  /** Lê contratos persistidos e mantém corrupção explícita, em vez de inventar ausência. */
  public JsonNode read(String value) {
    try {
      return json.readTree(value == null || value.isBlank() ? "{}" : value);
    } catch (Exception ex) {
      log.error("Quartzo: contrato comercial persistido inválido", ex);
      throw new IllegalStateException("O contrato comercial contém JSON inválido.", ex);
    }
  }

  /** Exige uma condição funcional antes de produzir qualquer comprovação. */
  public static void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException(message);
  }
}
