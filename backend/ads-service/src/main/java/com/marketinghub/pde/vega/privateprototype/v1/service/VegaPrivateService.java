package com.marketinghub.pde.vega.privateprototype.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.pde.vega.privateprototype.v1.*;
import com.marketinghub.pde.vega.privateprototype.v1.service.contract.VegaPrivateContract.*;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.vega.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: persistir a experiência privada do Vega, seus sinais e a fila de geração. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VegaPrivateService {
  private final VegaPrivateSessionRepository sessions;
  private final VegaAdjustmentExecutionRepository executions;
  private final LearningSalesCycleRepository cycles;
  private final ProductRepository products;
  private final ObjectMapper json;

  @Value("${PDE_VEGA_PROTOTYPE_VERSION:musa-pde-entry-v12-primeiro-ajuste-aplicavel}")
  private String version;

  private static final Set<String> TERMINAL = Set.of("COMPLETED", "FAILED", "BLOCKED");
  private static final List<String> SIGNALS =
      List.of(
          "EXPERIENCE_STARTED",
          "VALUE_MOMENT",
          "READY_RESULT_USED",
          "PREFERRED_OVER_FREE",
          "CHECKOUT_STARTED");

  /** Informa apenas capacidades implementadas, sem abrir sessão nem emitir eventos. */
  @Transactional(readOnly = true)
  public JsonNode contract() {
    return json.valueToTree(
        Map.of(
            "productSlug",
            "metodo-musa-7-dias",
            "prototypeVersion",
            version,
            "requiredSignals",
            SIGNALS,
            "checkoutMode",
            "SIMULATED_NO_CHARGE",
            "paymentEnabled",
            false,
            "published",
            false,
            "mediaSpendBrl",
            0,
            "maxValueTimeMinutes",
            5));
  }

  /** Cria acesso sintético ou convite humano, isolado por leitura, ciclo e versão. */
  public JsonNode create(InternalSession input) {
    var cycle =
        cycles
            .findLockedById(input.cycleId())
            .orElseThrow(() -> fail(404, "Ciclo não encontrado."));
    var product = products.findById(cycle.getProductId()).orElseThrow();
    require(
        "metodo-musa-7-dias".equals(product.getSlug())
            && "OPEN".equals(cycle.getStatus())
            && !cycle.isBaseline()
            && Set.of("ADJUSTMENT", "VALIDATION").contains(cycle.getStage()),
        "A leitura exige um ciclo privado aberto do Vega na fase de ajuste ou validação.");
    require(
        version.equals(input.prototypeVersion()) && version.equals(cycle.getProductVersion()),
        "A versão do runtime não corresponde à versão declarada neste ciclo.");
    require(
        Set.of("QA_INTERNAL", "AGENT_VALIDATION", "HUMAN").contains(input.origin()),
        "Origem inválida.");
    if ("HUMAN".equals(input.origin())) {
      require(
          input.readingNumber() != null && input.readingNumber() >= 1 && input.readingNumber() <= 2,
          "Selecione uma das duas leituras privadas.");
      require(
          !sessions.existsByCycleIdAndPrototypeVersionAndOriginAndReadingNumberAndRevokedFalse(
              cycle.getId(), version, "HUMAN", input.readingNumber()),
          "Esta leitura já possui um convite ativo.");
    }
    var row = new VegaPrivateSession();
    row.setId(UUID.randomUUID().toString());
    row.setCycleId(cycle.getId());
    row.setProductId(product.getId());
    row.setExperimentId(cycle.getExperimentId());
    row.setPrototypeVersion(version);
    row.setOrigin(input.origin());
    row.setReadingNumber(input.readingNumber());
    row.setState("NEW");
    row.setCreatedAt(Instant.now());
    row.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
    row.setEventsJson("{}");
    String secret = token();
    if ("HUMAN".equals(input.origin())) row.setGrantHash(hash(secret));
    else row.setSessionHash(hash(secret));
    sessions.saveAndFlush(row);
    ObjectNode response = view(row);
    response.put("HUMAN".equals(input.origin()) ? "accessToken" : "sessionToken", secret);
    log.info(
        "Vega criou leitura privada cycleId={} sessionId={} origin={} version={}",
        cycle.getId(),
        row.getId(),
        row.getOrigin(),
        version);
    return response;
  }

  /** Troca convite por sessão após consentimento e restaura a leitura sem criar outra. */
  public JsonNode access(Access request) {
    require(request.consentAccepted(), "Confirme o consentimento para começar.");
    var row =
        sessions
            .findByGrantHash(hash(request.accessToken()))
            .orElseThrow(() -> fail(401, "Acesso inválido."));
    active(row);
    String secret = token();
    row.setSessionHash(hash(secret));
    if (row.getConsentedAt() == null) row.setConsentedAt(Instant.now());
    ObjectNode response = view(row);
    response.put("sessionToken", secret);
    return response;
  }

  /** Retorna o estado persistido da própria leitura sem regenerar o cartão. */
  public JsonNode session(String secret) {
    return view(sessionRow(secret));
  }

  /** Registra o início explícito; abrir a página nunca equivale a iniciar a experiência. */
  public JsonNode start(String secret) {
    var row = sessionRow(secret);
    if (row.getConsentedAt() == null) row.setConsentedAt(Instant.now());
    emit(row, "EXPERIENCE_STARTED", "Começar");
    if ("NEW".equals(row.getState())) row.setState("INPUT");
    return view(row);
  }

  /** Persiste a entrada e enfileira uma tentativa, sem acessar o modelo no backend. */
  public JsonNode generate(String secret, Input input) {
    var row = sessionRow(secret);
    require(
        events(row).has("EXPERIENCE_STARTED"), "Comece a experiência antes de criar seu ajuste.");
    if (row.getExecutionId() != null) {
      var previous = executions.findById(row.getExecutionId()).orElseThrow();
      if (!TERMINAL.contains(previous.getStatus()) || "COMPLETED".equals(previous.getStatus()))
        return view(row);
    }
    log.info(
        "Vega entrada bruta sessionId={} cycleId={} payload={}",
        row.getId(),
        row.getCycleId(),
        input);
    row.setInputJson(write(input));
    var execution = new VegaAdjustmentExecution();
    execution.setSessionId(row.getId());
    execution.setStatus("QUEUED");
    execution.setCreatedAt(Instant.now());
    ObjectNode context = json.createObjectNode();
    context.put("sessionId", row.getId());
    context.put("cycleId", row.getCycleId());
    context.put("experimentId", row.getExperimentId());
    context.put("productId", row.getProductId());
    context.put("prototypeVersion", row.getPrototypeVersion());
    context.set("input", json.valueToTree(input));
    context.put("origin", row.getOrigin());
    var cycle = cycles.findById(row.getCycleId()).orElseThrow();
    context.set("inheritedLearning", read(cycle.getInheritedLearningJson()));
    execution.setInputJson(write(context));
    executions.saveAndFlush(execution);
    row.setExecutionId(execution.getId());
    row.setState("GENERATING");
    return view(row);
  }

  /** Aceita sinais somente depois das ações e pré-requisitos funcionais correspondentes. */
  public JsonNode event(String secret, Event input) {
    var row = sessionRow(secret);
    ObjectNode ledger = events(row);
    var execution =
        row.getExecutionId() == null
            ? null
            : executions.findById(row.getExecutionId()).orElse(null);
    require(
        execution != null && "COMPLETED".equals(execution.getStatus()),
        "O cartão completo precisa estar disponível.");
    String event = input.eventType();
    String answer = input.answer() == null ? "" : input.answer().trim();
    switch (event) {
      case "VALUE_MOMENT" ->
          require(!answer.isBlank(), "Confirme se entendeu como aplicar o ajuste.");
      case "READY_RESULT_USED" ->
          require(
              ledger.has("VALUE_MOMENT") && !answer.isBlank(),
              "Aplique o ajuste e registre sua autoavaliação.");
      case "PREFERENCE" -> {
        require(
            ledger.has("READY_RESULT_USED") && Set.of("CARD", "FREE").contains(answer),
            "Escolha sua preferência após usar o resultado.");
        if (row.getPreference() != null)
          require(row.getPreference().equals(answer), "Sua escolha já foi registrada.");
        row.setPreference(answer);
        if ("CARD".equals(answer)) emit(row, "PREFERRED_OVER_FREE", answer);
        return view(row);
      }
      case "CHECKOUT_STARTED" ->
          require(ledger.has("READY_RESULT_USED"), "Experimente o ajuste antes da simulação.");
      default -> throw fail(400, "Ação não reconhecida.");
    }
    log.info("Vega evento bruto sessionId={} payload={}", row.getId(), input);
    emit(row, event, answer);
    return view(row);
  }

  /** Encerra a leitura preservando o cartão útil e todos os limites na retomada. */
  public JsonNode finish(String secret) {
    var row = sessionRow(secret);
    row.setState("FINISHED");
    return view(row);
  }

  /** Revoga somente o convite solicitado, preservando sua evidência histórica. */
  public void revoke(String id) {
    var row = sessions.findById(id).orElseThrow(() -> fail(404, "Leitura não encontrada."));
    row.setRevoked(true);
  }

  /** Expõe pendências e reservas expiradas para controle operacional pelo worker. */
  @Transactional(readOnly = true)
  public JsonNode pending() {
    return json.valueToTree(
        executions.pending(Instant.now(), PageRequest.of(0, 10)).stream()
            .map(this::executionView)
            .toList());
  }

  /** Reserva uma execução de forma atômica e não substitui um resultado terminal. */
  public JsonNode claim(Long id) {
    var row = executions.findLocked(id).orElseThrow(() -> fail(404, "Execução não encontrada."));
    if (TERMINAL.contains(row.getStatus())) return executionView(row);
    require(
        !"RUNNING".equals(row.getStatus())
            || row.getLeaseUntil() == null
            || row.getLeaseUntil().isBefore(Instant.now()),
        "Execução já reservada.");
    row.setStatus("RUNNING");
    row.setStartedAt(Instant.now());
    row.setLeaseUntil(Instant.now().plus(10, ChronoUnit.MINUTES));
    return executionView(row);
  }

  /** Preserva a requisição sanitizada antes de o worker acessar o provedor. */
  public void request(Long id, RequestAudit input) {
    var row = executions.findLocked(id).orElseThrow();
    require("RUNNING".equals(row.getStatus()), "Execução não está reservada.");
    require(
        !input.request().has("api_key") && !input.request().has("headers"),
        "Não envie credenciais na auditoria.");
    row.setRequestJson(write(input.request()));
    row.setModel(input.model());
  }

  /** Aplica o callback idempotente e valida o artefato antes de disponibilizá-lo à cliente. */
  public JsonNode complete(Long id, Result input) {
    var row = executions.findLocked(id).orElseThrow(() -> fail(404, "Execução não encontrada."));
    if (TERMINAL.contains(row.getStatus())) return executionView(row);
    require(
        "RUNNING".equals(row.getStatus()) && TERMINAL.contains(input.status()),
        "Estado de callback inválido.");
    log.info("Vega callback bruto executionId={} payload={}", id, input);
    row.setResponseJson(write(input.rawResponse()));
    row.setModel(input.model());
    row.setInputTokens(input.inputTokens());
    row.setOutputTokens(input.outputTokens());
    row.setCostUsd(input.costUsd());
    String status = input.status();
    String error = input.error();
    if ("COMPLETED".equals(status)
        && (row.getRequestJson() == null
            || !validCard(input.card())
            || !String.valueOf(id).equals(input.card().path("cardId").asText())
            || !read(row.getInputJson())
                .path("input")
                .path("occasion")
                .asText()
                .equals(input.card().path("occasion").asText()))) {
      status = "FAILED";
      error =
          "O modelo não entregou um cartão completo e seguro; nenhum sinal de valor foi registrado.";
    }
    row.setStatus(status);
    row.setError(error);
    row.setFinishedAt(Instant.now());
    if ("COMPLETED".equals(status)) row.setResultJson(write(input.card()));
    return executionView(row);
  }

  /** Publica o relatório auditável por leitura e tentativa, com origem e sinais separados. */
  @Transactional(readOnly = true)
  public JsonNode report(Long cycleId) {
    var rows = sessions.findByCycleIdOrderByCreatedAtAsc(cycleId);
    var result = json.createObjectNode();
    result.put("cycleId", cycleId);
    var readings = result.putArray("readings");
    for (var row : rows) {
      var item = view(row);
      item.remove("input");
      item.put("revoked", row.isRevoked());
      var attempts = item.putArray("executions");
      executions
          .findBySessionIdOrderByIdAsc(row.getId())
          .forEach(e -> attempts.add(executionView(e)));
      readings.add(item);
    }
    result.put(
        "humanReadings",
        rows.stream()
            .filter(r -> "HUMAN".equals(r.getOrigin()) && r.getConsentedAt() != null)
            .count());
    result.put("testReadings", rows.stream().filter(r -> !"HUMAN".equals(r.getOrigin())).count());
    result.put("sales", 0);
    result.put("revenueBrl", 0);
    return result;
  }

  /** Valida o contrato mínimo do cartão e impede recomendação dependente de aquisição. */
  static boolean validCard(JsonNode card) {
    if (card == null || !card.isObject() || !card.path("usesOnlyAvailableItems").asBoolean(false))
      return false;
    Set<String> required =
        Set.of(
            "cardId",
            "action",
            "application",
            "occasion",
            "selfAssessmentPrompt",
            "usesOnlyAvailableItems");
    Set<String> actual = new HashSet<>();
    card.fieldNames().forEachRemaining(actual::add);
    if (!actual.equals(required)) return false;
    for (String key : required)
      if (!key.equals("usesOnlyAvailableItems")
          && (!card.path(key).isTextual()
              || card.path(key).asText().isBlank()
              || card.path(key).asText().length() > 1000)) return false;
    return !java.util.regex.Pattern.compile(
            "(?iu)\\b(compre|comprar|adquira|adquirir|emagreça|emagrecer)\\b")
        .matcher(card.toString())
        .find();
  }

  /** Monta a visão funcional sem vazar convite, sessão, prompt ou resposta bruta. */
  private ObjectNode view(VegaPrivateSession row) {
    var out = json.createObjectNode();
    out.put("id", row.getId());
    out.put("cycleId", row.getCycleId());
    out.put("productId", row.getProductId());
    out.put("experimentId", row.getExperimentId());
    out.put("prototypeVersion", row.getPrototypeVersion());
    out.put("origin", row.getOrigin());
    out.put("state", row.getState());
    out.set("events", events(row));
    out.set(
        "input", row.getInputJson() == null ? json.createObjectNode() : read(row.getInputJson()));
    out.put("preference", row.getPreference());
    if (row.getExecutionId() != null) {
      var execution = executions.findById(row.getExecutionId()).orElseThrow();
      out.put("executionId", execution.getId());
      out.put("generationStatus", execution.getStatus());
      out.put("error", execution.getError());
      out.set("card", execution.getResultJson() == null ? null : read(execution.getResultJson()));
    }
    out.put("checkoutMode", "SIMULATED_NO_CHARGE");
    out.put("priceBrl", 67);
    out.put("paymentEnabled", false);
    out.put("published", false);
    out.put("mediaSpendBrl", 0);
    return out;
  }

  /** Monta auditoria interna estruturada sem serializar JSON dentro de JSON. */
  private JsonNode executionView(VegaAdjustmentExecution row) {
    ObjectNode out = json.createObjectNode();
    out.put("id", row.getId());
    out.put("sessionId", row.getSessionId());
    out.put("status", row.getStatus());
    out.set("context", read(row.getInputJson()));
    out.set("request", row.getRequestJson() == null ? null : read(row.getRequestJson()));
    out.set("rawResponse", row.getResponseJson() == null ? null : read(row.getResponseJson()));
    out.set("card", row.getResultJson() == null ? null : read(row.getResultJson()));
    out.put("error", row.getError());
    out.put("model", row.getModel());
    out.set("inputTokens", json.valueToTree(row.getInputTokens()));
    out.set("outputTokens", json.valueToTree(row.getOutputTokens()));
    out.set("costUsd", json.valueToTree(row.getCostUsd()));
    out.put("costStatus", row.getCostUsd() == null ? "NOT_PROVIDED" : "REPORTED");
    out.put("createdAt", row.getCreatedAt().toString());
    if (row.getStartedAt() != null) out.put("startedAt", row.getStartedAt().toString());
    if (row.getFinishedAt() != null) out.put("finishedAt", row.getFinishedAt().toString());
    return out;
  }

  /** Resolve e bloqueia o acesso inválido, expirado ou revogado antes de qualquer leitura. */
  private VegaPrivateSession sessionRow(String secret) {
    var row =
        sessions
            .findBySessionHash(hash(secret))
            .orElseThrow(
                () -> fail(401, "Seu acesso não é válido. Use o convite privado novamente."));
    active(row);
    return row;
  }

  /** Aplica o prazo do convite sem confundir validade com aprovação comercial. */
  private void active(VegaPrivateSession row) {
    if (row.isRevoked() || row.getExpiresAt().isBefore(Instant.now()))
      throw fail(401, "Este acesso foi encerrado ou expirou.");
  }

  /** Registra cada sinal uma única vez após a ação explícita validada. */
  private void emit(VegaPrivateSession row, String name, String answer) {
    ObjectNode ledger = events(row);
    if (ledger.has(name)) return;
    ledger
        .putObject(name)
        .put("at", Instant.now().toString())
        .put("action", answer)
        .put("origin", row.getOrigin());
    row.setEventsJson(write(ledger));
  }

  /** Lê o ledger da sessão sem transformar ausência de sinal em sucesso. */
  private ObjectNode events(VegaPrivateSession row) {
    return (ObjectNode) read(row.getEventsJson());
  }

  /** Lê somente JSON persistido e registra falha com rastreabilidade. */
  private JsonNode read(String value) {
    try {
      return json.readTree(value);
    } catch (Exception ex) {
      log.error("Vega: falha na leitura de contrato persistido", ex);
      throw new IllegalStateException("Contrato privado ilegível.", ex);
    }
  }

  /** Serializa a auditoria e preserva a exceção completa em caso de falha. */
  private String write(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (Exception ex) {
      log.error("Vega: falha na persistência de contrato", ex);
      throw new IllegalStateException("Contrato privado inválido.", ex);
    }
  }

  /** Gera credencial opaca com entropia suficiente para convites privados. */
  private String token() {
    byte[] value = new byte[32];
    new SecureRandom().nextBytes(value);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
  }

  /** Mantém no banco apenas o hash da credencial e não registra seu valor. */
  private String hash(String value) {
    if (value == null || value.isBlank()) throw fail(401, "Acesso necessário.");
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      log.error("Vega: falha no hash de acesso", ex);
      throw new IllegalStateException(ex);
    }
  }

  /** Interrompe comandos incompatíveis com o estado da leitura. */
  private void require(boolean condition, String message) {
    if (!condition) throw fail(409, message);
  }

  /** Expõe falha acionável pela API preservando o código HTTP. */
  private ResponseStatusException fail(int status, String message) {
    return new ResponseStatusException(HttpStatus.valueOf(status), message);
  }
}
