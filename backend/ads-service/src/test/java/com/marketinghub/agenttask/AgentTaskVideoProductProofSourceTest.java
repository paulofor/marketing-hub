package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.repository.jpa.agenttask.AgentTaskVisualEvidenceRepository;
import com.marketinghub.salesvideo.service.VideoProductProofSource;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Confirma que captura complementar depende da prova aceita do mesmo produto e versão. */
class AgentTaskVideoProductProofSourceTest {
  /**
   * O contexto canônico permanece obrigatório; outro produto, versão, gate ou referência bloqueia.
   */
  @ParameterizedTest
  @ValueSource(longs = {90105, 90218})
  void requiresExactCurrentGate(long experiment) {
    var context = mock(CommunicationMaterializationContextProvider.class);
    var source =
        new AgentTaskVideoProductProofSource(
            mock(AgentTaskVisualEvidenceRepository.class),
            mock(AgentTaskVisualEvidenceService.class),
            context,
            new ObjectMapper());
    String reference = "experiment:" + experiment;
    var proof =
        new VideoProductProofSource.Proof(
            9901L, 9902L, "COMPLETED", reference + "@video-proof-v1", "{}", "abc");
    var authorization =
        new HashMap<String, Object>(
            Map.of(
                "contractVersion",
                "COMMUNICATION_VISUAL_PROOF_AUTHORIZATION_V1",
                "targetSourceReference",
                reference,
                "productId",
                91007L,
                "prototypeVersion",
                "fixture-v3",
                "gateInstanceId",
                99L,
                "publicUrl",
                "https://fixture.invalid/kit"));
    var input =
        new HashMap<String, Object>(
            Map.of(
                "inputReadiness",
                "READY",
                "visualProofAuthorization",
                authorization,
                "product",
                Map.of("id", 91007L),
                "approvedDestination",
                Map.of("url", "https://fixture.invalid/kit")));
    when(context.resolve(reference)).thenReturn(Optional.of(input));
    assertThat(source.isSupplementalAuthorizedFor(proof, reference, 91007L, "fixture-v3")).isTrue();
    assertThat(source.isSupplementalAuthorizedFor(proof, reference, 92018L, "fixture-v3"))
        .isFalse();
    assertThat(source.isSupplementalAuthorizedFor(proof, reference, 91007L, "fixture-v2"))
        .isFalse();
    assertThat(source.isSupplementalAuthorizedFor(proof, "experiment:1", 91007L, "fixture-v3"))
        .isFalse();
    authorization.put("gateInstanceId", 0L);
    assertThat(source.isSupplementalAuthorizedFor(proof, reference, 91007L, "fixture-v3"))
        .isFalse();
    authorization.put("gateInstanceId", 99L);
    input.put("inputReadiness", "BLOCKED");
    assertThat(source.isSupplementalAuthorizedFor(proof, reference, 91007L, "fixture-v3"))
        .isFalse();
  }
}
