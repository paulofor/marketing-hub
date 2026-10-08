package com.marketinghub.pde.kit.privateprototype.v1.service;

import static com.marketinghub.pde.kit.privateprototype.v1.service.KitArtifactContract.hash;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.pde.kit.privateprototype.v1.*;
import com.marketinghub.pde.kit.privateprototype.v1.service.contract.KitPrivateContract.*;
import com.marketinghub.repository.jpa.kit.*;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: governar acesso, fila, reservas e resultados da implementação privada de kits.
 */
@Service
@Slf4j
public class KitPrivateService {
  private static final Set<String> SCENARIOS = Set.of("ADHERENT", "RECOVERY", "SAFETY");
  private static final Set<String> DEVICES = Set.of("DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7");
  private static final Set<String> ACTIONS =
      Set.of(
          "VALUE_MOMENT",
          "READY_RESULT_USED",
          "PREFERRED_OVER_FREE",
          "CHECKOUT_STARTED",
          "DOWNLOAD_COMPLETED",
          "SESSION_EXITED",
          "SESSION_RETURNED");
  private final KitPrivateSessionRepository sessions;
  private final KitPrivateArtifactRepository artifacts;
  private final LearningSalesCycleRepository cycles;
  private final KitPrototypeCapabilities capabilities;
  private final KitArtifactContract contract;
  private final ObjectMapper json;
  private final String pepper;
  private final Clock clock;

  /** Configura persistência canônica e assinatura de acessos sem manter segredo bruto no banco. */
  public KitPrivateService(
      KitPrivateSessionRepository sessions,
      KitPrivateArtifactRepository artifacts,
      LearningSalesCycleRepository cycles,
      KitPrototypeCapabilities capabilities,
      KitArtifactContract contract,
      ObjectMapper json,
      @Value("${integrations.pde-platform.internal-token:${PDE_INTERNAL_API_TOKEN:}}")
          String pepper) {
    this.sessions = sessions;
    this.artifacts = artifacts;
    this.cycles = cycles;
    this.capabilities = capabilities;
    this.contract = contract;
    this.json = json;
    this.pepper = pepper;
    this.clock = Clock.systemUTC();
  }

  /** Emite acesso idempotente somente após conferir todos os contratos da versão selecionada. */
  @Transactional
  public Created create(Create input) {
    require(!pepper.isBlank(), "A assinatura interna da experiência privada está ausente.");
    require(
        SCENARIOS.contains(input.scenarioCode()) && DEVICES.contains(input.deviceProfile()),
        "Cenário ou dispositivo não suportado.");
    var cycle =
        cycles
            .findLocked(input.productId(), input.cycleId())
            .orElseThrow(() -> conflict("Ciclo não pertence ao produto."));
    require(
        cycle.getProductVersion().equals(input.prototypeVersion()), "A versão diverge do ciclo.");
    var capability = capabilities.resolve(cycle);
    require(capability.available(), capability.reason());
    var previous =
        sessions
            .findByCycleIdAndRequestKey(cycle.getId(), input.requestKey().toString())
            .orElse(null);
    if (previous != null) {
      require(
          previous.getScenarioCode().equals(input.scenarioCode())
              && previous.getDeviceProfile().equals(input.deviceProfile()),
          "Chave de acesso já usada com outro conteúdo.");
      return new Created(previous.getId(), secret(previous.getId()), view(previous));
    }
    var session = new KitPrivateSession();
    session.setId(UUID.randomUUID().toString());
    session.setCycleId(cycle.getId());
    session.setProductId(cycle.getProductId());
    session.setExperimentId(cycle.getExperimentId());
    session.setPrototypeVersion(cycle.getProductVersion());
    session.setScenarioCode(input.scenarioCode());
    session.setDeviceProfile(input.deviceProfile());
    session.setRequestKey(input.requestKey().toString());
    session.setSessionHash(hash(secret(session.getId()).getBytes(StandardCharsets.UTF_8)));
    session.setCreatedAt(clock.instant());
    session.setExpiresAt(clock.instant().plus(Duration.ofDays(30)));
    var data =
        json.createObjectNode()
            .put("status", "INPUT")
            .put("profileCode", capability.profileCode())
            .put("transfers", 0)
            .put("testMarker", "AGENT_VALIDATION")
            .put("commercialEvidenceEligible", false);
    data.putArray("events");
    record(data, "EXPERIENCE_STARTED", UUID.randomUUID().toString());
    session.setPayloadJson(write(data));
    sessions.saveAndFlush(session);
    log.info(
        "Kit privado: acesso emitido productId={} cycleId={} experimentId={} sessionId={} version={}",
        cycle.getProductId(),
        cycle.getId(),
        cycle.getExperimentId(),
        session.getId(),
        cycle.getProductVersion());
    return new Created(session.getId(), secret(session.getId()), view(session));
  }

  /** Recupera a sessão autenticada sem emitir eventos ou reservar novos recursos. */
  @Transactional(readOnly = true)
  public SessionView session(String token) {
    return view(active(token, false));
  }

  /** Vincula a prova declarada somente à capacidade e ao arquivo aceito da mesma versão. */
  @Transactional(readOnly = true)
  public void validateRegistration(
      com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle cycle,
      JsonNode proof) {
    var capability = capabilities.resolve(cycle);
    require(
        capability.available()
            && capability.profileCode().equals(proof.path("profileCode").asText())
            && capability.prototypeUrl().equals(proof.path("privateAccessUrl").asText()),
        "A prova não corresponde ao perfil e à URL da implementação registrada.");
    require(
        artifacts.existsByCycleIdAndProductIdAndExperimentIdAndPrototypeVersionAndProfileCodeAndStatus(
            cycle.getId(), cycle.getProductId(), cycle.getExperimentId(), cycle.getProductVersion(),
            capability.profileCode(), "READY"),
        "Prepare e confira o pacote utilizável desta versão antes de registrar sua prova.");
  }

  /** Valida o briefing antes de enfileirar uma composição durável e reutilizável. */
  @Transactional
  public SessionView input(String token, Input input) {
    var session = active(token, true);
    var cycle = cycles.findLockedById(session.getCycleId()).orElseThrow();
    require(
        "OPEN".equals(cycle.getStatus())
            && cycle.getProductVersion().equals(session.getPrototypeVersion()),
        "O ciclo ou a versão mudou; preserve o pacote anterior.");
    require(input.consentAccepted(), "Confirme o consentimento simulado antes da preparação.");
    require(
        input.email().endsWith("@sandbox.local") && "00000000000".equals(input.whatsapp()),
        "Esta experiência usa somente contatos fictícios sem rota externa.");
    var payload = read(session.getPayloadJson());
    String raw = write(input);
    String digest = hash(raw.getBytes(StandardCharsets.UTF_8));
    if (session.getArtifactId() != null) {
      var artifact = artifacts.findById(session.getArtifactId()).orElseThrow();
      require(
          artifact.getInputSha256().equals(digest),
          "O pacote já foi reservado. Outra entrada exige um novo acesso de QA.");
      return view(session);
    }
    payload.set("input", json.valueToTree(input));
    payload.put("consentObservedAt", clock.instant().toString());
    if ("SAFETY".equals(session.getScenarioCode())) {
      payload
          .put("status", "BLOCKED_SAFE")
          .put(
              "reason",
              "A tentativa de origem ou efeito externo foi bloqueada antes da composição.");
      record(payload, "SAFETY_LIMIT_BLOCKED", UUID.randomUUID().toString());
      session.setPayloadJson(write(payload));
      return view(sessions.saveAndFlush(session));
    }
    String owner = session.getScenarioCode();
    var artifact =
        artifacts
            .findByCycleIdAndFixtureOwnerAndInputSha256(cycle.getId(), owner, digest)
            .orElse(null);
    if (artifact == null) {
      require(
          artifacts.countByCycleId(cycle.getId()) < 6
              && artifacts.countByCycleIdAndFixtureOwner(cycle.getId(), owner) < 2,
          "O limite de seis composições por ciclo e duas por fixture foi atingido. Não há renovação por dispositivo ou repetição.");
      artifact = new KitPrivateArtifact();
      artifact.setId(UUID.randomUUID().toString());
      artifact.setCycleId(cycle.getId());
      artifact.setProductId(cycle.getProductId());
      artifact.setExperimentId(cycle.getExperimentId());
      artifact.setPrototypeVersion(cycle.getProductVersion());
      artifact.setProfileCode(payload.path("profileCode").asText());
      artifact.setFixtureOwner(owner);
      artifact.setInputSha256(digest);
      artifact.setInputJson(raw);
      artifact.setStatus("QUEUED");
      artifact.setCreatedAt(clock.instant());
      artifacts.saveAndFlush(artifact);
    }
    session.setArtifactId(artifact.getId());
    payload.put("status", "INPUT_CONFIRMED");
    session.setPayloadJson(write(payload));
    sessions.saveAndFlush(session);
    return view(session);
  }

  /** Fornece somente trabalho previamente reservado pelo backend para o compositor. */
  @Transactional(readOnly = true)
  public List<Pending> pending() {
    return artifacts.pending(PageRequest.of(0, 3)).stream()
        .map(id -> pending(artifacts.findById(id).orElseThrow()))
        .toList();
  }

  /** Reserva a tentativa exata sem permitir claims concorrentes ou reabrir uma falha terminal. */
  @Transactional
  public Pending claim(String id, Claim input) {
    var a = artifacts.locked(id).orElseThrow();
    require(
        "OPEN".equals(cycles.findById(a.getCycleId()).orElseThrow().getStatus()),
        "O ciclo foi encerrado.");
    if ("RUNNING".equals(a.getStatus()) && input.claimKey().toString().equals(a.getClaimKey()))
      return pending(a);
    require("QUEUED".equals(a.getStatus()), "A composição não está pendente.");
    a.setStatus("RUNNING");
    a.setClaimKey(input.claimKey().toString());
    a.setStartedAt(clock.instant());
    artifacts.saveAndFlush(a);
    return pending(a);
  }

  /** Valida e aplica o resultado integral sem transformar resposta inválida em sucesso. */
  @Transactional
  public void complete(String id, Result input) {
    var a = artifacts.locked(id).orElseThrow();
    require(
        input.claimKey().toString().equals(a.getClaimKey()),
        "Callback não pertence à tentativa reservada.");
    require(input.providerCalls() == 0, "A composição privada não admite provedor pago.");
    byte[] bytes = Base64.getDecoder().decode(input.zipBase64());
    require(hash(bytes).equals(input.zipSha256()), "Hash do arquivo recebido diverge do callback.");
    if ("READY".equals(a.getStatus())) {
      require(input.zipSha256().equals(a.getZipSha256()), "Callback duplicado alterou o artefato.");
      return;
    }
    require("RUNNING".equals(a.getStatus()), "A tentativa não está em execução.");
    try {
      var manifest = contract.validate(bytes);
      a.setZipBytes(bytes);
      a.setZipSha256(input.zipSha256());
      a.setManifestJson(write(manifest));
      a.setStatus("READY");
      a.setFinishedAt(clock.instant());
      artifacts.saveAndFlush(a);
      log.info(
          "Kit privado: composição aceita artifactId={} productId={} cycleId={} experimentId={} sha256={} bytes={} providerCalls=0",
          a.getId(),
          a.getProductId(),
          a.getCycleId(),
          a.getExperimentId(),
          a.getZipSha256(),
          bytes.length);
    } catch (Exception ex) {
      log.error("Kit privado: resultado inválido artifactId={} cycleId={}", id, a.getCycleId(), ex);
      throw conflict("O arquivo não comprovou o contrato da composição: " + ex.getMessage());
    }
  }

  /** Preserva a tentativa falha e seu motivo sem reenfileirar produção ou apagar consumo. */
  @Transactional
  public void fail(String id, Failure input) {
    var a = artifacts.locked(id).orElseThrow();
    require(
        input.claimKey().toString().equals(a.getClaimKey()), "Falha não corresponde à tentativa.");
    if ("FAILED".equals(a.getStatus())) {
      require(input.error().equals(a.getErrorMessage()), "Falha duplicada divergente.");
      return;
    }
    require("RUNNING".equals(a.getStatus()), "Tentativa não está em execução.");
    a.setStatus("FAILED");
    a.setErrorMessage(input.error());
    a.setFinishedAt(clock.instant());
    artifacts.saveAndFlush(a);
  }

  /** Registra ação concluída da própria sessão, preservando origem técnica e idempotência. */
  @Transactional
  public SessionView event(String token, Event input) {
    var s = active(token, true);
    var data = read(s.getPayloadJson());
    require(ACTIONS.contains(input.code()), "Evento não permitido.");
    String id = input.eventId().toString();
    for (var e : data.path("events"))
      if (id.equals(e.path("eventId").asText())) {
        require(
            input.code().equals(e.path("code").asText()),
            "Identificador de evento reutilizado com outro conteúdo.");
        return view(s);
      }
    if (!Set.of("SESSION_EXITED", "SESSION_RETURNED").contains(input.code()))
      require(
          s.getArtifactId() != null
              && "READY".equals(artifacts.findById(s.getArtifactId()).orElseThrow().getStatus()),
          "O evento exige pacote íntegro da sessão.");
    if ("SESSION_RETURNED".equals(input.code()))
      require(
          data.path("events").findValuesAsText("code").contains("SESSION_EXITED"),
          "Retorno exige saída registrada.");
    record(data, input.code(), id);
    s.setPayloadJson(write(data));
    return view(sessions.saveAndFlush(s));
  }

  /** Autoriza uma transferência própria, sem permitir acesso por ID de outro artefato. */
  @Transactional
  public byte[] download(String token) {
    var s = active(token, true);
    var data = read(s.getPayloadJson());
    require(
        data.path("transfers").asInt() < 10,
        "O limite técnico de dez transferências foi atingido.");
    var a = readyArtifact(s);
    data.put("transfers", data.path("transfers").asInt() + 1);
    s.setPayloadJson(write(data));
    sessions.saveAndFlush(s);
    return a.getZipBytes();
  }

  /** Entrega somente a imagem do pacote autorizado e recusa caminhos ou nomes fora do contrato. */
  @Transactional(readOnly = true)
  public byte[] asset(String token, String name) {
    require(
        name.matches("(post|story)-0[1-9]\\.png|(post|story)-10\\.png"), "Arquivo não permitido.");
    var a = readyArtifact(active(token, false));
    try {
      var value = contract.entries(a.getZipBytes()).get(name);
      require(value != null, "Arquivo ausente.");
      return value;
    } catch (Exception ex) {
      log.error("Kit privado: falha ao ler imagem artifactId={} file={}", a.getId(), name, ex);
      throw conflict("A imagem privada não pôde ser conferida.");
    }
  }

  /** Revoga acesso sem apagar entrada, custo, eventos ou resultado anteriores. */
  @Transactional
  public void revoke(String id) {
    var s = sessions.findById(id).orElseThrow();
    s.setRevoked(true);
    sessions.saveAndFlush(s);
  }

  /** Expõe auditoria sanitizada para o ciclo solicitado, nunca credenciais nem bytes do ZIP. */
  @Transactional(readOnly = true)
  public Report report(Long cycleId) {
    return new Report(
        "PDE_PRIVATE_KIT_REPORT_V1",
        cycleId,
        sessions.findByCycleIdOrderByCreatedAtAsc(cycleId).stream().map(this::view).toList(),
        0,
        false);
  }

  /** Resolve acesso válido sem revelar se outro pacote existe. */
  private KitPrivateSession active(String token, boolean locked) {
    require(token != null && token.length() <= 256, "Acesso privado inválido.");
    String digest = hash(token.getBytes(StandardCharsets.UTF_8));
    var s =
        (locked ? sessions.locked(digest) : sessions.findBySessionHash(digest))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Acesso privado inválido."));
    if (s.isRevoked() || !s.getExpiresAt().isAfter(clock.instant()))
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Acesso privado expirado ou revogado.");
    return s;
  }

  /** Exige artefato íntegro vinculado à sessão autenticada. */
  private KitPrivateArtifact readyArtifact(KitPrivateSession s) {
    require(s.getArtifactId() != null, "O pacote ainda não está disponível.");
    var a = artifacts.findById(s.getArtifactId()).orElseThrow();
    require(
        a.getCycleId().equals(s.getCycleId())
            && a.getProductId().equals(s.getProductId())
            && a.getPrototypeVersion().equals(s.getPrototypeVersion())
            && a.getFixtureOwner().equals(s.getScenarioCode())
            && "READY".equals(a.getStatus()),
        "Pacote indisponível ou incompatível com o acesso.");
    return a;
  }

  /** Organiza estado e saída reais para a interface, com metadados técnicos separados. */
  private SessionView view(KitPrivateSession s) {
    var data = read(s.getPayloadJson());
    KitPrivateArtifact a =
        s.getArtifactId() == null ? null : artifacts.findById(s.getArtifactId()).orElseThrow();
    String status = a == null ? data.path("status").asText() : a.getStatus();
    var manifest = a == null || a.getManifestJson() == null ? null : read(a.getManifestJson());
    String reason =
        switch (status) {
          case "QUEUED" -> "A composição está reservada e aguarda o executor.";
          case "RUNNING" -> "O compositor está produzindo o pacote desta entrada.";
          case "READY" ->
              "O pacote completo está disponível. Abra a primeira aplicação e guarde sua cópia.";
          case "FAILED" -> a.getErrorMessage();
          case "BLOCKED_SAFE" -> data.path("reason").asText();
          default -> "Confira o briefing sintético antes da preparação.";
        };
    var list = new ArrayList<JsonNode>();
    data.path("events").forEach(list::add);
    return new SessionView(
        s.getId(),
        s.getProductId(),
        s.getCycleId(),
        s.getExperimentId(),
        s.getPrototypeVersion(),
        s.getScenarioCode(),
        s.getDeviceProfile(),
        data.path("profileCode").asText(),
        "nails-v1".equals(data.path("profileCode").asText())
            ? "Agenda Cheia Nail Design"
            : "Kit de barbearia",
        status,
        reason,
        data.path("input"),
        manifest == null ? null : manifest.path("firstApplication"),
        manifest,
        list,
        data.path("transfers").asInt(),
        s.getExpiresAt());
  }

  /** Entrega contexto de composição sem acesso, byte de arquivo ou efeito externo. */
  private Pending pending(KitPrivateArtifact a) {
    return new Pending(
        a.getId(),
        a.getProductId(),
        a.getCycleId(),
        a.getExperimentId(),
        a.getPrototypeVersion(),
        a.getProfileCode(),
        a.getFixtureOwner(),
        read(a.getInputJson()));
  }

  /** Calcula credencial estável por sessão, tornando a emissão idempotente sem segredo no banco. */
  private String secret(String id) {
    return hash((pepper + ":" + id).getBytes(StandardCharsets.UTF_8));
  }

  /** Registra correlação temporal e origem segregada de cada ação técnica. */
  private void record(ObjectNode payload, String code, String id) {
    payload
        .withArray("events")
        .addObject()
        .put("eventId", id)
        .put("code", code)
        .put("occurredAt", clock.instant().toString())
        .put("receivedAt", clock.instant().toString())
        .put("origin", "AGENT_VALIDATION")
        .put("mh_internal_test", true);
  }

  /** Lê payload de auditoria sem mascarar corrupção como estado válido. */
  private ObjectNode read(String raw) {
    try {
      return (ObjectNode) json.readTree(raw);
    } catch (Exception ex) {
      log.error("Kit privado: falha ao ler estado persistido", ex);
      throw new IllegalStateException(ex);
    }
  }

  /** Serializa contratos estruturados antes da gravação canônica. */
  private String write(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (Exception ex) {
      log.error("Kit privado: falha ao serializar estado", ex);
      throw new IllegalStateException(ex);
    }
  }

  /** Recusa uma condição funcional mantendo a causa visível ao operador. */
  private void require(boolean valid, String reason) {
    if (!valid) throw conflict(reason);
  }

  /** Converte conflito funcional sem apagar a prova anterior. */
  private ResponseStatusException conflict(String reason) {
    return new ResponseStatusException(HttpStatus.CONFLICT, reason);
  }
}
