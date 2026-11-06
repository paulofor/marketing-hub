package com.marketinghub.videomanagement.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Responsabilidade: inserir gravação funcional íntegra em tempo real, sem animar imagens nem
 * inventar gestos.
 */
final class FunctionalRecordingOverlay {
  private static final Logger log = LoggerFactory.getLogger(FunctionalRecordingOverlay.class);
  private final VideoManagementProperties properties;
  private final WebClient backend;
  private final ObjectMapper json = new ObjectMapper();

  /** Usa somente o backend do módulo e temporários locais limitados. */
  FunctionalRecordingOverlay(VideoManagementProperties properties, WebClient backend) {
    this.properties = properties;
    this.backend = backend;
  }

  /**
   * Identifica o contrato temporal explícito sem alterar o tratamento anterior das capturas PNG.
   */
  static boolean requested(JsonNode metadata) {
    return "FUNCTIONAL_SCREEN_RECORDING"
        .equals(metadata.at("/post_production/product_proof/mediaKind").asText());
  }

  /** Confere identidade, bytes, streams e janela útil antes de consumir qualquer provedor pago. */
  void verify(JsonNode metadata, Long jobId) {
    Path recording = load(metadata, jobId);
    try {
      window(metadata);
    } finally {
      delete(recording, jobId);
    }
  }

  /**
   * Mantém a velocidade original entre entrada, resultado e retomada, registrando o relógio de cada
   * ação.
   */
  ProductUgcReferenceOverlay.OverlayResult apply(Path source, JsonNode metadata, Long jobId) {
    Path recording = load(metadata, jobId), output = null, processLog = null;
    try {
      double[] timing = window(metadata);
      double start = timing[0], end = timing[1], trim = timing[2];
      double duration =
          metadata
              .path("postProductionDurationSeconds")
              .asDouble(metadata.path("targetDurationSeconds").asDouble());
      output = Files.createTempFile("functional-proof-output-", ".mp4");
      processLog = Files.createTempFile("functional-proof-process-", ".log");
      String filter =
          "[0:v]scale=1080:1920:force_original_aspect_ratio=decrease,pad=1080:1920:(ow-iw)/2:(oh-ih)/2[base];"
              + "[1:v]trim=start="
              + number(trim)
              + ",setpts=PTS-STARTPTS,scale=880:1480:force_original_aspect_ratio=decrease,"
              + "pad=1080:1920:(ow-iw)/2:180:color=0xf8f3ee,tpad=stop_mode=clone:stop_duration="
              + number(end - start)
              + ","
              + "trim=duration="
              + number(end - start)
              + ",drawtext=text='Funcionamento local - dados de"
              + " teste':fontsize=32:fontcolor=0x173f35:x=(w-text_w)/2:y=75,setpts=PTS+"
              + number(start)
              + "/TB[proof];[base][proof]overlay=0:0:enable='between(t,"
              + number(start)
              + ","
              + number(end)
              + ")':eof_action=pass[out]";
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
              "-threads",
              "1",
              "-i",
              recording.toString(),
              "-filter_complex_threads",
              "1",
              "-filter_complex",
              filter,
              "-map",
              "[out]",
              "-map",
              "0:a?",
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
              "-t",
              number(duration),
              "-movflags",
              "+faststart",
              output.toString());
      run(command, processLog, jobId);
      var proof = metadata.at("/post_production/product_proof");
      var moments = new ArrayList<Double>();
      var actions = new ArrayList<Map<String, Object>>();
      for (var action : proof.path("actions")) {
        double at = start + action.path("seconds").asDouble() - trim;
        moments.add(Math.min(end - .1, at + .4));
        actions.add(
            Map.of(
                "action",
                action.path("action").asText(),
                "sourceSeconds",
                action.path("seconds").asDouble(),
                "finalSeconds",
                at));
      }
      Map<String, Object> audit = new LinkedHashMap<>();
      audit.put("contractVersion", "PDE_PRIVATE_VIDEO_PROOF_V1");
      audit.put("status", "APPLIED");
      audit.put("compositionMode", "FUNCTIONAL_RECORDING_REALTIME_V1");
      audit.put("sha256", proof.path("sha256").asText());
      audit.put("source", proof.path("contentPath").asText());
      audit.put("sourcePrototypeVersion", proof.path("sourcePrototypeVersion").asText());
      audit.put("captureEnvironment", "LOCAL_SANDBOX");
      audit.put("syntheticScenario", true);
      audit.put("commercialEvidenceClaimed", false);
      audit.put("qualityApproved", false);
      audit.put("reviewMoments", moments);
      audit.put("actions", actions);
      audit.put("window", List.of(start, end));
      audit.put("sourceTrimStart", trim);
      audit.put("playbackRate", 1);
      return new ProductUgcReferenceOverlay.OverlayResult(output, audit);
    } catch (Exception ex) {
      delete(output, jobId);
      log.error("Falha ao compor gravação funcional; jobId={}", jobId, ex);
      if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
      if (ex instanceof VideoProviderException failure) throw failure;
      throw invalid("Não foi possível compor a gravação funcional.");
    } finally {
      delete(recording, jobId);
      delete(processLog, jobId);
    }
  }

  /**
   * Materializa somente o MP4 do projeto proprietário e valida duração real e integridade antes da
   * composição.
   */
  private Path load(JsonNode metadata, Long jobId) {
    Path file = null, probeLog = null;
    var proof = metadata.at("/post_production/product_proof");
    long projectId = metadata.path("videoProjectId").asLong();
    String path = "/api/sales-videos/projects/" + projectId + "/product-recording";
    String tenant = metadata.path("tenantId").asText();
    if (projectId <= 0
        || projectId != proof.path("projectId").asLong()
        || tenant.isBlank()
        || !tenant.equals(proof.path("tenantId").asText())
        || !path.equals(proof.path("contentPath").asText())
        || metadata.path("productId").asLong() != proof.path("productId").asLong()
        || metadata.path("experimentId").asLong() != proof.path("experimentId").asLong()
        || !metadata
            .path("productVersion")
            .asText(metadata.path("campaignKey").asText())
            .equals(proof.path("productVersion").asText())
        || proof.path("commercialEvidenceClaimed").asBoolean(true)
        || proof.path("qualityApproved").asBoolean(true)
        || !"LOCAL_SANDBOX".equals(proof.path("captureEnvironment").asText()))
      throw invalid("Gravação funcional de outro contexto ou sem segregação.");
    try {
      var request = backend.get().uri(path).header("X-Tenant-ID", tenant);
      if (properties.getAuthToken() != null && !properties.getAuthToken().isBlank())
        request.header("Authorization", "Bearer " + properties.getAuthToken());
      byte[] bytes = request.retrieve().bodyToMono(byte[].class).block(Duration.ofSeconds(60));
      if (bytes == null
          || bytes.length == 0
          || bytes.length > 8 * 1024 * 1024
          || !HexFormat.of()
              .formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
              .equals(proof.path("sha256").asText()))
        throw invalid("Bytes da gravação funcional alterados ou excessivos.");
      file = Files.createTempFile("functional-recording-", ".mp4");
      Files.write(file, bytes);
      probeLog = Files.createTempFile("functional-recording-probe-", ".json");
      run(
          List.of(
              properties.getProviders().getPostProduction().getFfprobePath(),
              "-v",
              "error",
              "-show_format",
              "-show_streams",
              "-of",
              "json",
              file.toString()),
          probeLog,
          jobId);
      var probe = json.readTree(Files.readString(probeLog));
      int videos = 0, audios = 0;
      for (var stream : probe.path("streams")) {
        if ("audio".equals(stream.path("codec_type").asText())) audios++;
        if ("video".equals(stream.path("codec_type").asText())) {
          videos++;
          if (!"h264".equals(stream.path("codec_name").asText())
              || stream.path("width").asInt() != proof.at("/manifest/width").asInt()
              || stream.path("height").asInt() != proof.at("/manifest/height").asInt())
            throw invalid("Streams da gravação divergem do manifesto.");
        }
      }
      if (videos != 1
          || audios != 0
          || Math.abs(
                  probe.at("/format/duration").asDouble()
                      - proof.path("durationSeconds").asDouble())
              > .1)
        throw invalid("Duração ou streams da gravação inválidos; voz será tratada separadamente.");
      return file;
    } catch (Exception ex) {
      delete(file, jobId);
      log.error("Falha ao validar gravação funcional; jobId={} endpoint={}", jobId, path, ex);
      if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
      if (ex instanceof VideoProviderException failure) throw failure;
      throw invalid("Gravação funcional indisponível ou inválida.");
    } finally {
      delete(probeLog, jobId);
    }
  }

  /**
   * Reserva janela contínua de resultado e prova sem cobrir todo o desenvolvimento cinematográfico.
   */
  private double[] window(JsonNode metadata) {
    var proof = metadata.at("/post_production/product_proof");
    double elapsed = 0, start = -1, end = -1;
    for (var cut : metadata.path("cut_plan")) {
      double seconds = cut.path("duration_seconds").asDouble();
      if (!Double.isFinite(seconds) || seconds <= 0) throw invalid("Corte sem duração válida.");
      if (Set.of("RESULTADO", "PROVA").contains(cut.path("role").asText())) {
        if (start < 0) start = elapsed;
        end = elapsed + seconds;
      }
      elapsed += seconds;
    }
    var actions = proof.path("actions");
    if (!actions.isArray()
        || actions.size() < 4
        || actions.size() > 16
        || start < 0
        || Math.abs(elapsed - metadata.path("targetDurationSeconds").asDouble()) > .1)
      throw invalid("Plano sem janela suficiente para a gravação funcional.");
    double previous = -1;
    Set<String> observed = new HashSet<>();
    for (var action : actions) {
      double at = action.path("seconds").asDouble(-1);
      if (at < 0 || at < previous || at >= proof.path("durationSeconds").asDouble())
        throw invalid("Ações fora do relógio da gravação.");
      observed.add(action.path("action").asText());
      previous = at;
    }
    double trim = Math.max(0, actions.get(0).path("seconds").asDouble() - 1);
    if (!observed.containsAll(
            Set.of("INPUT_SUBMITTED", "RESULT_VISIBLE", "SAVE_AND_COMPLETE", "REOPEN_SAVED"))
        || previous - trim + 2 > end - start)
      throw invalid("A gravação exige mais tempo legível; ajustar edição antes de gerar clipes.");
    return new double[] {start, end, trim};
  }

  /** Executa somente argumentos locais, com timeout e diagnóstico preservado na mesma operação. */
  private void run(List<String> command, Path output, Long jobId) throws Exception {
    var process =
        new ProcessBuilder(command)
            .redirectErrorStream(true)
            .redirectOutput(output.toFile())
            .start();
    if (!process.waitFor(120, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      throw invalid("A validação ou composição excedeu o tempo limite.");
    }
    if (process.exitValue() != 0)
      throw invalid("Falha local na gravação funcional: " + Files.readString(output));
  }

  /** Normaliza segundos sem aceitar expressão enviada no manifesto. */
  private String number(double value) {
    return String.format(Locale.ROOT, "%.3f", value);
  }

  /** Expõe bloqueio funcional antes de criar outra mídia ou chamada paga. */
  private VideoProviderException invalid(String message) {
    return new VideoProviderException("PDE_PRODUCT_PROOF_INVALID", message);
  }

  /** Remove apenas o temporário criado pela operação atual. */
  private void delete(Path file, Long jobId) {
    if (file == null) return;
    try {
      Files.deleteIfExists(file);
    } catch (java.io.IOException ex) {
      log.warn("Falha ao limpar gravação funcional; jobId={} path={}", jobId, file, ex);
    }
  }
}
