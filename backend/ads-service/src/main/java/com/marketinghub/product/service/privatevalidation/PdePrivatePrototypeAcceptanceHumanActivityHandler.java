package com.marketinghub.product.service.privatevalidation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityHandler;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityRequirement;
import com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution.ProductProcessActivityExecutionRequest;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: comprovar que existe um protótipo privado utilizável antes das leituras. */
@Service
@Slf4j
public class PdePrivatePrototypeAcceptanceHumanActivityHandler
    implements HumanProductProcessActivityHandler {
  static final String ACTIVITY_ID = "prototypeAcceptance";
  static final String WORKSPACE_CODE = "PDE_PRIVATE_PROTOTYPE_ACCEPTANCE";
  private static final Pattern VERSION = Pattern.compile("^[a-z0-9][a-z0-9._-]{2,63}$");
  private final ProductRepository products;
  private final ObjectMapper objectMapper;

  /** Configura a persistência do produto e o leitor dos contratos versionados. */
  public PdePrivatePrototypeAcceptanceHumanActivityHandler(
      ProductRepository products, ObjectMapper objectMapper) {
    this.products = products;
    this.objectMapper = objectMapper;
  }

  /** Reconhece somente a aceitação humana do protótipo privado no processo canônico. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition) {
    return process != null
        && activityDefinition != null
        && PdePrivateReadingHumanActivityHandler.PROCESS_CODE.equals(process.getProcessCode())
        && ACTIVITY_ID.equals(activityDefinition.getActivityId());
  }

  /** Expõe o formulário somente para o produto planejado e a referência privada corretos. */
  @Override
  @Transactional(readOnly = true)
  public HumanProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference) {
    boolean productReference = belongsToProduct(sourceReference, product);
    boolean contractReady = hasPlanningContract(product);
    List<HumanProductProcessActivityRequirement> requirements =
        List.of(
            new HumanProductProcessActivityRequirement(
                "PRIVATE_PRODUCT_REFERENCE",
                "Produto privado segregado",
                productReference,
                productReference
                    ? "A aceitação será vinculada somente a este produto."
                    : "A referência operacional não pertence ao produto selecionado.",
                "Abra o protótipo pela cadeia do produto correto."),
            new HumanProductProcessActivityRequirement(
                "PRIVATE_PROTOTYPE_PLAN",
                "Plano de protótipo congelado",
                contractReady,
                contractReady
                    ? "Atena e Dédalo definiram escopo, instrumentação e limite sem cobrança."
                    : "O produto não possui o contrato privado completo.",
                "Reconstrua o handoff de Atena e Dédalo antes de aceitar o protótipo."));
    boolean ready = productReference && contractReady;
    return new HumanProductProcessActivityReadiness(
        ready,
        ready
            ? "Registre a versão realmente utilizável antes de convidar qualquer participante."
            : requirements.stream()
                .filter(requirement -> !requirement.satisfied())
                .findFirst()
                .map(HumanProductProcessActivityRequirement::recommendation)
                .orElse("O protótipo privado ainda não está pronto."),
        "Confirmar protótipo privado",
        "Informe uma URL privada acessível aos revisores, versão, instrumentação e fonte comercial vigente. Esta decisão não publica, cobra ou gasta.",
        "Protótipo privado utilizável",
        "Confirmo que testei a experiência em desktop e celular, com acesso restrito, eventos próprios, pagamento desativado, publicação desativada e mídia zerada.",
        "CONFIRM:" + PdePrivateReadingHumanActivityHandler.PROCESS_CODE + ":" + ACTIVITY_ID,
        WORKSPACE_CODE,
        product == null ? null : product.getId(),
        requirements);
  }

  /** Persiste somente a aceitação privada comprovada, sem criar efeito comercial externo. */
  @Override
  @Transactional
  public void approve(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference,
      ProductProcessActivityExecutionRequest request) {
    if (!belongsToProduct(sourceReference, product) || !hasPlanningContract(product)) {
      throw new IllegalStateException("O produto não possui contexto privado válido.");
    }
    Map<String, Object> evidence = request.structuredEvidence();
    String prototypeVersion = text(evidence.get("prototypeVersion"));
    String privateAccessUrl = text(evidence.get("privateAccessUrl"));
    String instrumentationReference = text(evidence.get("instrumentationReference"));
    String sourceEvidenceReference = text(evidence.get("sourceEvidenceReference"));
    String sourceEvaluatedAtText = text(evidence.get("sourceEvaluatedAt"));
    if (!VERSION.matcher(prototypeVersion).matches()) {
      throw new IllegalArgumentException(
          "Informe uma versão de 3 a 64 caracteres usando letras minúsculas, números, ponto, hífen ou sublinhado.");
    }
    if (!httpUrl(privateAccessUrl)) {
      throw new IllegalArgumentException(
          "Informe uma URL HTTP ou HTTPS válida para o acesso privado.");
    }
    if (instrumentationReference.length() < 3 || sourceEvidenceReference.length() < 3) {
      throw new IllegalArgumentException(
          "Informe referências auditáveis da instrumentação e das fontes comerciais vigentes.");
    }
    Instant acceptedAt = Instant.now();
    Instant sourceEvaluatedAt = sourceEvaluatedAt(sourceEvaluatedAtText, product.getId());
    int sourceMaxAgeDays = sourceMaxAgeDays(product);
    if (sourceEvaluatedAt.isAfter(acceptedAt)
        || !sourceEvaluatedAt.plusSeconds(sourceMaxAgeDays * 86_400L).isAfter(acceptedAt)) {
      throw new IllegalArgumentException(
          "A fonte comercial precisa estar vigente dentro do prazo congelado por Atena.");
    }
    for (String confirmation :
        List.of(
            "privateAccessConfirmed",
            "paymentDisabled",
            "publicationDisabled",
            "noMediaSpendConfirmed",
            "firstPartyEventsConfirmed",
            "desktopValidated",
            "mobileValidated")) {
      if (!Boolean.TRUE.equals(evidence.get(confirmation))) {
        throw new IllegalArgumentException(
            "Todas as confirmações técnicas e comerciais do protótipo são obrigatórias.");
      }
    }
    ObjectNode validation = object(product.getValidationDefinitionJson(), product.getId());
    ObjectNode acceptance = validation.putObject("privatePrototypeAcceptance");
    writeAcceptance(
        acceptance,
        prototypeVersion,
        privateAccessUrl,
        instrumentationReference,
        sourceEvidenceReference,
        sourceEvaluatedAt,
        request.evidenceReference(),
        acceptedAt);
    validation.put("purchaseMomentStatus", "WAITING_PRIVATE_READINGS");
    ObjectNode experience = object(product.getPdeExperienceJson(), product.getId());
    experience.put("status", "PRIVATE_PROTOTYPE_READY");
    ObjectNode experienceAcceptance = experience.putObject("privatePrototypeAcceptance");
    writeAcceptance(
        experienceAcceptance,
        prototypeVersion,
        privateAccessUrl,
        instrumentationReference,
        sourceEvidenceReference,
        sourceEvaluatedAt,
        request.evidenceReference(),
        acceptedAt);
    product.setValidationDefinitionJson(write(validation, product.getId()));
    product.setPdeExperienceJson(write(experience, product.getId()));
    products.save(product);
  }

  /** Grava o mesmo contrato de aceitação nas duas projeções funcionais do produto. */
  private void writeAcceptance(
      ObjectNode acceptance,
      String prototypeVersion,
      String privateAccessUrl,
      String instrumentationReference,
      String sourceEvidenceReference,
      Instant sourceEvaluatedAt,
      String acceptanceEvidenceReference,
      Instant acceptedAt) {
    acceptance.put("status", "READY");
    acceptance.put("prototypeVersion", prototypeVersion);
    acceptance.put("privateAccessUrl", privateAccessUrl);
    acceptance.put("instrumentationReference", instrumentationReference);
    acceptance.put("sourceEvidenceReference", sourceEvidenceReference);
    acceptance.put("sourceQualityPassed", true);
    acceptance.put("sourceQualityEvaluatedAt", sourceEvaluatedAt.toString());
    acceptance.put("acceptanceEvidenceReference", acceptanceEvidenceReference);
    acceptance.put("acceptedAt", acceptedAt.toString());
    acceptance.put("privateAccessConfirmed", true);
    acceptance.put("paymentEnabled", false);
    acceptance.put("published", false);
    acceptance.put("mediaSpendBrl", 0);
    acceptance.put("eventSource", "FIRST_PARTY_EVENTS");
    acceptance.put("testMarker", "PRIVATE_PROTOTYPE");
    acceptance.put("desktopValidated", true);
    acceptance.put("mobileValidated", true);
  }

  /** Confirma que o handoff contém plano, protótipo e limite sem cobrança. */
  private boolean hasPlanningContract(Product product) {
    if (product == null
        || !"PLANNED".equals(product.getCommercialStatus())
        || !"PDE_PRIVATE_VALIDATION_V1".equals(product.getValidationDefinitionVersion())
        || product.getValidationDefinitionJson() == null) {
      return false;
    }
    try {
      JsonNode definition = objectMapper.readTree(product.getValidationDefinitionJson());
      JsonNode plan = definition.path("privateValidationPlan");
      JsonNode signals = plan.path("requiredSignals");
      List<String> actualSignals = new ArrayList<>();
      signals.forEach(value -> actualSignals.add(value.isTextual() ? value.asText() : ""));
      int sourceMaxAgeDays = plan.path("sourceMaxAgeDays").asInt(0);
      return plan.isObject()
          && plan.path("minimumIndependentReadings").asInt(0) == 2
          && plan.path("minimumEligibleParticipantsPerReading").asInt(0) == 1
          && sourceMaxAgeDays >= 1
          && sourceMaxAgeDays <= 90
          && actualSignals.size() == PdePrivateReadingHumanActivityHandler.REQUIRED_SIGNALS.size()
          && Set.copyOf(actualSignals)
              .equals(Set.copyOf(PdePrivateReadingHumanActivityHandler.REQUIRED_SIGNALS))
          && definition.path("privatePrototype").isObject()
          && "SIMULATED_NO_CHARGE"
              .equals(definition.path("privatePrototype").path("checkoutMode").asText());
    } catch (Exception ex) {
      log.error("Falha ao ler o contrato do protótipo privado. productId={}", product.getId(), ex);
      return false;
    }
  }

  /** Lê o prazo de vigência predeclarado sem aceitar valor ausente ou fora do contrato. */
  private int sourceMaxAgeDays(Product product) {
    ObjectNode definition = object(product.getValidationDefinitionJson(), product.getId());
    int days = definition.path("privateValidationPlan").path("sourceMaxAgeDays").asInt(0);
    if (days < 1 || days > 90) {
      throw new IllegalStateException("O prazo das fontes comerciais não foi congelado por Atena.");
    }
    return days;
  }

  /** Converte o instante informado pela tela e registra a falha antes de rejeitar o gate. */
  private Instant sourceEvaluatedAt(String value, Long productId) {
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException ex) {
      log.error("Data inválida da fonte comercial no gate privado. productId={}", productId, ex);
      throw new IllegalArgumentException("Informe a data válida da fonte comercial.", ex);
    }
  }

  /** Impede que uma referência de outro produto aceite este protótipo. */
  private boolean belongsToProduct(String sourceReference, Product product) {
    return product != null
        && product.getId() != null
        && sourceReference != null
        && sourceReference.equals("product:" + product.getId() + "@private-validation-v1");
  }

  /** Aceita somente URL explícita que o revisor visual possa abrir. */
  private boolean httpUrl(String value) {
    try {
      URI uri = URI.create(value);
      return uri.getHost() != null
          && ("http".equalsIgnoreCase(uri.getScheme())
              || "https".equalsIgnoreCase(uri.getScheme()));
    } catch (IllegalArgumentException ex) {
      log.warn("URL inválida recebida no gate do protótipo privado.", ex);
      return false;
    }
  }

  /** Lê um JSON de produto sem ocultar contrato corrompido. */
  private ObjectNode object(String raw, Long productId) {
    try {
      JsonNode value = objectMapper.readTree(raw);
      if (value instanceof ObjectNode object) return object;
      throw new IllegalArgumentException("JSON não representa objeto.");
    } catch (Exception ex) {
      log.error("Falha ao ler contrato privado. productId={}", productId, ex);
      throw new IllegalStateException("Não foi possível ler o contrato privado.", ex);
    }
  }

  /** Serializa a atualização do contrato com contexto operacional. */
  private String write(JsonNode value, Long productId) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception ex) {
      log.error("Falha ao serializar contrato privado. productId={}", productId, ex);
      throw new IllegalStateException("Não foi possível persistir o contrato privado.", ex);
    }
  }

  /** Normaliza um campo textual recebido no formulário especializado. */
  private String text(Object value) {
    return value == null ? "" : String.valueOf(value).trim();
  }
}
