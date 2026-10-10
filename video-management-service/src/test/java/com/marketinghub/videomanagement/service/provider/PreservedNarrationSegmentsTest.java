package com.marketinghub.videomanagement.service.provider;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.reactive.function.client.WebClient;

/** Confere áudio auditado e impede nova síntese como fallback de recuperação inválida. */
class PreservedNarrationSegmentsTest {
  private final ObjectMapper mapper = new ObjectMapper();

  /** Recupera bytes em contextos distintos e conserva custo original desconhecido. */
  @ParameterizedTest
  @ValueSource(longs = {21256, 91099})
  void restoresWithoutAnotherTtsRequest(long jobId) throws Exception {
    try (var server = new MockWebServer()) {
      server.start();
      byte[] audio = {1, 2, 3, 4};
      server.enqueue(new MockResponse().setBody(new Buffer().write(audio)));
      var result = PreservedNarrationSegments.restore(metadata(server.url("/audio.mp3").toString(), audio),
          List.of("Texto recebido"), jobId, WebClient.builder().build(), mapper);
      try {
        assertThat(result).hasSize(1);
        assertThat(Files.readAllBytes(result.getFirst().file())).containsExactly(audio);
        assertThat(result.getFirst().interaction()).containsEntry("incremental_cost_usd", 0)
            .containsEntry("interaction_type", "TEXT_TO_SPEECH_REUSE");
        assertThat(result.getFirst().interaction().toString()).contains("PENDING_PROVIDER_RECONCILIATION");
        assertThat(server.getRequestCount()).isEqualTo(1);
        assertThat(server.takeRequest().getPath()).isEqualTo("/audio.mp3");
      } finally {
        for (var segment : result) Files.deleteIfExists(segment.file());
      }
    }
  }

  /** Bloqueia contrato ou binário divergente sem usar TTS para mascarar a falha. */
  @ParameterizedTest
  @ValueSource(strings = {"hash", "text", "tenant", "order", "tts", "url"})
  void rejectsInvalidRecovery(String scenario) throws Exception {
    try (var server = new MockWebServer()) {
      server.start();
      byte[] audio = {1, 2, 3, 4};
      server.enqueue(new MockResponse().setBody(new Buffer().write(audio)));
      var metadata = metadata(server.url("/audio.mp3").toString(), audio);
      var contract = (ObjectNode) metadata.path("preservedNarration");
      var segment = (ObjectNode) contract.path("segments").get(0);
      switch (scenario) {
        case "hash" -> segment.put("sha256", "a".repeat(64));
        case "text" -> segment.put("text", "Outro texto");
        case "tenant" -> contract.put("tenantId", "other");
        case "order" -> segment.put("segmentIndex", 2);
        case "tts" -> contract.put("newTtsAuthorized", true);
        case "url" -> segment.put("url", "file:///etc/private");
      }
      assertThatThrownBy(() -> PreservedNarrationSegments.restore(metadata, List.of("Texto recebido"),
          91099L, WebClient.builder().build(), mapper))
          .isInstanceOf(VideoProviderException.class).hasFieldOrPropertyWithValue("code", "PRESERVED_NARRATION_INVALID");
      assertThat(server.getRequestCount()).isEqualTo(scenario.equals("hash") ? 1 : 0);
    }
  }

  /** Cria contrato isolado que será verificado contra bytes entregues pela rede local. */
  private ObjectNode metadata(String url, byte[] bytes) throws Exception {
    var node = mapper.createObjectNode().put("tenantId", "fixture").put("captionText", "Texto recebido");
    var contract = node.putObject("preservedNarration");
    contract.put("contractVersion", "PRESERVED_TTS_NARRATION_V1").put("sourceJobId", 91098)
        .put("tenantId", "fixture").put("captionText", "Texto recebido").put("newTtsAuthorized", false);
    var segment = contract.putArray("segments").addObject();
    segment.put("segmentIndex", 1).put("assetId", 91097).put("url", url).put("text", "Texto recebido")
        .put("model", "tts-model").put("voice", "marin")
        .put("sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
    segment.putObject("sourcePricing").put("reconciliation_status", "PENDING_PROVIDER_RECONCILIATION").putNull("known_cost_usd");
    return node;
  }
}
