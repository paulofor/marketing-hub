package com.marketinghub.pde.vega.style.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.pde.vega.privateprototype.v1.VegaPrivateSession;
import com.marketinghub.pde.vega.style.v1.service.contract.VegaStyleContract.*;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.vega.VegaPrivateSessionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Persiste e protege a jornada contextual privada, reutilizando sessões e locks canônicos de Vega.
 */
@Service
@Transactional
@Slf4j
public class VegaStyleService {
  public static final String VERSION = VegaContextualStyleRules.VERSION;
  public static final String NEXT_VERSION = "musa-pde-entry-v17-continuidade-clara";
  private static final Set<String> SUPPORTED_VERSIONS = Set.of(VERSION, NEXT_VERSION);
  private final VegaPrivateSessionRepository sessions;
  private final LearningSalesCycleRepository cycles;
  private final ProductRepository products;
  private final ObjectMapper json;
  private final JsonNode product;
  private static final Set<String> SIGNALS =
      Set.of(
          "EXPERIENCE_STARTED",
          "VALUE_MOMENT",
          "READY_RESULT_USED",
          "PREFERRED_OVER_FREE",
          "CHECKOUT_STARTED");

  /** Recebe os repositórios existentes e carrega o contrato imutável da nova versão. */
  public VegaStyleService(
      VegaPrivateSessionRepository sessions,
      LearningSalesCycleRepository cycles,
      ProductRepository products,
      ObjectMapper json) {
    this.sessions = sessions;
    this.cycles = cycles;
    this.products = products;
    this.json = json;
    try (var stream = getClass().getResourceAsStream("/pde/vega/style/v1/product.json")) {
      this.product = json.readTree(Objects.requireNonNull(stream));
    } catch (Exception ex) {
      log.error("Vega: falha ao carregar contrato contextual version={}", VERSION, ex);
      throw new IllegalStateException("Contrato contextual indisponível", ex);
    }
  }

  /** Informa versões compatíveis sem criar sessão, custo, migração ou aceite de homologação. */
  @Transactional(readOnly = true)
  public JsonNode contract() {
    ObjectNode response = json.valueToTree(
        Map.of(
            "prototypeVersion",
            VERSION,
            "supportedPrototypeVersions",
            SUPPORTED_VERSIONS,
            "model",
            VegaContextualStyleRules.MODEL,
            "generationMode",
            "DETERMINISTIC_CONTEXTUAL_RULES",
            "requiredSignals",
            SIGNALS,
            "inputComparisonApplicable",
            false,
            "providerCalls",
            0,
            "sessionLimit",
            18,
            "trafficClass",
            "AGENT_VALIDATION",
            "internalMarker",
            "mh_internal_test"));
    response.putArray("optionalReflectionPrototypeVersions").add(NEXT_VERSION);
    return response;
  }

  /**
   * Reserva uma sessão sintética sob lock do ciclo, preservando versão, histórico e limite do
   * runner.
   */
  public JsonNode create(Session request) {
    var cycle =
        cycles
            .findLockedById(request.cycleId())
            .orElseThrow(() -> fail(404, "Ciclo não encontrado."));
    var selected =
        products
            .findById(cycle.getProductId())
            .orElseThrow(() -> fail(404, "Produto não encontrado."));
    require(
        "metodo-musa-7-dias".equals(selected.getSlug())
            && "OPEN".equals(cycle.getStatus())
            && Set.of("ADJUSTMENT", "VALIDATION").contains(cycle.getStage())
            && !cycle.isBaseline()
            && SUPPORTED_VERSIONS.contains(request.prototypeVersion())
            && request.prototypeVersion().equals(cycle.getProductVersion()),
        "A sessão exige o ciclo aberto da versão contextual de Vega.");
    require(
        sessions.countByCycleIdAndPrototypeVersionAndOriginIn(
                cycle.getId(), request.prototypeVersion(), List.of("AGENT_VALIDATION"))
            < 18,
        "O limite desta homologação foi atingido. Preserve as provas existentes.");
    var row = new VegaPrivateSession();
    row.setId(UUID.randomUUID().toString());
    row.setCycleId(cycle.getId());
    row.setProductId(cycle.getProductId());
    row.setExperimentId(cycle.getExperimentId());
    row.setPrototypeVersion(request.prototypeVersion());
    row.setOrigin("AGENT_VALIDATION");
    row.setCreatedAt(Instant.now());
    row.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
    row.setState("NEW");
    row.setInputJson("{\"operations\":{},\"missions\":{},\"entitlement\":\"TRIAL\"}");
    row.setEventsJson("{}");
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    String secret = HexFormat.of().formatHex(bytes);
    row.setSessionHash(hash(secret));
    sessions.saveAndFlush(row);
    var view = evidence(row);
    view.put("sessionToken", secret);
    log.info(
        "Vega: sessão contextual criada cycleId={} sessionId={} version={}",
        row.getCycleId(),
        row.getId(),
        row.getPrototypeVersion());
    return view;
  }

  /**
   * Recupera a própria jornada com campos funcionais e sem credenciais ou artefatos de outra
   * sessão.
   */
  public JsonNode workspace(String secret) {
    var row = session(secret);
    var data = data(row);
    boolean full = fullAccess(data);
    ObjectNode response = json.createObjectNode();
    ObjectNode selectedProduct = product.deepCopy();
    selectedProduct.put("experienceVersion", row.getPrototypeVersion());
    ((ObjectNode) selectedProduct.path("commercialAccess"))
        .put("experienceVersion", row.getPrototypeVersion());
    response.set("product", selectedProduct);
    response.put("experienceVersion", row.getPrototypeVersion());
    response.put("reflectionRequiredForCompletion", reflectionRequired(row));
    response.put("email", "");
    response.put("accessSource", "AGENT_VALIDATION");
    response.put("subscriptionStatus", full ? "ACTIVE" : "TRIAL");
    response.put("started", !"NEW".equals(row.getState()));
    response.put(
        "progressPercent",
        (int)
            (data.path("missions").findValues("completed").stream()
                    .filter(JsonNode::asBoolean)
                    .count()
                * 100
                / 7));
    var completed = response.putArray("completedMissionIds");
    var interactions = response.putArray("missionInteractions");
    data.path("missions")
        .fields()
        .forEachRemaining(
            entry -> {
              if (entry.getValue().path("completed").asBoolean()) completed.add(entry.getKey());
              entry
                  .getValue()
                  .path("answers")
                  .fields()
                  .forEachRemaining(
                      answer -> {
                        var item = interactions.addObject();
                        item.put("missionId", entry.getKey());
                        item.put("questionKey", answer.getKey());
                        item.put("answerText", answer.getValue().asText());
                      });
            });
    response.set("latestResults", data.path("missions"));
    return response;
  }

  /** Registra o começo intencional e a ciência sintética, sem confundir carregamento com uso. */
  public JsonNode start(String secret) {
    var row = session(secret);
    if ("NEW".equals(row.getState())) row.setState("INPUT");
    if (row.getConsentedAt() == null) row.setConsentedAt(Instant.now());
    emit(row, "EXPERIENCE_STARTED", "");
    return workspace(secret);
  }

  /** Serializa operações pelo lock da sessão e recupera a mesma resposta sem executar novamente. */
  public JsonNode generate(String secret, String missionId, Generate request) {
    var row = session(secret);
    var data = data(row);
    authorizeMission(data, missionId);
    require(
        events(row).has("EXPERIENCE_STARTED"), "Comece a experiência antes de pedir a orientação.");
    require(
        row.getPrototypeVersion().equals(request.experienceVersion())
            && request.operationKey() != null
            && request.operationKey().matches("[A-Za-z0-9-]{16,80}"),
        "Operação ou versão inválida.");
    VegaStyleInput.validate(request.answers());
    ObjectNode operations = (ObjectNode) data.get("operations");
    JsonNode prior = operations.get(request.operationKey());
    if (prior != null) {
      require(
          missionId.equals(prior.path("missionId").asText())
              && json.valueToTree(request.answers()).equals(prior.path("input")),
          "Esta operação já possui outra entrada. Consulte o resultado preservado.");
      return prior.path("response");
    }
    require(
        !data.path("missions").path(missionId).path("completed").asBoolean(),
        "Este dia já ficou salvo. Continue para o próximo ou consulte seus registros.");
    var cycle =
        cycles
            .findLockedById(row.getCycleId())
            .orElseThrow(() -> fail(404, "Ciclo não encontrado."));
    require(
        "OPEN".equals(cycle.getStatus())
            && row.getPrototypeVersion().equals(cycle.getProductVersion())
            && Set.of("ADJUSTMENT", "VALIDATION").contains(cycle.getStage()),
        "O ciclo não permite novas operações desta versão.");
    long attempts =
        java.util.stream.StreamSupport.stream(operations.spliterator(), false)
            .filter(item -> missionId.equals(item.path("missionId").asText()))
            .count();
    require(
        attempts < 2,
        "Você já usou as tentativas disponíveis nesta prévia. Suas orientações ficaram guardadas.");
    var history = new LinkedHashMap<String, String>();
    data.path("missions")
        .fields()
        .forEachRemaining(
            entry ->
                entry
                    .getValue()
                    .path("answers")
                    .fields()
                    .forEachRemaining(
                        answer ->
                            history.put(
                                entry.getKey() + "." + answer.getKey(),
                                answer.getValue().asText())));
    log.info(
        "Vega: entrada bruta contextual sessionId={} missionId={} input={}",
        row.getId(),
        missionId,
        request.answers());
    Instant started = Instant.now();
    var decision = VegaContextualStyleRules.decide(missionId, request.answers(), history);
    ObjectNode response = json.createObjectNode();
    response.put("requestId", request.operationKey());
    response.put("status", decision.status());
    response.put("headline", decision.headline());
    response.put("summary", decision.summary());
    response.set("signals", json.valueToTree(decision.signals()));
    response.set("microActions", json.valueToTree(decision.actions()));
    response.put("errorMessage", decision.limitation());
    response.put(
        "caution", "Esta orientação usa somente o que você contou; você decide o que vale manter.");
    var operation = operations.putObject(request.operationKey());
    operation.put("missionId", missionId);
    operation.set("input", json.valueToTree(request.answers()));
    operation.set("decision", json.valueToTree(decision));
    operation.set("response", response);
    operation.put("startedAt", started.toString());
    operation.put("finishedAt", Instant.now().toString());
    operation.put("providerCalls", 0);
    operation.put("incrementalInferenceCostUsd", 0);
    var mission = ((ObjectNode) data.get("missions")).withObject("/" + missionId);
    mission.put("latestRequestId", request.operationKey());
    mission.set("answers", json.valueToTree(request.answers()));
    row.setInputJson(write(data));
    row.setState(decision.status());
    return response;
  }

  /**
   * Consulta um resultado somente dentro da sessão autenticada, incluindo bloqueios preservados.
   */
  public JsonNode result(String secret, String id) {
    var operation = data(session(secret)).path("operations").path(id);
    require(!operation.isMissingNode(), "Este resultado não está disponível neste acesso.");
    return operation.path("response");
  }

  /** Salva escolhas e comentário opcional da v17 somente para a orientação correspondente. */
  public void reflect(String secret, String missionId, Reflection request) {
    var row = session(secret);
    var data = data(row);
    authorizeMission(data, missionId);
    VegaStyleInput.validate(request.answers());
    var mission = data.path("missions").path(missionId);
    var operation = data.path("operations").path(mission.path("latestRequestId").asText());
    require(
        "COMPLETED".equals(operation.path("response").path("status").asText()),
        "Conclua uma orientação válida antes de registrar seu dia.");
    for (String field : List.of("occasion", "existingSelection", "stylePreference"))
      require(
          Objects.equals(
              request.answers().get(field), operation.path("input").path(field).asText()),
          "As escolhas mudaram. Gere a orientação correspondente antes de concluir.");
    require(
        !reflectionRequired(row) || !request.answers().getOrDefault("reflection", "").isBlank(),
        "Registre o que funcionou para você.");
    ((ObjectNode) mission).set("answers", json.valueToTree(request.answers()));
    row.setInputJson(write(data));
  }

  /** Conclui explicitamente uma orientação válida, respeitando a política de comentário da versão. */
  public void complete(String secret, String missionId) {
    var row = session(secret);
    var data = data(row);
    authorizeMission(data, missionId);
    var mission = data.path("missions").path(missionId);
    var operation = data.path("operations").path(mission.path("latestRequestId").asText());
    require(
        "COMPLETED".equals(operation.path("response").path("status").asText()),
        "Conclua uma orientação válida antes de registrar seu dia.");
    require(
        !reflectionRequired(row) || !mission.path("answers").path("reflection").asText().isBlank(),
        "Registre sua escolha antes de concluir o dia.");
    ((ObjectNode) mission).put("completed", true);
    row.setInputJson(write(data));
  }

  /** Preserva a exigência histórica da v16 sem transformar comentário opcional da v17 em gate. */
  private boolean reflectionRequired(VegaPrivateSession row) {
    return !NEXT_VERSION.equals(row.getPrototypeVersion());
  }

  /** Registra ações explícitas com pré-condições e impede opinião sintética pelo navegador. */
  public JsonNode event(String secret, Event request, boolean internal) {
    var row = session(secret);
    require(
        SIGNALS.contains(request.eventType()) && !"EXPERIENCE_STARTED".equals(request.eventType()),
        "Sinal inválido.");
    require(
        !"PREFERRED_OVER_FREE".equals(request.eventType()) || internal,
        "Sinal disponível somente para transporte técnico interno.");
    var response = result(secret, request.requestId());
    require(
        "COMPLETED".equals(response.path("status").asText()),
        "Não há orientação pronta para esta ação.");
    if ("READY_RESULT_USED".equals(request.eventType()))
      require(
          Objects.equals(
              request.requestId(), events(row).path("VALUE_MOMENT").path("requestId").asText()),
          "Confirme primeiro que entendeu esta orientação.");
    emit(row, request.eventType(), request.requestId());
    return json.valueToTree(
        Map.of("recorded", true, "checkoutMode", "SIMULATED_NO_CHARGE", "paymentEnabled", false));
  }

  /** Exporta escolhas e orientações da própria jornada somente com direito integral vigente. */
  public String material(String secret, String name) {
    var row = session(secret);
    var data = data(row);
    require(fullAccess(data), "O material exige acesso completo vigente.");
    require(
        Set.of("mapa-do-meu-estilo.txt", "antes-de-sair.txt", "minhas-formulas.txt").contains(name),
        "Material não encontrado.");
    StringBuilder text =
        new StringBuilder(
            switch (name) {
              case "mapa-do-meu-estilo.txt" ->
                  "Mapa do meu estilo\nMinhas escolhas e o que aprendi ao experimentar.\n";
              case "antes-de-sair.txt" ->
                  "Antes de sair, do meu jeito\nConfira ocasião, peças disponíveis e o que faz sentido para você.\n";
              default ->
                  "Minhas fórmulas para a semana\nEscolha o que quer repetir, adaptar ou deixar de lado.\n";
            });
    data.path("missions")
        .fields()
        .forEachRemaining(
            entry -> {
              var answers = entry.getValue().path("answers");
              text.append("\nDia ")
                  .append(entry.getKey().replace("estilo-dia-", ""))
                  .append("\nOcasião: ")
                  .append(answers.path("occasion").asText())
                  .append("\nMinhas peças: ")
                  .append(answers.path("existingSelection").asText())
                  .append("\nMinha preferência: ")
                  .append(answers.path("stylePreference").asText())
                  .append("\nMeu registro: ")
                  .append(answers.path("reflection").asText())
                  .append("\n");
              if (NEXT_VERSION.equals(row.getPrototypeVersion())) {
                var response =
                    data.path("operations")
                        .path(entry.getValue().path("latestRequestId").asText())
                        .path("response");
                if ("COMPLETED".equals(response.path("status").asText())) {
                  text.append("Minha orientação: ")
                      .append(response.path("summary").asText())
                      .append("\n");
                  response
                      .path("microActions")
                      .forEach(action -> text.append("- ").append(action.asText()).append("\n"));
                }
              }
            });
    return text.toString();
  }

  /** Controla direitos e expirações exclusivamente por fixtures internas, sem compra ou gateway. */
  public JsonNode fixture(String secret, String action) {
    var row = session(secret);
    var data = data(row);
    switch (action) {
      case "full-access" -> {
        data.put("entitlement", "COMPLETE");
        data.put("entitlementExpiresAt", row.getCreatedAt().plus(90, ChronoUnit.DAYS).toString());
      }
      case "expire-entitlement" ->
          data.put("entitlementExpiresAt", Instant.now().minusSeconds(1).toString());
      case "expire-session" -> row.setExpiresAt(Instant.now().minusSeconds(1));
      case "revoke" -> row.setRevoked(true);
      default -> throw fail(400, "Controle técnico desconhecido.");
    }
    row.setInputJson(write(data));
    return evidence(row);
  }

  /** Expõe o relatório persistido da mesma sessão sem sua credencial ou dados comerciais. */
  public JsonNode report(String secret) {
    return evidence(session(secret));
  }

  /** Compõe a prova auditável com origem, operações, horários e efeitos externos desativados. */
  private ObjectNode evidence(VegaPrivateSession row) {
    var node = json.createObjectNode();
    node.put("id", row.getId());
    node.put("cycleId", row.getCycleId());
    node.put("productId", row.getProductId());
    node.put("experimentId", row.getExperimentId());
    node.put("prototypeVersion", row.getPrototypeVersion());
    node.put("state", row.getState());
    node.put("trafficClass", "AGENT_VALIDATION");
    node.put("internalMarker", "mh_internal_test");
    node.put("createdAt", row.getCreatedAt().toString());
    node.put("expiresAt", row.getExpiresAt().toString());
    node.set("operations", data(row).path("operations"));
    node.set("missions", data(row).path("missions"));
    node.set("events", events(row));
    node.set(
        "sideEffects",
        json.valueToTree(
            Map.of(
                "paymentEnabled",
                false,
                "published",
                false,
                "campaignCreated",
                false,
                "mediaSpendBrl",
                0)));
    return node;
  }

  /**
   * Exige autenticação, versão própria e sessão ativa sob lock antes de qualquer leitura ou
   * gravação.
   */
  private VegaPrivateSession session(String secret) {
    if (secret == null || !secret.matches("[a-f0-9]{64}"))
      throw fail(401, "Este acesso não foi autorizado.");
    var row =
        sessions
            .findBySessionHash(hash(secret))
            .orElseThrow(() -> fail(401, "Este acesso não foi autorizado."));
    if (!SUPPORTED_VERSIONS.contains(row.getPrototypeVersion())
        || !"AGENT_VALIDATION".equals(row.getOrigin())
        || row.isRevoked()) throw fail(401, "Este acesso não foi autorizado.");
    if (!row.getExpiresAt().isAfter(Instant.now()))
      throw fail(401, "A sessão terminou. Recupere um acesso autorizado para continuar.");
    return row;
  }

  /** O backend exige a missão anterior e o direito integral antes dos dias pagos. */
  private void authorizeMission(JsonNode data, String missionId) {
    require(missionId != null && missionId.matches("estilo-dia-[1-7]"), "Dia inválido.");
    int day = Integer.parseInt(missionId.substring(missionId.length() - 1));
    require(day == 1 || fullAccess(data), "Este dia exige acesso completo vigente.");
    require(
        day == 1
            || data.path("missions").path("estilo-dia-" + (day - 1)).path("completed").asBoolean(),
        "Conclua o dia anterior para continuar.");
  }

  /** Distingue o direito de 90 dias da duração da sessão privada de sete dias. */
  private boolean fullAccess(JsonNode data) {
    return "COMPLETE".equals(data.path("entitlement").asText())
        && Instant.parse(data.path("entitlementExpiresAt").asText()).isAfter(Instant.now());
  }

  /** Deduplica sinais por sessão e mantém resultado e horário sem inferir ações ausentes. */
  private void emit(VegaPrivateSession row, String type, String resultId) {
    var ledger = events(row);
    if (!ledger.has(type))
      ledger.set(
          type,
          json.valueToTree(Map.of("occurredAt", Instant.now().toString(), "requestId", resultId)));
    row.setEventsJson(write(ledger));
  }

  /** Recupera o agregado funcional já persistido desta sessão. */
  private ObjectNode data(VegaPrivateSession row) {
    return read(row.getInputJson());
  }

  /** Recupera exclusivamente os sinais desta sessão sintética. */
  private ObjectNode events(VegaPrivateSession row) {
    return read(row.getEventsJson());
  }

  /** Recusa estado corrompido sem substituir resultados por dados vazios. */
  private ObjectNode read(String value) {
    try {
      return (ObjectNode) json.readTree(value);
    } catch (Exception ex) {
      log.error("Vega: falha ao ler estado contextual version={}", VERSION, ex);
      throw new IllegalStateException("Não foi possível recuperar o estado preservado", ex);
    }
  }

  /** Serializa o estado transacional sem criar um armazenamento paralelo. */
  private String write(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (Exception ex) {
      log.error("Vega: falha ao serializar estado contextual version={}", VERSION, ex);
      throw new IllegalStateException("Não foi possível preservar o estado", ex);
    }
  }

  /** Persiste somente o hash da credencial, nunca o segredo entregue ao runner. */
  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException ex) {
      log.error("Vega: SHA-256 indisponível", ex);
      throw new IllegalStateException(ex);
    }
  }

  /** Interrompe a progressão afetada com uma causa funcional preservável. */
  private static void require(boolean value, String message) {
    if (!value) throw fail(409, message);
  }

  /** Padroniza erros de acesso e contrato sem expor dados de outra sessão. */
  private static ResponseStatusException fail(int status, String message) {
    return new ResponseStatusException(HttpStatus.valueOf(status), message);
  }
}
