package com.marketinghub.experiment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTaskTargetContextProvider;
import com.marketinghub.agenttask.AgentTaskTargetResponse;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.pde.service.PdeCommercialCheckoutContractResolver;
import com.marketinghub.pde.service.PdeCommercialCheckoutContractResolver.CanonicalCheckout;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: publicar o alvo comercial segregado das tarefas ligadas a PDEs. */
@Service
public class ExperimentAgentTaskTargetContextProvider implements AgentTaskTargetContextProvider {
  private static final Logger log =
      LoggerFactory.getLogger(ExperimentAgentTaskTargetContextProvider.class);
  private static final Pattern EXPERIMENT_REFERENCE =
      Pattern.compile("experiment:([1-9][0-9]*)(?:@[^:]+)?(?:[:].*)?");
  private static final Pattern PRODUCT_REFERENCE =
      Pattern.compile("product:([1-9][0-9]*)(?:@[^:]+)?(?:[:].*)?");
  private static final Pattern COMMERCIAL_PLAN_REFERENCE =
      Pattern.compile("commercial-plan:([1-9][0-9]*)(?:@[^:]+)?(?:[:].*)?");
  private static final Pattern EXPERIMENT_SEGMENT =
      Pattern.compile("(?:^|:)experiment-([1-9][0-9]*)(?:$|:)");
  private static final List<String> VERSIONED_PDE_VISUAL_PROCESSES =
      List.of(
          "pde-commercial-homologation-activation",
          "opala-commercial-preparation-v1",
          "pde-construction-approval");
  private static final List<String> ACCEPTED_PRIVATE_PROTOTYPE_PROCESSES =
      List.of(
          "pde-communication-sales-journey",
          "creative-production-approval",
          "landing-page-generation");
  private final ExperimentRepository experiments;
  private final ProductRepository products;
  private final PdeProductionSlotRepository productionSlots;
  private final CommercialPlanRepository commercialPlans;
  private final ObjectMapper objectMapper;
  private final PdeCommercialCheckoutContractResolver checkoutResolver;

  @Autowired
  private com.marketinghub.businessprocesschain.learningcycle.v1.service
          .LearningCycleConstructionContext
      cycleConstructionContext;

  @Autowired(required = false)
  private com.marketinghub.opala.commercial.v1.service.OpalaCommercialContext opalaContext;

  @Autowired(required = false)
  private com.marketinghub.opala.commercial.v1.service.OpalaCommercialVersionContract
      opalaVersionContract;

  @Autowired(required = false)
  private com.marketinghub.quartzo.commercial.v1.service.QuartzoCommercialContext quartzoContext;

  @Autowired(required = false)
  private com.marketinghub.safira.commercial.v1.service.SafiraCommercialContext safiraContext;

  /** Configura as fontes canônicas de experimento, produto e contrato PDE. */
  @Autowired
  public ExperimentAgentTaskTargetContextProvider(
      ExperimentRepository experiments,
      ProductRepository products,
      ObjectMapper objectMapper,
      PdeProductionSlotRepository productionSlots,
      CommercialPlanRepository commercialPlans,
      PdeCommercialCheckoutContractResolver checkoutResolver) {
    this.experiments = experiments;
    this.products = products;
    this.objectMapper = objectMapper;
    this.productionSlots = productionSlots;
    this.commercialPlans = commercialPlans;
    this.checkoutResolver = checkoutResolver;
  }

  /** Mantém testes focados na identidade comercial sem exigir catálogo de slots produtivos. */
  ExperimentAgentTaskTargetContextProvider(
      ExperimentRepository experiments, ProductRepository products, ObjectMapper objectMapper) {
    this(
        experiments,
        products,
        objectMapper,
        null,
        null,
        new PdeCommercialCheckoutContractResolver(objectMapper));
  }

  /** Permite testar a resolução versionada do slot sem carregar um plano comercial. */
  ExperimentAgentTaskTargetContextProvider(
      ExperimentRepository experiments,
      ProductRepository products,
      ObjectMapper objectMapper,
      PdeProductionSlotRepository productionSlots) {
    this(
        experiments,
        products,
        objectMapper,
        productionSlots,
        null,
        new PdeCommercialCheckoutContractResolver(objectMapper));
  }

  /** Permite testar a resolução por plano usando o mesmo contrato canônico de checkout. */
  ExperimentAgentTaskTargetContextProvider(
      ExperimentRepository experiments,
      ProductRepository products,
      ObjectMapper objectMapper,
      PdeProductionSlotRepository productionSlots,
      CommercialPlanRepository commercialPlans) {
    this(
        experiments,
        products,
        objectMapper,
        productionSlots,
        commercialPlans,
        new PdeCommercialCheckoutContractResolver(objectMapper));
  }

  /** Resolve somente referências explícitas e nunca usa nome livre da tarefa como identidade. */
  @Override
  @Transactional(readOnly = true)
  public Optional<AgentTaskTargetResponse> resolve(String sourceReference) {
    return resolve(sourceReference, null);
  }

  /** Separa a landing do experimento da tela da versão produtiva exata do PDE. */
  @Override
  @Transactional(readOnly = true)
  public Optional<AgentTaskTargetResponse> resolve(String sourceReference, String processCode) {
    if (sourceReference == null || sourceReference.isBlank()) return Optional.empty();
    String normalized = sourceReference.trim();
    Matcher experimentMatcher = EXPERIMENT_REFERENCE.matcher(normalized);
    if (experimentMatcher.matches()) {
      return experiments
          .findById(Long.valueOf(experimentMatcher.group(1)))
          .flatMap(
              experiment -> target(normalized, experiment, experiment.getProduct(), processCode));
    }
    Matcher productMatcher = PRODUCT_REFERENCE.matcher(normalized);
    if (productMatcher.matches()) {
      return products
          .findById(Long.valueOf(productMatcher.group(1)))
          .flatMap(product -> target(normalized, null, product, processCode));
    }
    Matcher planMatcher = COMMERCIAL_PLAN_REFERENCE.matcher(normalized);
    if (!planMatcher.matches() || commercialPlans == null) return Optional.empty();
    return commercialPlans
        .findById(Long.valueOf(planMatcher.group(1)))
        .flatMap(plan -> commercialPlanExperiment(plan, normalized))
        .flatMap(
            experiment -> target(normalized, experiment, experiment.getProduct(), processCode));
  }

  /** Exige o experimento explícito das referências novas e limita o legado ao vínculo primário. */
  private Optional<Experiment> commercialPlanExperiment(
      CommercialPlan plan, String sourceReference) {
    Matcher segment = EXPERIMENT_SEGMENT.matcher(sourceReference);
    if (segment.find()) {
      Long experimentId = Long.valueOf(segment.group(1));
      return experiments
          .findById(experimentId)
          .filter(experiment -> belongsToPlan(plan, experimentId));
    }
    if (plan.getExperiment() != null) return Optional.of(plan.getExperiment());
    if (plan.getExperiments() != null && plan.getExperiments().size() == 1) {
      return plan.getExperiments().stream().findFirst();
    }
    return Optional.empty();
  }

  /** Confirma que o experimento declarado realmente pertence ao plano antes de expor sua URL. */
  private boolean belongsToPlan(CommercialPlan plan, Long experimentId) {
    return (plan.getExperiment() != null
            && Objects.equals(plan.getExperiment().getId(), experimentId))
        || (plan.getExperiments() != null
            && plan.getExperiments().stream()
                .anyMatch(experiment -> Objects.equals(experiment.getId(), experimentId)));
  }

  /**
   * Monta o alvo conforme o tipo: experiência Safira, página Quartzo, candidata Opala ou protótipo.
   */
  private Optional<AgentTaskTargetResponse> target(
      String sourceReference, Experiment experiment, Product product, String processCode) {
    if (product == null || product.getId() == null || blank(product.getSlug())) {
      return Optional.empty();
    }
    if (safiraContext != null
        && safiraContext.applies(product)
        && experiment != null
        && List.of("safira-commercial-preparation-v1", "pde-commercial-homologation-activation")
            .contains(Objects.requireNonNullElse(processCode, ""))) {
      var scope = safiraContext.scope(sourceReference, product.getId(), false);
      var snapshot = safiraContext.snapshot(sourceReference);
      return Optional.of(
          new AgentTaskTargetResponse(
              sourceReference,
              experiment.getId(),
              product.getId(),
              product.getSlug(),
              product.getName(),
              product.getInternalName(),
              scope.productVersion(),
              snapshot.path("publicUrl").asText(null),
              snapshot.path("checkoutProvider").asText(null),
              snapshot.path("checkoutReference").asText(null),
              snapshot.path("checkoutUrl").asText(null),
              experiment.getUnitPrice(),
              snapshot));
    }
    if (quartzoContext != null
        && quartzoContext.applies(product)
        && experiment != null
        && List.of("quartzo-commercial-preparation-v1", "pde-commercial-homologation-activation")
            .contains(Objects.requireNonNullElse(processCode, ""))) {
      var scope = quartzoContext.scope(sourceReference, product.getId(), false);
      var snapshot = quartzoContext.snapshot(sourceReference);
      return Optional.of(
          new AgentTaskTargetResponse(
              sourceReference,
              experiment.getId(),
              product.getId(),
              product.getSlug(),
              product.getName(),
              product.getInternalName(),
              scope.productVersion(),
              snapshot.path("destinationUrl").asText(null),
              null,
              null,
              snapshot.path("checkoutUrl").asText(null),
              experiment.getUnitPrice(),
              snapshot));
    }
    if ("opala-commercial-preparation-v1".equals(processCode)
        && experiment != null
        && opalaContext != null
        && opalaVersionContract != null) {
      return Optional.of(opalaTarget(sourceReference, experiment, product));
    }
    if (cycleConstructionContext != null) {
      var cycleTarget = cycleConstructionContext.resolve(sourceReference, experiment, processCode);
      if (cycleTarget.isPresent()) return cycleTarget;
    }
    // A preparação anterior ao experimento conserva a versão privada aceita em todos os executores.
    if (sourceReference.equals("product:" + product.getId() + "@agent-validation-v1")
        && "PDE_AGENT_VALIDATED_V1".equals(product.getValidationDefinitionVersion())
        && java.util.Set.of(
                "pde-communication-sales-journey",
                "creative-production-approval",
                "landing-page-generation")
            .contains(Objects.requireNonNullElse(processCode, ""))) {
      processCode = "pde-construction-approval";
    }
    String experienceVersion = experienceVersion(product, processCode);
    if (blank(experienceVersion)) return Optional.empty();
    boolean privateValidation = isPrivateValidation(product, processCode);
    Optional<CanonicalCheckout> canonicalCheckout =
        canonicalCheckout(experiment, product, processCode);
    return Optional.of(
        new AgentTaskTargetResponse(
            sourceReference,
            experiment == null ? null : experiment.getId(),
            product.getId(),
            product.getSlug(),
            product.getName(),
            product.getInternalName(),
            experienceVersion,
            publicUrl(experiment, product, experienceVersion, processCode),
            canonicalCheckout.map(CanonicalCheckout::provider).orElse(null),
            canonicalCheckout.map(CanonicalCheckout::offerReference).orElse(null),
            canonicalCheckout
                .map(CanonicalCheckout::checkoutUrl)
                .orElse(
                    privateValidation || experiment == null
                        ? null
                        : experiment.getCommercialCheckoutUrl()),
            commercialPrice(experiment, product, canonicalCheckout),
            pdeContext(experiment, product, processCode)));
  }

  /**
   * Monta o alvo Opala a partir da candidata persistida sem herdar o contrato publicado anterior.
   */
  private AgentTaskTargetResponse opalaTarget(
      String sourceReference, Experiment experiment, Product product) {
    var scope = opalaContext.scope(sourceReference);
    var candidate = opalaContext.candidate(scope);
    var versioned = opalaVersionContract.resolve(scope, candidate);
    if (!Objects.equals(scope.experiment().getId(), experiment.getId())
        || !Objects.equals(scope.cycle().getProductId(), product.getId())) {
      throw new IllegalStateException("O alvo Opala pertence a outra ocorrência comercial.");
    }
    return new AgentTaskTargetResponse(
        sourceReference,
        experiment.getId(),
        product.getId(),
        product.getSlug(),
        product.getName(),
        product.getInternalName(),
        scope.cycle().getProductVersion(),
        candidate.destinationUrl(),
        versioned.checkout().provider(),
        versioned.checkout().offerReference(),
        versioned.checkout().checkoutUrl(),
        versioned.checkout().priceBrl(),
        versioned.productContract());
  }

  /** Entrega o contrato privado reconciliado com a versão aceita e sua evidência de implantação. */
  private JsonNode pdeContext(Experiment experiment, Product product, String processCode) {
    if ("pde-commercial-plan-offer".equals(processCode) && experiment != null) {
      return commercialPlanningContext(experiment, product);
    }
    if (!isPrivateValidation(product, processCode) || blank(product.getPdeExperienceJson())) {
      return null;
    }
    try {
      JsonNode context = objectMapper.readTree(product.getPdeExperienceJson());
      if (!(context instanceof ObjectNode resolved)) return null;
      resolved.put("experienceVersion", experienceVersion(product, processCode));
      resolved.remove("technicalDeploymentEvidence");
      if (!blank(product.getValidationDefinitionJson())) {
        JsonNode validation = objectMapper.readTree(product.getValidationDefinitionJson());
        JsonNode acceptance = validation.path("privatePrototypeAcceptance");
        if (acceptance.isObject()) resolved.set("privatePrototypeAcceptance", acceptance);
        JsonNode deployment = validation.path("technicalDeploymentEvidence");
        JsonNode diagnostic = deployment.path("diagnosticSnapshot");
        if (deployment.isObject()
            && "PDE_TECHNICAL_DEPLOYMENT_EVIDENCE_V1"
                .equals(deployment.path("contractVersion").asText())
            && deployment.path("httpStatus").asInt() == 200
            && !deployment.path("observedAt").asText().isBlank()
            && "UP".equals(diagnostic.path("status").asText())
            && diagnostic.path("productId").asLong() == product.getId()
            && resolved
                .path("experienceVersion")
                .asText()
                .equals(diagnostic.path("experienceVersion").asText())
            && resolved
                .path("experienceVersion")
                .asText()
                .equals(diagnostic.path("imageVersionId").asText())
            && acceptance
                .path("privateAccessUrl")
                .asText()
                .equals(diagnostic.path("publicUrl").asText())
            && !diagnostic.path("image").asText().isBlank()) {
          resolved.set("technicalDeploymentEvidence", deployment);
        }
      }
      return resolved;
    } catch (Exception ex) {
      log.error(
          "Contrato PDE privado inválido ao montar contexto da tarefa. productId={} productSlug={} processCode={}",
          product.getId(),
          product.getSlug(),
          processCode,
          ex);
      return null;
    }
  }

  /**
   * Entrega à Atena a entrada persistida do primeiro planejamento sem fabricar um ciclo de vendas.
   */
  private JsonNode commercialPlanningContext(Experiment experiment, Product product) {
    CommercialPlan plan =
        commercialPlans == null
            ? null
            : Optional.ofNullable(commercialPlans.findByExperimentReference(experiment.getId()))
                .orElseGet(List::of)
                .stream()
                .findFirst()
                .orElse(null);
    Hypothesis hypothesis = experiment.getHypothesisRef();
    boolean successor = experiment.getSourceExperiment() != null;
    String mode =
        successor
            ? "SUCCESSOR_REQUIRES_LEARNING_CYCLE"
            : plan != null
                    && plan.getId() != null
                    && hypothesis != null
                    && hypothesis.getId() != null
                ? "INITIAL_PLANNED_EXPERIMENT"
                : "INITIAL_CONTEXT_INCOMPLETE";

    ObjectNode context = objectMapper.createObjectNode();
    context.put("contractVersion", "PDE_COMMERCIAL_PLANNING_INPUT_V1");
    context.put("mode", mode);
    context.put("commercialEvidenceStatus", "NOT_MEASURED");
    context.put("publicationAuthorized", false);
    context.put("mediaSpendAuthorized", false);

    ObjectNode productNode = context.putObject("product");
    productNode.put("id", product.getId());
    productNode.put("internalName", product.getInternalName());
    productNode.put("commercialName", product.getName());
    productNode.put("type", product.getProductType());
    productNode.put("format", product.getProductFormat());
    productNode.put("deliveryMode", product.getDeliveryMode());
    productNode.put("revenueModel", product.getRevenueModel());
    productNode.put("validationDefinitionVersion", product.getValidationDefinitionVersion());
    productNode.put("targetAudience", product.getTargetAudience());
    productNode.put("languageStyle", product.getLanguageStyle());
    productNode.put("explicitPain", product.getExplicitPain());
    productNode.put("promise", product.getPromise());
    productNode.put("uniqueMechanism", product.getUniqueMechanism());
    productNode.put("primaryCta", product.getPrimaryCta());
    productNode.put("currentPriceBrl", product.getCurrentPriceBrl());
    productNode.put("riskReversal", product.getRiskReversal());
    productNode.put("funnel", product.getFunnel());
    productNode.put("storytelling", product.getStorytelling());
    setStructuredJson(
        productNode,
        "desireAssociationMap",
        product.getDesireAssociationMapJson(),
        "mapa de desejo",
        product.getId());
    setStructuredJson(
        productNode,
        "pdeExperience",
        product.getPdeExperienceJson(),
        "experiência PDE",
        product.getId());

    ObjectNode experimentNode = context.putObject("experiment");
    experimentNode.put("id", experiment.getId());
    experimentNode.put(
        "sourceExperimentId",
        experiment.getSourceExperiment() == null ? null : experiment.getSourceExperiment().getId());
    experimentNode.put(
        "status", experiment.getStatus() == null ? null : experiment.getStatus().name());
    experimentNode.put(
        "platform", experiment.getPlatform() == null ? null : experiment.getPlatform().name());
    experimentNode.put("commercialObjective", experiment.getCommercialObjective());
    experimentNode.put("singlePain", experiment.getSinglePain());
    experimentNode.put("proofPreview", experiment.getFreeReward());
    experimentNode.put("funnelPromise", experiment.getFunnelPromise());
    experimentNode.put("primaryCta", experiment.getPrimaryCta());
    experimentNode.put("primaryMetric", experiment.getPrimaryMetric());
    experimentNode.put("sampleSize", experiment.getSampleSize());
    experimentNode.put("targetCvr", experiment.getTargetCvr());
    experimentNode.put("unitPriceBrl", experiment.getUnitPrice());
    experimentNode.put("dailyBudgetBrl", experiment.getDailyBudget());
    experimentNode.put("mediaSpendLimitBrl", experiment.getMediaSpendLimit());

    ObjectNode hypothesisNode = context.putObject("hypothesis");
    if (hypothesis != null) {
      hypothesisNode.put("id", hypothesis.getId() == null ? null : hypothesis.getId().toString());
      hypothesisNode.put("title", hypothesis.getTitle());
      hypothesisNode.put("versionNumber", hypothesis.getVersionNumber());
      hypothesisNode.put("persona", hypothesis.getPersona());
      hypothesisNode.put("problem", hypothesis.getProblem());
      hypothesisNode.put("promise", hypothesis.getPromise());
      hypothesisNode.put("mechanism", hypothesis.getMechanism());
      hypothesisNode.put("delivery", hypothesis.getEntrega());
      hypothesisNode.put("successRule", hypothesis.getSuccessRule());
      hypothesisNode.put("priceBrl", hypothesis.getPrice());
      setStructuredJson(
          hypothesisNode,
          "framework",
          hypothesis.getFrameworkJson(),
          "framework da hipótese",
          product.getId());
    }

    ObjectNode planNode = context.putObject("commercialPlan");
    if (plan != null) {
      planNode.put("id", plan.getId());
      planNode.put("name", plan.getName());
      planNode.put("commercialObjective", plan.getCommercialObjective());
      planNode.put("targetAudience", plan.getTargetAudience());
      planNode.put("mainPain", plan.getMainPain());
      planNode.put("mainOffer", plan.getMainOffer());
      planNode.put("mainLeadMagnet", plan.getMainLeadMagnet());
      planNode.put("mainChannel", plan.getMainChannel());
      planNode.put("mainMetric", plan.getMainMetric());
      planNode.put("successCriteria", plan.getSuccessCriteria());
      planNode.put("stopCriteria", plan.getStopCriteria());
      planNode.put("offerPriceBrl", plan.getOfferPriceBrl());
      planNode.put("maxBudgetBrl", plan.getMaxBudget());
      planNode.put("expectedCacBrl", plan.getExpectedCacBrl());
      planNode.put("variableCostPerSaleBrl", plan.getVariableCostPerSaleBrl());
      planNode.put("fixedOperationalCostBrl", plan.getFixedOperationalCostBrl());
      planNode.put("nextAction", plan.getNextAction());
      planNode.put("currentBlocker", plan.getCurrentBlocker());
    }
    return context;
  }

  /** Converte contratos JSON persistidos em objetos estruturados e registra qualquer corrupção. */
  private void setStructuredJson(
      ObjectNode target, String field, String raw, String contractName, Long productId) {
    if (blank(raw)) return;
    try {
      target.set(field, objectMapper.readTree(raw));
    } catch (Exception ex) {
      log.error(
          "Contrato JSON inválido ao montar contexto comercial. productId={} contract={}",
          productId,
          contractName,
          ex);
    }
  }

  /** Usa o preço da versão PDE e bloqueia qualquer experimento comercial divergente. */
  private BigDecimal commercialPrice(
      Experiment experiment, Product product, Optional<CanonicalCheckout> canonicalCheckout) {
    BigDecimal fallback =
        experiment == null ? product.getCurrentPriceBrl() : experiment.getUnitPrice();
    if (canonicalCheckout.isEmpty()) return fallback;
    BigDecimal canonicalPrice = canonicalCheckout.orElseThrow().priceBrl();
    if (fallback != null && fallback.compareTo(canonicalPrice) != 0) {
      throw new IllegalStateException(
          "Preço do alvo comercial diverge do checkout versionado do contrato PDE");
    }
    return canonicalPrice;
  }

  /** Usa o checkout da mesma versão PDE somente nos processos que revisam essa experiência. */
  private Optional<CanonicalCheckout> canonicalCheckout(
      Experiment experiment, Product product, String processCode) {
    if (isPrivateValidation(product, processCode)) return Optional.empty();
    if (processCode != null && VERSIONED_PDE_VISUAL_PROCESSES.contains(processCode)) {
      return checkoutResolver.resolve(product);
    }
    return Optional.empty();
  }

  /** Resolve a tela exata do PDE ou mantém a landing própria do experimento conforme o processo. */
  private String publicUrl(
      Experiment experiment, Product product, String experienceVersion, String processCode) {
    if (isPrivateValidation(product, processCode)) {
      return privatePrototypeUrl(product);
    }
    if ("opala-commercial-preparation-v1".equals(processCode)) {
      if (productionSlots == null || experiment == null) return null;
      return productionSlots
          .findFirstBySourceExperimentIdOrderByUpdatedAtDesc(experiment.getId())
          .filter(
              slot ->
                  Objects.equals(slot.getProductSlug(), product.getSlug())
                      && Objects.equals(slot.getExperienceVersion(), experienceVersion)
                      && List.of(PdeProductionSlotStatus.READY, PdeProductionSlotStatus.ACTIVE)
                          .contains(slot.getStatus())
                      && "OK".equals(slot.getValidationStatus()))
          .map(slot -> slot.getPublicUrl())
          .orElse(null);
    }
    if (processCode != null && VERSIONED_PDE_VISUAL_PROCESSES.contains(processCode)) {
      if (productionSlots == null) return null;
      return productionSlots
          .findFirstByProductSlugAndExperienceVersionAndStatusInOrderByPublishedAtDesc(
              product.getSlug(),
              experienceVersion,
              List.of(PdeProductionSlotStatus.READY, PdeProductionSlotStatus.ACTIVE))
          .map(slot -> blank(slot.getPublicUrl()) ? null : slot.getPublicUrl().trim())
          .orElse(null);
    }
    if (experiment != null && !blank(experiment.getFollowUpActionUrl())) {
      return experiment.getFollowUpActionUrl().trim();
    }
    return blank(product.getPublicUrl()) ? null : product.getPublicUrl().trim();
  }

  /**
   * Identifica a homologação ou comunicação do protótipo aceito para impedir versão, slot ou
   * checkout históricos.
   */
  private boolean isPrivateValidation(Product product, String processCode) {
    if ("pde-construction-approval".equals(processCode)) {
      return product.getValidationDefinitionVersion() != null
          && ("PDE_PRIVATE_VALIDATION_V1".equals(product.getValidationDefinitionVersion())
              || usesPdeAgentValidationV1(product));
    }
    return "PDE_AGENT_VALIDATED_V1".equals(product.getValidationDefinitionVersion())
        && ACCEPTED_PRIVATE_PROTOTYPE_PROCESSES.contains(
            Objects.requireNonNullElse(processCode, ""));
  }

  /** Reconhece apenas os estados previstos pelo contrato multiagente v1. */
  private boolean usesPdeAgentValidationV1(Product product) {
    return "PDE_AGENT_VALIDATION_V1".equals(product.getValidationDefinitionVersion())
        || "PDE_AGENT_VALIDATED_V1".equals(product.getValidationDefinitionVersion());
  }

  /** Lê a URL da versão privada aceita, sem confundi-la com uma publicação produtiva. */
  private String privatePrototypeUrl(Product product) {
    if (blank(product.getValidationDefinitionJson())) return null;
    try {
      JsonNode acceptance =
          objectMapper
              .readTree(product.getValidationDefinitionJson())
              .path("privatePrototypeAcceptance");
      String value = acceptance.path("privateAccessUrl").asText(null);
      return "READY".equals(acceptance.path("status").asText()) && !blank(value)
          ? value.trim()
          : null;
    } catch (Exception ex) {
      log.error(
          "Contrato privado inválido ao resolver alvo da tarefa. productId={} productSlug={}",
          product.getId(),
          product.getSlug(),
          ex);
      return null;
    }
  }

  /** Usa a versão aceita do protótipo privado e mantém o contrato geral como fallback histórico. */
  private String experienceVersion(Product product, String processCode) {
    if (isPrivateValidation(product, processCode)
        && !blank(product.getValidationDefinitionJson())) {
      try {
        JsonNode acceptance =
            objectMapper
                .readTree(product.getValidationDefinitionJson())
                .path("privatePrototypeAcceptance");
        String accepted = acceptance.path("prototypeVersion").asText(null);
        if ("READY".equals(acceptance.path("status").asText()) && !blank(accepted)) {
          return accepted.trim();
        }
      } catch (Exception ex) {
        log.error(
            "Contrato privado inválido ao resolver versão da tarefa. productId={} productSlug={}",
            product.getId(),
            product.getSlug(),
            ex);
        return null;
      }
    }
    if (blank(product.getPdeExperienceJson())) return null;
    try {
      JsonNode contract = objectMapper.readTree(product.getPdeExperienceJson());
      String value = contract.path("experienceVersion").asText(null);
      return blank(value) ? null : value.trim();
    } catch (Exception ex) {
      log.error(
          "Contrato PDE inválido ao resolver alvo da tarefa. productId={} productSlug={}",
          product.getId(),
          product.getSlug(),
          ex);
      return null;
    }
  }

  /** Verifica ausência sem transformar texto vazio em identidade válida. */
  private boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
