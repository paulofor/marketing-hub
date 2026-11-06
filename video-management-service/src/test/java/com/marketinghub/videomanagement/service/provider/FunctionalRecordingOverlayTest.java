package com.marketinghub.videomanagement.service.provider;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import okhttp3.mockwebserver.*;
import okio.Buffer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Responsabilidade: comprovar composição temporal da mesma gravação sem geração paga ou falsa
 * aprovação.
 */
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(
    named = "video.proof.real-ffmpeg",
    matches = "true")
class FunctionalRecordingOverlayTest {
  final ObjectMapper json = new ObjectMapper();
  @TempDir Path work;

  /** Compõe os gestos em velocidade real, mantém áudio e confere o arquivo final com ffprobe. */
  @Test
  void composesActualRecordingAndPreservesActionTimes() throws Exception {
    Path recording = recording();
    byte[] bytes = Files.readAllBytes(recording);
    var metadata = metadata(bytes);
    String actualManifest = System.getProperty("video.recording.manifest");
    if (actualManifest != null) {
      var manifest = json.readTree(Files.readString(Path.of(actualManifest)));
      var proof = (ObjectNode) metadata.at("/post_production/product_proof");
      proof.set("actions", manifest.path("actions"));
      proof.set("manifest", manifest);
      proof.put("durationSeconds", manifest.path("durationSeconds").asDouble());
    }
    try (var server = new MockWebServer()) {
      server.start();
      server.enqueue(
          new MockResponse()
              .setHeader("Content-Type", "video/mp4")
              .setBody(new Buffer().write(bytes)));
      var props = new VideoManagementProperties();
      props.setBackendBaseUrl(server.url("/").uri());
      Path base = work.resolve("source.mp4");
      run(
          List.of(
              "ffmpeg",
              "-v",
              "error",
              "-y",
              "-f",
              "lavfi",
              "-i",
              "color=c=blue:s=720x1280:r=25:d=32",
              "-f",
              "lavfi",
              "-i",
              "sine=frequency=440:duration=32",
              "-c:v",
              "libx264",
              "-threads",
              "2",
              "-preset",
              "ultrafast",
              "-pix_fmt",
              "yuv420p",
              "-c:a",
              "aac",
              base.toString()));
      var result =
          new PdeProductProofOverlay(props, WebClient.builder()).apply(base, metadata, 91009L);
      try {
        assertThat(result.audit())
            .containsEntry("compositionMode", "FUNCTIONAL_RECORDING_REALTIME_V1")
            .containsEntry("playbackRate", 1)
            .containsEntry("qualityApproved", false)
            .containsEntry("commercialEvidenceClaimed", false);
        assertThat((List<?>) result.audit().get("reviewMoments")).hasSize(4);
        Path target = Path.of("target/functional-recording-composed.mp4");
        Files.copy(result.videoFile(), target, StandardCopyOption.REPLACE_EXISTING);
        var probe =
            new ProcessBuilder(
                    "ffprobe",
                    "-v",
                    "error",
                    "-show_streams",
                    "-show_format",
                    "-of",
                    "json",
                    target.toString())
                .start();
        var measured = json.readTree(probe.getInputStream());
        assertThat(probe.waitFor()).isZero();
        assertThat(measured.at("/format/duration").asDouble()).isCloseTo(32, within(.1));
        assertThat(measured.at("/streams/0/width").asInt()).isEqualTo(1080);
        assertThat(measured.at("/streams/0/height").asInt()).isEqualTo(1920);
        assertThat(measured.path("streams")).hasSize(2);
        finishWithRealCutsMusicAndCaption(result.videoFile(), metadata, props);
        assertThat(server.getRequestCount()).isEqualTo(1);
        assertThat(server.takeRequest().getPath())
            .isEqualTo("/api/sales-videos/projects/91014/product-recording");
      } finally {
        Files.deleteIfExists(result.videoFile());
      }
    }
  }

  /**
   * Completa a cadeia local com sinal de voz sintético, trilha original e legenda queimada, sem
   * parecer premium.
   */
  private void finishWithRealCutsMusicAndCaption(
      Path source, ObjectNode metadata, VideoManagementProperties properties) throws Exception {
    var edited = EditorialCutComposition.apply(source, metadata, 91009L, properties);
    Path voice = work.resolve("synthetic-voice-signal.wav"),
        music = work.resolve("original-music.wav"),
        caption = work.resolve("captions.ass");
    Path finalMovie = Path.of("target/functional-recording-full-local-test.mp4");
    try {
      run(
          List.of(
              "ffmpeg",
              "-v",
              "error",
              "-y",
              "-f",
              "lavfi",
              "-i",
              "sine=frequency=440:sample_rate=44100:duration=32",
              voice.toString()));
      OriginalMusicBed.write(music, 32);
      String header =
          "[Script Info]\n"
              + "ScriptType: v4.00+\n"
              + "PlayResX: 1080\n"
              + "PlayResY: 1920\n"
              + "[V4+ Styles]\n"
              + "Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour,"
              + " BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle,"
              + " BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n"
              + "Style: Default,DejaVu"
              + " Sans,44,&H00FFFFFF,&H00FFFFFF,&H00000000,&H90000000,0,0,0,0,100,100,0,0,3,2,0,2,50,50,90,1\n"
              + "[Events]\n"
              + "Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n";
      var ass = new StringBuilder(header);
      for (int i = 0; i < 8; i++)
        ass.append(
            String.format(
                java.util.Locale.ROOT,
                "Dialogue: 0,0:00:%02d.00,0:00:%02d.00,Default,,0,0,0,,TESTE LOCAL - trecho %d, sem"
                    + " parecer premium\n",
                i * 4,
                (i + 1) * 4,
                i + 1));
      Files.writeString(caption, ass);
      var post =
          new PostProductionVideoProvider(properties, new ObjectMapper(), WebClient.builder());
      var render =
          PostProductionVideoProvider.class.getDeclaredMethod(
              "runFfmpegWithVoice",
              Path.class,
              Path.class,
              Path.class,
              Path.class,
              double.class,
              boolean.class,
              Path.class);
      render.setAccessible(true);
      render.invoke(post, edited.videoFile(), voice, caption, finalMovie, 32.0, true, music);
      var probe =
          new ProcessBuilder(
                  "ffprobe",
                  "-v",
                  "error",
                  "-show_format",
                  "-show_streams",
                  "-of",
                  "json",
                  finalMovie.toString())
              .start();
      var measured = json.readTree(probe.getInputStream());
      assertThat(probe.waitFor()).isZero();
      assertThat(measured.at("/format/duration").asDouble()).isCloseTo(32, within(.12));
      assertThat(measured.at("/streams/0/width").asInt()).isEqualTo(1080);
      assertThat(measured.at("/streams/0/height").asInt()).isEqualTo(1920);
      assertThat(measured.at("/streams/1/codec_type").asText()).isEqualTo("audio");
      run(
          List.of(
              "ffmpeg",
              "-v",
              "error",
              "-threads",
              "1",
              "-i",
              finalMovie.toString(),
              "-f",
              "null",
              "-"));
      var decode =
          new ProcessBuilder(
                  "ffmpeg",
                  "-v",
                  "error",
                  "-ss",
                  "1.3",
                  "-i",
                  finalMovie.toString(),
                  "-t",
                  "1",
                  "-vn",
                  "-ac",
                  "1",
                  "-ar",
                  "44100",
                  "-f",
                  "f32le",
                  "pipe:1")
              .start();
      byte[] bytes = decode.getInputStream().readAllBytes();
      assertThat(decode.waitFor()).isZero();
      var samples =
          java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
      for (double frequency : new double[] {440, 130.81278265}) {
        double sine = 0, cosine = 0;
        int count = samples.limit();
        for (int i = 0; i < count; i++) {
          double angle = 2 * Math.PI * frequency * i / 44100;
          sine += samples.get(i) * Math.sin(angle);
          cosine += samples.get(i) * Math.cos(angle);
        }
        assertThat(Math.hypot(sine, cosine) / count)
            .as("sinal em %s Hz", frequency)
            .isGreaterThan(.00001);
      }
      assertThat(edited.audit())
          .containsEntry("cutCount", 8)
          .containsEntry("additionalProviderCalls", 0);
    } finally {
      Files.deleteIfExists(edited.videoFile());
    }
  }

  /** Recusa contexto cruzado antes de baixar e recusa pixels alterados antes de iniciar geração. */
  @Test
  void refusesForeignContextAndChangedBytes() throws Exception {
    byte[] bytes = Files.readAllBytes(recording());
    var metadata = metadata(bytes);
    try (var server = new MockWebServer()) {
      server.start();
      var props = new VideoManagementProperties();
      props.setBackendBaseUrl(server.url("/").uri());
      var overlay = new PdeProductProofOverlay(props, WebClient.builder());
      ((ObjectNode) metadata.at("/post_production/product_proof")).put("productId", 999);
      assertThatThrownBy(() -> overlay.verify(metadata, 91009L))
          .hasMessageContaining("outro contexto");
      assertThat(server.getRequestCount()).isZero();
      ((ObjectNode) metadata.at("/post_production/product_proof"))
          .put("productId", 91004)
          .put("sha256", "a".repeat(64));
      server.enqueue(new MockResponse().setBody(new Buffer().write(bytes)));
      assertThatThrownBy(() -> overlay.verify(metadata, 91009L)).hasMessageContaining("alterados");
      assertThat(server.getRequestCount()).isEqualTo(1);
    }
  }

  /**
   * Usa gravação local real quando fornecida; em CI cria apenas um padrão técnico identificado como
   * teste.
   */
  private Path recording() throws Exception {
    String input = System.getProperty("video.recording.fixture");
    if (input != null) return Path.of(input);
    Path result = work.resolve("fixture.mp4");
    run(
        List.of(
            "ffmpeg",
            "-v",
            "error",
            "-y",
            "-f",
            "lavfi",
            "-i",
            "testsrc2=s=540x1170:r=25:d=11",
            "-an",
            "-c:v",
            "libx264",
            "-threads",
            "2",
            "-preset",
            "ultrafast",
            "-pix_fmt",
            "yuv420p",
            result.toString()));
    return result;
  }

  /**
   * Monta oito cortes e quatro ações sintéticas mantendo a mesma identidade em todas as camadas.
   */
  private ObjectNode metadata(byte[] bytes) throws Exception {
    var root =
        json.createObjectNode()
            .put("videoProjectId", 91014)
            .put("productId", 91004)
            .put("experimentId", 91114)
            .put("tenantId", "fixture-tenant")
            .put("productVersion", "video-target")
            .put("targetDurationSeconds", 32);
    var proof = root.putObject("post_production").putObject("product_proof");
    proof
        .put("contractVersion", "PDE_PRIVATE_VIDEO_PROOF_V1")
        .put("mediaKind", "FUNCTIONAL_SCREEN_RECORDING")
        .put("projectId", 91014)
        .put("productId", 91004)
        .put("experimentId", 91114)
        .put("tenantId", "fixture-tenant")
        .put("productVersion", "video-target")
        .put("sourcePrototypeVersion", "accepted-source")
        .put("contentPath", "/api/sales-videos/projects/91014/product-recording")
        .put("sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)))
        .put("durationSeconds", 11)
        .put("qualityApproved", false)
        .put("commercialEvidenceClaimed", false)
        .put("captureEnvironment", "LOCAL_SANDBOX");
    proof.putObject("manifest").put("width", 540).put("height", 1170);
    var actions = proof.putArray("actions");
    int at = 1;
    for (String action :
        List.of("INPUT_SUBMITTED", "RESULT_VISIBLE", "SAVE_AND_COMPLETE", "REOPEN_SAVED"))
      actions.addObject().put("action", action).put("seconds", at++);
    var cuts = root.putArray("cut_plan");
    for (int i = 0; i < 8; i++)
      cuts.addObject()
          .put("duration_seconds", 4)
          .put(
              "role",
              i == 0
                  ? "HOOK_DOR"
                  : i == 7 ? "CTA" : i == 6 ? "PROVA" : i < 4 ? "MECANISMO" : "RESULTADO");
    return root;
  }

  /** Executa apenas comandos locais de criação ou inspeção dos dados sintéticos. */
  private void run(List<String> command) throws Exception {
    var process = new ProcessBuilder(command).inheritIO().start();
    assertThat(process.waitFor()).isZero();
  }
}
