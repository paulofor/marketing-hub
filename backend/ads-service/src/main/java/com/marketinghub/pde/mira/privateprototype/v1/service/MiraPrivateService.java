package com.marketinghub.pde.mira.privateprototype.v1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.pde.mira.privateprototype.v1.MiraPrivateSession;
import com.marketinghub.pde.mira.privateprototype.v1.service.contract.MiraPrivateContract.*;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.mira.MiraPrivateSessionRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: governar a candidata privada determinística e auditável de Mira. */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MiraPrivateService {
  public static final String VERSION = "mira-private-candidate-v3";
  public static final String PRODUCT_SLUG = "pde-planejado-36";
  private final MiraPrivateSessionRepository sessions;
  private final LearningSalesCycleRepository cycles;
  private final ProductRepository products;
  private final ObjectMapper json;

  /** Expõe capacidades efetivamente implementadas, sem abrir acesso ou emitir sinal comercial. */
  public Contract contract() {
    return new Contract(
        PRODUCT_SLUG,
        VERSION,
        "DETERMINISTIC_DOCUMENTED_LABELS",
        2,
        12,
        600,
        "SIMULATED_NO_CHARGE",
        false,
        false,
        false,
        0);
  }

  /**
   * Emite credencial opaca e início sintético para contexto aberto compatível e testes segregados.
   */
  public CreatedSession create(Create request) {
    var cycle =
        cycles
            .findLockedById(request.cycleId())
            .orElseThrow(() -> fail(404, "Ciclo não encontrado."));
    var product =
        products
            .findById(cycle.getProductId())
            .orElseThrow(() -> fail(404, "Produto não encontrado."));
    require(PRODUCT_SLUG.equals(product.getSlug()), "O ciclo pertence a outro produto.");
    require(
        "OPEN".equals(cycle.getStatus())
            && !cycle.isBaseline()
            && Set.of("ADJUSTMENT", "VALIDATION").contains(cycle.getStage()),
        "O ciclo não está aberto para validação privada.");
    require(
        VERSION.equals(request.prototypeVersion()) && VERSION.equals(cycle.getProductVersion()),
        "A candidata e a versão do ciclo precisam corresponder.");
    require(
        ("experiment:" + cycle.getExperimentId()).equals(request.sourceReference()),
        "A referência não corresponde ao experimento do ciclo.");
    require(Set.of("REFERENCE", "REDUCED").contains(request.condition()), "Condição inválida.");
    require(
        Set.of("ADHERENT", "RECOVERY", "SAFETY").contains(request.scenarioCode()),
        "Cenário inválido.");
    require(
        Set.of("DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7").contains(request.deviceProfile()),
        "Dispositivo inválido.");
    var row = new MiraPrivateSession();
    row.setId(UUID.randomUUID().toString());
    row.setProductId(cycle.getProductId());
    row.setCycleId(cycle.getId());
    row.setExperimentId(cycle.getExperimentId());
    row.setPrototypeVersion(VERSION);
    row.setCreatedAt(Instant.now());
    row.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    row.setSessionHash(hash(secret));
    ObjectNode data = json.createObjectNode();
    data.put("status", "INPUT");
    data.put("firstInteractionAt", Instant.now().toString());
    data.put("condition", request.condition());
    data.put("scenarioCode", request.scenarioCode());
    data.put("deviceProfile", request.deviceProfile());
    data.put("sourceReference", request.sourceReference());
    data.put("organizationsUsed", 0);
    data.put("objective", "Organizar os produtos que já tenho em uma rotina simples");
    data.putArray("products");
    data.putArray("routine");
    data.putArray("events");
    eventOnce(data, "EXPERIENCE_STARTED");
    data.putArray("audit");
    audit(
        data,
        "SESSION_CREATED",
        request,
        json.createObjectNode().put("trafficClass", "AGENT_VALIDATION"));
    row.setPayloadJson(write(data));
    sessions.saveAndFlush(row);
    log.info(
        "Mira criou pacote privado productId={} cycleId={} experimentId={} sessionId={}",
        row.getProductId(),
        row.getCycleId(),
        row.getExperimentId(),
        row.getId());
    return new CreatedSession(secret, view(row));
  }

  /** Recupera o checkpoint autorizado sem regenerar ou aumentar contadores. */
  public SessionView session(String secret) {
    return view(active(secret));
  }

  /** Persiste os produtos e referências seguras, permitindo correção antes do resultado útil. */
  public SessionView input(String secret, Input request) {
    var row = active(secret);
    mutableContext(row);
    var data = read(row);
    var normalized =
        new Input(
            request.objective().trim(),
            request.products().stream()
                .map(
                    p ->
                        new ProductInput(
                            p.name().trim(), p.labelDirections().trim(), sourceUrl(p.sourceUrl())))
                .toList());
    log.info(
        "Mira entrada bruta sessionId={} cycleId={} payload={}",
        row.getId(),
        row.getCycleId(),
        request);
    boolean same =
        normalized.objective().equals(data.path("objective").asText())
            && json.valueToTree(normalized.products()).equals(data.path("products"));
    if (same && "READY".equals(data.path("status").asText())) return view(row);
    require(
        data.path("organizationsUsed").asInt() < 2,
        "As duas organizações já foram usadas. Seu último resultado continua disponível.");
    if (!data.has("firstInteractionAt")) data.put("firstInteractionAt", Instant.now().toString());
    data.put("objective", normalized.objective());
    data.set("products", json.valueToTree(normalized.products()));
    data.put("status", "INPUT_READY");
    data.remove("blocker");
    data.putArray("routine");
    audit(data, "INPUT_SAVED", normalized, json.createObjectNode().put("state", "INPUT_READY"));
    save(row, data);
    return view(row);
  }

  /** Organiza instruções documentais sem provedor pago e reutiliza resultado já gravado. */
  public SessionView generate(String secret) {
    var row = active(secret);
    mutableContext(row);
    var data = read(row);
    if ("READY".equals(data.path("status").asText())) return view(row);
    require(
        data.path("products").isArray() && !data.path("products").isEmpty(),
        "Informe ao menos um produto e o texto documental.");
    require(
        data.path("organizationsUsed").asInt() < 2, "O limite de duas organizações foi atingido.");
    if (Instant.parse(data.path("firstInteractionAt").asText())
        .plusSeconds(600)
        .isBefore(Instant.now())) {
      data.put("status", "BLOCKED");
      data.put(
          "blocker",
          "O tempo desta tentativa terminou. Sua entrada está preservada; encerre o teste e registre a pendência.");
      audit(
          data,
          "DEADLINE_BLOCKED",
          data.path("products"),
          json.createObjectNode().put("costUsd", 0));
      save(row, data);
      return view(row);
    }
    List<MiraRoutinePolicy.ProductInput> inputs = new ArrayList<>();
    data.path("products")
        .forEach(
            p ->
                inputs.add(
                    new MiraRoutinePolicy.ProductInput(
                        p.path("name").asText(),
                        p.path("labelDirections").asText(),
                        p.path("sourceUrl").asText(null))));
    var decision = MiraRoutinePolicy.organize(data.path("objective").asText(), inputs);
    data.set("routine", json.valueToTree(decision.routine()));
    if (decision.blocked()) {
      data.put("status", "BLOCKED");
      data.put("blocker", decision.blocker());
    } else {
      data.put("status", "READY");
      data.remove("blocker");
      data.put("organizationsUsed", data.path("organizationsUsed").asInt() + 1);
      data.put("generatedAt", Instant.now().toString());
      List<ProductInput> provided = new ArrayList<>();
      data.path("products").forEach(p -> provided.add(json.convertValue(p, ProductInput.class)));
      data.withArray("resultHistory")
          .add(
              json.valueToTree(
                  new ReadyResult(
                      data.path("organizationsUsed").asInt(),
                      data.path("objective").asText(),
                      provided,
                      decision.routine(),
                      data.path("generatedAt").asText())));
      eventOnce(data, "VALUE_MOMENT");
    }
    audit(data, "ORGANIZATION_COMPLETED", inputs, decision);
    save(row, data);
    return view(row);
  }

  /** Registra somente eventos coerentes com o estado funcional, sem compra simulada virar venda. */
  public SessionView event(String secret, Event request) {
    var row = active(secret);
    mutableContext(row);
    var data = read(row);
    String name = request.eventType();
    require(
        Set.of(
                "READY_RESULT_USED",
                "PREFERRED_OVER_FREE",
                "CHECKOUT_STARTED",
                "RECOVERY_COMPLETED",
                "SAFETY_LIMIT_BLOCKED",
                "AGENT_SCENARIO_COMPLETED")
            .contains(name),
        "Evento privado inválido.");
    String status = data.path("status").asText();
    if ("SAFETY_LIMIT_BLOCKED".equals(name))
      require("BLOCKED".equals(status), "O bloqueio precisa existir antes da confirmação.");
    else if ("AGENT_SCENARIO_COMPLETED".equals(name))
      require(
          ("READY".equals(status)
                  && hasEvent(data, "READY_RESULT_USED")
                  && (!"RECOVERY".equals(data.path("scenarioCode").asText())
                      || hasEvent(data, "RECOVERY_COMPLETED")))
              || ("SAFETY".equals(data.path("scenarioCode").asText())
                  && "BLOCKED".equals(status)
                  && hasEvent(data, "SAFETY_LIMIT_BLOCKED")),
          "Conclua o percurso funcional antes de encerrar o cenário.");
    else require("READY".equals(status), "O resultado precisa estar pronto antes desta ação.");
    if ("RECOVERY_COMPLETED".equals(name))
      require(
          "RECOVERY".equals(data.path("scenarioCode").asText())
              && hasEvent(data, "READY_RESULT_USED"),
          "Confirme a consulta no cenário de recuperação.");
    if (Set.of("PREFERRED_OVER_FREE", "CHECKOUT_STARTED").contains(name))
      require(hasEvent(data, "READY_RESULT_USED"), "Consulte o resultado antes da simulação.");
    if ("CHECKOUT_STARTED".equals(name))
      require(hasEvent(data, "PREFERRED_OVER_FREE"), "Simule a comparação antes da continuidade.");
    eventOnce(data, name);
    audit(data, "EVENT_RECORDED", request, json.createObjectNode().put("status", status));
    save(row, data);
    return view(row);
  }

  /** Revoga credencial preservando a trilha técnica para auditoria interna. */
  public void revoke(String id) {
    var row = sessions.findLockedById(id).orElseThrow(() -> fail(404, "Pacote não encontrado."));
    row.setRevoked(true);
  }

  /** Entrega relatório do ciclo sem credenciais, PII ou eventos de outro produto. */
  public Report report(Long cycleId) {
    var rows = sessions.findByCycleIdOrderByCreatedAtAsc(cycleId);
    return new Report(
        rows.stream().map(this::view).toList(),
        rows.stream().map(r -> read(r).path("audit")).toList(),
        "AGENT_VALIDATION",
        false,
        false);
  }

  /** Bloqueia mutações quando o ciclo encerrou ou mudou de versão, preservando sua história. */
  private void mutableContext(MiraPrivateSession row) {
    var cycle =
        cycles
            .findLockedById(row.getCycleId())
            .orElseThrow(() -> fail(404, "Ciclo não encontrado."));
    require(
        "OPEN".equals(cycle.getStatus())
            && row.getPrototypeVersion().equals(cycle.getProductVersion())
            && Set.of("ADJUSTMENT", "VALIDATION").contains(cycle.getStage()),
        "Este ciclo encerrou ou mudou de versão. O resultado anterior foi preservado.");
  }

  /** Confere expiração e revogação usando o hash da credencial, nunca um simples identificador. */
  private MiraPrivateSession active(String secret) {
    var row =
        sessions
            .findBySessionHash(hash(secret))
            .orElseThrow(() -> fail(401, "Acesso privado inválido."));
    if (row.isRevoked() || row.getExpiresAt().isBefore(Instant.now()))
      throw fail(401, "Acesso privado expirado ou revogado.");
    return row;
  }

  /** Projeta a verdade persistida com limites e próximo movimento compreensíveis. */
  private SessionView view(MiraPrivateSession row) {
    var data = read(row);
    List<ProductInput> input = new ArrayList<>();
    List<MiraRoutinePolicy.RoutineCard> routine = new ArrayList<>();
    List<String> events = new ArrayList<>();
    data.path("products").forEach(p -> input.add(json.convertValue(p, ProductInput.class)));
    data.path("routine")
        .forEach(p -> routine.add(json.convertValue(p, MiraRoutinePolicy.RoutineCard.class)));
    data.path("events").forEach(e -> events.add(e.asText()));
    List<ReadyResult> previousResults = new ArrayList<>();
    for (var result : data.path("resultHistory")) {
      if (!"READY".equals(data.path("status").asText())
          || result.path("organization").asInt() < data.path("organizationsUsed").asInt())
        previousResults.add(json.convertValue(result, ReadyResult.class));
    }
    return new SessionView(
        row.getId(),
        row.getProductId(),
        row.getCycleId(),
        row.getExperimentId(),
        row.getPrototypeVersion(),
        data.path("condition").asText(),
        data.path("scenarioCode").asText(),
        data.path("deviceProfile").asText(),
        "AGENT_VALIDATION",
        true,
        data.path("status").asText(),
        data.path("objective").asText(),
        input,
        routine,
        previousResults,
        data.path("blocker").isMissingNode() ? null : data.path("blocker").asText(),
        data.path("organizationsUsed").asInt(),
        2,
        events,
        data.path("firstInteractionAt").asText(null),
        data.path("generatedAt").asText(null),
        "DETERMINISTIC_DOCUMENTED_LABELS",
        "SIMULATED_NO_CHARGE",
        false,
        false,
        0);
  }

  /** Preserva request e response da operação real com custo externo observado nulo. */
  private void audit(ObjectNode data, String operation, Object request, Object response) {
    ObjectNode entry = data.withArray("audit").addObject();
    entry.put("operation", operation);
    entry.put("observedAt", Instant.now().toString());
    entry.set("request", json.valueToTree(request));
    entry.set("response", json.valueToTree(response));
    entry.put("providerCalls", 0);
    entry.put("costUsd", 0);
    entry.put("costKind", "OBSERVED_NO_EXTERNAL_PROVIDER");
    entry.put("humanEvidenceClaimed", false);
    entry.put("commercialEvidenceClaimed", false);
  }

  /** Registra evento de forma idempotente dentro da transação reservada. */
  private void eventOnce(ObjectNode data, String event) {
    if (!hasEvent(data, event)) data.withArray("events").add(event);
  }

  /** Consulta o evento no ledger persistido. */
  private boolean hasEvent(ObjectNode data, String event) {
    for (var item : data.path("events")) if (event.equals(item.asText())) return true;
    return false;
  }

  /** Atualiza o checkpoint no banco e força sua validação antes da resposta. */
  private void save(MiraPrivateSession row, ObjectNode data) {
    row.setPayloadJson(write(data));
    sessions.saveAndFlush(row);
  }

  /** Recupera estado estruturado sem silenciar corrupção ou perder a stack trace. */
  private ObjectNode read(MiraPrivateSession row) {
    try {
      return (ObjectNode) json.readTree(row.getPayloadJson());
    } catch (Exception ex) {
      log.error(
          "Mira falhou ao recuperar pacote sessionId={} cycleId={}",
          row.getId(),
          row.getCycleId(),
          ex);
      throw fail(500, "Não foi possível recuperar este pacote.");
    }
  }

  /** Serializa entrada e saída auditáveis sem converter falha em sucesso. */
  private String write(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (Exception ex) {
      log.error("Mira falhou ao serializar operação privada", ex);
      throw fail(500, "Não foi possível preservar esta operação.");
    }
  }

  /** Calcula hash da credencial sem armazená-la nem registrá-la em logs. */
  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      log.error("Mira falhou ao comparar acesso privado", ex);
      throw fail(500, "Não foi possível validar este acesso.");
    }
  }

  /** Recusa contrato inválido antes de persistir efeito. */
  private void require(boolean valid, String message) {
    if (!valid) throw fail(409, message);
  }

  /** Aceita referência HTTPS sem credenciais, sem acessar ou interpretar o endereço informado. */
  private String sourceUrl(String value) {
    if (value == null || value.isBlank()) return null;
    try {
      var uri = java.net.URI.create(value.trim());
      require(
          "https".equals(uri.getScheme())
              && uri.getHost() != null
              && uri.getUserInfo() == null
              && uri.getFragment() == null,
          "A referência do fabricante precisa ser HTTPS e não conter credenciais.");
      return uri.toString();
    } catch (IllegalArgumentException ex) {
      log.warn("Mira recebeu referência documental inválida", ex);
      throw fail(409, "Informe um endereço HTTPS válido para a referência documental.");
    }
  }

  /** Cria erro HTTP coerente com o limite funcional. */
  private ResponseStatusException fail(int status, String message) {
    return new ResponseStatusException(HttpStatus.valueOf(status), message);
  }
}
