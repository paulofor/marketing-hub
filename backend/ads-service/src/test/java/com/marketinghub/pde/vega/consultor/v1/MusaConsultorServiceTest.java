package com.marketinghub.pde.vega.consultor.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.pde.vega.consultor.v1.service.MusaConsultorService;
import com.marketinghub.pde.vega.privateprototype.v1.VegaPrivateSession;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.vega.VegaPrivateSessionRepository;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

/** Exercita continuidade, isolamento, validade e recuperação com identidades independentes. */
class MusaConsultorServiceTest {
  private final ObjectMapper json = new ObjectMapper();
  private final VegaPrivateSessionRepository sessions = mock(VegaPrivateSessionRepository.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final List<VegaPrivateSession> rows = new ArrayList<>();
  private MusaConsultorService service;

  /** Mantém cadastros sintéticos e agregados reais para testar o serviço sem integrações. */
  @BeforeEach
  void prepare() {
    service = new MusaConsultorService(sessions, cycles, json);
    for (long id : List.of(71022L, 82022L)) {
      var c = new LearningSalesCycle();
      c.setId(id);
      c.setProductId(id + 1);
      c.setExperimentId(id + 2);
      c.setStatus("OPEN");
      c.setStage("ADJUSTMENT");
      c.setProductVersion(MusaConsultorService.VERSION);
      when(cycles.findLockedById(id)).thenReturn(Optional.of(c));
      when(cycles.findById(id)).thenReturn(Optional.of(c));
    }
    when(sessions.saveAndFlush(any()))
        .thenAnswer(
            call -> {
              VegaPrivateSession row = call.getArgument(0);
              rows.add(row);
              return row;
            });
    when(sessions.findBySessionHash(anyString()))
        .thenAnswer(
            call ->
                rows.stream()
                    .filter(r -> Objects.equals(r.getSessionHash(), call.getArgument(0)))
                    .findFirst());
    when(sessions.findByGrantHash(anyString()))
        .thenAnswer(
            call ->
                rows.stream()
                    .filter(r -> Objects.equals(r.getGrantHash(), call.getArgument(0)))
                    .findFirst());
    when(sessions.findById(anyString()))
        .thenAnswer(
            call -> rows.stream().filter(r -> r.getId().equals(call.getArgument(0))).findFirst());
    when(sessions.findByCycleIdOrderByCreatedAtAsc(anyLong()))
        .thenAnswer(
            call -> rows.stream().filter(r -> r.getCycleId().equals(call.getArgument(0))).toList());
  }

  /** A mesma capacidade funciona em outro produto e mantém escolhas originais na adaptação. */
  @ParameterizedTest
  @ValueSource(longs = {71022, 82022})
  void savesAndResumesWithoutReenteringPieces(long cycle) {
    String token = access(cycle);
    var first = generate(token, "meal-operation-001", 0, "almoço", null);
    assertThat(first.path("action").asText()).contains("alça");
    service.command(
        token,
        "save",
        json.valueToTree(Map.of("operationId", "meal-operation-001", "revision", 1)),
        false);
    var input =
        json.valueToTree(
            Map.of(
                "operationKey",
                "video-operation-001",
                "revision",
                2,
                "parentChoiceId",
                "meal-operation-001",
                "answers",
                Map.of("occasion", "videochamada")));
    var resumed = service.command(token, "generate", input, false);
    assertThat(resumed.path("action").asText()).contains("pescoço");
    assertThat(resumed.path("answers").path("existingSelection"))
        .isEqualTo(first.path("answers").path("existingSelection"));
    assertThat(
            service
                .workspace(token)
                .path("choices")
                .path("meal-operation-001")
                .path("answers")
                .path("occasion")
                .asText())
        .isEqualTo("almoço");
    assertThat(service.workspace(token).path("choices").size()).isEqualTo(1);
    assertThat(service.report(token).path("execution").path("signals").has("READY_RESULT_USED"))
        .isFalse();
  }

  /** Replay e salvamento repetido recuperam o mesmo registro; entrada divergente é recusada. */
  @Test
  void recoversIdempotentlyAndRejectsStaleWrites() {
    String token = access(71022);
    var op = generate(token, "recovery-operation-001", 0, "almoço", null);
    var input = op.path("request");
    assertThat(service.command(token, "generate", input, false)).isEqualTo(op);
    assertThat(service.operation(token, "recovery-operation-001")).isEqualTo(op);
    var changed = input.deepCopy();
    ((ObjectNode) changed).put("revision", 99);
    assertThatThrownBy(() -> service.command(token, "generate", changed, false))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(
            () ->
                service.command(
                    token,
                    "draft",
                    json.valueToTree(Map.of("revision", 0, "answers", Map.of("occasion", "outra"))),
                    false))
        .isInstanceOf(ResponseStatusException.class);
    var save = json.valueToTree(Map.of("operationId", "recovery-operation-001", "revision", 1));
    assertThat(service.command(token, "save", save, false))
        .isEqualTo(service.command(token, "save", save, false));
    assertThat(service.report(token).path("execution").path("operations").size()).isEqualTo(1);
  }

  /** Acesso de outra identidade nunca encontra operação ou escolha alheia. */
  @Test
  void isolatesOwnersAndVersions() {
    String a = access(71022);
    String b = access(71022);
    generate(a, "owner-operation-0001", 0, "almoço", null);
    assertThatThrownBy(() -> service.operation(b, "owner-operation-0001"))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(
            () ->
                service.command(
                    b,
                    "generate",
                    json.valueToTree(
                        Map.of(
                            "operationKey",
                            "other-operation-0001",
                            "revision",
                            0,
                            "parentChoiceId",
                            "owner-operation-0001",
                            "answers",
                            Map.of("occasion", "jantar"))),
                    false))
        .isInstanceOf(ResponseStatusException.class);
    rows.getFirst().setPrototypeVersion("historical-version");
    assertThatThrownBy(() -> service.workspace(a)).isInstanceOf(ResponseStatusException.class);
  }

  /** Emissão dura 90 dias; login não renova; renovação explícita mantém dados e proprietário. */
  @Test
  void preservesNinetyDaysAndExplicitRenewal() {
    var issued = service.issue(71022L);
    var row = rows.getFirst();
    assertThat(Duration.between(row.getCreatedAt(), row.getExpiresAt()).toDays()).isEqualTo(90);
    var expires = row.getExpiresAt();
    String token = service.login(issued.path("accessKey").asText()).path("sessionToken").asText();
    assertThat(row.getExpiresAt()).isEqualTo(expires);
    service.command(token, "consent", json.valueToTree(Map.of("accepted", true)), false);
    generate(token, "renew-operation-0001", 0, "almoço", null);
    row.setExpiresAt(Instant.now().minusSeconds(1));
    assertThatThrownBy(() -> service.workspace(token)).isInstanceOf(ResponseStatusException.class);
    var renewed = service.renew(row.getId());
    assertThat(
            Duration.between(
                Instant.parse(renewed.path("issuedAt").asText()),
                Instant.parse(renewed.path("expiresAt").asText())))
        .isEqualTo(Duration.ofDays(90));
    assertThat(row.getExpiresAt()).isAfter(Instant.now().plus(89, ChronoUnit.DAYS));
    assertThat(service.operation(token, "renew-operation-0001").path("status").asText())
        .isEqualTo("COMPLETED");
  }

  /** Encerramento impede acesso, renovação e reemissão sem apagar resultados. */
  @Test
  void revokesAtClosureAndCannotReissue() {
    String token = access(71022);
    generate(token, "closed-operation-001", 0, "almoço", null);
    service.close(71022L);
    assertThat(rows.getFirst().isRevoked()).isTrue();
    assertThat(rows.getFirst().getInputJson()).contains("closed-operation-001");
    assertThatThrownBy(() -> service.workspace(token)).isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> service.renew(rows.getFirst().getId()))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> service.issue(71022L)).isInstanceOf(ResponseStatusException.class);
  }

  /** O fechamento canônico do ciclo nega também leituras e renovação sem chamada adicional. */
  @Test
  void deniesAfterCanonicalCycleCloses() {
    String token = access(71022);
    cycles.findById(71022L).orElseThrow().setStatus("CLOSED");
    assertThatThrownBy(() -> service.workspace(token)).isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> service.renew(rows.getFirst().getId()))
        .isInstanceOf(ResponseStatusException.class);
  }

  /** Fora da cobertura não existe cartão útil, e sinais não representam uso ou compra. */
  @Test
  void blocksUnsupportedAndSyntheticCommercialClaims() {
    String token = access(71022);
    var result =
        service.command(
            token,
            "generate",
            json.valueToTree(
                Map.of(
                    "operationKey",
                    "unsupported-operation-001",
                    "revision",
                    0,
                    "answers",
                    Map.of(
                        "occasion",
                        "passeio",
                        "existingSelection",
                        "calça e camiseta",
                        "stylePreference",
                        "Discrição"))),
            false);
    assertThat(result.path("status").asText()).isEqualTo("BLOCKED");
    assertThat(result.path("action").asText()).isEmpty();
    assertThatThrownBy(
            () ->
                service.command(
                    token,
                    "event",
                    json.valueToTree(
                        Map.of(
                            "eventType",
                            "VALUE_MOMENT",
                            "operationId",
                            "unsupported-operation-001")),
                    false))
        .isInstanceOf(ResponseStatusException.class);
    generate(token, "signal-operation-0001", 1, "almoço", null);
    for (String type : List.of("PREFERRED_OVER_FREE", "CHECKOUT_STARTED"))
      assertThatThrownBy(
              () ->
                  service.command(
                      token,
                      "event",
                      json.valueToTree(
                          Map.of("eventType", type, "operationId", "signal-operation-0001")),
                      false))
          .isInstanceOf(ResponseStatusException.class);
  }

  /**
   * Preferência editada e confirmada no rascunho orienta a próxima retomada sem alterar o original.
   */
  @Test
  void usesLatestConfirmedPreferenceOnResume() {
    String token = access(71022);
    generate(token, "preference-meal-001", 0, "almoço", null);
    service.command(
        token,
        "save",
        json.valueToTree(Map.of("operationId", "preference-meal-001", "revision", 1)),
        false);
    service.command(
        token,
        "draft",
        json.valueToTree(Map.of("revision", 2, "answers", Map.of("stylePreference", "Discrição"))),
        false);
    var next =
        service.command(
            token,
            "generate",
            json.valueToTree(
                Map.of(
                    "operationKey",
                    "preference-video-001",
                    "revision",
                    3,
                    "parentChoiceId",
                    "preference-meal-001",
                    "answers",
                    Map.of("occasion", "videochamada"))),
            false);
    assertThat(next.path("ruleId").asText()).isEqualTo("SCARF_VIDEO_STRAP");
    assertThat(
            service
                .workspace(token)
                .path("choices")
                .path("preference-meal-001")
                .path("answers")
                .path("stylePreference")
                .asText())
        .isEqualTo("Cor e criatividade");
  }

  /** A sessão começa com ciência explícita e nenhuma resposta pronta de fixture. */
  private String access(long cycle) {
    var issued = service.issue(cycle);
    String token = service.login(issued.path("accessKey").asText()).path("sessionToken").asText();
    service.command(token, "consent", json.valueToTree(Map.of("accepted", true)), false);
    return token;
  }

  /** Produz orientação usando as regras reais e fatos sintéticos declarados. */
  private JsonNode generate(
      String token, String key, int revision, String occasion, String parent) {
    return service.command(
        token,
        "generate",
        json.valueToTree(
            Map.of(
                "operationKey",
                key,
                "revision",
                revision,
                "parentChoiceId",
                parent == null ? "" : parent,
                "answers",
                Map.of(
                    "occasion",
                    occasion,
                    "existingSelection",
                    "Camisa, calça, lenço e bolsa. Posso usar o lenço no pescoço ou na alça da bolsa.",
                    "stylePreference",
                    "Cor e criatividade"))),
        false);
  }
}
