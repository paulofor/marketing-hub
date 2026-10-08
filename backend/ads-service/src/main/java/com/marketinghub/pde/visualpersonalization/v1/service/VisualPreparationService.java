package com.marketinghub.pde.visualpersonalization.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.imagegenerator.ImageGenerationRequest;
import com.marketinghub.pde.visualpersonalization.v1.service.contract.VisualPreparationContract.Create;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.imagegenerator.ImageGenerationRequestRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: abrir e recuperar a entrega privada visual com identidade e entrada
 * persistidas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VisualPreparationService {
  public static final String PREFIX = "pde-visual-v1-";
  public static final String VERSION = "PDE_VISUAL_PERSONALIZATION_V1";
  private final ProductRepository products;
  private final CommercialPlanRepository plans;
  private final ExperimentRepository experiments;
  private final ImageGenerationRequestRepository images;
  private final VisualPreparationBudget budgets;
  private final ObjectMapper json;

  @Value("${image-generator.openai.model:gpt-5.6}")
  private String model;

  /** Responsabilidade: conservar o produto e plano já conferidos na mesma transação. */
  public record Scope(Product product, CommercialPlan plan) {}

  /** Expõe o orçamento e a versão atuais, mantendo a decisão comercial separada da homologação. */
  @Transactional(readOnly = true)
  public JsonNode context(Long productId, Long planId, Long experimentId) {
    var scope = scope(productId, planId, experimentId, false);
    ObjectNode result = json.createObjectNode();
    result.put("productId", productId);
    result.put("productName", scope.product().getName());
    result.put("commercialPlanId", planId);
    result.put("experimentId", experimentId);
    result.put("contractVersion", VERSION);
    result.put(
        "publicUrl",
        read(scope.product().getPdeExperienceJson())
            .path("privatePrototypeAcceptance")
            .path("privateAccessUrl")
            .asText());
    result.set(
        "budget",
        json.valueToTree(budgets.read(scope.product(), scope.plan(), experimentId, null)));
    result.put("published", false);
    result.put("paymentEnabled", false);
    result.put("trafficClass", "AGENT_VALIDATION");
    latestSummary(productId, planId, experimentId)
        .ifPresent(proof -> result.set("latestPreparation", proof));
    return result;
  }

  /** Expõe a prova técnica do contexto exato sem imagem, credencial ou parecer fabricado. */
  @Transactional(readOnly = true)
  public java.util.Optional<ObjectNode> latestSummary(
      Long productId, Long planId, Long experimentId) {
    return images
        .findFirstByProductIdAndCommercialPlanIdAndExperimentIdAndJobIdStartingWithOrderByCreatedAtDescIdDesc(
            productId, planId, experimentId, PREFIX)
        .map(
            row -> {
              var result = view(row, false);
              result.remove("input");
              return result;
            });
  }

  /** Enfileira uma única tentativa sintética após conferir consentimento, origem e orçamento. */
  public JsonNode create(Long productId, Create input) {
    var scope = scope(productId, input.commercialPlanId(), input.experimentId(), true);
    String operation = PREFIX + input.operationKey();
    String serializedInput = write(input.input());
    String hash = VisualPreparationBudget.hash(serializedInput);
    var previous =
        images.findByProductIdAndCommercialPlanIdAndExperimentIdAndBatchJobId(
            productId, input.commercialPlanId(), input.experimentId(), operation);
    if (previous.isPresent()) {
      var row = previous.get();
      var saved = read(row.getPrompt());
      require(
          saved.path("inputHash").asText().equals(hash),
          "Esta operação já identifica outra entrada.");
      authorize(row, input.accessToken());
      return view(row);
    }
    var budget = budgets.read(scope.product(), scope.plan(), input.experimentId(), null);
    require(
        budget.authorizationHash().equals(input.authorizationHash())
            && budget.productVersion().equals(input.productVersion()),
        "A autorização ou a versão mudou; releia a preparação sem reutilizar outra origem.");
    require(
        budget.costComplete() && budget.availableUsd().signum() > 0,
        budget.blocker() == null ? "O orçamento não pode ser conferido." : budget.blocker());
    require(
        !images.existsByProductIdAndExperimentIdAndStatusIn(
            productId,
            input.experimentId(),
            List.of("PDE_QUEUED", "PDE_RUNNING", "PDE_RAW_RECEIVED")),
        "Já existe geração em andamento neste contexto; recupere a entrega preservada.");
    require(
        input.syntheticConsent(),
        "Confirme que a entrada é sintética e usada somente na homologação.");
    require(
        !serializedInput
            .toLowerCase(java.util.Locale.ROOT)
            .matches(
                "(?s).*(emagrec|peso corporal|cirurgia|diagnóstico|nudez|foto corporal|foto do corpo|avaliar meu corpo).*"),
        "A entrada deve tratar apenas ocasião, peças e restrições práticas.");
    ObjectNode context = json.createObjectNode();
    context.put("contractVersion", VERSION);
    context.put("productName", scope.product().getName());
    context.put("productVersion", budget.productVersion());
    context.put("authorizationHash", budget.authorizationHash());
    context.put("authorizedSince", budget.authorizedSince().toString());
    context.put("maximumTotalUsd", budget.maximumUsd());
    context.put("inputHash", hash);
    context.put("sessionHash", VisualPreparationBudget.hash(input.accessToken()));
    context.put("trafficClass", "AGENT_VALIDATION");
    context.set("input", json.valueToTree(input.input()));
    var row = new ImageGenerationRequest();
    row.setProductId(productId);
    row.setCommercialPlanId(input.commercialPlanId());
    row.setExperimentId(input.experimentId());
    row.setJobId(PREFIX + UUID.randomUUID());
    row.setBatchJobId(operation);
    row.setStatus("PDE_QUEUED");
    row.setModel(model);
    row.setServiceTier("flex");
    row.setOutputFormat("png");
    row.setPrompt(write(context));
    row.setCreatedAt(Instant.now());
    images.saveAndFlush(row);
    log.info(
        "Entrada sintética PDE enfileirada productId={} experimentId={} jobId={} input={}",
        productId,
        input.experimentId(),
        row.getJobId(),
        input.input());
    return view(row);
  }

  /** Recupera exatamente a sessão e sua imagem, sem reenfileirar nem acessar o provedor. */
  @Transactional(readOnly = true)
  public JsonNode session(String jobId, String secret) {
    var row =
        images.findFirstByJobId(jobId).orElseThrow(() -> fail(404, "Preparação não encontrada."));
    authorize(row, secret);
    return view(row);
  }

  /** Confere produto em PLAY, experimento planejado e plano diretamente vinculado. */
  public Scope scope(Long productId, Long planId, Long experimentId, boolean lock) {
    var product =
        (lock ? products.findLockedById(productId) : products.findById(productId))
            .orElseThrow(() -> fail(404, "Produto não encontrado."));
    require(
        !Boolean.FALSE.equals(product.getAutomaticExecutionEnabled()), "O produto está em STOP.");
    var plan = plans.findById(planId).orElseThrow(() -> fail(404, "Plano não encontrado."));
    var experiment =
        experiments
            .findById(experimentId)
            .orElseThrow(() -> fail(404, "Experimento não encontrado."));
    require(
        experiment.getProduct() != null
            && productId.equals(experiment.getProduct().getId())
            && "PLANNED".equals(experiment.getStatus().name())
            && plan.getExperiment() != null
            && experimentId.equals(plan.getExperiment().getId()),
        "A homologação exige produto, plano e experimento PLANNED da mesma origem.");
    return new Scope(product, plan);
  }

  /** Confere a credencial opaca da própria preparação sem expor a credencial persistida. */
  public void authorize(ImageGenerationRequest row, String secret) {
    require(row.getJobId().startsWith(PREFIX), "A tentativa não pertence à preparação PDE.");
    String expected = read(row.getPrompt()).path("sessionHash").asText();
    if (secret == null
        || expected.isBlank()
        || !MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            VisualPreparationBudget.hash(secret).getBytes(StandardCharsets.UTF_8)))
      throw fail(401, "Acesso inválido à preparação.");
  }

  /** Separa saída utilizável e dados funcionais da resposta bruta e dos segredos. */
  public ObjectNode view(ImageGenerationRequest row) {
    return view(row, true);
  }

  /** Monta relatório funcional e carrega o artefato somente quando o consumidor precisa dele. */
  private ObjectNode view(ImageGenerationRequest row, boolean includeImage) {
    var context = read(row.getPrompt());
    ObjectNode result = json.createObjectNode();
    result.put("jobId", row.getJobId());
    result.put("productId", row.getProductId());
    result.put("experimentId", row.getExperimentId());
    result.put("productName", context.path("productName").asText());
    result.put("productVersion", context.path("productVersion").asText());
    result.put("contractVersion", VERSION);
    result.put("status", row.getStatus());
    result.put("inputHash", context.path("inputHash").asText());
    result.put("createdAt", row.getCreatedAt().toString());
    if (row.getFinishedAt() != null) result.put("finishedAt", row.getFinishedAt().toString());
    result.set("input", context.path("input"));
    result.put("trafficClass", "AGENT_VALIDATION");
    result.put("published", false);
    result.put("paymentEnabled", false);
    result.put("error", row.getErrorMessage());
    result.set("estimatedCostUsd", context.path("estimatedCostUsd"));
    if (context.has("providerCalled"))
      result.put("providerCalls", context.path("providerCalled").asBoolean() ? 1 : 0);
    else if (row.getOpenAiRequestBody() == null) result.put("providerCalls", 0);
    else result.putNull("providerCalls");
    result.put("functionalReviewStatus", "PENDING_INDEPENDENT_REVIEW");
    if (includeImage
        && context.path("imageValidated").asBoolean()
        && row.getOpenAiResponseBody() != null) {
      for (var output : read(row.getOpenAiResponseBody()).path("output")) {
        if ("image_generation_call".equals(output.path("type").asText()))
          result.put("imageBase64", output.path("result").asText());
      }
    }
    return result;
  }

  /** Lê dados persistidos e registra corrupção sem descartá-los. */
  public JsonNode read(String value) {
    try {
      return json.readTree(value);
    } catch (Exception ex) {
      log.error("Falha ao ler auditoria de preparação PDE.", ex);
      throw fail(409, "A auditoria preservada não pode ser lida; não repita a geração.");
    }
  }

  /** Serializa contexto auditável antes de enfileirar ou receber um resultado. */
  public String write(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (Exception ex) {
      log.error("Falha ao serializar preparação PDE.", ex);
      throw fail(409, "A preparação não pôde ser registrada.");
    }
  }

  /** Recusa avanço sem a condição funcional necessária. */
  public static void require(boolean condition, String message) {
    if (!condition) throw fail(409, message);
  }

  /** Preserva status e causa acionável do contrato. */
  public static ResponseStatusException fail(int status, String message) {
    return new ResponseStatusException(HttpStatus.valueOf(status), message);
  }
}
