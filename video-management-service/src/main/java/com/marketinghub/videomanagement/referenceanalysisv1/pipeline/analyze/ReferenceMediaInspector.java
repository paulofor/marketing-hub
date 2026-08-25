package com.marketinghub.videomanagement.referenceanalysisv1.pipeline.analyze;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.referenceanalysisv1.pipeline.ReferenceAnalysisStageContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.InetAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Extrai evidência técnica determinística e frames-chave sem depender da interpretação da IA. */
@Component
public class ReferenceMediaInspector {
    private static final Logger log = LoggerFactory.getLogger(ReferenceMediaInspector.class);
    private static final Pattern SCENE_TIME = Pattern.compile("pts_time:([0-9.]+)");
    private static final Pattern INTEGRATED_LOUDNESS = Pattern.compile("I:\\s*(-?[0-9.]+) LUFS");
    private static final Pattern TRUE_PEAK = Pattern.compile("Peak:\\s*(-?[0-9.]+) dBFS");
    private final VideoManagementProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20)).followRedirects(HttpClient.Redirect.NEVER).build();

    /** Inicializa o inspetor com limites operacionais e serializador JSON. */
    public ReferenceMediaInspector(VideoManagementProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /** Baixa, inspeciona e remove o vídeo, devolvendo somente métricas e contact sheets compactos. */
    public Evidence inspect(ReferenceAnalysisStageContext context) {
        Path directory = null;
        try {
            directory = Files.createTempDirectory("reference-analysis-" + context.executionId() + "-");
            Path video = directory.resolve("reference.mp4");
            URI source = validatedSource(context.input().path("sourceUrl").asText());
            download(context.executionId(), source, video);
            JsonNode probe = probe(context.executionId(), video);
            double durationSeconds = duration(probe);
            List<Double> scenes = detectScenes(context.executionId(), video);
            AudioMetrics audio = analyzeAudio(context.executionId(), video);
            List<Path> sheets = createContactSheets(context.executionId(), video, directory, durationSeconds);
            ObjectNode artifacts = artifacts(video, probe, durationSeconds, scenes, audio, sheets);
            List<String> images = sheets.stream().map(this::dataUrl).toList();
            return new Evidence(artifacts, images);
        } catch (IOException ex) {
            log.error("Falha de I/O ao inspecionar vídeo de referência; executionId={}", context.executionId(), ex);
            throw new IllegalStateException("Não foi possível inspecionar o arquivo de referência", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Inspeção interrompida; executionId={}", context.executionId(), ex);
            throw new IllegalStateException("Inspeção de mídia interrompida", ex);
        } finally {
            deleteTree(directory);
        }
    }

    /** Valida protocolo e origem antes de iniciar qualquer download. */
    private URI validatedSource(String value) {
        URI uri = URI.create(value);
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException("A referência deve usar URL pública HTTP ou HTTPS");
        }
        if (uri.getHost() == null) {
            throw new IllegalArgumentException("A referência deve possuir um host público válido");
        }
        if (!properties.getReferenceAnalysis().isAllowPrivateSourceUrls()) {
            try {
                for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                    if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                            || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                            || address.isMulticastAddress()) {
                        throw new IllegalArgumentException("A referência não pode apontar para rede privada ou local");
                    }
                }
            } catch (IOException ex) {
                log.error("Falha ao resolver host da referência; host={}", uri.getHost(), ex);
                throw new IllegalArgumentException("Não foi possível resolver o host público da referência", ex);
            }
        }
        return uri;
    }

    /** Baixa o payload bruto com limite estrito e registra a origem para auditoria de ingestão. */
    private void download(Long executionId, URI source, Path destination) throws IOException, InterruptedException {
        URI current = source;
        for (int redirectCount = 0; redirectCount <= 5; redirectCount++) {
            log.info("Ingestão bruta da referência; executionId={} url={}", executionId, current);
            HttpRequest request = HttpRequest.newBuilder(current).timeout(Duration.ofMinutes(3)).GET().build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 300 && response.statusCode() < 400) {
                try (InputStream ignored = response.body()) {
                    if (redirectCount == 5) {
                        throw new IllegalArgumentException("A referência excedeu o limite de redirecionamentos");
                    }
                    String location = response.headers().firstValue("location")
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Redirecionamento da referência sem cabeçalho Location"));
                    current = validatedSource(current.resolve(location).toString());
                }
                continue;
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                try (InputStream ignored = response.body()) {
                    throw new IllegalStateException("Download da referência falhou com HTTP "
                            + response.statusCode());
                }
            }
            long declared = response.headers().firstValueAsLong("content-length").orElse(-1L);
            if (declared > properties.getReferenceAnalysis().getMaxDownloadBytes()) {
                try (InputStream ignored = response.body()) {
                    throw new IllegalArgumentException("Vídeo excede o limite configurado para análise");
                }
            }
            try (InputStream input = response.body(); OutputStream output = Files.newOutputStream(destination)) {
                byte[] buffer = new byte[8192];
                long total = 0;
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    total += read;
                    if (total > properties.getReferenceAnalysis().getMaxDownloadBytes()) {
                        throw new IllegalArgumentException("Vídeo excede o limite configurado para análise");
                    }
                    output.write(buffer, 0, read);
                }
            }
            return;
        }
        throw new IllegalArgumentException("A referência excedeu o limite de redirecionamentos");
    }

    /** Obtém streams, codec, resolução, taxa de quadros e duração pelo ffprobe. */
    private JsonNode probe(Long executionId, Path video) throws IOException, InterruptedException {
        String output = command(executionId, List.of(
                properties.getReferenceAnalysis().getFfprobePath(), "-v", "error", "-show_format", "-show_streams",
                "-of", "json", video.toString()));
        return objectMapper.readTree(output);
    }

    /** Detecta viradas visuais usando o mesmo limiar em todas as referências. */
    private List<Double> detectScenes(Long executionId, Path video) throws IOException, InterruptedException {
        String filter = "select='gt(scene," + properties.getReferenceAnalysis().getSceneThreshold() + ")',showinfo";
        String output = command(executionId, List.of(properties.getReferenceAnalysis().getFfmpegPath(), "-hide_banner",
                "-i", video.toString(), "-vf", filter, "-an", "-f", "null", "-"));
        Matcher matcher = SCENE_TIME.matcher(output);
        List<Double> times = new ArrayList<>();
        while (matcher.find() && times.size() < 120) {
            times.add(Double.parseDouble(matcher.group(1)));
        }
        return times;
    }

    /** Mede loudness integrado e true peak para orientar voz, trilha e acabamento. */
    private AudioMetrics analyzeAudio(Long executionId, Path video) throws IOException, InterruptedException {
        String output = command(executionId, List.of(properties.getReferenceAnalysis().getFfmpegPath(), "-hide_banner",
                "-i", video.toString(), "-filter_complex", "ebur128=peak=true", "-f", "null", "-"));
        return new AudioMetrics(lastNumber(INTEGRATED_LOUDNESS, output), lastNumber(TRUE_PEAK, output));
    }

    /** Cria dois painéis de doze frames para leitura visual multimodal com custo previsível. */
    private List<Path> createContactSheets(Long executionId, Path video, Path directory, double duration)
            throws IOException, InterruptedException {
        double half = Math.max(1.0, duration / 2.0);
        List<Path> sheets = new ArrayList<>();
        for (int index = 0; index < 2; index++) {
            double start = index * half;
            Path sheet = directory.resolve("contact-sheet-" + (index + 1) + ".jpg");
            double fps = 12.0 / half;
            command(executionId, List.of(properties.getReferenceAnalysis().getFfmpegPath(), "-hide_banner", "-y",
                    "-ss", format(start), "-t", format(half), "-i", video.toString(), "-vf",
                    "fps=" + format(fps) + ",scale=240:-1,tile=4x3:padding=4:margin=4", "-frames:v", "1",
                    "-q:v", "3", sheet.toString()));
            sheets.add(sheet);
        }
        return sheets;
    }

    /** Consolida evidências técnicas e hashes sem persistir cópias binárias no banco. */
    private ObjectNode artifacts(Path video, JsonNode probe, double duration, List<Double> scenes,
                                 AudioMetrics audio, List<Path> sheets) throws IOException {
        ObjectNode result = objectMapper.createObjectNode();
        JsonNode videoStream = firstStream(probe, "video");
        JsonNode audioStream = firstStream(probe, "audio");
        result.put("sha256", sha256(video));
        result.put("bytes", Files.size(video));
        result.put("durationSeconds", duration);
        result.put("width", videoStream.path("width").asInt());
        result.put("height", videoStream.path("height").asInt());
        result.put("videoCodec", videoStream.path("codec_name").asText());
        result.put("frameRate", videoStream.path("avg_frame_rate").asText());
        result.put("audioCodec", audioStream.path("codec_name").asText());
        putFinite(result, "integratedLoudnessLufs", audio.integratedLoudness());
        putFinite(result, "truePeakDbfs", audio.truePeak());
        result.put("sceneChangeThreshold", properties.getReferenceAnalysis().getSceneThreshold());
        ArrayNode sceneArray = result.putArray("sceneChangeTimesSeconds");
        scenes.forEach(sceneArray::add);
        result.put("sceneChangeCount", scenes.size());
        ArrayNode contactSheets = result.putArray("contactSheets");
        for (int index = 0; index < sheets.size(); index++) {
            ObjectNode sheet = contactSheets.addObject();
            sheet.put("part", index + 1);
            sheet.put("sha256", sha256(sheets.get(index)));
            sheet.put("frames", 12);
        }
        return result;
    }

    /** Localiza o primeiro stream do tipo solicitado no retorno do ffprobe. */
    private JsonNode firstStream(JsonNode probe, String type) {
        for (JsonNode stream : probe.path("streams")) {
            if (type.equals(stream.path("codec_type").asText())) {
                return stream;
            }
        }
        return objectMapper.createObjectNode();
    }

    /** Resolve duração preferindo o container e usando stream como fallback. */
    private double duration(JsonNode probe) {
        double value = probe.path("format").path("duration").asDouble(0);
        if (value > 0) {
            return value;
        }
        for (JsonNode stream : probe.path("streams")) {
            value = Math.max(value, stream.path("duration").asDouble(0));
        }
        if (value <= 0) {
            throw new IllegalArgumentException("Não foi possível determinar a duração do vídeo");
        }
        return value;
    }

    /** Executa ferramenta local e exige saída bem-sucedida. */
    private String command(Long executionId, List<String> arguments) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(arguments).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("Ferramenta de mídia falhou; executionId={} tool={} exitCode={} output={}",
                    executionId, arguments.getFirst(), exitCode, output);
            throw new IllegalStateException("Ferramenta de mídia falhou: " + arguments.getFirst());
        }
        return output;
    }

    /** Persiste métrica finita ou nulo explícito quando a mídia não possui faixa mensurável. */
    private void putFinite(ObjectNode target, String field, double value) {
        if (Double.isFinite(value)) {
            target.put(field, value);
        } else {
            target.putNull(field);
        }
    }

    /** Recupera a última métrica numérica registrada pelo filtro. */
    private double lastNumber(Pattern pattern, String output) {
        Matcher matcher = pattern.matcher(output);
        double value = Double.NaN;
        while (matcher.find()) {
            value = Double.parseDouble(matcher.group(1));
        }
        return value;
    }

    /** Converte contact sheet em data URL para a entrada multimodal da IA. */
    private String dataUrl(Path path) {
        try {
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(path));
        } catch (IOException ex) {
            log.error("Falha ao carregar contact sheet da análise; path={}", path, ex);
            throw new IllegalStateException("Não foi possível carregar contact sheet", ex);
        }
    }

    /** Calcula hash auditável em streaming para não carregar o vídeo inteiro na heap. */
    private String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            log.error("SHA-256 indisponível ao analisar mídia; path={}", path, ex);
            throw new IllegalStateException("SHA-256 indisponível", ex);
        }
    }

    /** Formata números para filtros ffmpeg de forma independente do locale. */
    private String format(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    /** Remove os arquivos temporários da execução mesmo quando um comando falha. */
    private void deleteTree(Path directory) {
        if (directory == null) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted((first, second) -> second.compareTo(first)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    log.warn("Falha ao remover artefato temporário da análise; path={}", path, ex);
                }
            });
        } catch (IOException ex) {
            log.warn("Falha ao remover diretório temporário da análise; path={}", directory, ex);
        }
    }

    /** Evidência técnica acompanhada das imagens efêmeras enviadas à IA. */
    public record Evidence(ObjectNode artifacts, List<String> contactSheetDataUrls) { }

    /** Métricas objetivas da faixa de áudio do arquivo. */
    private record AudioMetrics(double integratedLoudness, double truePeak) { }
}
