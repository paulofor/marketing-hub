package com.marketinghub.opportunitydossier.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskCompletionHook;
import com.marketinghub.agenttask.CompleteAgentTaskRequest;
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
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.opportunitydossier.OpportunityDossierRepository;
import com.marketinghub.repository.jpa.producttype.ProductTypeDefinitionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Responsabilidade: criar plano e produto planejado após os três gates comerciais aprovados. */
@Service
public class OpportunityProductMaterializationCompletionHook implements AgentTaskCompletionHook {
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
  private final CommercialPlanService commercialPlanService;
  private final ProductService productService;
  private final ObjectMapper objectMapper;

  /** Configura as fontes necessárias para materialização atômica e idempotente. */
  public OpportunityProductMaterializationCompletionHook(
      OpportunityDossierRepository dossierRepository,
      AgentTaskRepository taskRepository,
      ProductTypeDefinitionRepository productTypeRepository,
      CommercialPlanService commercialPlanService,
      ProductService productService,
      ObjectMapper objectMapper) {
    this.dossierRepository = dossierRepository;
    this.taskRepository = taskRepository;
    this.productTypeRepository = productTypeRepository;
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
      JsonNode strategy = strategyResult.path("marketStrategicContract");
      requirePrivateValidationReadiness(strategy);
      JsonNode economicsResult = completedResult(tasks, "economics");
      JsonNode economics = economicsResult.path("economics");
      JsonNode metrics = economicsResult.path("metrics");
      JsonNode architectureResult = objectMapper.readTree(request.resultJson());
      requireApprove(architectureResult, "Dédalo");
      JsonNode architecture = architectureResult.path("productArchitecture");
      requirePrivatePrototypeReadiness(architecture);

      CommercialPlan plan = createPlan(dossier, strategy, economics, metrics);
      Product product =
          createProduct(
              dossier, plan, strategy, economics, metrics, architecture, architectureResult);
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
            dossier.getTitle(),
            null,
            null,
            null,
            text(strategy, "desiredOutcome"),
            firstText(text(strategy, "buyer"), dossier.getTargetAudience()),
            firstText(text(strategy, "problem"), dossier.getMainPain()),
            text(strategy, "offerThesis"),
            null,
            "Instagram",
            text(metrics, "primary"),
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
            dossier.getKnownRisks()));
  }

  /** Cria o cadastro PDE planejado com a linhagem factual, estratégica e econômica completa. */
  private Product createProduct(
      OpportunityDossier dossier,
      CommercialPlan plan,
      JsonNode strategy,
      JsonNode economics,
      JsonNode metrics,
      JsonNode architecture,
      JsonNode architectureResult)
      throws JsonProcessingException {
    ProductTypeDefinition pdeType =
        productTypeRepository
            .findByCode("PDE")
            .orElseThrow(() -> new IllegalStateException("Tipo canônico PDE não foi encontrado."));
    CreateProductRequest product = new CreateProductRequest();
    String plannedName = dossier.getTitle() + " · PDE planejado #" + dossier.getId();
    product.setSlug("pde-planejado-" + dossier.getId());
    product.setName(limit(plannedName, 191));
    product.setInternalName(limit(plannedName, 191));
    product.setProductTypeId(pdeType.getId());
    product.setProductFormat(limit(text(architecture, "format"), 64));
    product.setDeliveryMode("EXPERIÊNCIA_PERSONALIZADA_POR_IA");
    product.setRevenueModel("HIPÓTESE_A_VALIDAR");
    product.setValueUnit(limit(firstArrayText(architecture.path("deliverables")), 191));
    product.setValueEvidenceMetric(
        limit(firstText(firstArrayText(metrics.path("delivery")), text(metrics, "primary")), 191));
    product.setValidationDefinitionVersion("PDE_PRIVATE_VALIDATION_V1");
    product.setValidationDefinitionJson(
        objectMapper.writeValueAsString(
            validationDefinition(strategy, economics, metrics, architecture)));
    product.setCommercialStatus("PLANNED");
    product.setCurrentPriceBrl(decimal(economics, "offerPriceBrl"));
    product.setPrimaryHypothesis(text(strategy, "causalHypothesis"));
    product.setCommercialNotes(
        "Criado automaticamente como planejamento do dossiê #"
            + dossier.getId()
            + " e plano #"
            + plan.getId()
            + ". Próximo gate: construir o protótipo privado e obter duas leituras independentes."
            + " Não está publicado nem autorizado para contato, campanha, pagamento ou gasto.");
    product.setSevenDayJourney(objectMapper.writeValueAsString(architecture.path("valueJourney")));
    product.setTargetAudience(firstText(text(strategy, "buyer"), dossier.getTargetAudience()));
    product.setNiche(text(strategy, "segment"));
    product.setAvatar(text(strategy, "buyer"));
    product.setExplicitPain(firstText(text(strategy, "problem"), dossier.getMainPain()));
    product.setPromise(text(strategy, "desiredOutcome"));
    product.setUniqueMechanism(text(strategy, "valueMechanism"));
    product.setPdeExperienceJson(
        objectMapper.writeValueAsString(
            pdeExperience(dossier, plan, strategy, economics, metrics, architectureResult)));
    product.setCheckoutMonetization(objectMapper.writeValueAsString(economics));
    product.setFunnel("Instagram → experiência PDE → checkout governado");
    Product saved = productService.createProduct(product);
    productService.updateAutomaticExecution(saved.getId(), false, "pde-discovery-handoff");
    saved.setAutomaticExecutionEnabled(false);
    return saved;
  }

  /** Monta a definição de construção e validação privada que governa o produto planejado. */
  private ObjectNode validationDefinition(
      JsonNode strategy, JsonNode economics, JsonNode metrics, JsonNode architecture) {
    Instant frozenAt = Instant.now();
    ObjectNode definition = objectMapper.createObjectNode();
    definition.set("problem", valueNode(strategy, "problem"));
    definition.set("promise", valueNode(strategy, "desiredOutcome"));
    definition.set("mechanism", valueNode(strategy, "valueMechanism"));
    definition.set("format", valueNode(architecture, "format"));
    definition.set("delivery", architecture.deepCopy());
    definition.set("privateValidationPlan", strategy.path("privateValidationPlan").deepCopy());
    ((ObjectNode) definition.path("privateValidationPlan"))
        .put("criteriaDeclaredAt", frozenAt.toString())
        .put("sourceQualityEvaluatedAt", frozenAt.toString());
    definition.set("privatePrototype", architecture.path("privatePrototype").deepCopy());
    definition.put("purchaseMomentStatus", "WAITING_PRIVATE_PROTOTYPE");
    definition.put("finalCommercialPrioritizationEligible", false);
    definition.set("economics", economics.deepCopy());
    definition.set("successEvidence", metrics.path("delivery").deepCopy());
    definition.set("decisionRules", metrics.deepCopy());
    return definition;
  }

  /** Monta o harness planejado separado dos metadados técnicos da execução. */
  private ObjectNode pdeExperience(
      OpportunityDossier dossier,
      CommercialPlan plan,
      JsonNode strategy,
      JsonNode economics,
      JsonNode metrics,
      JsonNode architectureResult) {
    ObjectNode experience = objectMapper.createObjectNode();
    experience.put("contractVersion", "PDE_HARNESS_PLAN_V1");
    experience.put("experienceVersion", "private-validation-v1");
    experience.put("status", "PLANNED");
    ObjectNode lineage = experience.putObject("lineage");
    lineage.put("cycleId", dossier.getProductDiscoveryCycle().getId());
    lineage.put("opportunityId", dossier.getProductDiscoveryOpportunity().getId());
    lineage.put("dossierId", dossier.getId());
    lineage.put("commercialPlanId", plan.getId());
    experience.set("marketStrategy", strategy.deepCopy());
    experience.set("economics", economics.deepCopy());
    experience.set("metrics", metrics.deepCopy());
    experience.set("harness", architectureResult.path("productArchitecture").deepCopy());
    experience.set("privateValidationPlan", strategy.path("privateValidationPlan").deepCopy());
    experience.put(
        "publicationBoundary",
        "Planejamento e construção privada sem autorização de contato, publicação, campanha, pagamento, orçamento ou gasto.");
    return experience;
  }

  /** Exige que Atena tenha liberado somente o protótipo, nunca a operação comercial. */
  private void requirePrivateValidationReadiness(JsonNode strategy) {
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
        || !canonicalHumanValueDelivery(validationPlan.path("humanValueDelivery"))
        || validationPlan.path("strongestFreeAlternative").asText().isBlank()
        || validationPlan.path("prototypeAdvantage").asText().isBlank()
        || validationPlan.path("publicationBoundary").asText().isBlank()
        || (validationPlan.path("sourceRefreshRequired").asBoolean(false)
            && validationPlan.path("sourceRefreshAction").asText().isBlank())) {
      throw new IllegalStateException(
          "Atena não liberou um plano válido para protótipo e duas leituras privadas.");
    }
  }

  /** Confirma que Dédalo entregou um protótipo privado limitado, observável e sem cobrança. */
  private void requirePrivatePrototypeReadiness(JsonNode architecture) {
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

  /** Confirma que a candidata preserva valor humano e entrega pronta sem transferir a IA. */
  private boolean canonicalHumanValueDelivery(JsonNode delivery) {
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
}
