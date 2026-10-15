package com.marketinghub.videomanagement.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

/** Compõe amostras comerciais íntegras com cenas variadas, sem simular interface ou resultado real. */
final class DeliveryExamplesMontage {
    private static final Logger log = LoggerFactory.getLogger(DeliveryExamplesMontage.class);
    private static final String CONTRACT = "APPROVED_DELIVERY_EXAMPLES_V1";
    private final VideoManagementProperties properties;
    private final WebClient client;

    /** Configura download limitado das fontes congeladas pelo backend e o compositor local. */
    DeliveryExamplesMontage(VideoManagementProperties properties, WebClient.Builder builder) {
        this.properties = properties;
        this.client = builder.clone().codecs(c -> c.defaultCodecs().maxInMemorySize(20 * 1024 * 1024)).build();
    }

    /** Reconhece somente a seleção explícita; os contratos antigos preservam a montagem existente. */
    static boolean requested(JsonNode metadata) {
        return !metadata.at("/post_production/deliveryExamples").isMissingNode();
    }

    /** Confere contexto, direitos e pixels antes de qualquer síntese de voz. */
    Prepared prepare(JsonNode metadata, Long jobId) {
        com.marketinghub.videomanagement.service.CustomerVideoCopyGuard.validate(metadata);
        JsonNode contract = metadata.at("/post_production/deliveryExamples");
        if (!CONTRACT.equals(contract.path("contractVersion").asText())
                || contract.path("commercialEvidenceClaimed").asBoolean(true)
                || contract.path("runtimeCaptureClaimed").asBoolean(true)
                || metadata.path("tenantId").asText().isBlank()
                || !metadata.path("tenantId").asText().equals(contract.path("tenantId").asText())
                || !metadata.path("campaign_key").asText().equals(contract.path("productVersion").asText()))
            throw invalid("As amostras não correspondem ao contexto audiovisual.");
        for (String id : List.of("productId", "experimentId"))
            if (metadata.path(id).asLong() <= 0 || metadata.path(id).asLong() != contract.path(id).asLong())
                throw invalid("Amostra de outro produto ou experimento.");
        if (metadata.path("videoProjectId").asLong() <= 0
                || metadata.path("videoProjectId").asLong() != contract.path("projectId").asLong())
            throw invalid("Amostra de outro projeto.");
        JsonNode shots = contract.path("shots");
        String[] captions = metadata.path("captionText").asText().split("\\s*\\|\\s*", -1);
        if (!shots.isArray() || shots.size() < 2 || shots.size() > 12
                || !metadata.at("/technicalQualityGate/captionMustMatchNarration").asBoolean())
            throw invalid("A montagem exige amostras e trechos de fala sincronizados.");
        int previous = -1;
        var hashes = new java.util.HashSet<String>();
        for (JsonNode shot : shots) {
            int cue = shot.path("captionCueIndex").asInt(-1);
            String hash = shot.path("sha256").asText();
            if (cue <= previous || cue >= captions.length || (previous == -1 && cue != 0)
                    || shot.path("visualAssetId").asLong() <= 0 || !hash.matches("[0-9a-f]{64}")
                    || shot.path("headline").asText().length() > 90
                    || shot.path("rightsStatement").asText().isBlank() || shot.path("origin").asText().isBlank())
                throw invalid("Amostras sem sequência, identidade, direitos ou hash válidos.");
            var uri = java.net.URI.create(shot.path("url").asText());
            if (!uri.isAbsolute() || !("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null)
                throw invalid("Origem da amostra inválida.");
            previous = cue; hashes.add(hash);
        }
        if (hashes.size() < 2) throw invalid("Repetir o mesmo arquivo não demonstra variedade da entrega.");
        var files = new LinkedHashMap<String, Path>();
        try {
            for (JsonNode shot : shots) {
                String hash = shot.path("sha256").asText();
                if (files.containsKey(hash)) continue;
                log.info("Baixando amostra audiovisual; jobId={} visualAssetId={} url={}", jobId,
                        shot.path("visualAssetId").asLong(), shot.path("url").asText());
                byte[] bytes = client.get().uri(shot.path("url").asText()).header("User-Agent", "MarketingHub-Video/1.0")
                        .retrieve().bodyToMono(byte[].class).block(java.time.Duration.ofSeconds(60));
                if (bytes == null || !hash.equals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))))
                    throw invalid("A amostra mudou depois da aprovação.");
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                if (image == null || image.getWidth() < 600 || image.getHeight() < 600
                        || image.getHeight() > image.getWidth() * 2L || image.getWidth() > image.getHeight() * 2L)
                    throw invalid("A amostra precisa ser uma arte legível, não uma captura longa reduzida.");
                Path file = Files.createTempFile("delivery-example-" + jobId, ".png");
                files.put(hash, file); Files.write(file, bytes);
                log.info("Amostra audiovisual recebida; jobId={} visualAssetId={} bytes={} sha256={}",
                        jobId, shot.path("visualAssetId").asLong(), bytes.length, hash);
            }
            return new Prepared(contract, files, jobId);
        } catch (Exception ex) {
            files.values().forEach(path -> delete(path, jobId));
            log.error("Falha ao validar amostras antes da voz; jobId={}", jobId, ex);
            if (ex instanceof VideoProviderException failure) throw failure;
            throw new VideoProviderException("DELIVERY_EXAMPLES_INVALID", "Não foi possível validar as amostras.", ex);
        }
    }

    /** Monta cenas no relógio da voz, alternando enquadramentos sem ocultar o conteúdo das artes. */
    ProductUgcReferenceOverlay.OverlayResult compose(Prepared prepared, JsonNode metadata,
            List<PdeProofCaptionTiming.Window> captions, double duration) {
        var temporary = new ArrayList<Path>();
        Path output = null;
        var audits = new ArrayList<Map<String, Object>>();
        try {
            var shots = prepared.contract().path("shots");
            var clips = new ArrayList<Path>();
            String[] copy = metadata.path("captionText").asText().split("\\s*\\|\\s*", -1);
            for (int index = 0; index < shots.size(); index++) {
                JsonNode shot = shots.get(index);
                int cue = shot.path("captionCueIndex").asInt();
                double start = captions.get(cue).start();
                double end = index + 1 < shots.size()
                        ? captions.get(shots.get(index + 1).path("captionCueIndex").asInt()).start() : duration;
                if (!Double.isFinite(start) || !Double.isFinite(end) || end <= start || end > duration + .05)
                    throw invalid("O relógio da narração não corresponde às amostras.");
                int variations = Math.max(1, (int) Math.ceil((end - start) / 5));
                for (int part = 0; part < variations; part++) {
                    double partStart = start + (end - start) * part / variations;
                    double partEnd = start + (end - start) * (part + 1) / variations;
                    double seconds = (Math.round(partEnd * 30) - Math.round(partStart * 30)) / 30.0;
                    int layout = (index + part) % 3;
                    Path background = Files.createTempFile("delivery-layout-" + prepared.jobId(), ".png");
                    temporary.add(background);
                    String headline = shot.path("headline").asText().strip();
                    drawBackground(background, headline.isEmpty() ? copy[cue] : headline, index + 1, shots.size(), layout);
                    Path clip = Files.createTempFile("delivery-scene-" + prepared.jobId(), ".mp4");
                    temporary.add(clip); clips.add(clip);
                    Path image = prepared.files().get(shot.path("sha256").asText());
                    Path companion = prepared.files().get(shots.get((index + 1) % shots.size()).path("sha256").asText());
                    render(background, image, companion, clip, seconds, layout, prepared.jobId());
                }
                audits.add(Map.of("visualAssetId", shot.path("visualAssetId").asLong(), "sha256", shot.path("sha256").asText(),
                        "startSeconds", start, "endSeconds", end, "reviewMoment", (start + end) / 2,
                        "captionCueIndex", cue, "layouts", variations));
            }
            Path list = Files.createTempFile("delivery-concat-" + prepared.jobId(), ".txt");
            temporary.add(list);
            Files.writeString(list, String.join("\n", clips.stream().map(path -> "file '" + path + "'").toList()), StandardCharsets.UTF_8);
            output = Files.createTempFile("delivery-montage-" + prepared.jobId(), ".mp4");
            run(List.of(ffmpeg(), "-v", "error", "-y", "-f", "concat", "-safe", "0", "-i", list.toString(),
                    "-c", "copy", "-t", number(duration), "-movflags", "+faststart", output.toString()), prepared.jobId());
            return new ProductUgcReferenceOverlay.OverlayResult(output, Map.of("contractVersion", CONTRACT,
                    "status", "APPLIED", "compositionMode", "NARRATED_DELIVERY_MONTAGE_V1", "shots", audits,
                    "motionPolicy", "ENTRY_SETTLE_READING_V1",
                    "runtimeCaptureClaimed", false, "commercialEvidenceClaimed", false,
                    "reviewMoments", audits.stream().map(value -> value.get("reviewMoment")).toList()));
        } catch (Exception ex) {
            delete(output, prepared.jobId());
            log.error("Falha na montagem das amostras; jobId={}", prepared.jobId(), ex);
            if (ex instanceof VideoProviderException failure) throw failure;
            throw new VideoProviderException("DELIVERY_EXAMPLES_INVALID", "Falha na composição das amostras.", ex);
        } finally {
            temporary.forEach(path -> delete(path, prepared.jobId()));
        }
    }

    /** Compõe títulos legíveis e identifica as amostras discretamente, reservando espaço às legendas. */
    private void drawBackground(Path path, String copy, int position, int count, int layout) throws IOException {
        var image = new BufferedImage(1080, 1920, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, new Color(0x291923), 1080, 1750, new Color(0x662f48)));
            g.fillRect(0, 0, 1080, 1920);
            g.setColor(new Color(0x914b67)); g.fillOval(layout == 1 ? -450 : 600, 480, 940, 1200);
            g.setColor(new Color(0xffeddd));
            g.setFont(new Font("DejaVu Sans", Font.BOLD, 26));
            g.drawString("KIT PERSONALIZADO PARA DIVULGAÇÃO", 80, 160);
            int size = 72;
            List<String> lines;
            do {
                g.setFont(new Font("DejaVu Sans", Font.BOLD, size));
                lines = wrap(copy, g.getFontMetrics(), 920); size -= 2;
            } while (lines.size() > 4 && size >= 42);
            if (lines.size() > 5) throw invalid("Texto longo demais para abertura legível da cena.");
            for (int i = 0; i < lines.size(); i++) g.drawString(lines.get(i), 80, 270 + i * (size + 14));
            g.setFont(new Font("DejaVu Sans", Font.PLAIN, 26));
            g.drawString("Amostras ilustrativas do kit", 80, 1510);
            g.setColor(new Color(0xbb8093)); g.fillRoundRect(80, 1460, 920, 5, 5, 5);
            g.setColor(new Color(0xffd4ba)); g.fillRoundRect(80, 1460, 920 * position / count, 5, 5, 5);
            ImageIO.write(image, "png", path.toFile());
        } finally { g.dispose(); }
    }

    /** Quebra palavras pela largura medida, preservando o conteúdo aprovado sem cortes ocultos. */
    private List<String> wrap(String text, java.awt.FontMetrics metrics, int width) {
        var lines = new ArrayList<String>();
        String line = "";
        for (String word : text.strip().split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && metrics.stringWidth(candidate) > width) { lines.add(line); line = word; }
            else line = candidate;
        }
        if (!line.isEmpty()) lines.add(line);
        return lines;
    }

    /** Faz uma entrada suave e estabiliza as artes para leitura; a progressão vem das cenas. */
    private void render(Path background, Path image, Path companion, Path output, double seconds, int layout, Long jobId) {
        int size = layout == 1 ? 810 : 900;
        String slide = layout == 2 ? "-" : "+";
        String entrance = "pow(max(0,1-t/0.45),3)";
        String loop = "loop=loop=-1:size=1:start=0,setpts=N/(30*TB)";
        String filter = "[0:v]format=yuv420p," + loop + "[background];"
                + "[1:v]scale=" + size + ":850:force_original_aspect_ratio=decrease,format=rgba," + loop + "[art];"
                + (layout == 1 ? "[2:v]scale=630:720:force_original_aspect_ratio=decrease,format=rgba,rotate=-0.10:fillcolor=none," + loop + "[other];"
                    + "[background][other]overlay=x='-75-25*" + entrance + "':y=610[base];" : "[background]null[base];")
                + "[base][art]overlay=x='(W-w)/2" + slide + "60*" + entrance + "'"
                + ":y=575[out]";
        run(List.of(ffmpeg(), "-v", "error", "-y", "-threads", "1", "-i", background.toString(),
                "-threads", "1", "-i", image.toString(), "-threads", "1", "-i", companion.toString(),
                "-filter_complex_threads", "1", "-filter_complex", filter, "-map", "[out]", "-an", "-t", number(seconds),
                "-c:v", "libx264", "-threads", "2", "-preset", "veryfast", "-crf", "20", "-pix_fmt", "yuv420p",
                "-r", "30", "-movflags", "+faststart", output.toString()), jobId);
    }

    /** Executa composição local com prazo e saída de diagnóstico, sem comandos de shell. */
    private void run(List<String> command, Long jobId) {
        Path output = null;
        Process process = null;
        try {
            output = Files.createTempFile("delivery-ffmpeg-" + jobId, ".log");
            process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
            if (!process.waitFor(120, TimeUnit.SECONDS)) { process.destroyForcibly(); throw invalid("Tempo de montagem excedido."); }
            if (process.exitValue() != 0) throw invalid("Falha na montagem: " + Files.readString(output));
        } catch (IOException | InterruptedException | RuntimeException ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            log.error("Composição local falhou; jobId={}", jobId, ex);
            if (ex instanceof VideoProviderException failure) throw failure;
            throw new VideoProviderException("DELIVERY_EXAMPLES_INVALID", "Falha no compositor local.", ex);
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
            delete(output, jobId);
        }
    }

    /** Lê o executável configurado no worker responsável pela montagem. */
    private String ffmpeg() { return properties.getProviders().getPostProduction().getFfmpegPath(); }
    /** Formata o relógio sem separador decimal regional. */
    private String number(double value) { return String.format(Locale.ROOT, "%.6f", value); }
    /** Expõe a causa funcional sem aprovar conteúdo ou tentar outra fonte. */
    private static VideoProviderException invalid(String message) { return new VideoProviderException("DELIVERY_EXAMPLES_INVALID", message); }
    /** Remove somente temporários pertencentes a esta execução. */
    private static void delete(Path path, Long jobId) {
        if (path == null) return;
        try { Files.deleteIfExists(path); }
        catch (IOException ex) { log.warn("Falha ao limpar montagem; jobId={} path={}", jobId, path, ex); }
    }

    /** Mantém as fontes verificadas até o fim do acabamento, com limpeza mesmo em falha de voz. */
    record Prepared(JsonNode contract, Map<String, Path> files, Long jobId) implements AutoCloseable {
        /** Libera fontes temporárias sem apagar os artefatos persistidos do produto. */
        @Override public void close() { files.values().forEach(path -> delete(path, jobId)); }
    }
}
