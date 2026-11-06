package com.marketinghub.videomanagement.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsabilidade: materializar cortes editoriais reais sobre os mesmos pixels, sem contratar
 * tomadas extras.
 */
final class EditorialCutComposition {
  private static final Logger log = LoggerFactory.getLogger(EditorialCutComposition.class);

  /** Conserva o caminho anterior quando o pedido não exige montagem editorial explícita. */
  static boolean requested(JsonNode metadata) {
    return metadata.path("editorialCutCompositionRequired").asBoolean(false);
  }

  /**
   * Monta intervalos contínuos com reenquadramento digital discreto, preservando relógio, áudio e
   * legendas.
   */
  static ProductUgcReferenceOverlay.OverlayResult apply(
      Path source, JsonNode metadata, Long jobId, VideoManagementProperties properties) {
    JsonNode cuts = metadata.path("cut_plan");
    if (!cuts.isArray() || cuts.size() < 2 || cuts.size() > 48)
      throw invalid("Plano de cortes editoriais inválido.");
    double elapsed = 0;
    var closeFrames = new ArrayList<String>();
    var audit = new ArrayList<Map<String, Object>>();
    for (int i = 0; i < cuts.size(); i++) {
      double seconds = cuts.get(i).path("duration_seconds").asDouble();
      if (!Double.isFinite(seconds) || seconds <= 0) throw invalid("Corte sem duração válida.");
      double end = elapsed + seconds;
      double crop = i % 2 == 0 ? 1 : .96;
      if (i % 2 == 1)
        closeFrames.add(
            "between(on," + Math.round(elapsed * 25) + "," + (Math.round(end * 25) - 1) + ")");
      audit.add(
          Map.of(
              "order",
              i + 1,
              "startSeconds",
              elapsed,
              "endSeconds",
              end,
              "cropFactor",
              crop,
              "role",
              cuts.get(i).path("role").asText()));
      elapsed = end;
    }
    if (Math.abs(elapsed - metadata.path("targetDurationSeconds").asDouble()) > .05)
      throw invalid("Cortes divergem da duração final autorizada.");
    String filter =
        "[0:v]scale=1080:1920:force_original_aspect_ratio=decrease,pad=1080:1920:(ow-iw)/2:(oh-ih)/2,fps=25,"
            + "zoompan=z='if("
            + String.join("+", closeFrames)
            + ",1.041666667,1)':x='(iw-iw/zoom)/2':y='(ih-ih/zoom)/2':d=1:s=1080x1920:fps=25,setsar=1[out]";
    Path output = null, processLog = null;
    try {
      output = Files.createTempFile("editorial-cuts-", ".mp4");
      processLog = Files.createTempFile("editorial-cuts-", ".log");
      var command =
          List.of(
              properties.getProviders().getPostProduction().getFfmpegPath(),
              "-hide_banner",
              "-loglevel",
              "error",
              "-y",
              "-threads",
              "1",
              "-i",
              source.toString(),
              "-filter_complex_threads",
              "1",
              "-filter_complex",
              filter,
              "-map",
              "[out]",
              "-map",
              "0:a?",
              "-map",
              "0:s?",
              "-c:v",
              "libx264",
              "-threads",
              "2",
              "-preset",
              "ultrafast",
              "-crf",
              "20",
              "-pix_fmt",
              "yuv420p",
              "-c:a",
              "copy",
              "-c:s",
              "copy",
              "-t",
              number(elapsed),
              "-movflags",
              "+faststart",
              output.toString());
      log.info(
          "Compondo cortes reais no relógio dos frames; jobId={} cutCount={}", jobId, cuts.size());
      var process =
          new ProcessBuilder(command)
              .redirectErrorStream(true)
              .redirectOutput(processLog.toFile())
              .start();
      if (!process.waitFor(120, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        throw invalid("Montagem dos cortes excedeu o prazo.");
      }
      if (process.exitValue() != 0)
        throw invalid("Falha na montagem dos cortes: " + Files.readString(processLog));
      return new ProductUgcReferenceOverlay.OverlayResult(
          output,
          Map.of(
              "contractVersion",
              "EDITORIAL_REAL_CUTS_V1",
              "cutCount",
              cuts.size(),
              "cuts",
              audit,
              "sourceOrderPreserved",
              true,
              "additionalProviderCalls",
              0,
              "generatedCameraAnglesClaimed",
              false));
    } catch (Exception ex) {
      log.error("Falha ao materializar cortes editoriais; jobId={}", jobId, ex);
      delete(output, jobId);
      if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
      if (ex instanceof VideoProviderException failure) throw failure;
      throw invalid("Cortes editoriais não foram materializados.");
    } finally {
      delete(processLog, jobId);
    }
  }

  /** Usa somente números finitos validados, sem aceitar expressão de filtro do editor. */
  private static String number(double value) {
    return String.format(Locale.ROOT, "%.3f", value);
  }

  /** Interrompe montagem inválida sem solicitar nova geração. */
  private static VideoProviderException invalid(String reason) {
    return new VideoProviderException("EDITORIAL_CUTS_INVALID", reason);
  }

  /** Remove apenas temporários desta operação sem apagar a fonte recuperável. */
  private static void delete(Path path, Long jobId) {
    if (path == null) return;
    try {
      Files.deleteIfExists(path);
    } catch (java.io.IOException ex) {
      log.warn("Falha ao limpar montagem editorial; jobId={} path={}", jobId, path, ex);
    }
  }
}
