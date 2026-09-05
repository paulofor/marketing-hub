package com.marketinghub.product.service.privatevalidation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityCompletion;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityHandler;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityRequirement;
import com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution.ProductProcessActivityExecutionRequest;
import com.marketinghub.product.Product;
import com.marketinghub.product.privatereading.service.PdePrivateReadingService;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: validar as duas leituras humanas privadas e pseudonimizadas de um PDE. */
@Service
@Slf4j
public class PdePrivateReadingHumanActivityHandler implements HumanProductProcessActivityHandler {
  static final String PROCESS_CODE = "pde-construction-approval";
  static final String FIRST_READING = "privateReading1";
  static final String SECOND_READING = "privateReading2";
  static final String WORKSPACE_CODE = "PDE_PRIVATE_READING";
  static final List<String> REQUIRED_SIGNALS =
      List.of(
          "EXPERIENCE_STARTED",
          "VALUE_MOMENT",
          "READY_RESULT_USED",
          "PREFERRED_OVER_FREE",
          "CHECKOUT_STARTED");
  private static final Set<String> ACTIVITIES = Set.of(FIRST_READING, SECOND_READING);
  private static final Pattern PARTICIPANT_REFERENCE = Pattern.compile("^PV-[A-F0-9]{12}$");
  private final BusinessProcessActivityInstanceRepository instances;
  private final ObjectMapper objectMapper;
  private final PdePrivateReadingService assistedReadings;

  /** Configura a consulta de prova própria para atividades que possuem protótipo integrado. */
  public PdePrivateReadingHumanActivityHandler(
      BusinessProcessActivityInstanceRepository instances,
      ObjectMapper objectMapper,
      PdePrivateReadingService assistedReadings) {
    this.instances = instances;
    this.objectMapper = objectMapper;
    this.assistedReadings = assistedReadings;
  }

  /** Reconhece somente as duas leituras privadas do processo de construção PDE. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition) {
    return process != null
        && activityDefinition != null
        && PROCESS_CODE.equals(process.getProcessCode())
        && ACTIVITIES.contains(activityDefinition.getActivityId());
  }

  /** Expõe a assistência integrada ou o formulário legado somente com contrato privado válido. */
  @Override
  @Transactional(readOnly = true)
  public HumanProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference) {
    boolean productReference = belongsToProduct(sourceReference, product);
    boolean privatePlan = hasPrivatePlan(product);
    boolean prototypeReady = hasAcceptedPrototype(product);
    List<HumanProductProcessActivityRequirement> requirements =
        List.of(
            new HumanProductProcessActivityRequirement(
                "PRIVATE_PRODUCT_REFERENCE",
                "Produto privado segregado",
                productReference,
                productReference
                    ? "A leitura será vinculada somente a este produto."
                    : "A referência operacional não pertence ao produto selecionado.",
                "Abra a leitura pela cadeia do produto correto."),
            new HumanProductProcessActivityRequirement(
                "PRIVATE_PROTOTYPE_CONTRACT",
                "Protótipo e sinais definidos",
                privatePlan,
                privatePlan
                    ? "O contrato exige duas pessoas e taxa integral nos cinco sinais."
                    : "O produto não possui o contrato PDE_PRIVATE_VALIDATION_V1 completo.",
                "Reconstrua o handoff de Atena e Dédalo antes da leitura."),
            new HumanProductProcessActivityRequirement(
                "PRIVATE_PROTOTYPE_ACCEPTED",
                "Protótipo privado utilizável",
                prototypeReady,
                prototypeReady
                    ? "URL privada, versão, fontes e eventos próprios foram aceitos."
                    : "Ainda não existe aceitação auditável do protótipo utilizável.",
                "Conclua a atividade Confirmar protótipo privado."));
    boolean ready = productReference && privatePlan && prototypeReady;
    String activityId = activityDefinition.getActivityId();
    if (ready && assistedReadings.supports(product)) {
      return new HumanProductProcessActivityReadiness(
          true,
          "O protótipo aceito está disponível para a leitura privada.",
          "Registrar resultado da leitura",
          "Abra o protótipo, acompanhe a pessoa e atualize o resultado. Os sinais e a referência serão buscados automaticamente.",
          FIRST_READING.equals(activityId)
              ? "Primeira leitura privada"
              : "Segunda leitura privada independente",
          "Confirmo que acompanhei uma pessoa real, aderente ao público e consentida, e revisei o resultado desta leitura.",
          "CONFIRM:" + PROCESS_CODE + ":" + activityId,
          "PDE_PRIVATE_READING_ASSISTED",
          product.getId(),
          requirements,
          HumanProductProcessActivityReadiness.REVIEW_AND_ACCEPT,
          "pde-private-reading:product:" + product.getId() + ":" + activityId);
    }
    return new HumanProductProcessActivityReadiness(
        ready,
        ready
            ? "O protótipo está apto a receber uma leitura privada auditável."
            : requirements.stream()
                .filter(requirement -> !requirement.satisfied())
                .findFirst()
                .map(HumanProductProcessActivityRequirement::recommendation)
                .orElse("A leitura ainda não está pronta."),
        "Registrar leitura privada",
        "Registre uma pessoa consentida por código aleatório PV, usando eventos próprios do protótipo. A tentativa fica bloqueada e repetível quando qualquer critério não for atingido.",
        FIRST_READING.equals(activityId)
            ? "Primeira leitura privada"
            : "Segunda leitura privada independente",
        "Confirmo que a pessoa consentiu, usou o protótipo privado e que os sinais refletem somente o que foi observado.",
        "CONFIRM:" + PROCESS_CODE + ":" + activityId,
        WORKSPACE_CODE,
        product == null ? null : product.getId(),
        requirements);
  }

  /** Valida os dados estruturados antes de permitir que o executor persista a leitura. */
  @Override
  @Transactional(readOnly = true)
  public void approve(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference,
      ProductProcessActivityExecutionRequest request) {
    evaluatedReading(process, activityDefinition, product, sourceReference, request);
  }

  /** Recalcula os cinco sinais e preserva também uma leitura válida abaixo do gate. */
  @Override
  @Transactional(readOnly = true)
  public HumanProductProcessActivityCompletion completeApproval(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference,
      ProductProcessActivityExecutionRequest request) {
    Map<String, Object> reading =
        evaluatedReading(process, activityDefinition, product, sourceReference, request);
    if (Boolean.TRUE.equals(reading.get("criteriaPassed"))) {
      return HumanProductProcessActivityCompletion.completed(reading);
    }
    return HumanProductProcessActivityCompletion.blocked(
        "A leitura foi preservada, mas ficou abaixo dos critérios predeclarados.",
        "A pessoa não comprovou os cinco sinais do momento de compra; ajuste o protótipo e repita esta leitura com outra evidência própria.",
        reading);
  }

  /** Reconsulta prova integrada quando disponível; rejeita sinais ausentes e pessoa repetida. */
  private Map<String, Object> evaluatedReading(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference,
      ProductProcessActivityExecutionRequest request) {
    if (!belongsToProduct(sourceReference, product)
        || !hasPrivatePlan(product)
        || !hasAcceptedPrototype(product)) {
      throw new IllegalStateException("O protótipo privado ainda não está apto à leitura.");
    }
    Map<String, Object> evidence =
        assistedReadings.supports(product)
            ? assistedReadings.verifiedEvidence(
                product, activityDefinition.getActivityId(), request.structuredEvidence())
            : request.structuredEvidence();
    String participantReference = text(evidence.get("participantReference"));
    if (!PARTICIPANT_REFERENCE.matcher(participantReference).matches()) {
      throw new IllegalArgumentException(
          "Use um código aleatório no formato PV- seguido por 12 caracteres hexadecimais.");
    }
    if (!Boolean.TRUE.equals(evidence.get("consentConfirmed"))) {
      throw new IllegalArgumentException("Confirme o consentimento antes de registrar a leitura.");
    }
    if (!Boolean.TRUE.equals(evidence.get("firstPartyEvidenceConfirmed"))) {
      throw new IllegalArgumentException(
          "Confirme que os sinais vieram dos eventos próprios do protótipo.");
    }
    Map<String, Object> signals = map(evidence.get("signals"));
    if (!signals.keySet().equals(Set.copyOf(REQUIRED_SIGNALS))
        || REQUIRED_SIGNALS.stream()
            .anyMatch(signal -> !(signals.get(signal) instanceof Boolean))) {
      throw new IllegalArgumentException(
          "Registre exatamente os cinco sinais canônicos como observado ou não observado.");
    }
    if (SECOND_READING.equals(activityDefinition.getActivityId())
        && participantReference.equals(firstParticipant(process, sourceReference))) {
      throw new IllegalArgumentException(
          "A segunda leitura precisa ser feita por uma pessoa distinta da primeira.");
    }
    JsonNode definition = definition(product);
    JsonNode plan = definition.path("privateValidationPlan");
    JsonNode acceptance = definition.path("privatePrototypeAcceptance");
    Map<String, Object> rates = new LinkedHashMap<>();
    rates.put("experienceStartRate", rate(signals, "EXPERIENCE_STARTED"));
    rates.put("valueMomentRate", rate(signals, "VALUE_MOMENT"));
    rates.put("readyResultUseRate", rate(signals, "READY_RESULT_USED"));
    rates.put("prototypePreferenceRate", rate(signals, "PREFERRED_OVER_FREE"));
    rates.put("checkoutStartRate", rate(signals, "CHECKOUT_STARTED"));
    boolean criteriaPassed =
        atLeast(rates, "experienceStartRate", plan, "minimumExperienceStartRate")
            && atLeast(rates, "valueMomentRate", plan, "minimumValueMomentRate")
            && atLeast(rates, "readyResultUseRate", plan, "minimumReadyResultUseRate")
            && atLeast(rates, "prototypePreferenceRate", plan, "minimumPrototypePreferenceRate")
            && atLeast(rates, "checkoutStartRate", plan, "minimumCheckoutStartRate");
    Map<String, Object> enriched = new LinkedHashMap<>(evidence);
    enriched.put("readingId", participantReference);
    enriched.put("observedAt", Instant.now().toString());
    enriched.put("eligibleParticipants", 1);
    enriched.put("experienceStarted", count(signals, "EXPERIENCE_STARTED"));
    enriched.put("valueMoments", count(signals, "VALUE_MOMENT"));
    enriched.put("readyResultsUsedWithoutAssembly", count(signals, "READY_RESULT_USED"));
    enriched.put("prototypePreferredOverFree", count(signals, "PREFERRED_OVER_FREE"));
    enriched.put("checkoutStarted", count(signals, "CHECKOUT_STARTED"));
    enriched.put("rates", Map.copyOf(rates));
    enriched.put("criteriaPassed", criteriaPassed);
    enriched.put("eventSource", "FIRST_PARTY_EVENTS");
    enriched.put("testMarker", "PRIVATE_PROTOTYPE");
    enriched.put("prototypeVersion", acceptance.path("prototypeVersion").asText());
    return Map.copyOf(enriched);
  }

  /** Localiza o participante pseudonimizado da primeira leitura já concluída. */
  private String firstParticipant(BusinessProcessDefinition process, String sourceReference) {
    return instances
        .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
            process.getId(), sourceReference)
        .stream()
        .filter(BusinessProcessActivityInstance::isObjectiveAchieved)
        .filter(instance -> "COMPLETED".equals(instance.getStatus()))
        .filter(instance -> FIRST_READING.equals(instance.getActivityDefinition().getActivityId()))
        .map(this::participantReference)
        .filter(value -> !value.isBlank())
        .findFirst()
        .orElse("");
  }

  /** Extrai o código pseudonimizado de uma evidência já persistida. */
  private String participantReference(BusinessProcessActivityInstance instance) {
    try {
      JsonNode evidence = objectMapper.readTree(instance.getObjectiveEvidenceJson());
      return evidence.path("structuredEvidence").path("participantReference").asText("");
    } catch (Exception ex) {
      log.error(
          "Falha ao ler evidência da primeira leitura privada. activityInstanceId={} sourceReference={}",
          instance.getId(),
          instance.getSourceReference(),
          ex);
      throw new IllegalStateException("A primeira leitura possui evidência inválida.", ex);
    }
  }

  /** Confirma identidade e contrato antes de expor o formulário especializado. */
  private boolean hasPrivatePlan(Product product) {
    if (product == null
        || !"PDE_PRIVATE_VALIDATION_V1".equals(product.getValidationDefinitionVersion())
        || product.getValidationDefinitionJson() == null) {
      return false;
    }
    try {
      JsonNode definition = objectMapper.readTree(product.getValidationDefinitionJson());
      JsonNode plan = definition.path("privateValidationPlan");
      JsonNode prototype = definition.path("privatePrototype");
      List<String> values = new ArrayList<>();
      plan.path("requiredSignals").forEach(item -> values.add(item.asText()));
      return plan.path("minimumIndependentReadings").asInt(0) == 2
          && plan.path("minimumEligibleParticipantsPerReading").asInt(0) == 1
          && values.size() == REQUIRED_SIGNALS.size()
          && values.containsAll(REQUIRED_SIGNALS)
          && unitRate(plan, "minimumExperienceStartRate")
          && unitRate(plan, "minimumValueMomentRate")
          && unitRate(plan, "minimumReadyResultUseRate")
          && unitRate(plan, "minimumPrototypePreferenceRate")
          && unitRate(plan, "minimumCheckoutStartRate")
          && prototype.isObject()
          && "SIMULATED_NO_CHARGE".equals(prototype.path("checkoutMode").asText());
    } catch (Exception ex) {
      log.error("Falha ao ler contrato da validação privada. productId={}", product.getId(), ex);
      return false;
    }
  }

  /** Confirma a aceitação da mesma versão privada, sem publicação, cobrança ou mídia. */
  private boolean hasAcceptedPrototype(Product product) {
    if (!hasPrivatePlan(product)) return false;
    try {
      JsonNode definition = definition(product);
      JsonNode plan = definition.path("privateValidationPlan");
      JsonNode acceptance = definition.path("privatePrototypeAcceptance");
      Instant now = Instant.now();
      Instant declaredAt = Instant.parse(plan.path("criteriaDeclaredAt").asText(""));
      Instant sourceEvaluatedAt =
          Instant.parse(acceptance.path("sourceQualityEvaluatedAt").asText(""));
      Instant acceptedAt = Instant.parse(acceptance.path("acceptedAt").asText(""));
      int sourceMaxAgeDays = plan.path("sourceMaxAgeDays").asInt(0);
      return sourceMaxAgeDays >= 1
          && sourceMaxAgeDays <= 90
          && !declaredAt.isAfter(now)
          && !sourceEvaluatedAt.isAfter(acceptedAt)
          && !acceptedAt.isBefore(declaredAt)
          && !acceptedAt.isAfter(now)
          && sourceEvaluatedAt.plusSeconds(sourceMaxAgeDays * 86_400L).isAfter(now)
          && "READY".equals(acceptance.path("status").asText())
          && acceptance.path("sourceQualityPassed").asBoolean(false)
          && acceptance.path("privateAccessConfirmed").asBoolean(false)
          && acceptance.path("desktopValidated").asBoolean(false)
          && acceptance.path("mobileValidated").asBoolean(false)
          && !acceptance.path("privateAccessUrl").asText().isBlank()
          && !acceptance.path("prototypeVersion").asText().isBlank()
          && !acceptance.path("instrumentationReference").asText().isBlank()
          && !acceptance.path("sourceEvidenceReference").asText().isBlank()
          && !acceptance.path("paymentEnabled").asBoolean(true)
          && !acceptance.path("published").asBoolean(true)
          && Double.compare(acceptance.path("mediaSpendBrl").asDouble(-1), 0d) == 0
          && "FIRST_PARTY_EVENTS".equals(acceptance.path("eventSource").asText())
          && "PRIVATE_PROTOTYPE".equals(acceptance.path("testMarker").asText());
    } catch (RuntimeException ex) {
      log.error(
          "Falha ao validar a vigência do protótipo antes da leitura. productId={}",
          product.getId(),
          ex);
      return false;
    }
  }

  /** Lê o contrato do produto com log contextual em qualquer corrupção persistida. */
  private JsonNode definition(Product product) {
    try {
      return objectMapper.readTree(product.getValidationDefinitionJson());
    } catch (Exception ex) {
      log.error("Falha ao ler contrato da leitura privada. productId={}", product.getId(), ex);
      throw new IllegalStateException("O contrato da leitura privada é inválido.", ex);
    }
  }

  /** Confirma uma taxa predeclarada integral para a leitura individual. */
  private boolean unitRate(JsonNode plan, String field) {
    return plan.path(field).isNumber() && Double.compare(plan.path(field).asDouble(), 1d) == 0;
  }

  /** Converte um sinal booleano em contagem observada com denominador um. */
  private int count(Map<String, Object> signals, String signal) {
    return Boolean.TRUE.equals(signals.get(signal)) ? 1 : 0;
  }

  /** Converte um sinal booleano na taxa auditável da leitura individual. */
  private double rate(Map<String, Object> signals, String signal) {
    return count(signals, signal);
  }

  /** Compara a taxa recalculada com o critério congelado no produto. */
  private boolean atLeast(
      Map<String, Object> rates, String rateField, JsonNode plan, String criterionField) {
    return ((Number) rates.get(rateField)).doubleValue() >= plan.path(criterionField).asDouble(2d);
  }

  /** Impede que uma referência de outro produto seja usada na leitura. */
  private boolean belongsToProduct(String sourceReference, Product product) {
    return product != null
        && product.getId() != null
        && sourceReference != null
        && sourceReference.equals("product:" + product.getId() + "@private-validation-v1");
  }

  /** Converte um objeto JSON em mapa sem aceitar outro tipo de entrada. */
  private Map<String, Object> map(Object value) {
    if (!(value instanceof Map<?, ?> raw)) return Map.of();
    Map<String, Object> result = new LinkedHashMap<>();
    raw.forEach((key, item) -> result.put(String.valueOf(key), item));
    return result;
  }

  /** Normaliza um texto recebido no formulário especializado. */
  private String text(Object value) {
    return value == null ? "" : String.valueOf(value).trim();
  }
}
