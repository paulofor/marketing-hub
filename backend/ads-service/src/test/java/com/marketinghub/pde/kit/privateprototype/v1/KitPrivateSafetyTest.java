package com.marketinghub.pde.kit.privateprototype.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.pde.kit.privateprototype.v1.service.KitPrivateService;
import com.marketinghub.pde.kit.privateprototype.v1.service.KitPrototypeCapabilities;
import com.marketinghub.pde.kit.privateprototype.v1.service.contract.KitPrivateContract.*;
import com.marketinghub.repository.jpa.kit.*;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir resultado falso e causa fabricada nos bloqueios dos kits privados. */
class KitPrivateSafetyTest {
  /** Confere bloqueio, causa preservada e ação do mesmo ciclo em dois produtos distintos. */
  @Test
  void preservesExplainedBlockWithoutArtifactOrValueEvent() {
    for (long productId : List.of(8107L, 9218L)) {
      for (SafetyCase safetyCase : SafetyCase.values()) {
        var fixture = fixture(productId);
        var request =
            new Create(
                UUID.randomUUID(),
                productId,
                productId + 100,
                "test-v2",
                "SAFETY",
                "DESKTOP_1440",
                safetyCase);
        var access = fixture.service.create(request);
        var blocked = fixture.service.input(access.sessionToken(), briefing());
        assertThat(blocked.status()).isEqualTo("BLOCKED_SAFE");
        assertThat(blocked.presentation().title()).contains("bloqueada").doesNotContain("pronto");
        assertThat(blocked.presentation().introduction()).contains("Nenhum pacote foi gerado");
        assertThat(blocked.presentation().reasonCode()).isEqualTo(safetyCase.name());
        assertThat(blocked.reason())
            .contains(
                safetyCase == SafetyCase.UNVERIFIED_VISUAL_ORIGIN
                    ? "origem das imagens"
                    : "publicação, envio ou cobrança");
        assertThat(blocked.presentation().nextActionPath())
            .isEqualTo(
                "/business-process-chains/learning-cycles?productId="
                    + productId
                    + "&cycleId="
                    + (productId + 100));
        assertThat(blocked.manifest()).isNull();
        assertThat(fixture.service.session(access.sessionToken())).isEqualTo(blocked);
        assertThat(fixture.service.create(request).sessionId()).isEqualTo(access.sessionId());
        assertThatThrownBy(
                () ->
                    fixture.service.create(
                        new Create(
                            request.requestKey(),
                            productId,
                            productId + 100,
                            "test-v2",
                            "SAFETY",
                            "DESKTOP_1440",
                            safetyCase == SafetyCase.EXTERNAL_ACTION
                                ? SafetyCase.UNVERIFIED_VISUAL_ORIGIN
                                : SafetyCase.EXTERNAL_ACTION)))
            .hasMessageContaining("outro conteúdo");
        assertThatThrownBy(
                () ->
                    fixture.service.event(
                        access.sessionToken(), new Event(UUID.randomUUID(), "VALUE_MOMENT")))
            .hasMessageContaining("pacote íntegro");
        verifyNoInteractions(fixture.artifacts);
      }
    }
  }

  /** Mantém a falta de causa histórica explícita, mesmo quando a razão antiga era genérica. */
  @Test
  void doesNotInventSpecificCauseForLegacySession() throws Exception {
    var fixture = fixture(8300L);
    var access =
        fixture.service.create(
            new Create(UUID.randomUUID(), 8300L, 8400L, "test-v2", "SAFETY", "PIXEL_7"));
    var blocked = fixture.service.input(access.sessionToken(), briefing());
    assertThat(blocked.presentation().reasonCode()).isEqualTo("REVIEW_REQUIRED");
    assertThat(blocked.reason()).contains("não registrou qual limite");
    var session = fixture.persisted.values().iterator().next();
    var payload = new ObjectMapper().readTree(session.getPayloadJson());
    ((com.fasterxml.jackson.databind.node.ObjectNode) payload).remove("safetyCase");
    ((com.fasterxml.jackson.databind.node.ObjectNode) payload)
        .put(
            "reason", "A tentativa de origem ou efeito externo foi bloqueada antes da composição.");
    session.setPayloadJson(payload.toString());
    assertThat(fixture.service.session(access.sessionToken()).reason())
        .contains("não registrou qual limite");
    verifyNoInteractions(fixture.artifacts);
  }

  /** Preserva o resultado utilizável e sua apresentação quando o pacote está realmente aceito. */
  @Test
  void displaysReadyResultOnlyForAcceptedArtifact() {
    var fixture = fixture(8500L);
    var access =
        fixture.service.create(
            new Create(UUID.randomUUID(), 8500L, 8600L, "test-v2", "ADHERENT", "DESKTOP_1440"));
    assertThat(access.session().presentation().title()).doesNotContain("pronto");
    var session = fixture.persisted.values().iterator().next();
    session.setArtifactId("artifact-test");
    var artifact = new KitPrivateArtifact();
    artifact.setStatus("READY");
    artifact.setManifestJson("{\"firstApplication\":{\"caption\":\"Resultado preservado\"}}");
    when(fixture.artifacts.findById("artifact-test")).thenReturn(Optional.of(artifact));
    var ready = fixture.service.session(access.sessionToken());
    assertThat(ready.presentation().title()).contains("pronto para usar");
    assertThat(ready.firstApplication().path("caption").asText()).isEqualTo("Resultado preservado");
    assertThat(ready.presentation().nextActionPath()).isEmpty();
  }

  /** Configura repositories simulados, mantendo o service real e a persistência observável. */
  private Fixture fixture(long productId) {
    var sessions = mock(KitPrivateSessionRepository.class);
    var artifacts = mock(KitPrivateArtifactRepository.class);
    var cycles = mock(LearningSalesCycleRepository.class);
    var capabilities = mock(KitPrototypeCapabilities.class);
    var persisted = new HashMap<String, KitPrivateSession>();
    var cycle = new LearningSalesCycle();
    cycle.setId(productId + 100);
    cycle.setProductId(productId);
    cycle.setExperimentId(productId + 200);
    cycle.setProductVersion("test-v2");
    cycle.setStatus("OPEN");
    when(cycles.findLocked(productId, productId + 100)).thenReturn(Optional.of(cycle));
    when(cycles.findLockedById(productId + 100)).thenReturn(Optional.of(cycle));
    when(capabilities.resolve(cycle))
        .thenReturn(new Capability(true, "nails-v1", "QA", "https://private.example/prototype"));
    when(sessions.saveAndFlush(any()))
        .thenAnswer(
            a -> {
              KitPrivateSession s = a.getArgument(0);
              persisted.put(s.getId(), s);
              return s;
            });
    when(sessions.findByCycleIdAndRequestKey(anyLong(), anyString()))
        .thenAnswer(
            a ->
                persisted.values().stream()
                    .filter(s -> s.getRequestKey().equals(a.getArgument(1)))
                    .findFirst());
    when(sessions.findBySessionHash(anyString()))
        .thenAnswer(
            a ->
                persisted.values().stream()
                    .filter(s -> s.getSessionHash().equals(a.getArgument(0)))
                    .findFirst());
    when(sessions.locked(anyString()))
        .thenAnswer(a -> sessions.findBySessionHash(a.getArgument(0)));
    var service =
        new KitPrivateService(
            sessions,
            artifacts,
            cycles,
            capabilities,
            null,
            new ObjectMapper(),
            "synthetic-unit-test");
    return new Fixture(service, artifacts, persisted);
  }

  /** Fornece somente dados fictícios válidos, sem contato ou ação externa. */
  private Input briefing() {
    return new Input(
        "teste+seguranca@sandbox.local",
        "Studio Teste",
        "Cidade Exemplo",
        "00000000000",
        "Serviço fictício",
        "elegante",
        "Apresentar serviço",
        "rosa",
        "",
        true);
  }

  /** Agrupa a implementação testada e seus registros sem acrescentar lógica de negócio. */
  private record Fixture(
      KitPrivateService service,
      KitPrivateArtifactRepository artifacts,
      Map<String, KitPrivateSession> persisted) {}
}
