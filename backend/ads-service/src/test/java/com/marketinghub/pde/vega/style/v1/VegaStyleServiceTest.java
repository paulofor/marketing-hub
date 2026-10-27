package com.marketinghub.pde.vega.style.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.pde.vega.privateprototype.v1.VegaPrivateSession;
import com.marketinghub.pde.vega.style.v1.service.*;
import com.marketinghub.pde.vega.style.v1.service.contract.VegaStyleContract.*;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.vega.VegaPrivateSessionRepository;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Comprova decisões contextuais, direitos, recuperação e isolamento sem provedor ou gasto. */
class VegaStyleServiceTest {
  private final VegaPrivateSessionRepository sessions = mock(VegaPrivateSessionRepository.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final ObjectMapper json = new ObjectMapper();
  private final List<VegaPrivateSession> saved = new ArrayList<>();
  private VegaStyleService service;
  private static final String VERSION = VegaStyleService.VERSION;

  /** A sucessora mantém ciclo, versão e sessão próprios sem converter o histórico da v16. */
  @Test
  void successorKeepsItsIdentityAndRejectsCrossVersionOperations() {
    var successor = cycles.findLockedById(92016L).orElseThrow();
    successor.setProductVersion(VegaStyleService.NEXT_VERSION);
    String original = create(91016L);
    String next =
        service
            .create(new Session(92016L, VegaStyleService.NEXT_VERSION))
            .path("sessionToken")
            .asText();
    service.start(original);
    service.start(next);
    assertThat(service.workspace(original).path("product").path("experienceVersion").asText())
        .isEqualTo(VERSION);
    assertThat(service.workspace(next).path("product").path("experienceVersion").asText())
        .isEqualTo(VegaStyleService.NEXT_VERSION);
    assertThat(
            service
                .workspace(next)
                .path("product")
                .path("commercialAccess")
                .path("experienceVersion")
                .asText())
        .isEqualTo(VegaStyleService.NEXT_VERSION);
    var request =
        new Generate(
            VegaStyleService.NEXT_VERSION,
            "successor-operation-001",
            answers("almoço", "Cor e criatividade"));
    var result = service.generate(next, "estilo-dia-1", request);
    assertThat(service.generate(next, "estilo-dia-1", request)).isEqualTo(result);
    assertThatThrownBy(() -> service.generate(original, "estilo-dia-1", request))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> service.create(new Session(91016L, VegaStyleService.NEXT_VERSION)))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(service.report(original).path("operations").size()).isZero();
    assertThat(service.report(next).path("prototypeVersion").asText())
        .isEqualTo(VegaStyleService.NEXT_VERSION);
    assertThat(service.contract().path("supportedPrototypeVersions").toString())
        .contains(VERSION, VegaStyleService.NEXT_VERSION);
  }

  /** Simula cadastros e mantém objetos persistidos para exercitar os comandos reais do serviço. */
  @BeforeEach
  void setUp() {
    for (long id : List.of(91016L, 92016L)) {
      var cycle = new LearningSalesCycle();
      cycle.setId(id);
      cycle.setProductId(id + 1);
      cycle.setExperimentId(id + 2);
      cycle.setStatus("OPEN");
      cycle.setStage("ADJUSTMENT");
      cycle.setProductVersion(VERSION);
      when(cycles.findLockedById(id)).thenReturn(Optional.of(cycle));
      when(products.findById(id + 1))
          .thenReturn(Optional.of(Product.builder().id(id + 1).slug("metodo-musa-7-dias").build()));
    }
    when(sessions.saveAndFlush(any()))
        .thenAnswer(
            call -> {
              saved.add(call.getArgument(0));
              return call.getArgument(0);
            });
    when(sessions.findBySessionHash(anyString()))
        .thenAnswer(
            call ->
                saved.stream()
                    .filter(s -> s.getSessionHash().equals(call.getArgument(0)))
                    .findFirst());
    service = new VegaStyleService(sessions, cycles, products, json);
  }

  /** A ocasião e a preferência mudam a decisão; remover recurso torna a orientação indisponível. */
  @Test
  void comparesRealContextAndFailsClosedWithoutInventingResources() {
    var lunch =
        VegaContextualStyleRules.decide(
            "estilo-dia-1", answers("almoço", "Cor e criatividade"), Map.of());
    var video =
        VegaContextualStyleRules.decide(
            "estilo-dia-1", answers("videochamada", "Cor e criatividade"), Map.of());
    var discreet =
        VegaContextualStyleRules.decide(
            "estilo-dia-1", answers("videochamada", "Discrição"), Map.of());
    assertThat(lunch.actions().getFirst()).contains("alça");
    assertThat(video.actions().getFirst()).contains("pescoço");
    assertThat(discreet.ruleId()).isNotEqualTo(video.ruleId());
    var missing = new LinkedHashMap<>(answers("almoço", "Cor e criatividade"));
    missing.put("existingSelection", "camisa e lenço; posso usar no pescoço");
    var blocked = VegaContextualStyleRules.decide("estilo-dia-1", missing, Map.of());
    assertThat(blocked.status()).isEqualTo("BLOCKED");
    assertThat(blocked.actions()).isEmpty();
    assertThat(blocked.headline()).isEmpty();
    assertThat(blocked.limitation()).isNotBlank();
    for (String text :
        List.of(
            "não tenho bolsa",
            "camisa, lenço, sem bolsa; posso usar lenço no pescoço ou na alça",
            "quero comprar uma bolsa",
            "talvez possa usar lenço",
            "vestido sem opções declaradas")) {
      missing.put("existingSelection", text);
      assertThat(VegaContextualStyleRules.decide("estilo-dia-1", missing, Map.of()).status())
          .isEqualTo("BLOCKED");
    }
  }

  /** O replay consulta a resposta persistida e recusa outra entrada para a mesma operação. */
  @Test
  void recoversWithoutGenerationAndIsolatesSessionsAndVersions() {
    String token = create(91016L);
    service.start(token);
    var request = request("operation-context-0001", answers("almoço", "Cor e criatividade"));
    var first = service.generate(token, "estilo-dia-1", request);
    service = new VegaStyleService(sessions, cycles, products, json);
    assertThat(service.generate(token, "estilo-dia-1", request)).isEqualTo(first);
    assertThat(service.report(token).path("operations").size()).isEqualTo(1);
    assertThatThrownBy(
            () ->
                service.generate(
                    token,
                    "estilo-dia-1",
                    request("operation-context-0001", answers("videochamada", "Discrição"))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("outra entrada");
    String other = create(92016L);
    service.start(other);
    assertThatThrownBy(() -> service.result(other, request.operationKey()))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(
            () ->
                service.generate(
                    token,
                    "estilo-dia-1",
                    new Generate("older-version", "operation-context-0002", request.answers())))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(service.generate(other, "estilo-dia-1", request).path("status").asText())
        .isEqualTo("COMPLETED");
    assertThat(saved.getFirst().getSessionHash()).doesNotContain(token);
  }

  /** Sete missões e materiais preservam reflexões; acesso gratuito não concede a continuidade. */
  @Test
  void completesJourneyWithIndependentFixtureAndPreservesExpiry() {
    String token = create(91016L);
    service.start(token);
    assertThatThrownBy(
            () ->
                service.generate(
                    token,
                    "estilo-dia-2",
                    request("operation-day-0002", answers("almoço", "Discrição"))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("acesso completo");
    assertThatThrownBy(() -> service.material(token, "mapa-do-meu-estilo.txt"))
        .isInstanceOf(ResponseStatusException.class);
    service.fixture(token, "full-access");
    for (int day = 1; day <= 7; day++) {
      var input = new LinkedHashMap<>(answers("almoço", "Discrição"));
      var result =
          service.generate(token, "estilo-dia-" + day, request("operation-day-000" + day, input));
      if (day == 7)
        assertThat(result.path("signals").toString())
            .contains("Reflexão sintética dia 1", "Reflexão sintética dia 6");
      String missionId = "estilo-dia-" + day;
      assertThatThrownBy(() -> service.complete(token, missionId))
          .isInstanceOf(ResponseStatusException.class);
      input.put("reflection", "Reflexão sintética dia " + day);
      service.reflect(token, "estilo-dia-" + day, new Reflection(input));
      service.complete(token, "estilo-dia-" + day);
    }
    assertThat(service.workspace(token).path("progressPercent").asInt()).isEqualTo(100);
    for (String material :
        List.of("mapa-do-meu-estilo.txt", "antes-de-sair.txt", "minhas-formulas.txt"))
      assertThat(service.material(token, material))
          .contains("Reflexão sintética dia 1", "Reflexão sintética dia 7");
    service.fixture(token, "expire-entitlement");
    assertThat(service.workspace(token).path("subscriptionStatus").asText()).isEqualTo("TRIAL");
    assertThatThrownBy(() -> service.material(token, "mapa-do-meu-estilo.txt"))
        .isInstanceOf(ResponseStatusException.class);
    service.fixture(token, "expire-session");
    assertThatThrownBy(() -> service.workspace(token))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("terminou");
  }

  /** Uma resposta bloqueada não produz sinais de valor, e checkout não depende de preferência. */
  @Test
  void separatesEventsFromLoadingFailuresAndCommercialActions() {
    String token = create(91016L);
    assertThat(service.report(token).path("events").size()).isZero();
    service.start(token);
    var input = answers("almoço", "Discrição");
    service.generate(token, "estilo-dia-1", request("operation-event-0001", input));
    assertThatThrownBy(
            () ->
                service.event(token, new Event("READY_RESULT_USED", "operation-event-0001"), false))
        .isInstanceOf(ResponseStatusException.class);
    service.event(token, new Event("CHECKOUT_STARTED", "operation-event-0001"), false);
    assertThat(service.workspace(token).path("subscriptionStatus").asText()).isEqualTo("TRIAL");
    assertThatThrownBy(
            () ->
                service.event(
                    token, new Event("PREFERRED_OVER_FREE", "operation-event-0001"), false))
        .isInstanceOf(ResponseStatusException.class);
    service.event(token, new Event("VALUE_MOMENT", "operation-event-0001"), false);
    service.event(token, new Event("VALUE_MOMENT", "operation-event-0001"), false);
    service.event(token, new Event("READY_RESULT_USED", "operation-event-0001"), false);
    service.event(token, new Event("PREFERRED_OVER_FREE", "operation-event-0001"), true);
    assertThat(service.report(token).path("events").size()).isEqualTo(5);
    var invalid = new LinkedHashMap<>(input);
    invalid.put("existingSelection", "quero comprar outra roupa");
    var blocked = service.generate(token, "estilo-dia-1", request("operation-event-0002", invalid));
    assertThat(blocked.path("status").asText()).isEqualTo("BLOCKED");
    assertThat(blocked.path("microActions").size()).isZero();
    assertThatThrownBy(
            () -> service.event(token, new Event("VALUE_MOMENT", "operation-event-0002"), false))
        .isInstanceOf(ResponseStatusException.class);
  }

  /** Cria uma identidade técnica independente, sem codificar IDs produtivos na implementação. */
  private String create(long cycle) {
    return service.create(new Session(cycle, VERSION)).path("sessionToken").asText();
  }

  /** Mantém constante a disponibilidade das duas formas para isolar o efeito do contexto. */
  private Map<String, String> answers(String occasion, String preference) {
    return Map.of(
        "occasion",
        occasion,
        "existingSelection",
        "camisa, lenço e bolsa; posso usar o lenço no pescoço ou na alça",
        "stylePreference",
        preference);
  }

  /** Declara identidade idempotente independente do conteúdo da resposta. */
  private Generate request(String key, Map<String, String> answers) {
    return new Generate(VERSION, key, answers);
  }
}
