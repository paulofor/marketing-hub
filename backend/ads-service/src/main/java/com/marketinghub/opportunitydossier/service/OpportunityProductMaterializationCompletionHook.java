package com.marketinghub.opportunitydossier.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskCompletionHook;
import com.marketinghub.agenttask.CompleteAgentTaskRequest;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.opportunitydossier.OpportunityDossier;
import com.marketinghub.opportunitydossier.OpportunityDossierStatus;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CreateCommercialPlanRequest;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.product.Product;
import com.marketinghub.product.dto.CreateProductRequest;
import com.marketinghub.product.service.ProductService;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunityMaturity;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.producttype.ProductTypeStatus;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.repository.jpa.opportunitydossier.OpportunityDossierRepository;
import com.marketinghub.repository.jpa.producttype.ProductTypeDefinitionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Responsabilidade: criar plano e produto planejado após os três gates comerciais aprovados. */
@Service
public class OpportunityProductMaterializationCompletionHook implements AgentTaskCompletionHook {
  private static final int PRODUCT_CLASSIFICATION_MAX_LENGTH = 255;
  private static final Logger log =
      LoggerFactory.getLogger(OpportunityProductMaterializationCompletionHook.class);
  private static final String PROCESS_CODE = "pde-commercial-plan-offer";
  private static final String SOURCE_PREFIX = "product-discovery-cycle:";
  private static final List<String> PRIVATE_VALIDATION_SIGNALS =
      List.of(
          "EXPERIENCE_STARTED",
          "VALUE_MOMENT",
          "READY_RESULT_USED",
          "PREFERRED_OVER_FREE",
          "CHECKOUT_STARTED");
  private final OpportunityDossierRepository dossierRepository;
  private final AgentTaskRepository taskRepository;
  private final ProductTypeDefinitionRepository productTypeRepository;
  private final MarketNicheRepository marketNicheRepository;
  private final CommercialPlanService commercialPlanService;
  private final ProductService productService;
  private final ObjectMapper objectMapper;

  /** Configura as fontes necessárias para materialização atômica e idempotente. */
  public OpportunityProductMaterializationCompletionHook(
      OpportunityDossierRepository dossierRepository,
      AgentTaskRepository taskRepository,
      ProductTypeDefinitionRepository productTypeRepository,
      MarketNicheRepository marketNicheRepository,
      CommercialPlanService commercialPlanService,
      ProductService productService,
      ObjectMapper objectMapper) {
    this.dossierRepository = dossierRepository;
    this.taskRepository = taskRepository;
    this.productTypeRepository = productTypeRepository;
    this.marketNicheRepository = marketNicheRepository;
    this.commercialPlanService = commercialPlanService;
    this.productService = productService;
    this.objectMapper = objectMapper;
  }

  /** Restringe a materialização ao parecer final de arquitetura do dossiê autônomo. */
  @Override
  public boolean supports(AgentTask task) {
    return task.getProcessDefinition() != null
        && PROCESS_CODE.equals(task.getProcessDefinition().getProcessCode())
        && "productArchitecture".equals(task.getProcessActivityId())
        && "landing-generator".equals(task.getAssignedAgent().getAgentKey())
        && task.getSourceReference() != null
        && task.getSourceReference().startsWith(SOURCE_PREFIX);
  }

  /** Consolida contratos aprovados e cria uma única versão planejada, sem publicar ou gastar. */
  @Override
  public CompletionDisposition apply(AgentTask task, CompleteAgentTaskRequest request) {
    try {
      List<AgentTask> tasks =
          taskRepository.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
              task.getProcessDefinition().getId(), task.getSourceReference());
      JsonNode strategyResult = completedResult(tasks, "marketStrategy");
      OpportunityDossier dossier =
          requiredSelectedDossier(task.getSourceReference(), strategyResult);
      if (dossier.getCreatedProduct() != null) return CompletionDisposition.COMPLETE;
      ProductIdentity productIdentity = resolveProductIdentity(task, dossier, strategyResult);
      JsonNode strategy = strategyResult.path("marketStrategicContract");
      requireValidationReadiness(strategy);
      JsonNode economicsResult = completedResult(tasks, "economics");
      JsonNode economics = economicsResult.path("economics");
      JsonNode metrics = economicsResult.path("metrics");
      JsonNode architectureResult = objectMapper.readTree(request.resultJson());
      requireApprove(architectureResult, "Dédalo");
      JsonNode architecture = architectureResult.path("productArchitecture");
      requirePrototypeReadiness(architecture);

      CommercialPlan plan = createPlan(dossier, strategy, economics, metrics);
      Product product =
          createProduct(
              dossier,
              plan,
              productIdentity,
              strategy,
              economics,
              metrics,
              architecture,
              architectureResult);
      dossier.setConvertedPlan(plan);
      dossier.setCreatedProduct(product);
      dossier.setProposedOffer(text(strategy, "offerThesis"));
      dossier.setPreliminaryPrice(decimal(economics, "offerPriceBrl"));
      dossier.setDeliveryModel(text(architecture, "format"));
      dossier.setStatus(OpportunityDossierStatus.CONVERTED_TO_PLAN);
      dossierRepository.save(dossier);
      return CompletionDisposition.COMPLETE;
    } catch (Exception ex) {
      log.error(
          "Falha ao materializar produto da descoberta. taskId={} sourceReference={}",
          task.getId(),
          task.getSourceReference(),
          ex);
      throw new IllegalArgumentException(
          "Os contratos aprovados não puderam materializar o produto planejado.", ex);
    }
  }

  /** Cria o plano comercial com premissas explícitas, ainda sem autorização de execução. */
  private CommercialPlan createPlan(
      OpportunityDossier dossier, JsonNode strategy, JsonNode economics, JsonNode metrics) {
    return commercialPlanService.create(
        new CreateCommercialPlanRequest(
            limit(dossier.getTitle(), 191),
            null,
            null,
            null,
            text(strategy, "desiredOutcome"),
            limit(firstText(text(strategy, "buyer"), dossier.getTargetAudience()), 512),
            limit(firstText(text(strategy, "problem"), dossier.getMainPain()), 512),
            limit(text(strategy, "offerThesis"), 512),
            null,
            "Instagram",
            limit(text(metrics, "primary"), 191),
            text(metrics, "continueCriteria"),
            text(metrics, "stopCriteria"),
            requiredDate(text(economics, "deadline")),
            decimal(economics, "maxBudgetBrl"),
            decimal(economics, "targetRevenueBrl"),
            decimal(economics, "targetRevenueBrl"),
            decimal(economics, "offerPriceBrl"),
            decimal(economics, "variableCostPerSaleBrl"),
            integer(economics, "expectedTraffic"),
            decimal(economics, "expectedConversionPercent"),
            decimal(economics, "maxCacBrl"),
            decimal(economics, "expectedRefundPercent"),
            decimal(economics, "fixedInitialCostBrl"),
            1,
            0,
            1,
            1,
            1,
            0,
            "Construir e homologar a experiência PDE antes de qualquer publicação.",
            "Produto planejado; construção funcional, comunicação e homologação ainda pendentes.",
            limit(dossier.getKnownRisks(), 512)));
  }

  /** Cria o produto planejado com a identidade escolhida e a linhagem comercial completa. */
  private Product createProduct(
      OpportunityDossier dossier,
      CommercialPlan plan,
      ProductIdentity productIdentity,
      JsonNode strategy,
      JsonNode economics,
      JsonNode metrics,
      JsonNode architecture,
      JsonNode architectureResult)
      throws JsonProcessingException {
    MarketNiche marketNiche = resolveMarketNiche(dossier, strategy);
    CreateProductRequest product = new CreateProductRequest();
    String plannedName = dossier.getTitle() + " · PDE planejado #" + dossier.getId();
    product.setSlug("pde-planejado-" + dossier.getId());
    product.setName(limit(plannedName, 191));
    product.setInternalName(productIdentity.internalName());
    product.setProductTypeId(productIdentity.type().getId());
    product.setMarketNicheId(marketNiche.getId());
    product.setProductFormat(limit(text(architecture, "format"), 64));
    product.setDeliveryMode("EXPERIÊNCIA_PERSONALIZADA_POR_IA");
    product.setRevenueModel("HIPÓTESE_A_VALIDAR");
    product.setValueUnit(limit(firstArrayText(architecture.path("deliverables")), 191));
    product.setValueEvidenceMetric(
        limit(firstText(firstArrayText(metrics.path("delivery")), text(metrics, "primary")), 191));
    product.setValidationDefinitionVersion("PDE_AGENT_VALIDATION_V1");
    product.setValidationDefinitionJson(
        objectMapper.writeValueAsString(
            validationDefinition(productIdentity, strategy, economics, metrics, architecture)));
    product.setCommercialStatus("PLANNED");
    product.setCurrentPriceBrl(decimal(economics, "offerPriceBrl"));
    product.setPrimaryHypothesis(text(strategy, "causalHypothesis"));
    product.setCommercialNotes(
        "Criado automaticamente como planejamento do dossiê #"
            + dossier.getId()
            + " e plano #"
            + plan.getId()
            + ". Identidade interna "
            + productIdentity.internalName()
            + " e tipo "
            + productIdentity.type().getInternalName()
            + " ("
            + productIdentity.type().getCode()
            + ") escolhidos por Atena"
            + ". Próximo gate: construir o protótipo e homologá-lo por agentes independentes."
            + " Não está publicado nem autorizado para contato, campanha, pagamento ou gasto;"
            + " somente o mercado poderá comprovar demanda.");
    product.setSevenDayJourney(objectMapper.writeValueAsString(architecture.path("valueJourney")));
    product.setTargetAudience(firstText(text(strategy, "buyer"), dossier.getTargetAudience()));
    product.setNiche(limit(text(strategy, "segment"), PRODUCT_CLASSIFICATION_MAX_LENGTH));
    product.setAvatar(limit(text(strategy, "buyer"), PRODUCT_CLASSIFICATION_MAX_LENGTH));
    product.setExplicitPain(firstText(text(strategy, "problem"), dossier.getMainPain()));
    product.setPromise(text(strategy, "desiredOutcome"));
    product.setUniqueMechanism(text(strategy, "valueMechanism"));
    product.setPdeExperienceJson(
        objectMapper.writeValueAsString(
            pdeExperience(
                dossier, plan, productIdentity, strategy, economics, metrics, architectureResult)));
    product.setCheckoutMonetization(objectMapper.writeValueAsString(economics));
    product.setFunnel("Instagram → experiência PDE → checkout governado");
    Product saved = productService.createProduct(product);
    productService.updateAutomaticExecution(saved.getId(), false, "pde-discovery-handoff");
    saved.setAutomaticExecutionEnabled(false);
    finalizeAgentValidationContracts(saved);
    return saved;
  }

  /** Grava a referência multiagente exata depois que o banco atribui o identificador do produto. */
  private void finalizeAgentValidationContracts(Product product) throws JsonProcessingException {
    String sourceReference = "product:" + product.getId() + "@agent-validation-v1";
    ObjectNode validation =
        (ObjectNode) objectMapper.readTree(product.getValidationDefinitionJson());
    ((ObjectNode) validation.path("agentValidationPlan")).put("sourceReference", sourceReference);
    ObjectNode experience = (ObjectNode) objectMapper.readTree(product.getPdeExperienceJson());
    experience.put("agentValidationSourceReference", sourceReference);
    ((ObjectNode) experience.path("agentValidationPlan")).put("sourceReference", sourceReference);
    ((ObjectNode) experience.path("marketStrategy").path("agentValidationPlan"))
        .put("sourceReference", sourceReference);
    product.setValidationDefinitionVersion("PDE_AGENT_VALIDATION_V1");
    product.setValidationDefinitionJson(objectMapper.writeValueAsString(validation));
    product.setPdeExperienceJson(objectMapper.writeValueAsString(experience));
    productService.updateValidationContracts(
        product.getId(),
        product.getValidationDefinitionVersion(),
        product.getValidationDefinitionJson(),
        product.getPdeExperienceJson());
  }

  /** Reutiliza ou cria o nicho aprovado por Atena para manter atribuição comercial do produto. */
  private MarketNiche resolveMarketNiche(OpportunityDossier dossier, JsonNode strategy) {
    String fallback =
        dossier.getProductDiscoveryCycle() == null
            ? dossier.getTitle()
            : dossier.getProductDiscoveryCycle().getTheme();
    String name = limit(firstText(text(strategy, "segment"), fallback), 191);
    return marketNicheRepository
        .findFirstByNameIgnoreCaseOrderByIdAsc(name)
        .orElseGet(
            () ->
                marketNicheRepository.save(
                    MarketNiche.builder()
                        .name(name)
                        .description(
                            "Nicho materializado a partir do dossiê factual #"
                                + dossier.getId()
                                + " e da estratégia aprovada por Atena.")
                        .totalCost(BigDecimal.ZERO)
                        .totalRevenue(BigDecimal.ZERO)
                        .build()));
  }

  /** Monta a definição de construção e homologação multiagente do produto planejado. */
  private ObjectNode validationDefinition(
      ProductIdentity productIdentity,
      JsonNode strategy,
      JsonNode economics,
      JsonNode metrics,
      JsonNode architecture) {
    Instant frozenAt = Instant.now();
    ObjectNode definition = objectMapper.createObjectNode();
    definition.set("problem", valueNode(strategy, "problem"));
    definition.set("promise", valueNode(strategy, "desiredOutcome"));
    definition.set("mechanism", valueNode(strategy, "valueMechanism"));
    definition.set("format", valueNode(architecture, "format"));
    definition.set("delivery", architecture.deepCopy());
    definition.set("agentValidationPlan", agentValidationPlan(strategy));
    ((ObjectNode) definition.path("agentValidationPlan"))
        .put("criteriaDeclaredAt", frozenAt.toString())
        .put("sourceQualityEvaluatedAt", frozenAt.toString());
    definition.set("privatePrototype", architecture.path("privatePrototype").deepCopy());
    definition.put("purchaseMomentStatus", "WAITING_AGENT_HOMOLOGATION");
    definition.put("finalCommercialPrioritizationEligible", false);
    definition.put("communicationPreparationEligible", false);
    definition.set("economics", economics.deepCopy());
    definition.set("successEvidence", metrics.path("delivery").deepCopy());
    definition.set("decisionRules", metrics.deepCopy());
    definition.set("productIdentity", productIdentity.contract().deepCopy());
    return definition;
  }

  /** Monta o harness planejado separado dos metadados técnicos da execução. */
  private ObjectNode pdeExperience(
      OpportunityDossier dossier,
      CommercialPlan plan,
      ProductIdentity productIdentity,
      JsonNode strategy,
      JsonNode economics,
      JsonNode metrics,
      JsonNode architectureResult) {
    ObjectNode experience = objectMapper.createObjectNode();
    experience.put("contractVersion", "PDE_HARNESS_PLAN_V1");
    experience.put("experienceVersion", "agent-validation-v1");
    experience.put("status", "AGENT_VALIDATION_PLANNED");
    experience.put("validationMode", "MULTI_AGENT_V1");
    ObjectNode lineage = experience.putObject("lineage");
    lineage.put("cycleId", dossier.getProductDiscoveryCycle().getId());
    lineage.put("opportunityId", dossier.getProductDiscoveryOpportunity().getId());
    lineage.put("dossierId", dossier.getId());
    lineage.put("commercialPlanId", plan.getId());
    experience.set("marketStrategy", marketStrategyForProduct(strategy));
    experience.set("economics", economics.deepCopy());
    experience.set("metrics", metrics.deepCopy());
    experience.set("harness", architectureResult.path("productArchitecture").deepCopy());
    experience.set("agentValidationPlan", agentValidationPlan(strategy));
    experience.set("productIdentity", productIdentity.contract().deepCopy());
    experience.put(
        "publicationBoundary",
        "Planejamento e homologação multiagente sem autorização de contato, publicação, campanha, pagamento, orçamento ou gasto; agentes não constituem prova de mercado.");
    return experience;
  }

  /** Converte estratégias vigentes ou históricas no único plano multiagente executável. */
  private ObjectNode agentValidationPlan(JsonNode strategy) {
    JsonNode current = strategy.path("agentValidationPlan");
    JsonNode legacy = strategy.path("privateValidationPlan");
    ObjectNode plan =
        current instanceof ObjectNode currentObject
            ? currentObject.deepCopy()
            : objectMapper.createObjectNode();
    JsonNode source = current.isObject() ? current : legacy;
    plan.put("contractVersion", "PDE_AGENT_VALIDATION_V1");
    copyIfMissing(plan, "hypothesis", source.path("hypothesis"));
    copyIfMissing(plan, "prototypeObjective", source.path("prototypeObjective"));
    copyIfMissing(plan, "purchaseScene", source.path("purchaseScene"));
    copyIfMissing(plan, "strongestFreeAlternative", source.path("strongestFreeAlternative"));
    copyIfMissing(plan, "prototypeAdvantage", source.path("prototypeAdvantage"));
    JsonNode valueDelivery =
        source.path("customerValueDelivery").isObject()
            ? source.path("customerValueDelivery")
            : source.path("humanValueDelivery");
    copyIfMissing(plan, "customerValueDelivery", valueDelivery);
    copyIfMissing(plan, "sourceMaxAgeDays", source.path("sourceMaxAgeDays"));
    copyIfMissing(plan, "continueCriteria", source.path("continueCriteria"));
    copyIfMissing(plan, "adjustCriteria", source.path("adjustCriteria"));
    copyIfMissing(plan, "stopCriteria", source.path("stopCriteria"));
    copyIfMissing(plan, "sourceRefreshRequired", source.path("sourceRefreshRequired"));
    copyIfMissing(plan, "sourceRefreshAction", source.path("sourceRefreshAction"));
    copyIfMissing(plan, "publicationBoundary", source.path("publicationBoundary"));
    plan.put("trafficClass", "AGENT_VALIDATION");
    plan.put("internalMarker", "mh_internal_test");
    plan.putArray("requiredScenarios").add("ADHERENT").add("RECOVERY").add("SAFETY");
    plan.putArray("requiredDevices").add("DESKTOP_1440").add("IPHONE_15_PRO").add("PIXEL_7");
    plan.put("maxReadyResultSeconds", 600);
    plan.put("humanEvidenceClaimed", false);
    plan.put("commercialEvidenceClaimed", false);
    plan.put("paymentEnabled", false);
    plan.put("publicationAuthorized", false);
    plan.put("campaignAuthorized", false);
    plan.put("mediaSpendAuthorizedBrl", 0);
    return plan;
  }

  /** Copia evidência estratégica apenas quando o contrato vigente ainda não declarou o campo. */
  private void copyIfMissing(ObjectNode target, String field, JsonNode value) {
    if (!target.has(field) && value != null && !value.isMissingNode() && !value.isNull()) {
      target.set(field, value.deepCopy());
    }
  }

  /** Remove do produto o gate humano legado sem apagar o resultado bruto auditável da tarefa. */
  private ObjectNode marketStrategyForProduct(JsonNode strategy) {
    ObjectNode snapshot = ((ObjectNode) strategy).deepCopy();
    snapshot.put("contractVersion", "MARKET_STRATEGY_V4");
    snapshot.put("status", "READY_FOR_AGENT_VALIDATION");
    snapshot.remove("privateValidationPlan");
    snapshot.set("agentValidationPlan", agentValidationPlan(strategy));
    return snapshot;
  }

  /** Resolve a identidade nova de Atena e preserva somente o fallback das versões históricas. */
  private ProductIdentity resolveProductIdentity(
      AgentTask task, OpportunityDossier dossier, JsonNode strategyResult) {
    Integer processVersion = task.getProcessDefinition().getVersionNumber();
    if (processVersion == null || processVersion < 9) {
      ProductTypeDefinition legacyType =
          productTypeRepository
              .findByCode("PDE")
              .orElseThrow(
                  () -> new IllegalStateException("Tipo canônico PDE não foi encontrado."));
      String plannedName = limit(dossier.getTitle() + " · PDE planejado #" + dossier.getId(), 191);
      ObjectNode legacyContract = objectMapper.createObjectNode();
      legacyContract.put("contractVersion", "LEGACY_PRODUCT_IDENTITY_FALLBACK_V1");
      legacyContract.put("mode", "LEGACY");
      legacyContract.put("internalName", plannedName);
      legacyContract.put("productTypeCode", legacyType.getCode());
      legacyContract.put("productTypeInternalName", legacyType.getInternalName());
      legacyContract.put(
          "classificationRationale",
          "Compatibilidade com execução anterior ao contrato de identidade do Processo 2.");
      return new ProductIdentity(plannedName, legacyType, legacyContract);
    }

    JsonNode contract = strategyResult.path("productIdentity");
    String internalName = requiredText(contract, "internalName");
    String typeCode = requiredText(contract, "productTypeCode");
    String typeInternalName = requiredText(contract, "productTypeInternalName");
    if (!"PRODUCT_IDENTITY_V1".equals(contract.path("contractVersion").asText())
        || !"CREATE".equals(contract.path("mode").asText())
        || requiredText(contract, "classificationRationale").isBlank()
        || internalName.length() > 191
        || isProvisionalInternalName(internalName)) {
      throw new IllegalArgumentException(
          "Atena não definiu nome interno estável e classificação PRODUCT_IDENTITY_V1.");
    }
    ProductTypeDefinition type =
        productTypeRepository
            .findByCode(typeCode)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "O tipo escolhido por Atena não existe no catálogo ativo."));
    if (type.getStatus() != ProductTypeStatus.ACTIVE
        || type.getInternalName() == null
        || !type.getInternalName().equals(typeInternalName)) {
      throw new IllegalArgumentException(
          "O tipo escolhido por Atena não corresponde a uma classificação ativa do catálogo.");
    }
    return new ProductIdentity(internalName, type, contract.deepCopy());
  }

  /** Rejeita rótulos de descoberta usados anteriormente como se fossem identidade estável. */
  private boolean isProvisionalInternalName(String value) {
    String normalized = value.toLowerCase(Locale.ROOT);
    return normalized.contains("planejado")
        || normalized.contains("rascunho")
        || normalized.startsWith("pde ")
        || normalized.startsWith("produto #");
  }

  /** Exige texto real no contrato de identidade sem aceitar nulo ou espaços. */
  private String requiredText(JsonNode node, String field) {
    String value = node.path(field).asText("").trim();
    if (value.isBlank()) {
      throw new IllegalArgumentException("Atena não informou " + field + " na identidade.");
    }
    return value;
  }

  /** Exige prontidão multiagente ou converte somente o contrato histórico completo ainda em voo. */
  private void requireValidationReadiness(JsonNode strategy) {
    if ("MARKET_STRATEGY_V4".equals(strategy.path("contractVersion").asText())) {
      JsonNode plan = strategy.path("agentValidationPlan");
      if (!"READY_FOR_AGENT_VALIDATION".equals(strategy.path("status").asText())
          || !validAgentValidationPlan(plan)) {
        throw new IllegalStateException(
            "Atena não liberou um plano válido de homologação multiagente.");
      }
      return;
    }
    JsonNode validationPlan = strategy.path("privateValidationPlan");
    if (!"MARKET_STRATEGY_V3".equals(strategy.path("contractVersion").asText())
        || !"READY_FOR_PRIVATE_VALIDATION".equals(strategy.path("status").asText())
        || !validationPlan.isObject()
        || validationPlan.path("minimumIndependentReadings").asInt(0) != 2
        || validationPlan.path("minimumEligibleParticipantsPerReading").asInt(0) != 1
        || !hasExactSignals(validationPlan.path("requiredSignals"))
        || !unitRate(validationPlan, "minimumExperienceStartRate")
        || !unitRate(validationPlan, "minimumValueMomentRate")
        || !unitRate(validationPlan, "minimumReadyResultUseRate")
        || !unitRate(validationPlan, "minimumPrototypePreferenceRate")
        || !unitRate(validationPlan, "minimumCheckoutStartRate")
        || validationPlan.path("sourceMaxAgeDays").asInt(0) < 1
        || validationPlan.path("sourceMaxAgeDays").asInt(0) > 90
        || validationPlan.path("prototypeObjective").asText().isBlank()
        || !completePurchaseScene(validationPlan.path("purchaseScene"))
        || !canonicalCustomerValueDelivery(validationPlan.path("humanValueDelivery"))
        || validationPlan.path("strongestFreeAlternative").asText().isBlank()
        || validationPlan.path("prototypeAdvantage").asText().isBlank()
        || validationPlan.path("publicationBoundary").asText().isBlank()
        || (validationPlan.path("sourceRefreshRequired").asBoolean(false)
            && validationPlan.path("sourceRefreshAction").asText().isBlank())) {
      throw new IllegalStateException(
          "Atena não liberou um plano histórico completo que possa ser migrado para agentes.");
    }
  }

  /** Valida o contrato multiagente sem confundir cenário sintético com evidência de mercado. */
  private boolean validAgentValidationPlan(JsonNode plan) {
    return plan.isObject()
        && "PDE_AGENT_VALIDATION_V1".equals(plan.path("contractVersion").asText())
        && completePurchaseScene(plan.path("purchaseScene"))
        && canonicalCustomerValueDelivery(plan.path("customerValueDelivery"))
        && hasText(plan, "prototypeObjective")
        && hasText(plan, "strongestFreeAlternative")
        && hasText(plan, "prototypeAdvantage")
        && exactValues(plan.path("requiredScenarios"), List.of("ADHERENT", "RECOVERY", "SAFETY"))
        && exactValues(
            plan.path("requiredDevices"), List.of("DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7"))
        && plan.path("maxReadyResultSeconds").asInt(0) == 600
        && "AGENT_VALIDATION".equals(plan.path("trafficClass").asText())
        && "mh_internal_test".equals(plan.path("internalMarker").asText())
        && !plan.path("humanEvidenceClaimed").asBoolean(true)
        && !plan.path("commercialEvidenceClaimed").asBoolean(true)
        && !plan.path("paymentEnabled").asBoolean(true)
        && !plan.path("publicationAuthorized").asBoolean(true)
        && !plan.path("campaignAuthorized").asBoolean(true)
        && plan.path("mediaSpendAuthorizedBrl").asInt(-1) == 0
        && plan.path("sourceMaxAgeDays").asInt(0) >= 1
        && plan.path("sourceMaxAgeDays").asInt(0) <= 90
        && hasText(plan, "publicationBoundary")
        && (!plan.path("sourceRefreshRequired").asBoolean(false)
            || hasText(plan, "sourceRefreshAction"));
  }

  /** Confirma que Dédalo entregou um protótipo limitado, observável e sem cobrança. */
  private void requirePrototypeReadiness(JsonNode architecture) {
    JsonNode prototype = architecture.path("privatePrototype");
    int maxValueTimeMinutes = prototype.path("maxValueTimeMinutes").asInt(0);
    if (!prototype.isObject()
        || prototype.path("scope").asText().isBlank()
        || prototype.path("simpleInput").asText().isBlank()
        || prototype.path("readyResult").asText().isBlank()
        || maxValueTimeMinutes < 1
        || maxValueTimeMinutes > 10
        || !hasExactSignals(prototype.path("instrumentationEvents"))
        || !"SIMULATED_NO_CHARGE".equals(prototype.path("checkoutMode").asText())
        || !prototype.path("excludedFromPrototype").isArray()) {
      throw new IllegalStateException(
          "Dédalo não entregou um protótipo privado limitado e instrumentado.");
    }
  }

  /** Exige os cinco sinais canônicos exatamente uma vez. */
  private boolean hasExactSignals(JsonNode signals) {
    if (!signals.isArray() || signals.size() != PRIVATE_VALIDATION_SIGNALS.size()) return false;
    List<String> values = new java.util.ArrayList<>();
    signals.forEach(signal -> values.add(signal.asText()));
    return values.stream().distinct().count() == PRIVATE_VALIDATION_SIGNALS.size()
        && values.containsAll(PRIVATE_VALIDATION_SIGNALS);
  }

  /** Compara cenários e dispositivos como conjuntos exatos, sem duplicidade ou valor extra. */
  private boolean exactValues(JsonNode values, List<String> expected) {
    if (!values.isArray() || values.size() != expected.size()) return false;
    List<String> actual = new java.util.ArrayList<>();
    values.forEach(value -> actual.add(value.asText()));
    return actual.stream().distinct().count() == expected.size() && actual.containsAll(expected);
  }

  /** Exige taxa integral porque cada uma das duas leituras representa uma pessoa. */
  private boolean unitRate(JsonNode plan, String field) {
    return plan.path(field).isNumber() && Double.compare(plan.path(field).asDouble(), 1d) == 0;
  }

  /** Confirma os seis fatos necessários para interpretar o momento concreto de compra. */
  private boolean completePurchaseScene(JsonNode scene) {
    return hasText(scene, "trigger")
        && hasText(scene, "deadline")
        && hasText(scene, "costOfError")
        && hasText(scene, "budgetEvidence")
        && hasText(scene, "failedAttempt")
        && hasText(scene, "currentPaidBehavior");
  }

  /** Confirma que a candidata preserva valor ao cliente e entrega pronta sem transferir a IA. */
  private boolean canonicalCustomerValueDelivery(JsonNode delivery) {
    return delivery.isObject()
        && delivery.path("territories").isArray()
        && !delivery.path("territories").isEmpty()
        && delivery.path("evidenceSourceIds").isArray()
        && delivery.path("evidenceSourceIds").size() >= 2
        && delivery.path("evidencePathways").isArray()
        && delivery.path("evidencePathways").size() >= 2
        && hasText(delivery, "desiredTransformation")
        && hasText(delivery, "readyMadeOutcome")
        && hasText(delivery, "minimumCustomerInput")
        && hasText(delivery, "automationBoundary")
        && !delivery.path("requiresPromptEngineering").asBoolean(true)
        && !delivery.path("requiresManualAssembly").asBoolean(true)
        && delivery.path("usableWithoutAiKnowledge").asBoolean(false)
        && delivery.path("customerStepsToValue").asInt(0) >= 1
        && delivery.path("customerStepsToValue").asInt(0) <= 5
        && delivery.path("timeToUsableResultMinutes").asInt(0) >= 1
        && delivery.path("timeToUsableResultMinutes").asInt(0) <= 10;
  }

  /** Verifica texto obrigatório em um objeto de contrato. */
  private boolean hasText(JsonNode value, String field) {
    return value.isObject() && !value.path(field).asText("").trim().isBlank();
  }

  /** Localiza a candidata escolhida por Atena e comprova sua pertença ao ciclo em execução. */
  private OpportunityDossier requiredSelectedDossier(
      String sourceReference, JsonNode strategyResult) {
    if (!strategyResult.path("selectedDossierId").canConvertToLong()
        || !strategyResult.path("selectedOpportunityId").canConvertToLong()) {
      throw new IllegalArgumentException("Atena não selecionou um dossiê factual para avançar.");
    }
    Long cycleId = Long.valueOf(sourceReference.substring(SOURCE_PREFIX.length()));
    Long dossierId = strategyResult.path("selectedDossierId").longValue();
    Long opportunityId = strategyResult.path("selectedOpportunityId").longValue();
    OpportunityDossier dossier =
        dossierRepository
            .findById(dossierId)
            .orElseThrow(
                () -> new IllegalArgumentException("Dossiê do handoff não foi encontrado."));
    if (dossier.getProductDiscoveryCycle() == null
        || !cycleId.equals(dossier.getProductDiscoveryCycle().getId())
        || dossier.getProductDiscoveryOpportunity() == null
        || !opportunityId.equals(dossier.getProductDiscoveryOpportunity().getId())
        || dossier.getProductDiscoveryOpportunity().getMaturity()
            != ProductDiscoveryOpportunityMaturity.DOSSIER_READY) {
      throw new IllegalArgumentException(
          "A seleção de Atena não pertence ao ciclo ou não possui maturidade factual.");
    }
    return dossier;
  }

  /** Exige que a atividade predecessora esteja concluída e aprovada. */
  private JsonNode completedResult(List<AgentTask> tasks, String activityId)
      throws JsonProcessingException {
    AgentTask task =
        tasks.stream()
            .filter(item -> activityId.equals(item.getProcessActivityId()))
            .filter(item -> "COMPLETED".equals(item.getStatus()))
            .reduce((first, second) -> second)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Atividade predecessora ainda não foi concluída: " + activityId));
    JsonNode result = objectMapper.readTree(task.getResultJson());
    requireApprove(result, activityId);
    return result;
  }

  /** Bloqueia materialização quando um parecer não contém aprovação funcional explícita. */
  private void requireApprove(JsonNode result, String agent) {
    if (!"APPROVE".equals(result.path("decision").asText())) {
      throw new IllegalStateException(agent + " não aprovou o contrato para materialização.");
    }
  }

  /** Lê texto opcional sem fabricar conteúdo comercial. */
  private String text(JsonNode node, String field) {
    String value = node.path(field).asText("").trim();
    return value.isBlank() ? null : value;
  }

  /** Retorna o primeiro texto real disponível. */
  private String firstText(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  /** Lê o primeiro item textual de uma lista estruturada. */
  private String firstArrayText(JsonNode values) {
    if (!values.isArray() || values.isEmpty()) return null;
    String value = values.get(0).asText("").trim();
    return value.isBlank() ? null : value;
  }

  /** Converte valor decimal opcional preservando ausência. */
  private BigDecimal decimal(JsonNode node, String field) {
    return node.hasNonNull(field) && node.get(field).isNumber()
        ? node.get(field).decimalValue()
        : null;
  }

  /** Converte valor inteiro opcional preservando ausência. */
  private Integer integer(JsonNode node, String field) {
    return node.hasNonNull(field) && node.get(field).canConvertToInt()
        ? node.get(field).intValue()
        : null;
  }

  /** Exige data ISO para que a hipótese econômica não receba prazo fabricado. */
  private LocalDate requiredDate(String value) {
    if (value == null) throw new IllegalArgumentException("Plutus não informou prazo econômico.");
    return LocalDate.parse(value);
  }

  /** Preserva o tipo original do valor ao compor o contrato de validação. */
  private JsonNode valueNode(JsonNode node, String field) {
    return node.has(field) ? node.get(field).deepCopy() : objectMapper.nullNode();
  }

  /**
   * Limita textos produzidos por modelo à capacidade explícita do cadastro sem perder o JSON bruto.
   */
  private String limit(String value, int maxLength) {
    return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
  }

  /** Agrupa o nome interno, o tipo catalogado e o snapshot exato escolhido por Atena. */
  private record ProductIdentity(
      String internalName, ProductTypeDefinition type, JsonNode contract) {}
}
