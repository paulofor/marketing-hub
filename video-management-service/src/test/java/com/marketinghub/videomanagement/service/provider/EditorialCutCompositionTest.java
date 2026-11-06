package com.marketinghub.videomanagement.service.provider;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Responsabilidade: comprovar oito mudanças reais de enquadramento sem oito novas chamadas de
 * geração.
 */
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(
    named = "video.proof.real-ffmpeg",
    matches = "true")
class EditorialCutCompositionTest {
  @TempDir Path directory;

  /** Decodifica frames nos sete limites, mantém 32 segundos e preserva streams da fonte. */
  @Test
  void materializesEightRealCutsWithNoNewGeneration() throws Exception {
    Path source = directory.resolve("source.mp4"), captions = directory.resolve("source.srt");
    Files.writeString(
        captions, "1\n00:00:00,000 --> 00:00:32,000\nTeste técnico local, sem geração paga.\n");
    var create =
        new ProcessBuilder(
                "ffmpeg",
                "-v",
                "error",
                "-y",
                "-f",
                "lavfi",
                "-i",
                "color=c=0x284d67:s=720x1280:r=25:d=32,drawgrid=w=60:h=80:t=4:c=white@0.8",
                "-f",
                "lavfi",
                "-i",
                "sine=frequency=440:duration=32",
                "-i",
                captions.toString(),
                "-map",
                "0:v",
                "-map",
                "1:a",
                "-map",
                "2:s",
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
                "-c:s",
                "mov_text",
                source.toString())
            .inheritIO()
            .start();
    assertThat(create.waitFor()).isZero();
    var json = new ObjectMapper();
    var metadata =
        json.createObjectNode()
            .put("targetDurationSeconds", 32)
            .put("editorialCutCompositionRequired", true);
    var cuts = metadata.putArray("cut_plan");
    for (int i = 0; i < 8; i++) cuts.addObject().put("duration_seconds", 4).put("role", "TEST");
    var result =
        EditorialCutComposition.apply(source, metadata, 91014L, new VideoManagementProperties());
    try {
      Files.copy(
          result.videoFile(),
          Path.of("target/editorial-cuts-local-test.mp4"),
          StandardCopyOption.REPLACE_EXISTING);
      assertThat(result.audit())
          .containsEntry("cutCount", 8)
          .containsEntry("additionalProviderCalls", 0)
          .containsEntry("generatedCameraAnglesClaimed", false);
      var probe =
          new ProcessBuilder(
                  "ffprobe",
                  "-v",
                  "error",
                  "-show_streams",
                  "-show_format",
                  "-of",
                  "json",
                  result.videoFile().toString())
              .start();
      var measured = json.readTree(probe.getInputStream());
      assertThat(probe.waitFor()).isZero();
      assertThat(measured.at("/format/duration").asDouble()).isCloseTo(32, within(.12));
      assertThat(measured.path("streams")).hasSize(3);
      var select = new ArrayList<String>();
      for (int boundary = 100; boundary <= 700; boundary += 100) {
        select.add("eq(n," + (boundary - 1) + ")");
        select.add("eq(n," + boundary + ")");
      }
      var frames =
          new ProcessBuilder(
                  "ffmpeg",
                  "-v",
                  "error",
                  "-threads",
                  "1",
                  "-i",
                  result.videoFile().toString(),
                  "-vf",
                  "select='" + String.join("+", select) + "',scale=270:480",
                  "-vsync",
                  "0",
                  "-f",
                  "rawvideo",
                  "-pix_fmt",
                  "rgb24",
                  "pipe:1")
              .start();
      byte[] rgb = frames.getInputStream().readAllBytes();
      assertThat(frames.waitFor()).isZero();
      int size = 270 * 480 * 3;
      assertThat(rgb.length).isEqualTo(14 * size);
      for (int pair = 0; pair < 7; pair++) {
        long difference = 0;
        int first = pair * 2 * size, second = first + size;
        for (int i = 0; i < size; i++)
          difference += Math.abs((rgb[first + i] & 255) - (rgb[second + i] & 255));
        assertThat((double) difference / size)
            .as("corte editorial em %ss", (pair + 1) * 4)
            .isGreaterThan(1);
      }
    } finally {
      Files.deleteIfExists(result.videoFile());
    }
  }
}
