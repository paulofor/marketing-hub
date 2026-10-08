package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir que prova de outro ciclo ou versão libere a construção privada. */
class LearningCyclePrototypeContextTest {
  private final ObjectMapper json = new ObjectMapper();
  private final LearningSalesCycleEventRepository events =
      mock(LearningSalesCycleEventRepository.class);
  private final LearningCyclePrototypeContext service =
      new LearningCyclePrototypeContext(events, json);

  /** Monta uma prova sintética completa da versão privada. */
  private ObjectNode proof() {
    var p = json.createObjectNode();
    p.put("prototypeVersion", "new-v9");
    p.put("privateAccessUrl", "https://private.invalid/vega-private");
    p.put("image", "repo/image:tested");
    p.put("evidenceReference", "Duas rodadas locais, IDs sintéticos, sem operação comercial");
    p.put("observedAt", Instant.now().toString());
    for (String k :
        List.of(
            "desktopValidated",
            "mobileValidated",
            "firstResultValidated",
            "resumeValidated",
            "failuresValidated",
            "testDataExcluded",
            "noExternalSideEffects")) p.put(k, true);
    return p;
  }

  /** Recusa URL com segredo, prova incompleta ou versão diferente. */
  @Test
  void rejectsInvalidProofs() {
    assertThatCode(() -> service.validate(proof(), "new-v9")).doesNotThrowAnyException();
    assertThatThrownBy(() -> service.validate(proof(), "other-version"));
    var p = proof();
    p.put("privateAccessUrl", "https://private.invalid/vega-private?token=secret");
    assertThatThrownBy(() -> service.validate(p, "new-v9"));
    var q = proof();
    q.put("resumeValidated", false);
    assertThatThrownBy(() -> service.validate(q, "new-v9"));
  }

  /** Resolve somente a evidência do ciclo solicitado e perde aceitação ao trocar a versão. */
  @Test
  void bindsOnlyExactVersionAndCycle() throws Exception {
    var cycle = new LearningSalesCycle();
    cycle.setId(2L);
    cycle.setProductVersion("new-v9");
    var event = new LearningSalesCycleEvent();
    event.setId(9L);
    event.setCycleId(2L);
    event.setAction("REWORK");
    event.setCreatedAt(Instant.now());
    var evidence = json.createObjectNode();
    evidence.put("productVersion", "new-v9");
    evidence.set("privatePrototype", proof());
    event.setEvidenceJson(json.writeValueAsString(evidence));
    when(events.findByCycleIdOrderByRevisionAsc(2L)).thenReturn(List.of(event));
    assertThat(service.resolve(cycle).orElseThrow().path("status").asText()).isEqualTo("READY");
    cycle.setProductVersion("new-v10");
    assertThat(service.resolve(cycle)).isEmpty();
    verify(events, times(2)).findByCycleIdOrderByRevisionAsc(2L);
  }

  /** Preserva a prova antiga do kit e usa somente o suplemento do pacote versionado corrigido. */
  @Test
  void preservesPreviousKitProofAndRequiresCorrectedPackage() throws Exception {
    var cycle = new LearningSalesCycle();
    cycle.setId(819L);
    cycle.setStatus("OPEN");
    cycle.setProductVersion("kit-version-17");
    var original = new LearningSalesCycleEvent();
    original.setId(710L);
    original.setAction("REGISTER_PROTOTYPE");
    original.setCreatedAt(Instant.now());
    String originalEvidence =
        "{\"productVersion\":\"kit-version-17\",\"privatePrototype\":{\"prototypeVersion\":\"kit-version-17\",\"runtimeKind\":\"DETERMINISTIC_PRIVATE_KIT_V1\"}}";
    original.setEvidenceJson(originalEvidence);
    when(events.findByCycleIdOrderByRevisionAsc(819L)).thenReturn(List.of(original));
    assertThat(service.resolve(cycle)).isEmpty();
    assertThat(service.hasSupersededKitProof(cycle)).isTrue();
    var supplement = new LearningSalesCycleEvent();
    supplement.setId(711L);
    supplement.setAction("REGISTER_PROTOTYPE");
    supplement.setCreatedAt(Instant.now());
    var corrected = (ObjectNode) json.readTree(originalEvidence);
    ((ObjectNode) corrected.path("privatePrototype"))
        .put("packageContractVersion", "PDE_PRIVATE_KIT_PACKAGE_V2");
    supplement.setEvidenceJson(corrected.toString());
    when(events.findByCycleIdOrderByRevisionAsc(819L)).thenReturn(List.of(original, supplement));
    assertThat(service.resolve(cycle).orElseThrow().path("packageContractVersion").asText())
        .isEqualTo("PDE_PRIVATE_KIT_PACKAGE_V2");
    assertThat(original.getEvidenceJson()).isEqualTo(originalEvidence);
  }

  /** Mantém a leitura da prova encerrada sem renovar aceite, pacote ou orçamento. */
  @Test
  void closedKitCycleKeepsItsHistoricalProof() {
    var cycle = new LearningSalesCycle();
    cycle.setId(941L);
    cycle.setStatus("CLOSED");
    cycle.setProductVersion("historical-kit-v1");
    var event = new LearningSalesCycleEvent();
    event.setAction("REGISTER_PROTOTYPE");
    event.setCreatedAt(Instant.EPOCH);
    String original =
        "{\"productVersion\":\"historical-kit-v1\",\"privatePrototype\":{\"prototypeVersion\":\"historical-kit-v1\",\"runtimeKind\":\"DETERMINISTIC_PRIVATE_KIT_V1\"}}";
    event.setEvidenceJson(original);
    when(events.findByCycleIdOrderByRevisionAsc(941L)).thenReturn(List.of(event));
    assertThat(service.resolve(cycle)).isPresent();
    assertThat(service.hasSupersededKitProof(cycle)).isFalse();
    assertThat(event.getEvidenceJson()).isEqualTo(original);
  }
}
