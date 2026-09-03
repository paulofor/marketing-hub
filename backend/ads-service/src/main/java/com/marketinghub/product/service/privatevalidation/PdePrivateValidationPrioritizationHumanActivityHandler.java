package com.marketinghub.product.service.privatevalidation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityHandler;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityRequirement;
import com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution.ProductProcessActivityExecutionRequest;
import com.marketinghub.opportunitydossier.OpportunityDossier;
import com.marketinghub.product.Product;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunity;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.opportunitydossier.OpportunityDossierRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryOpportunityRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilidade: concluir o gate privado e encaminhar somente um PDE comprovado à comunicação.
 */
@Service
@Slf4j
public class PdePrivateValidationPrioritizationHumanActivityHandler
    implements HumanProductProcessActivityHandler {
  static final String PROCESS_CODE = "pde-construction-approval";
  static final String ACTIVITY_ID = "finalPrioritization";
  private static final String PSIQUE_ACTIVITY = "humanExperienceReview";
  private static final String TEMIS_ACTIVITY = "commercialIntegrityReview";
  private static final String PSIQUE_AGENT = "customer-agent";
  private static final String TEMIS_AGENT = "meta-ad-approver";
  private static final Set<String> APPROVED_DECISIONS = Set.of("APPROVE", "APPROVED");
  private final BusinessProcessActivityInstanceRepository instances;
  private final AgentTaskRepository tasks;
  private final ProductRepository products;
  private final OpportunityDossierRepository dossiers;
  private final ProductDiscoveryOpportunityRepository opportunities;
  private final ObjectMapper objectMapper;

  /** Configura as evidências privadas e as entidades cuja posição comercial será atualizada. */
  public PdePrivateValidationPrioritizationHumanActivityHandler(
      BusinessProcessActivityInstanceRepository instances,
      AgentTaskRepository tasks,
      ProductRepository products,
      OpportunityDossierRepository dossiers,
      ProductDiscoveryOpportunityRepository opportunities,
      ObjectMapper objectMapper) {
    this.instances = instances;
    this.tasks = tasks;
    this.products = products;
    this.dossiers = dossiers;
    this.opportunities = opportunities;
    this.objectMapper = objectMapper;
  }

  /** Reconhece somente a decisão humana posterior às leituras e revisões independentes. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition) {
    return process != null
        && activityDefinition != null
        && PROCESS_CODE.equals(process.getProcessCode())
        && ACTIVITY_ID.equals(activityDefinition.getActivityId());
  }

  /** Confirma protótipo, duas leituras aprovadas e pareceres explícitos antes da decisão final. */
  @Override
  @Transactional(readOnly = true)
  public HumanProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference) {
    boolean productReference = belongsToProduct(sourceReference, product);
    PrivateContract contract = contract(product);
    List<PrivateReading> readings = readings(process.getId(), sourceReference, contract);
    Optional<PrivateReading> first =
        reading(readings, PdePrivateReadingHumanActivityHandler.FIRST_READING);
    Optional<PrivateReading> second =
        reading(readings, PdePrivateReadingHumanActivityHandler.SECOND_READING);
    Optional<IndependentReview> psique =
        review(process.getId(), sourceReference, PSIQUE_ACTIVITY, PSIQUE_AGENT);
    Optional<IndependentReview> temis =
        review(process.getId(), sourceReference, TEMIS_ACTIVITY, TEMIS_AGENT);
    boolean prototypeReady = contract.valid();
    boolean firstReady = first.map(PrivateReading::valid).orElse(false);
    boolean secondReady = second.map(PrivateReading::valid).orElse(false);
    boolean distinct =
        firstReady
            && secondReady
            && !Objects.equals(
                first.orElseThrow().participantReference(),
                second.orElseThrow().participantReference());
    boolean psiqueApproved = psique.map(IndependentReview::approved).orElse(false);
    boolean temisApproved = temis.map(IndependentReview::approved).orElse(false);
    boolean reviewsAfterReadings =
        firstReady
            && secondReady
            && psiqueApproved
            && temisApproved
            && reviewsAfterReadings(
                first.orElseThrow(),
                second.orElseThrow(),
                psique.orElseThrow(),
                temis.orElseThrow());
    List<HumanProductProcessActivityRequirement> requirements =
        List.of(
            requirement(
                "PRIVATE_PRODUCT_REFERENCE",
                "Produto e versão privados segregados",
                productReference,
                "A decisão pertence ao produto e à versão privada em revisão.",
                "Abra a priorização final pela cadeia do mesmo produto e versão."),
            requirement(
                "PRIVATE_PROTOTYPE_ACCEPTED",
                "Protótipo privado aceito",
                prototypeReady,
                "A versão, a fonte e a instrumentação do protótipo estão congeladas.",
                "Confirme primeiro um protótipo privado utilizável e instrumentado."),
            readingRequirement("PRIVATE_READING_1", "Primeira leitura aprovada", firstReady),
            readingRequirement("PRIVATE_READING_2", "Segunda leitura aprovada", secondReady),
            requirement(
                "DISTINCT_PRIVATE_PARTICIPANTS",
                "Pessoas distintas",
                distinct,
                "Os códigos aleatórios das duas leituras são diferentes.",
                "Repita a segunda leitura com outra pessoa consentida."),
            reviewRequirement("PSIQUE_APPROVED", "Parecer de Psique aprovado", psiqueApproved),
            reviewRequirement("TEMIS_APPROVED", "Parecer de Têmis aprovado", temisApproved),
            requirement(
                "REVIEWS_AFTER_PRIVATE_READINGS",
                "Pareceres posteriores às leituras",
                reviewsAfterReadings,
                "Psique e Têmis revisaram a evidência depois das duas leituras.",
                "Execute novamente os pareceres depois de concluir as duas leituras privadas."));
    boolean ready =
        requirements.stream().allMatch(HumanProductProcessActivityRequirement::satisfied);
    String auditReference =
        ready
            ? "activity-instance:"
                + first.orElseThrow().instanceId()
                + ";activity-instance:"
                + second.orElseThrow().instanceId()
                + ";agent-task:"
                + psique.orElseThrow().taskId()
                + ";agent-task:"
                + temis.orElseThrow().taskId()
            : null;
    return new HumanProductProcessActivityReadiness(
        ready,
        ready
            ? "O protótipo, duas leituras integrais e os pareceres independentes comprovam o gate privado."
            : requirements.stream()
                .filter(requirement -> !requirement.satisfied())
                .findFirst()
                .map(HumanProductProcessActivityRequirement::recommendation)
                .orElse("A validação privada ainda está incompleta."),
        "Priorizar para comunicação",
        "Aprovar move somente o produto validado para a preparação de comunicação e o mantém em STOP. Não cria experimento, campanha, contato, cobrança ou gasto.",
        "Priorização comercial após validação privada",
        "Confirmo que o protótipo, duas leituras integrais, fontes e pareceres independentes sustentam avançar para comunicação.",
        "CONFIRM:" + PROCESS_CODE + ":" + ACTIVITY_ID,
        null,
        product == null ? null : product.getId(),
        requirements,
        HumanProductProcessActivityReadiness.REVIEW_AND_ACCEPT,
        auditReference);
  }

  /** Recalcula o gate, persiste a prova e retorna o produto a STOP antes da comunicação. */
  @Override
  @Transactional
  public void approve(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference,
      ProductProcessActivityExecutionRequest request) {
    if (!belongsToProduct(sourceReference, product)) {
      throw new IllegalStateException("A priorização não pertence ao produto e à versão privados.");
    }
    PrivateContract contract = contract(product);
    if (!contract.valid()) {
      throw new IllegalStateException("O protótipo privado aceito não corresponde ao contrato.");
    }
    List<PrivateReading> readings = readings(process.getId(), sourceReference, contract);
    PrivateReading first =
        reading(readings, PdePrivateReadingHumanActivityHandler.FIRST_READING)
            .filter(PrivateReading::valid)
            .orElseThrow(() -> new IllegalStateException("A primeira leitura privada é inválida."));
    PrivateReading second =
        reading(readings, PdePrivateReadingHumanActivityHandler.SECOND_READING)
            .filter(PrivateReading::valid)
            .orElseThrow(() -> new IllegalStateException("A segunda leitura privada é inválida."));
    if (Objects.equals(first.participantReference(), second.participantReference())) {
      throw new IllegalStateException(
          "As leituras privadas precisam representar pessoas distintas.");
    }
    IndependentReview psique =
        requiredApprovedReview(process.getId(), sourceReference, PSIQUE_ACTIVITY, PSIQUE_AGENT);
    IndependentReview temis =
        requiredApprovedReview(process.getId(), sourceReference, TEMIS_ACTIVITY, TEMIS_AGENT);
    if (!reviewsAfterReadings(first, second, psique, temis)) {
      throw new IllegalStateException(
          "Os pareceres independentes precisam ser posteriores às duas leituras privadas.");
    }
    Instant decidedAt = Instant.now();
    ObjectNode validation = contract.definition();
    validation.put("purchaseMomentStatus", "PASS");
    validation.put("finalCommercialPrioritizationEligible", true);
    validation.put("privateValidationCompletedAt", decidedAt.toString());
    validation.set("privateReadings", readingArray(first, second, psique, temis));
    ObjectNode experience =
        object(product.getPdeExperienceJson(), "experiência PDE", product.getId());
    experience.put("status", "PRIVATE_VALIDATED");
    ObjectNode privateValidation = experience.putObject("privateValidation");
    privateValidation.put("status", "PASS");
    privateValidation.put("completedAt", decidedAt.toString());
    privateValidation.set("readings", readingArray(first, second, psique, temis));
    ObjectNode reviews = privateValidation.putObject("independentReviews");
    writeReview(reviews.putObject("psique"), psique);
    writeReview(reviews.putObject("temis"), temis);
    privateValidation.put("decisionEvidenceReference", request.evidenceReference());
    product.setValidationDefinitionVersion("PDE_PRIVATE_VALIDATION_V1_COMPLETED");
    product.setValidationDefinitionJson(
        write(validation, "definição de validação", product.getId()));
    product.setPdeExperienceJson(write(experience, "experiência PDE", product.getId()));
    product.setCommercialStatus("COMUNICACAO_E_JORNADA");
    product.setAutomaticExecutionEnabled(false);
    product.setAutomaticExecutionChangedAt(decidedAt);
    product.setAutomaticExecutionChangedBy("pde-private-validation-final-prioritization");
    products.save(product);
    updateDiscoveryEvidence(
        product, contract, first, second, psique, temis, decidedAt, request.evidenceReference());
  }

  /** Atualiza o relatório de origem sem transformar leitura ou checkout simulado em venda. */
  private void updateDiscoveryEvidence(
      Product product,
      PrivateContract contract,
      PrivateReading first,
      PrivateReading second,
      IndependentReview psique,
      IndependentReview temis,
      Instant decidedAt,
      String decisionEvidenceReference) {
    Optional<OpportunityDossier> dossier = dossiers.findByCreatedProductId(product.getId());
    if (dossier.isEmpty() || dossier.orElseThrow().getProductDiscoveryOpportunity() == null) {
      throw new IllegalStateException("O produto validado não possui dossiê factual de origem.");
    }
    ProductDiscoveryOpportunity opportunity =
        dossier.orElseThrow().getProductDiscoveryOpportunity();
    ObjectNode evidence =
        object(opportunity.getEvidenceJson(), "evidência da oportunidade", opportunity.getId());
    ObjectNode gate = objectOrCreate(evidence, "purchaseMomentGate");
    gate.put("required", true);
    gate.put("status", "PASS");
    gate.put("sourceQualityPassed", true);
    gate.put("finalPrioritizationEligible", true);
    gate.put("minimumIndependentReadings", 2);
    gate.put("recordedIndependentReadings", 2);
    gate.put("completedAt", decidedAt.toString());
    gate.put("decisionEvidenceReference", decisionEvidenceReference);
    ObjectNode sourceQuality = objectOrCreate(gate, "sourceQuality");
    sourceQuality.put("passed", true);
    sourceQuality.put("evaluatedAt", contract.sourceQualityEvaluatedAt().toString());
    sourceQuality.put("maxAgeDays", contract.sourceMaxAgeDays());
    sourceQuality.putArray("reasons");
    ObjectNode criteria = objectOrCreate(gate, "successCriteria");
    criteria.put("declaredAt", contract.criteriaDeclaredAt().toString());
    criteria.put("minimumEligibleParticipantsPerReading", 1);
    criteria.put("minimumExperienceStartRate", 1);
    criteria.put("minimumValueMomentRate", 1);
    criteria.put("minimumReadyResultUseRate", 1);
    criteria.put("minimumPrototypePreferenceRate", 1);
    criteria.put("minimumCheckoutStartRate", 1);
    gate.putArray("eligibleCandidateNames").add(opportunity.getName());
    ObjectNode candidate = gate.putArray("candidates").addObject();
    candidate.put("candidateName", opportunity.getName());
    candidate.put("status", "PASS");
    candidate.put("eligibleForFinalPrioritization", true);
    candidate.set("scene", contract.plan().path("purchaseScene").deepCopy());
    ObjectNode freeAlternative = candidate.putObject("freeAlternative");
    freeAlternative.put("name", contract.plan().path("strongestFreeAlternative").asText());
    freeAlternative.put("prototypeAdvantage", contract.plan().path("prototypeAdvantage").asText());
    candidate.set("humanValueDelivery", contract.plan().path("humanValueDelivery").deepCopy());
    ObjectNode prototype = candidate.putObject("prototype");
    prototype.put("prototypeId", "product:" + product.getId() + "@" + contract.prototypeVersion());
    prototype.put("private", true);
    prototype.put("published", false);
    prototype.put("paymentEnabled", false);
    prototype.put("mediaSpend", 0);
    prototype.put("testMarker", "PRIVATE_PROTOTYPE");
    prototype.put("instrumentationReference", contract.instrumentationReference());
    candidate.set("readings", readingArray(first, second, psique, temis));
    gate.set("readings", readingArray(first, second, psique, temis));
    opportunity.setEvidenceJson(write(evidence, "evidência da oportunidade", opportunity.getId()));
    opportunities.save(opportunity);
  }

  /** Lista a última ocorrência concluída de cada leitura da referência atual. */
  private List<PrivateReading> readings(
      Long processId, String sourceReference, PrivateContract contract) {
    Map<String, PrivateReading> latest = new LinkedHashMap<>();
    instances
        .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
            processId, sourceReference)
        .stream()
        .filter(instance -> "COMPLETED".equals(instance.getStatus()))
        .filter(BusinessProcessActivityInstance::isObjectiveAchieved)
        .filter(
            instance ->
                Set.of(
                        PdePrivateReadingHumanActivityHandler.FIRST_READING,
                        PdePrivateReadingHumanActivityHandler.SECOND_READING)
                    .contains(instance.getActivityDefinition().getActivityId()))
        .map(instance -> reading(instance, contract))
        .forEach(reading -> latest.put(reading.activityId(), reading));
    return List.copyOf(latest.values());
  }

  /** Converte a evidência persistida em leitura tipada e recalculada pelo backend. */
  private PrivateReading reading(
      BusinessProcessActivityInstance instance, PrivateContract contract) {
    try {
      JsonNode evidence = objectMapper.readTree(instance.getObjectiveEvidenceJson());
      JsonNode structured = evidence.path("structuredEvidence");
      Map<String, Boolean> signals = new LinkedHashMap<>();
      PdePrivateReadingHumanActivityHandler.REQUIRED_SIGNALS.forEach(
          signal -> {
            JsonNode value = structured.path("signals").get(signal);
            if (value != null && value.isBoolean()) signals.put(signal, value.booleanValue());
          });
      return new PrivateReading(
          instance.getId(),
          instance.getActivityDefinition().getActivityId(),
          structured.path("participantReference").asText(""),
          structured.path("consentConfirmed").asBoolean(false),
          structured.path("firstPartyEvidenceConfirmed").asBoolean(false),
          structured.path("criteriaPassed").asBoolean(false),
          integer(structured, "eligibleParticipants"),
          integer(structured, "experienceStarted"),
          integer(structured, "valueMoments"),
          integer(structured, "readyResultsUsedWithoutAssembly"),
          integer(structured, "prototypePreferredOverFree"),
          integer(structured, "checkoutStarted"),
          instant(structured.path("observedAt"), "leitura privada", instance.getId()),
          structured.path("eventSource").asText(""),
          structured.path("testMarker").asText(""),
          structured.path("prototypeVersion").asText(""),
          evidence.path("evidenceReference").asText(""),
          Map.copyOf(signals),
          contract);
    } catch (Exception ex) {
      log.error(
          "Falha ao ler evidência da validação privada. activityInstanceId={} sourceReference={}",
          instance.getId(),
          instance.getSourceReference(),
          ex);
      return PrivateReading.invalid(
          instance.getId(), instance.getActivityDefinition().getActivityId(), contract);
    }
  }

  /** Localiza o parecer mais recente de um agente sem aceitar mera conclusão técnica. */
  private Optional<IndependentReview> review(
      Long processId, String sourceReference, String activityId, String agentKey) {
    return tasks
        .findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            processId, sourceReference)
        .stream()
        .filter(task -> activityId.equals(task.getProcessActivityId()))
        .filter(task -> task.getAssignedAgent() != null)
        .filter(task -> agentKey.equals(task.getAssignedAgent().getAgentKey()))
        .max(Comparator.comparing(AgentTask::getId))
        .map(this::review);
  }

  /** Lê a decisão explícita do parecer preservando falhas de contrato como reprovação. */
  private IndependentReview review(AgentTask task) {
    try {
      JsonNode result = objectMapper.readTree(task.getResultJson());
      String decision = result.path("decision").asText("");
      boolean evidenceApproved = structuredReviewEvidenceApproved(task, result);
      return new IndependentReview(
          task.getId(),
          task.getProcessActivityId(),
          task.getAssignedAgent().getAgentKey(),
          decision,
          task.getDeliveredAt(),
          "COMPLETED".equals(task.getStatus())
              && APPROVED_DECISIONS.contains(decision)
              && evidenceApproved);
    } catch (Exception ex) {
      log.error(
          "Falha ao ler parecer independente da validação privada. taskId={} activityId={}",
          task.getId(),
          task.getProcessActivityId(),
          ex);
      return new IndependentReview(
          task.getId(),
          task.getProcessActivityId(),
          task.getAssignedAgent().getAgentKey(),
          "INVALID",
          task.getDeliveredAt(),
          false);
    }
  }

  /** Recalcula os checks estruturados de Psique e Têmis sem confiar apenas na decisão textual. */
  private boolean structuredReviewEvidenceApproved(AgentTask task, JsonNode result) {
    if (!result.path("evidence").isArray() || result.path("evidence").isEmpty()) {
      return false;
    }
    if (PSIQUE_ACTIVITY.equals(task.getProcessActivityId())) {
      return allTrue(
          result.path("privateExperienceChecks"),
          List.of(
              "sameProductAndVersion",
              "twoDistinctParticipants",
              "fiveSignalsPassedTwice",
              "firstPartyEvents",
              "lowEffortReadyResult",
              "desktopAndMobileUsable",
              "consentAndPrivacyPreserved",
              "noMaterialHarm"));
    }
    if (TEMIS_ACTIVITY.equals(task.getProcessActivityId())) {
      return allTrue(
          result.path("privateValidationChecks"),
          List.of(
              "sameProductAndVersion",
              "criteriaPredeclared",
              "twoDistinctParticipants",
              "fiveSignalsPassedTwice",
              "firstPartyEvents",
              "privateAndUnpublished",
              "paymentDisabled",
              "zeroMediaSpend",
              "privacyPreserved"));
    }
    return false;
  }

  /**
   * Confirma que o objeto possui exatamente controles booleanos aprovados para cada fato exigido.
   */
  private boolean allTrue(JsonNode checks, List<String> requiredChecks) {
    return checks.isObject()
        && checks.size() == requiredChecks.size()
        && requiredChecks.stream()
            .allMatch(field -> checks.path(field).isBoolean() && checks.path(field).asBoolean());
  }

  /** Exige um parecer concluído e aprovado pelo agente canônico da atividade. */
  private IndependentReview requiredApprovedReview(
      Long processId, String sourceReference, String activityId, String agentKey) {
    return review(processId, sourceReference, activityId, agentKey)
        .filter(IndependentReview::approved)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "O parecer independente de " + agentKey + " ainda não foi aprovado."));
  }

  /** Lê e valida o contrato congelado que governa protótipo, fontes e duas leituras. */
  private PrivateContract contract(Product product) {
    if (product == null || product.getValidationDefinitionJson() == null) {
      return PrivateContract.invalid(objectMapper.createObjectNode());
    }
    try {
      ObjectNode definition =
          object(product.getValidationDefinitionJson(), "definição de validação", product.getId());
      JsonNode plan = definition.path("privateValidationPlan");
      JsonNode acceptance = definition.path("privatePrototypeAcceptance");
      Instant declaredAt =
          instant(plan.path("criteriaDeclaredAt"), "critério privado", product.getId());
      Instant sourceEvaluatedAt =
          instant(
              acceptance.path("sourceQualityEvaluatedAt"),
              "fonte do protótipo privado",
              product.getId());
      Instant acceptedAt =
          instant(acceptance.path("acceptedAt"), "aceitação do protótipo", product.getId());
      int sourceMaxAgeDays = integer(plan, "sourceMaxAgeDays");
      String prototypeVersion = acceptance.path("prototypeVersion").asText("");
      String instrumentationReference = acceptance.path("instrumentationReference").asText("");
      Instant now = Instant.now();
      boolean valid =
          "PDE_PRIVATE_VALIDATION_V1".equals(product.getValidationDefinitionVersion())
              && plan.path("minimumIndependentReadings").asInt(0) == 2
              && plan.path("minimumEligibleParticipantsPerReading").asInt(0) == 1
              && exactRequiredSignals(plan.path("requiredSignals"))
              && unitRate(plan, "minimumExperienceStartRate")
              && unitRate(plan, "minimumValueMomentRate")
              && unitRate(plan, "minimumReadyResultUseRate")
              && unitRate(plan, "minimumPrototypePreferenceRate")
              && unitRate(plan, "minimumCheckoutStartRate")
              && sourceMaxAgeDays >= 1
              && sourceMaxAgeDays <= 90
              && !declaredAt.isAfter(now)
              && !sourceEvaluatedAt.isAfter(acceptedAt)
              && !acceptedAt.isBefore(declaredAt)
              && !acceptedAt.isAfter(now)
              && sourceEvaluatedAt.plus(sourceMaxAgeDays, ChronoUnit.DAYS).isAfter(now)
              && completePurchaseScene(plan.path("purchaseScene"))
              && completeHumanValueDelivery(plan.path("humanValueDelivery"))
              && !plan.path("strongestFreeAlternative").asText("").isBlank()
              && !plan.path("prototypeAdvantage").asText("").isBlank()
              && "READY".equals(acceptance.path("status").asText())
              && acceptance.path("sourceQualityPassed").asBoolean(false)
              && !acceptance.path("privateAccessUrl").asText("").isBlank()
              && !prototypeVersion.isBlank()
              && !instrumentationReference.isBlank()
              && !acceptance.path("sourceEvidenceReference").asText("").isBlank()
              && acceptance.path("privateAccessConfirmed").asBoolean(false)
              && acceptance.path("desktopValidated").asBoolean(false)
              && acceptance.path("mobileValidated").asBoolean(false)
              && !acceptance.path("paymentEnabled").asBoolean(true)
              && !acceptance.path("published").asBoolean(true)
              && Double.compare(acceptance.path("mediaSpendBrl").asDouble(-1), 0d) == 0
              && "FIRST_PARTY_EVENTS".equals(acceptance.path("eventSource").asText())
              && "PRIVATE_PROTOTYPE".equals(acceptance.path("testMarker").asText());
      return new PrivateContract(
          definition,
          plan,
          acceptance,
          declaredAt,
          acceptedAt,
          sourceEvaluatedAt,
          sourceMaxAgeDays,
          prototypeVersion,
          instrumentationReference,
          valid);
    } catch (RuntimeException ex) {
      log.error(
          "Falha ao validar contrato da priorização privada. productId={}", product.getId(), ex);
      return PrivateContract.invalid(objectMapper.createObjectNode());
    }
  }

  /** Exige os cinco sinais canônicos, sem ausência, duplicidade ou extensão silenciosa. */
  private boolean exactRequiredSignals(JsonNode values) {
    if (!values.isArray()
        || values.size() != PdePrivateReadingHumanActivityHandler.REQUIRED_SIGNALS.size()) {
      return false;
    }
    java.util.Set<String> actual = new java.util.LinkedHashSet<>();
    values.forEach(value -> actual.add(value.isTextual() ? value.asText() : ""));
    return actual.size() == values.size()
        && actual.equals(Set.copyOf(PdePrivateReadingHumanActivityHandler.REQUIRED_SIGNALS));
  }

  /** Exige que Psique e Têmis tenham recebido as duas leituras antes de decidir. */
  private boolean reviewsAfterReadings(
      PrivateReading first,
      PrivateReading second,
      IndependentReview psique,
      IndependentReview temis) {
    Instant readingsCompletedAt =
        first.observedAt().isAfter(second.observedAt()) ? first.observedAt() : second.observedAt();
    return psique.deliveredAt() != null
        && temis.deliveredAt() != null
        && !psique.deliveredAt().isBefore(readingsCompletedAt)
        && !temis.deliveredAt().isBefore(psique.deliveredAt());
  }

  /** Confirma os fatos estruturados da cena real de compra congelada por Atena. */
  private boolean completePurchaseScene(JsonNode scene) {
    return hasText(scene, "trigger")
        && hasText(scene, "deadline")
        && hasText(scene, "costOfError")
        && hasText(scene, "budgetEvidence")
        && hasText(scene, "failedAttempt")
        && hasText(scene, "currentPaidBehavior");
  }

  /** Confirma entrega pronta, baixo esforço e evidência humana suficiente para um PDE. */
  private boolean completeHumanValueDelivery(JsonNode delivery) {
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

  /** Localiza uma das leituras pelo identificador canônico da atividade. */
  private Optional<PrivateReading> reading(List<PrivateReading> readings, String activityId) {
    return readings.stream().filter(reading -> activityId.equals(reading.activityId())).findFirst();
  }

  /** Explica a prontidão de uma leitura no gate final. */
  private HumanProductProcessActivityRequirement readingRequirement(
      String code, String title, boolean ready) {
    return requirement(
        code,
        title,
        ready,
        "A leitura possui consentimento, evento próprio e taxa integral nos cinco sinais.",
        "Ajuste o protótipo e repita esta leitura até comprovar os cinco sinais predeclarados.");
  }

  /** Explica a prontidão de um parecer independente no gate final. */
  private HumanProductProcessActivityRequirement reviewRequirement(
      String code, String title, boolean ready) {
    return requirement(
        code,
        title,
        ready,
        "O agente independente concluiu com decisão explícita de aprovação.",
        "Conclua o parecer independente com decisão aprovada e evidência persistida.");
  }

  /** Cria um requisito funcional com detalhe e ação corretiva estáveis. */
  private HumanProductProcessActivityRequirement requirement(
      String code, String title, boolean ready, String success, String recommendation) {
    return new HumanProductProcessActivityRequirement(
        code, title, ready, ready ? success : recommendation, recommendation);
  }

  /** Serializa contagens e taxas recalculadas, vinculando os pareceres independentes. */
  private ArrayNode readingArray(
      PrivateReading first,
      PrivateReading second,
      IndependentReview psique,
      IndependentReview temis) {
    ArrayNode values = objectMapper.createArrayNode();
    for (PrivateReading reading : List.of(first, second)) {
      ObjectNode value = values.addObject();
      value.put("activityInstanceId", reading.instanceId());
      value.put("readingId", reading.participantReference());
      value.put("participantReference", reading.participantReference());
      value.put("consentConfirmed", reading.consentConfirmed());
      value.put("observedAt", reading.observedAt().toString());
      value.put("eligibleParticipants", reading.eligibleParticipants());
      value.put("experienceStarted", reading.experienceStarted());
      value.put("valueMoments", reading.valueMoments());
      value.put("readyResultsUsedWithoutAssembly", reading.readyResultsUsedWithoutAssembly());
      value.put("prototypePreferredOverFree", reading.prototypePreferredOverFree());
      value.put("checkoutStarted", reading.checkoutStarted());
      ObjectNode rates = value.putObject("rates");
      rates.put("experienceStartRate", reading.rate(reading.experienceStarted()));
      rates.put("valueMomentRate", reading.rate(reading.valueMoments()));
      rates.put("readyResultUseRate", reading.rate(reading.readyResultsUsedWithoutAssembly()));
      rates.put("prototypePreferenceRate", reading.rate(reading.prototypePreferredOverFree()));
      rates.put("checkoutStartRate", reading.rate(reading.checkoutStarted()));
      value.put("criteriaPassed", true);
      value.put("passed", true);
      value.put("eventSource", reading.eventSource());
      value.put("testMarker", reading.testMarker());
      value.put("prototypeVersion", reading.prototypeVersion());
      value.put("evidenceReference", reading.evidenceReference());
      value.put("psiqueDecision", "APPROVE");
      value.put("temisDecision", "APPROVE");
      value.put("psiqueTaskId", psique.taskId());
      value.put("temisTaskId", temis.taskId());
      value.set("signals", objectMapper.valueToTree(reading.signals()));
    }
    return values;
  }

  /** Registra a identidade e a decisão do parecer sem copiar sua auditoria técnica. */
  private void writeReview(ObjectNode target, IndependentReview review) {
    target.put("taskId", review.taskId());
    target.put("activityId", review.activityId());
    target.put("agentKey", review.agentKey());
    target.put("decision", review.decision());
    if (review.deliveredAt() != null) target.put("deliveredAt", review.deliveredAt().toString());
  }

  /** Obtém um objeto filho preservado ou cria a projeção quando ela ainda não existe. */
  private ObjectNode objectOrCreate(ObjectNode parent, String field) {
    JsonNode value = parent.get(field);
    if (value instanceof ObjectNode object) return object;
    return parent.putObject(field);
  }

  /** Lê um inteiro não negativo sem converter texto ou número fracionário. */
  private int integer(JsonNode parent, String field) {
    JsonNode value = parent.path(field);
    return value.isIntegralNumber() && value.canConvertToInt() && value.asInt() >= 0
        ? value.asInt()
        : -1;
  }

  /** Lê um instante ISO e mantém o contexto do campo em qualquer inconsistência. */
  private Instant instant(JsonNode value, String label, Long entityId) {
    try {
      return Instant.parse(value.asText(""));
    } catch (Exception ex) {
      log.error("Falha ao ler instante de {}. entityId={}", label, entityId, ex);
      throw new IllegalStateException("Instante inválido em " + label + ".", ex);
    }
  }

  /** Confirma uma taxa predeclarada integral para cada leitura individual. */
  private boolean unitRate(JsonNode plan, String field) {
    return plan.path(field).isNumber() && Double.compare(plan.path(field).asDouble(), 1d) == 0;
  }

  /** Confirma um campo textual real sem aceitar coerção de objeto ou lista. */
  private boolean hasText(JsonNode value, String field) {
    return value.isObject()
        && value.path(field).isTextual()
        && !value.path(field).asText().trim().isBlank();
  }

  /** Impede que evidências de outra versão ou produto sejam usadas na priorização final. */
  private boolean belongsToProduct(String sourceReference, Product product) {
    return product != null
        && product.getId() != null
        && sourceReference != null
        && sourceReference.equals("product:" + product.getId() + "@private-validation-v1");
  }

  /** Lê um JSON de domínio sem esconder corrupção persistida. */
  private ObjectNode object(String raw, String label, Long entityId) {
    try {
      JsonNode value = objectMapper.readTree(raw);
      if (value instanceof ObjectNode object) return object;
      throw new IllegalArgumentException("JSON não representa objeto.");
    } catch (Exception ex) {
      log.error("Falha ao ler {}. entityId={}", label, entityId, ex);
      throw new IllegalStateException("Não foi possível ler " + label + ".", ex);
    }
  }

  /** Serializa uma atualização de domínio com erro contextualizado. */
  private String write(JsonNode value, String label, Long entityId) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception ex) {
      log.error("Falha ao serializar {}. entityId={}", label, entityId, ex);
      throw new IllegalStateException("Não foi possível persistir " + label + ".", ex);
    }
  }

  /** Representa o contrato congelado usado para recalcular todas as provas privadas. */
  private record PrivateContract(
      ObjectNode definition,
      JsonNode plan,
      JsonNode acceptance,
      Instant criteriaDeclaredAt,
      Instant prototypeAcceptedAt,
      Instant sourceQualityEvaluatedAt,
      int sourceMaxAgeDays,
      String prototypeVersion,
      String instrumentationReference,
      boolean valid) {

    /** Cria um contrato sentinela que mantém o gate fechado sem inventar evidência. */
    private static PrivateContract invalid(ObjectNode definition) {
      return new PrivateContract(
          definition,
          definition.missingNode(),
          definition.missingNode(),
          Instant.EPOCH,
          Instant.EPOCH,
          Instant.EPOCH,
          -1,
          "",
          "",
          false);
    }
  }

  /** Representa uma leitura privada persistida com suas contagens verificáveis. */
  private record PrivateReading(
      Long instanceId,
      String activityId,
      String participantReference,
      boolean consentConfirmed,
      boolean firstPartyEvidenceConfirmed,
      boolean criteriaPassed,
      int eligibleParticipants,
      int experienceStarted,
      int valueMoments,
      int readyResultsUsedWithoutAssembly,
      int prototypePreferredOverFree,
      int checkoutStarted,
      Instant observedAt,
      String eventSource,
      String testMarker,
      String prototypeVersion,
      String evidenceReference,
      Map<String, Boolean> signals,
      PrivateContract contract) {

    /** Cria uma leitura sentinela inválida sem perder a identidade da ocorrência. */
    private static PrivateReading invalid(
        Long instanceId, String activityId, PrivateContract contract) {
      return new PrivateReading(
          instanceId,
          activityId,
          "",
          false,
          false,
          false,
          -1,
          -1,
          -1,
          -1,
          -1,
          -1,
          Instant.EPOCH,
          "",
          "",
          "",
          "",
          Map.of(),
          contract);
    }

    /** Recalcula todos os critérios, sem confiar no booleano informado pelo formulário. */
    private boolean valid() {
      return contract.valid()
          && consentConfirmed
          && firstPartyEvidenceConfirmed
          && criteriaPassed
          && participantReference.matches("^PV-[A-F0-9]{12}$")
          && eligibleParticipants == 1
          && experienceStarted == 1
          && valueMoments == 1
          && readyResultsUsedWithoutAssembly == 1
          && prototypePreferredOverFree == 1
          && checkoutStarted == 1
          && !observedAt.isBefore(contract.criteriaDeclaredAt())
          && !observedAt.isBefore(contract.prototypeAcceptedAt())
          && !observedAt.isAfter(Instant.now())
          && "FIRST_PARTY_EVENTS".equals(eventSource)
          && "PRIVATE_PROTOTYPE".equals(testMarker)
          && contract.prototypeVersion().equals(prototypeVersion)
          && evidenceReference != null
          && !evidenceReference.isBlank()
          && signals
              .keySet()
              .equals(Set.copyOf(PdePrivateReadingHumanActivityHandler.REQUIRED_SIGNALS))
          && signals.values().stream().allMatch(Boolean.TRUE::equals);
    }

    /** Calcula a taxa da leitura individual com denominador persistido. */
    private double rate(int numerator) {
      return eligibleParticipants > 0 ? (double) numerator / eligibleParticipants : -1d;
    }
  }

  /** Representa o parecer explícito de um agente independente. */
  private record IndependentReview(
      Long taskId,
      String activityId,
      String agentKey,
      String decision,
      Instant deliveredAt,
      boolean approved) {}
}
