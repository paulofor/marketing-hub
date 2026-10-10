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
  /** Recupera o caso original sem chamada de TTS, conservando pixels, voz e relógio das legendas. */
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
        /** Entrega apenas binários locais e reprova qualquer tentativa de chamar serviço pago. */
        @Override public MockResponse dispatch(RecordedRequest request) {
          requests.incrementAndGet();
          String path = request.getPath();
          if (path.contains("/audio/speech")) { synthesisRequests.incrementAndGet(); return new MockResponse().setResponseCode(503); }
          Path file = path.contains("/product-proof") ? root.resolve("proof.png")
              : root.resolve(path.substring(1));
          try { return new MockResponse().setBody(new Buffer().write(Files.readAllBytes(file))); }
          catch (java.io.IOException ex) { throw new java.io.UncheckedIOException(ex); }
        }
      });
      server.start();
      metadata.put("sourceVideoUrl", server.url("/3000.mp4").toString());
      for (var segment : metadata.at("/preservedNarration/segments")) {
        ((ObjectNode) segment).put("url", server.url("/" + segment.path("assetId").asLong() + ".mp3").toString());
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
      var artifacts = new PostProductionVideoProvider(settings, mapper, WebClient.builder()).render(job, null, (p, s, m) -> {});
      assertThat(synthesisRequests.get()).isZero();
      assertThat(requests.get()).isEqualTo(7);
      assertThat(artifacts.metadata()).containsEntry("duration_seconds", 20.0)
          .containsEntry("tts_cost_reconciliation_status", "PENDING_PROVIDER_RECONCILIATION");
      assertThat(artifacts.metadata().get("tts_interactions").toString()).contains("TEXT_TO_SPEECH_REUSE", "source_job_id=21253");
      assertThat(artifacts.captionFile().content()).asString().contains("00:00:12.456 --> 00:00:20.000");
      Path output = Path.of("target/mira-narration-recovered.mp4");
      Files.write(output, artifacts.videoFile().content());
      Files.writeString(Path.of("target/mira-narration-recovered-metadata.json"), mapper.writerWithDefaultPrettyPrinter().writeValueAsString(artifacts.metadata()));
      var probe = new ProcessBuilder("ffprobe", "-v", "error", "-show_entries", "format=duration:stream=codec_type,width,height", "-of", "json", output.toString()).start();
      var measured = mapper.readTree(probe.getInputStream());
      assertThat(probe.waitFor()).isZero();
      assertThat(measured.at("/format/duration").asDouble()).isBetween(19.9, 20.1);
      assertThat(measured.at("/streams/0/width").asInt()).isEqualTo(1080);
      assertThat(measured.at("/streams/1/codec_type").asText()).isEqualTo("audio");
    }
  }
}
