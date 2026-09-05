package com.marketinghub.videomanagement.service.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/** Responsabilidade: substituir texto visual inventado por uma tela de produto aprovada. */
final class ProductUgcReferenceOverlay {
    private static final Logger log = LoggerFactory.getLogger(ProductUgcReferenceOverlay.class);
    private static final String PRODUCT_UGC_STRATEGY =
            "RUNWAY_PRODUCT_UGC_WITH_DETERMINISTIC_POST_PRODUCTION";
    private static final Pattern SCENE_CUT_TIME =
            Pattern.compile("^lavfi\\.scd\\.time=([0-9]+(?:\\.[0-9]+)?)$");
    private static final int MAX_REFERENCE_BYTES = 20 * 1024 * 1024;
    private static final long PROCESS_TIMEOUT_SECONDS = 120;

    private final VideoManagementProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    /** Configura download seguro e ferramentas locais da composição determinística. */
    ProductUgcReferenceOverlay(
            VideoManagementProperties properties,
            ObjectMapper objectMapper,
            WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder.clone()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs()
                                .maxInMemorySize(MAX_REFERENCE_BYTES))
                        .build())
                .build();
    }

    /** Permite simular o download em teste sem reduzir a validação HTTPS do contrato. */
    ProductUgcReferenceOverlay(
            VideoManagementProperties properties, ObjectMapper objectMapper, WebClient webClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.webClient = webClient;
    }

    /** Troca os planos alternados de produto pela referência canônica antes de voz e legenda. */
    OverlayResult apply(Path source, JsonNode metadata, Long jobId) {
        if (!requiresDeterministicReference(metadata)) {
            return new OverlayResult(source, Map.of());
        }
        URI referenceUri = productReferenceUri(metadata, jobId);
        Path reference = null;
        Path sceneCuts = null;
        Path output = null;
        try {
            reference = downloadReference(referenceUri, jobId);
            sceneCuts = Files.createTempFile("product-ugc-" + jobId + "-scene-cuts", ".txt");
            List<Double> cutTimes = detectSceneCuts(source, sceneCuts, jobId);
            if (cutTimes.isEmpty() || cutTimes.size() > 4) {
                throw invalidContract(
                        jobId,
                        "a montagem exige de um a quatro cortes para separar apresentadora e produto");
            }
            output = Files.createTempFile("product-ugc-" + jobId + "-approved-reference", ".mp4");
            replaceAlternatingProductScenes(source, reference, output, cutTimes, jobId);
            Map<String, Object> audit = buildAudit(referenceUri, reference, cutTimes);
            log.info(
                    "Tela aprovada aplicada ao Product UGC; jobId={} referenceUrl={} cutTimes={}",
                    jobId,
                    referenceUri,
                    cutTimes);
            return new OverlayResult(output, audit);
        } catch (IOException ex) {
            delete(output, jobId);
            log.error(
                    "Falha de arquivo ao aplicar tela aprovada; jobId={} referenceUrl={}",
                    jobId,
                    referenceUri,
                    ex);
            throw new VideoProviderException(
                    "APOLLO_PRODUCT_REFERENCE_OVERLAY_FAILED",
                    "Não foi possível aplicar a tela aprovada do produto.",
                    ex);
        } catch (VideoProviderException ex) {
            delete(output, jobId);
            log.error(
                    "Falha operacional ao aplicar tela aprovada; jobId={} referenceUrl={}",
                    jobId,
                    referenceUri,
                    ex);
            throw ex;
        } catch (RuntimeException ex) {
            delete(output, jobId);
            log.error(
                    "Falha inesperada ao aplicar tela aprovada; jobId={} referenceUrl={}",
                    jobId,
                    referenceUri,
                    ex);
            throw new VideoProviderException(
                    "APOLLO_PRODUCT_REFERENCE_OVERLAY_FAILED",
                    "Falha inesperada ao aplicar a tela aprovada do produto.",
                    ex);
        } finally {
            delete(reference, jobId);
            delete(sceneCuts, jobId);
        }
    }

    /** Confirma que o contrato proíbe texto do provider e solicita overlay determinístico. */
    private boolean requiresDeterministicReference(JsonNode metadata) {
        return PRODUCT_UGC_STRATEGY.equalsIgnoreCase(
                        metadata.path("generation_strategy").asText())
                && "DETERMINISTIC_OVERLAY".equalsIgnoreCase(
                        metadata.at("/post_production/text_rendering").asText())
                && !metadata.at("/post_production/provider_embedded_text_allowed").asBoolean(true);
    }

    /** Extrai a referência de produto que já passou pelo preflight da Runway. */
    private URI productReferenceUri(JsonNode metadata, Long jobId) {
        JsonNode requests = metadata.path("runwayRouterRequestsJson");
        try {
            JsonNode parsed = requests.isTextual()
                    ? objectMapper.readTree(requests.asText())
                    : requests;
            String value = parsed.path(0).path("productImage").path("uri").asText("").trim();
            if (StringUtils.hasText(value)) {
                URI uri = URI.create(value);
                if ("https".equalsIgnoreCase(uri.getScheme())) return uri;
            }
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            log.error("Referência Product UGC inválida; jobId={}", jobId, ex);
            throw invalidContract(jobId, "URL da referência de produto inválida");
        }
        throw invalidContract(jobId, "referência de produto HTTPS ausente");
    }

    /** Baixa a referência aprovada com limite de tamanho e registra URL e resposta. */
    private Path downloadReference(URI uri, Long jobId) throws IOException {
        log.info("Baixando referência aprovada; jobId={} url={}", jobId, uri);
        byte[] body = webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(byte[].class)
                .block(Duration.ofSeconds(PROCESS_TIMEOUT_SECONDS));
        if (body == null || body.length == 0 || body.length > MAX_REFERENCE_BYTES) {
            throw invalidContract(jobId, "arquivo de referência vazio ou acima do limite");
        }
        Path reference = Files.createTempFile("product-ugc-" + jobId + "-reference", ".png");
        Files.write(reference, body);
        log.info(
                "Referência aprovada recebida; jobId={} url={} bytes={}",
                jobId,
                uri,
                body.length);
        return reference;
    }

    /** Detecta os limites reais dos planos editoriais sem inferir tempos pelo roteiro. */
    private List<Double> detectSceneCuts(Path source, Path sceneCuts, Long jobId) {
        String ffmpeg = properties.getProviders().getPostProduction().getFfmpegPath();
        runProcess(
                List.of(
                        ffmpeg,
                        "-hide_banner",
                        "-loglevel",
                        "error",
                        "-i",
                        source.toAbsolutePath().toString(),
                        "-vf",
                        "scdet=threshold=10,metadata=print:key=lavfi.scd.time:file="
                                + sceneCuts.toAbsolutePath(),
                        "-an",
                        "-f",
                        "null",
                        "-"),
                "detectar cortes do Product UGC",
                jobId);
        try {
            List<Double> result = new ArrayList<>();
            for (String line : Files.readAllLines(sceneCuts, StandardCharsets.UTF_8)) {
                Matcher matcher = SCENE_CUT_TIME.matcher(line == null ? "" : line.trim());
                if (matcher.matches()) result.add(Double.parseDouble(matcher.group(1)));
            }
            return result;
        } catch (IOException | NumberFormatException ex) {
            log.error("Falha ao interpretar cortes do Product UGC; jobId={}", jobId, ex);
            throw new VideoProviderException(
                    "APOLLO_PRODUCT_REFERENCE_OVERLAY_FAILED",
                    "Não foi possível interpretar os cortes editoriais.",
                    ex);
        }
    }

    /** Substitui apenas cenas pares de produto, preservando as cenas ímpares da apresentadora. */
    private void replaceAlternatingProductScenes(
            Path source, Path reference, Path output, List<Double> cuts, Long jobId) {
        String ffmpeg = properties.getProviders().getPostProduction().getFfmpegPath();
        double durationSeconds = probeDurationSeconds(source, jobId);
        String enabledIntervals = enabledProductIntervals(cuts);
        String filter =
                "[1:v]scale=1080:1920:flags=lanczos[product];"
                        + "[0:v][product]overlay=0:0:enable='"
                        + enabledIntervals
                        + "'[vout]";
        runProcess(
                List.of(
                        ffmpeg,
                        "-hide_banner",
                        "-loglevel",
                        "error",
                        "-y",
                        "-i",
                        source.toAbsolutePath().toString(),
                        "-loop",
                        "1",
                        "-i",
                        reference.toAbsolutePath().toString(),
                        "-filter_complex",
                        filter,
                        "-map",
                        "[vout]",
                        "-map",
                        "0:a?",
                        "-c:v",
                        "libx264",
                        "-preset",
                        "veryfast",
                        "-crf",
                        "20",
                        "-pix_fmt",
                        "yuv420p",
                        "-c:a",
                        "copy",
                        "-t",
                        formatTime(durationSeconds),
                        "-movflags",
                        "+faststart",
                        output.toAbsolutePath().toString()),
                "aplicar a referência aprovada do produto",
                jobId);
    }

    /** Mede a duração da fonte para encerrar a referência estática sem render infinito. */
    private double probeDurationSeconds(Path source, Long jobId) {
        String ffprobe = properties.getProviders().getPostProduction().getFfprobePath();
        String output = runProcess(
                List.of(
                        ffprobe,
                        "-v",
                        "error",
                        "-show_entries",
                        "format=duration",
                        "-of",
                        "default=noprint_wrappers=1:nokey=1",
                        source.toAbsolutePath().toString()),
                "medir duração do Product UGC",
                jobId);
        try {
            double duration = Double.parseDouble(output.trim());
            if (duration >= 4 && duration <= 60) return duration;
        } catch (NumberFormatException ex) {
            log.error(
                    "Duração inválida no overlay Product UGC; jobId={} output={}",
                    jobId,
                    limit(output),
                    ex);
        }
        throw invalidContract(jobId, "duração da fonte fora do intervalo permitido");
    }

    /** Monta a expressão temporal das cenas 2, 4 e seguintes de produto. */
    private String enabledProductIntervals(List<Double> cuts) {
        List<String> intervals = new ArrayList<>();
        for (int index = 0; index < cuts.size(); index += 2) {
            double start = cuts.get(index);
            if (index + 1 < cuts.size()) {
                intervals.add(
                        "between(t,%s,%s)"
                                .formatted(formatTime(start), formatTime(cuts.get(index + 1))));
            } else {
                intervals.add("gte(t,%s)".formatted(formatTime(start)));
            }
        }
        return String.join("+", intervals);
    }

    /** Formata segundos sem depender do locale do host. */
    private String formatTime(double seconds) {
        return String.format(Locale.ROOT, "%.3f", seconds);
    }

    /** Persiste a origem, integridade e os planos substituídos para revisão humana. */
    private Map<String, Object> buildAudit(URI referenceUri, Path reference, List<Double> cuts)
            throws IOException {
        LinkedHashMap<String, Object> audit = new LinkedHashMap<>();
        audit.put("status", "APPROVED_REFERENCE_APPLIED");
        audit.put("method", "FFMPEG_SCENE_AWARE_ALTERNATING_PRODUCT_REFERENCE");
        audit.put("reference_url", referenceUri.toString());
        audit.put("reference_sha256", sha256(Files.readAllBytes(reference)));
        audit.put("scene_cut_times_seconds", cuts);
        List<Integer> scenes = new ArrayList<>();
        for (int index = 0; index < cuts.size(); index += 2) scenes.add(index + 2);
        audit.put("replaced_scene_numbers", scenes);
        audit.put("provider_embedded_text_removed", true);
        return audit;
    }

    /** Executa o ffmpeg com prazo finito e inclui a operação na falha auditável. */
    private String runProcess(List<String> command, String operation, Long jobId) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            boolean completed = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new VideoProviderException(
                        "APOLLO_PRODUCT_REFERENCE_OVERLAY_FAILED",
                        "Timeout ao " + operation + ".");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new VideoProviderException(
                        "APOLLO_PRODUCT_REFERENCE_OVERLAY_FAILED",
                        "Falha ao " + operation + ": " + limit(output));
            }
            return output;
        } catch (IOException ex) {
            log.error("Falha ao iniciar ffmpeg; jobId={} operation={}", jobId, operation, ex);
            throw new VideoProviderException(
                    "APOLLO_PRODUCT_REFERENCE_OVERLAY_FAILED",
                    "Não foi possível " + operation + ".",
                    ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Composição interrompida; jobId={} operation={}", jobId, operation, ex);
            throw new VideoProviderException(
                    "APOLLO_PRODUCT_REFERENCE_OVERLAY_FAILED",
                    "Composição interrompida ao " + operation + ".",
                    ex);
        }
    }

    /** Calcula a impressão digital da referência aplicada ao vídeo. */
    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível", ex);
        }
    }

    /** Cria uma falha fechada para contrato de referência inválido. */
    private VideoProviderException invalidContract(Long jobId, String reason) {
        return new VideoProviderException(
                "APOLLO_PRODUCT_REFERENCE_OVERLAY_INVALID",
                "Contrato de tela Product UGC inválido no job " + jobId + ": " + reason + ".");
    }

    /** Remove somente o temporário criado por esta composição. */
    private void delete(Path path, Long jobId) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("Falha ao remover temporário do overlay; jobId={} path={}", jobId, path, ex);
        }
    }

    /** Limita detalhes técnicos sem perder a causa operacional. */
    private String limit(String value) {
        if (!StringUtils.hasText(value)) return "sem saída";
        String normalized = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.length() > 1000 ? normalized.substring(0, 1000) : normalized;
    }

    /** Devolve o vídeo preparado e a evidência funcional que deve acompanhar o job. */
    record OverlayResult(Path videoFile, Map<String, Object> audit) {}
}
