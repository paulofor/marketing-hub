package com.marketinghub.videomanagement.service.provider;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.videomanagement.client.dto.*;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.mockwebserver.*;
import okio.Buffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.web.reactive.function.client.WebClient;

/** Reproduz localmente a recuperação com binários preservados, FFmpeg real e rede segregada. */
class PostProductionNarrationRecoveryIntegrationTest {
  /** Recupera voz, artes e capturas locais e comprova sincronia e nível sonoro após codificação. */
  @Test
  @EnabledIfSystemProperty(named = "video.recovery.media-dir", matches = ".+")
  void recoversFiveReceivedSegmentsWithRealFfmpeg() throws Exception {
    var mapper = new ObjectMapper();
    Path root = Path.of(System.getProperty("video.recovery.media-dir"));
    var metadata = (ObjectNode) mapper.readTree(Files.readString(root.resolve("recovery-metadata.json")));
    var requests = new AtomicInteger();
    var synthesisRequests = new AtomicInteger();
    try (var server = new MockWebServer()) {
      server.setDispatcher(new Dispatcher() {
        /** Entrega binários locais da sequência correta e reprova qualquer chamada de síntese paga. */
        @Override public MockResponse dispatch(RecordedRequest request) {
          requests.incrementAndGet();
          String path = request.getPath();
          if (path.contains("/audio/speech")) { synthesisRequests.incrementAndGet(); return new MockResponse().setResponseCode(503); }
          Path file = root.resolve(path.substring(1));
          if (path.contains("/product-proof")) {
            Path specific = root.resolve(path.substring(path.lastIndexOf('/') + 1) + ".png");
            file = Files.exists(specific) ? specific : root.resolve("proof.png");
          }
          try { return new MockResponse().setBody(new Buffer().write(Files.readAllBytes(file))); }
          catch (java.io.IOException ex) { throw new java.io.UncheckedIOException(ex); }
        }
      });
      server.start();
      metadata.put("sourceVideoUrl", server.url("/3000.mp4").toString());
      for (var segment : metadata.at("/preservedNarration/segments")) {
        ((ObjectNode) segment).put("url", server.url("/" + segment.path("assetId").asLong() + ".mp3").toString());
      }
      var deliveryAssets = new java.util.HashSet<Long>();
      for (var shot : metadata.at("/post_production/deliveryExamples/shots")) {
        long id = shot.path("visualAssetId").asLong();
        deliveryAssets.add(id);
        ((ObjectNode) shot).put("url", server.url("/visual-" + id + ".png").toString());
      }
      var settings = new VideoManagementProperties();
      settings.setBackendBaseUrl(server.url("/").uri());
      settings.getProviders().getPostProduction().setOpenAiTtsEnabled(true);
      settings.getProviders().getPostProduction().setOpenAiApiKey("local-test-never-billable");
      settings.getProviders().getPostProduction().setOpenAiBaseUrl(server.url("/").uri());
      var job = new SalesVideoJob(91099L, 91066L, 91067L, "default", SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
          "MUSA_POST_PRODUCTION", null, SalesVideoJobType.POST_PRODUCTION, SalesVideoStatus.VIDEO_REQUESTED,
          1, null, 91098L, null, 0, null, null, "fixture@sandbox.local", Instant.now(), null, null, null,
          null, null, null, mapper.writeValueAsString(metadata), Instant.now(), Instant.now());
      var progress = new java.util.ArrayList<String>();
      var artifacts = new PostProductionVideoProvider(settings, mapper, WebClient.builder()).render(job, mapper.readValue("{\"id\":91066}", SalesVideoProfile.class), (p, s, m) -> progress.add(m));
      assertThat(progress).contains("Sincronizando a prova real com o trecho narrado",
          "Voz auditada reutilizada, sem nova síntese", "Arquivo finalizado; revisão de uso pendente");
      assertThat(progress).noneMatch(message -> message.contains("gerada") || message.contains("finalizado para venda"));
      assertThat(synthesisRequests.get()).isZero();
      int shots = metadata.at("/post_production/proofShots").isArray()
          ? metadata.at("/post_production/proofShots").size() : 1;
      assertThat(requests.get()).isEqualTo(metadata.at("/preservedNarration/segments").size() + 1 + 2 * shots + deliveryAssets.size());
      double expectedDuration = metadata.path("postProductionDurationSeconds").asDouble(metadata.path("targetDurationSeconds").asDouble());
      assertThat(artifacts.metadata()).containsEntry("duration_seconds", expectedDuration)
          .containsEntry("tts_cost_reconciliation_status", "PENDING_PROVIDER_RECONCILIATION");
      assertThat(artifacts.metadata().get("tts_interactions").toString()).contains("TEXT_TO_SPEECH_REUSE", "source_job_id=" + metadata.at("/preservedNarration/sourceJobId").asLong());
      assertThat(artifacts.captionFile().content()).asString().contains("WEBVTT");
      assertThat(artifacts.metadata().get("product_reference_overlay").toString()).contains("NARRATION_CUE_V1");
      var audio = mapper.valueToTree(artifacts.metadata().get("audio"));
      assertThat(audio.at("/review/status").asText()).isEqualTo("APPROVED_FOR_TEST");
      assertThat(audio.at("/review/metrics/true_peak_dbfs").asDouble()).isLessThanOrEqualTo(-1.0);
      Path output = Path.of("target/mira-narration-recovered.mp4");
      Files.write(output, artifacts.videoFile().content());
      Files.writeString(Path.of("target/mira-narration-recovered-metadata.json"), mapper.writerWithDefaultPrettyPrinter().writeValueAsString(artifacts.metadata()));
      var probe = new ProcessBuilder("ffprobe", "-v", "error", "-show_entries", "format=duration:stream=codec_type,width,height", "-of", "json", output.toString()).start();
      var measured = mapper.readTree(probe.getInputStream());
      assertThat(probe.waitFor()).isZero();
      assertThat(measured.at("/format/duration").asDouble()).isBetween(expectedDuration - .1, expectedDuration + .1);
      assertThat(measured.at("/streams/0/width").asInt()).isEqualTo(1080);
      assertThat(measured.at("/streams/1/codec_type").asText()).isEqualTo("audio");
    }
  }
}
