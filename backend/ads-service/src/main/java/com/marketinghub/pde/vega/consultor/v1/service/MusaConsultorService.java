package com.marketinghub.pde.vega.consultor.v1.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.pde.vega.privateprototype.v1.VegaPrivateSession;
import com.marketinghub.pde.vega.style.v1.service.VegaContextualStyleRules;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.vega.VegaPrivateSessionRepository;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Persiste a jornada contínua privada e autoriza cada operação pelo proprietário e ciclo. */
@Service
@Transactional
@Slf4j
public class MusaConsultorService {
  public static final String VERSION = "musa-consultor-pwa-v1";
  private final VegaPrivateSessionRepository sessions;
  private final LearningSalesCycleRepository cycles;
  private final ObjectMapper json;
  private static final Set<String> SIGNALS =
      Set.of(
          "EXPERIENCE_STARTED",
          "VALUE_MOMENT",
          "READY_RESULT_USED",
          "PREFERRED_OVER_FREE",
          "CHECKOUT_STARTED");

  /** Reutiliza a persistência e os locks existentes, sem migrar sessões históricas. */
  public MusaConsultorService(
      VegaPrivateSessionRepository sessions,
      LearningSalesCycleRepository cycles,
      ObjectMapper json) {
    this.sessions = sessions;
    this.cycles = cycles;
    this.json = json;
  }

  /** Expõe capacidade real e limites privados, sem declarar aceite ou evidência de mercado. */
  public JsonNode contract() {
    var contract =
        json.<ObjectNode>valueToTree(
            Map.of(
                "prototypeVersion",
                VERSION,
                "generationMode",
                "DETERMINISTIC_CONTEXTUAL_RULES",
                "providerCalls",
                0,
                "accessDays",
                90,
                "accessPolicy",
                "MUSA_PRIVATE_ACCESS_POLICY_V1",
                "paymentEnabled",
                false,
                "commercialEvidenceClaimed",
                false,
                "trafficClass",
                "AGENT_VALIDATION",
                "inputLimits",
                Map.of(
                    "occasion",
                    160,
                    "existingSelection",
                    300,
                    "stylePreference",
                    300,
                    "reflection",
                    300),
                "requiredSignals",
                SIGNALS));
    contract.put("sessionLimit", 18);
    return contract;
  }

  /** Emite acesso sintético de 90 dias sob lock do ciclo, sem consumir provedores ou saldo. */
  public JsonNode issue(Long cycleId) {
    var cycle =
        cycles.findLockedById(cycleId).orElseThrow(() -> fail(404, "Ciclo não encontrado."));
    require(
        "OPEN".equals(cycle.getStatus())
            && !cycle.isBaseline()
            && VERSION.equals(cycle.getProductVersion())
            && Set.of("ADJUSTMENT", "VALIDATION").contains(cycle.getStage()),
        "Homologação indisponível para esta versão.");
    require(
        sessions.countByCycleIdAndPrototypeVersionAndOriginIn(
                cycleId, VERSION, List.of("AGENT_VALIDATION"))
            < 18,
        "Limite técnico de acessos atingido. Preserve e recupere os existentes.");
    require(
        sessions.findByCycleIdOrderByCreatedAtAsc(cycleId).stream()
            .filter(r -> VERSION.equals(r.getPrototypeVersion()))
            .noneMatch(r -> data(r).path("homologationClosed").asBoolean()),
        "Homologação encerrada. Não é permitido reemitir acessos.");
    var row = new VegaPrivateSession();
    row.setId(UUID.randomUUID().toString());
    row.setCycleId(cycleId);
    row.setProductId(cycle.getProductId());
    row.setExperimentId(cycle.getExperimentId());
    row.setPrototypeVersion(VERSION);
    row.setOrigin("AGENT_VALIDATION");
    row.setCreatedAt(Instant.now());
    row.setExpiresAt(row.getCreatedAt().plus(90, ChronoUnit.DAYS));
    row.setState("INPUT");
    var data = json.createObjectNode();
    data.put("revision", 0);
    data.put("issuedAt", row.getCreatedAt().toString());
    data.putObject("draft");
    data.putObject("choices");
    data.putObject("operations");
    data.putObject("signals");
    data.putArray("accessAudit")
        .addObject()
        .put("action", "ISSUED")
        .put("at", row.getCreatedAt().toString())
        .put("expiresAt", row.getExpiresAt().toString());
    row.setInputJson(data.toString());
    row.setEventsJson("{}");
    String key = secret();
    row.setGrantHash(hash(key));
    sessions.saveAndFlush(row);
    log.info(
        "MUSA: acesso privado emitido cycleId={} sessionId={} version={}",
        cycleId,
        row.getId(),
        VERSION);
    var view = identity(row);
    view.put("accessKey", key);
    return view;
  }

  /** Troca a chave privada por sessão HttpOnly, conservando proprietário, dados e expiração. */
  public JsonNode login(String key) {
    require(key != null && key.matches("[a-f0-9]{64}"), "Acesso inválido.");
    var row = sessions.findByGrantHash(hash(key)).orElseThrow(() -> fail(401, "Acesso inválido."));
    authorize(row);
    String token = secret();
    row.setSessionHash(hash(token));
    var authentication = data(row);
    authentication.put("authenticatedAt", Instant.now().toString());
    row.setInputJson(authentication.toString());
    var out = identity(row);
    out.put("sessionToken", token);
    return out;
  }

  /** Recupera o rascunho e as escolhas somente da identidade autorizada, sem gerar novamente. */
  public JsonNode workspace(String token) {
    var row = session(token);
    var data = data(row);
    var out = identity(row);
    out.set("draft", data.path("draft"));
    out.set("choices", data.path("choices"));
    out.put("revision", data.path("revision").asInt());
    out.put("preference", data.path("preference").asText());
    out.put("consented", row.getConsentedAt() != null);
    out.put("providerCalls", 0);
    return out;
  }

  /** Aplica um comando transacional idempotente sem conceder direitos ou chamar integrações. */
  public JsonNode command(String token, String action, JsonNode input, boolean internal) {
    var row = session(token);
    var data = data(row);
    JsonNode result;
    switch (action) {
      case "consent" -> {
        require(
            input.path("accepted").asBoolean(),
            "Confirme a ciência para usar dados sintéticos nesta homologação.");
        row.setConsentedAt(Instant.now());
        data.put("noticeVersion", "MUSA_PRIVATE_NOTICE_V1");
        result = json.valueToTree(Map.of("recorded", true));
      }
      case "draft" -> {
        consent(row);
        revision(data, input);
        var fields = answers(input.path("answers"), false);
        data.set("draft", json.valueToTree(fields));
        if (fields.containsKey("stylePreference") && !fields.get("stylePreference").isBlank())
          data.put("preference", fields.get("stylePreference"));
        if (!data.has("journeyStartedAt")) data.put("journeyStartedAt", Instant.now().toString());
        data.put("revision", data.path("revision").asInt() + 1);
        result = data.path("draft");
      }
      case "generate" -> {
        consent(row);
        result = generate(row, data, input);
      }
      case "save" -> {
        consent(row);
        result = save(data, input);
      }
      case "event" -> {
        consent(row);
        result = event(data, input, internal);
      }
      default -> throw fail(400, "Comando fora do contrato.");
    }
    row.setInputJson(data.toString());
    return result;
  }

  /** Renova a identidade sob lock do ciclo e leitura atual, somente com homologação aberta. */
  public JsonNode renew(String id) {
    var cycleId =
        sessions.findCycleIdById(id).orElseThrow(() -> fail(404, "Acesso não encontrado."));
    cycles.findLockedById(cycleId).orElseThrow(() -> fail(404, "Ciclo não encontrado."));
    var row = sessions.findLockedById(id).orElseThrow(() -> fail(404, "Acesso não encontrado."));
    boolean expired = !row.getExpiresAt().isAfter(Instant.now());
    if (expired) {
      require(!row.isRevoked(), "Acesso revogado.");
      requireOpen(row);
    } else authorize(row);
    Instant issuedAt = Instant.now();
    row.setExpiresAt(issuedAt.plus(90, ChronoUnit.DAYS));
    var data = data(row);
    data.put("issuedAt", issuedAt.toString());
    data.withArray("accessAudit")
        .addObject()
        .put("action", "RENEWED")
        .put("at", Instant.now().toString())
        .put("expiresAt", row.getExpiresAt().toString());
    row.setInputJson(data.toString());
    return identity(row);
  }

  /** Revoga acessos após obter seu estado bloqueado, preservando salvamentos concorrentes. */
  public JsonNode close(Long cycleId) {
    cycles.findLockedById(cycleId).orElseThrow(() -> fail(404, "Ciclo não encontrado."));
    int count = 0;
    for (var row : sessions.findLockedByCycleAndVersion(cycleId, VERSION)) {
      row.setRevoked(true);
      var data = data(row);
      data.put("homologationClosed", true);
      data.withArray("accessAudit")
          .addObject()
          .put("action", "CLOSED_REVOKED")
          .put("at", Instant.now().toString());
      row.setInputJson(data.toString());
      count++;
    }
    require(count > 0, "Nenhuma emissão desta versão foi encontrada para encerrar.");
    return json.valueToTree(Map.of("revoked", count, "preserved", true));
  }

  /** Recupera operação existente e saída protegida sem nova execução nem consumo. */
  public JsonNode operation(String token, String id) {
    var value = data(session(token)).path("operations").path(id);
    require(!value.isMissingNode(), "Orientação indisponível neste acesso.");
    return value;
  }

  /** Exporta provas técnicas protegidas, separadas da experiência e de métricas comerciais. */
  public JsonNode report(String token) {
    var row = session(token);
    var out = identity(row);
    out.set("execution", data(row));
    out.put("trafficClass", "AGENT_VALIDATION");
    out.put("internalMarker", "mh_internal_test");
    out.put("humanEvidenceClaimed", false);
    out.put("commercialEvidenceClaimed", false);
    out.put("providerCalls", 0);
    return out;
  }

  /** Combina escolha recuperada, contexto novo e preferência vigente antes da regra real. */
  private JsonNode generate(VegaPrivateSession row, ObjectNode data, JsonNode input) {
    String key = input.path("operationKey").asText();
    require(key.matches("[A-Za-z0-9-]{16,80}"), "Chave de recuperação inválida.");
    var existing = data.path("operations").path(key);
    if (!existing.isMissingNode()) {
      require(
          input.equals(existing.path("request")),
          "Esta chave já possui outra entrada. Consulte a orientação preservada.");
      return existing;
    }
    revision(data, input);
    require(
        data.path("operations").size() < 24,
        "Limite técnico atingido. Consulte as orientações preservadas.");
    String parent = input.path("parentChoiceId").asText();
    var previous = data.path("choices").path(parent);
    Map<String, String> fields = new LinkedHashMap<>();
    if (!parent.isBlank()) {
      require(!previous.isMissingNode(), "Escolha indisponível neste acesso.");
      previous
          .path("answers")
          .fields()
          .forEachRemaining(e -> fields.put(e.getKey(), e.getValue().asText()));
      if (data.has("preference")) fields.put("stylePreference", data.path("preference").asText());
    }
    fields.putAll(answers(input.path("answers"), false));
    answers(json.valueToTree(fields), true);
    var canonical = new LinkedHashMap<>(fields);
    String pref = fields.get("stylePreference");
    if (!com.marketinghub.pde.vega.style.v1.service.VegaStyleInput.PREFERENCES.contains(pref))
      canonical.put("stylePreference", "Ainda estou descobrindo");
    require(
        !String.join(" ", fields.values())
            .toLowerCase(Locale.ROOT)
            .matches(".*(inferior|superior|racis|emagrec|feia|gorda|clarear pele).*"),
        "Esta orientação respeita autonomia e diversidade. Não avalia corpos ou identidades.");
    var decision = VegaContextualStyleRules.decide("continuous", canonical, Map.of());
    var op = json.createObjectNode();
    op.set("request", input.deepCopy());
    op.put("operationId", key);
    op.put("startedAt", data.path("journeyStartedAt").asText(Instant.now().toString()));
    op.put("inputsCompletedAt", Instant.now().toString());
    op.put("status", decision.status());
    op.set("answers", json.valueToTree(fields));
    op.put("parentChoiceId", parent);
    op.put("ruleId", decision.ruleId());
    op.put("action", decision.status().equals("COMPLETED") ? decision.actions().getFirst() : "");
    op.put("reason", decision.summary());
    op.put("limitation", decision.limitation());
    op.put(
        "continuity",
        parent.isBlank()
            ? ""
            : ("Escolha anterior preservada: "
                + previous.path("answers").path("occasion").asText()
                + ". Peças recuperadas e preferência vigente aplicadas à nova ocasião."));
    op.put("finishedAt", Instant.now().toString());
    op.put("providerCalls", 0);
    op.put("incrementalInferenceCostUsd", 0);
    ((ObjectNode) data.path("operations")).set(key, op);
    data.set("draft", json.valueToTree(fields));
    data.put("preference", pref);
    data.put("revision", data.path("revision").asInt() + 1);
    data.withObject("/signals")
        .withObject("/EXPERIENCE_STARTED")
        .put(key, op.path("startedAt").asText());
    log.info(
        "MUSA: orientação preservada sessionId={} operationId={} ruleId={} status={}",
        row.getId(),
        key,
        decision.ruleId(),
        decision.status());
    return op;
  }

  /** Confirma salvamento explícito uma única vez e preserva a escolha de origem. */
  private JsonNode save(ObjectNode data, JsonNode input) {
    var operation = data.path("operations").path(input.path("operationId").asText());
    require(
        "COMPLETED".equals(operation.path("status").asText()),
        "Conclua uma orientação válida antes de salvar.");
    String id = operation.path("operationId").asText();
    if (data.path("choices").has(id)) return data.path("choices").path(id);
    revision(data, input);
    var choice = operation.deepCopy();
    ((ObjectNode) choice).put("savedAt", Instant.now().toString());
    ((ObjectNode) data.path("choices")).set(id, choice);
    data.put("revision", data.path("revision").asInt() + 1);
    return choice;
  }

  /** Confirma sinais isolados por operação, sem converter salvamento em uso ou venda. */
  private JsonNode event(ObjectNode data, JsonNode input, boolean internal) {
    String type = input.path("eventType").asText();
    String id = input.path("operationId").asText();
    require(SIGNALS.contains(type), "Sinal inválido.");
    require(
        !Set.of("PREFERRED_OVER_FREE", "CHECKOUT_STARTED").contains(type) || internal,
        "Sinal disponível somente em transporte técnico interno.");
    require(
        "COMPLETED".equals(data.path("operations").path(id).path("status").asText()),
        "Orientação válida indisponível.");
    var signals = data.withObject("/signals");
    if ("READY_RESULT_USED".equals(type))
      require(
          signals.path("VALUE_MOMENT").has(id), "Aguarde a confirmação de exibição da orientação.");
    var events = signals.withObject("/" + type);
    if (!events.has(id)) events.put(id, Instant.now().toString());
    return json.valueToTree(
        Map.of("recorded", true, "paymentEnabled", false, "checkoutMode", "SIMULATED_NO_CHARGE"));
  }

  /** Valida somente campos conhecidos, limites e presença mínima, sem recolher dados extras. */
  private Map<String, String> answers(JsonNode input, boolean complete) {
    require(input.isObject(), "Confira os campos da orientação.");
    var fields = new LinkedHashMap<String, String>();
    input
        .fields()
        .forEachRemaining(
            e -> {
              require(
                  Set.of("occasion", "existingSelection", "stylePreference", "reflection")
                          .contains(e.getKey())
                      && e.getValue().isTextual()
                      && e.getValue().asText().length()
                          <= (e.getKey().equals("occasion") ? 160 : 300),
                  "Confira o campo e seu limite de caracteres.");
              fields.put(e.getKey(), e.getValue().asText());
            });
    if (complete)
      for (String key : List.of("occasion", "existingSelection", "stylePreference"))
        require(
            fields.containsKey(key) && !fields.get(key).isBlank(),
            "Informe ocasião, peças disponíveis e preferência.");
    return fields;
  }

  /** Impede que uma revisão antiga sobrescreva o rascunho ou a preferência mais recente. */
  private void revision(ObjectNode data, JsonNode input) {
    require(
        input.has("revision") && input.path("revision").asInt(-1) == data.path("revision").asInt(),
        "Outra aba atualizou suas escolhas. Recarregue o repertório antes de conciliar.");
  }

  /**
   * Exige ciência explícita do uso privado de dados sintéticos, sem inferir consentimento humano.
   */
  private void consent(VegaPrivateSession row) {
    require(
        row.getConsentedAt() != null, "Confirme a ciência do aviso privado antes de continuar.");
  }

  /** Resolve e bloqueia a sessão antes de ler ou alterar qualquer dado do proprietário. */
  private VegaPrivateSession session(String token) {
    if (token == null || !token.matches("[a-f0-9]{64}")) throw fail(401, "Acesso inválido.");
    var row =
        sessions.findBySessionHash(hash(token)).orElseThrow(() -> fail(401, "Acesso inválido."));
    authorize(row);
    require(
        Instant.parse(data(row).path("authenticatedAt").asText())
            .plus(12, ChronoUnit.HOURS)
            .isAfter(Instant.now()),
        "Sua sessão terminou. Entre novamente com o mesmo acesso privado.");
    return row;
  }

  /** Confere a versão, revogação, prazo e vínculo atual em toda leitura e gravação. */
  private void authorize(VegaPrivateSession row) {
    if (!VERSION.equals(row.getPrototypeVersion())
        || !"AGENT_VALIDATION".equals(row.getOrigin())
        || row.isRevoked()
        || !row.getExpiresAt().isAfter(Instant.now()))
      throw fail(401, "Acesso expirado ou revogado. Os dados foram preservados.");
    requireOpen(row);
    require(!data(row).path("homologationClosed").asBoolean(), "A homologação foi encerrada.");
  }

  /** Bloqueia acessos quando o ciclo fecha, sem reescrever o histórico nem emitir nova validade. */
  private void requireOpen(VegaPrivateSession row) {
    var cycle =
        cycles.findById(row.getCycleId()).orElseThrow(() -> fail(404, "Ciclo não encontrado."));
    require(
        "OPEN".equals(cycle.getStatus())
            && Objects.equals(cycle.getProductId(), row.getProductId())
            && Objects.equals(cycle.getExperimentId(), row.getExperimentId())
            && VERSION.equals(cycle.getProductVersion()),
        "Homologação encerrada ou contexto alterado. Acesso revogado para operações.");
  }

  /** Publica identidade e prazo sem revelar credenciais nem condição comercial contratada. */
  private ObjectNode identity(VegaPrivateSession row) {
    return json.valueToTree(
        Map.of(
            "sessionId",
            row.getId(),
            "productId",
            row.getProductId(),
            "cycleId",
            row.getCycleId(),
            "experimentId",
            row.getExperimentId(),
            "prototypeVersion",
            VERSION,
            "issuedAt",
            data(row).path("issuedAt").asText(row.getCreatedAt().toString()),
            "expiresAt",
            row.getExpiresAt().toString(),
            "paymentEnabled",
            false));
  }

  /** Lê o agregado protegido e falha explicitamente em caso de corrupção. */
  private ObjectNode data(VegaPrivateSession row) {
    try {
      return (ObjectNode) json.readTree(row.getInputJson());
    } catch (Exception ex) {
      log.error("MUSA: agregado inválido sessionId={}", row.getId(), ex);
      throw new IllegalStateException("Estado preservado indisponível", ex);
    }
  }

  /** Gera credencial imprevisível para uma emissão ou autenticação explícita. */
  private String secret() {
    byte[] value = new byte[32];
    new SecureRandom().nextBytes(value);
    return HexFormat.of().formatHex(value);
  }

  /** Mantém somente hashes das chaves e sessões no banco. */
  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      log.error("MUSA: SHA-256 indisponível", ex);
      throw new IllegalStateException(ex);
    }
  }

  /** Recusa operação inválida com causa e recuperação legíveis. */
  private static void require(boolean value, String message) {
    if (!value) throw fail(409, message);
  }

  /** Preserva status e mensagem do contrato HTTP sem revelar outro proprietário. */
  private static ResponseStatusException fail(int status, String message) {
    return new ResponseStatusException(HttpStatus.valueOf(status), message);
  }
}
