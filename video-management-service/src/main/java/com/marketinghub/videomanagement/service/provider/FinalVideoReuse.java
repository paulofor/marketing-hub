package com.marketinghub.videomanagement.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.dto.AssetType;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/** Recupera os bytes de um acabamento pronto sem gerar novamente vídeo, voz ou legendas. */
final class FinalVideoReuse {
    private static final Logger log = LoggerFactory.getLogger(FinalVideoReuse.class);
    private final WebClient client;
    private final ObjectMapper mapper;

    /** Usa o cliente limitado de download do executor para recuperar os artefatos persistidos. */
    FinalVideoReuse(WebClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    /** Confere os hashes congelados pelo backend e preserva o conteúdo final e suas avaliações. */
    ProviderArtifacts restore(SalesVideoJob job, JsonNode metadata) {
        try {
            JsonNode source = metadata.path("delivery_source");
            if (!"VIDEO_FINAL_REUSE_V1".equals(source.path("contractVersion").asText())) {
                throw new IllegalStateException("Contrato de recuperação ausente.");
            }
            byte[] video = download(source, "videoUrl", "videoSha256");
            byte[] captions = download(source, "captionUrl", "captionSha256");
            Map<String, Object> audit = mapper.convertValue(metadata, LinkedHashMap.class);
            audit.remove("cost_estimation");
            audit.put("cost_usd", BigDecimal.ZERO);
            audit.put("delivery_only", true);
            audit.put("generation_repeated", false);
            audit.put("provider", "MUSA_POST_PRODUCTION");
            log.info("Acabamento reaproveitado sem geração; jobId={} sourceJobId={} videoSha256={}",
                    job.id(), source.path("jobId").asLong(), source.path("videoSha256").asText());
            return new ProviderArtifacts("delivery-only-" + job.id(),
                    new ProviderFile("sales-video-" + job.id() + "-final.mp4", MediaType.valueOf("video/mp4"),
                            AssetType.VIDEO, ProviderAssetRole.VIDEO, video), null,
                    new ProviderFile("sales-video-" + job.id() + "-final.vtt", MediaType.valueOf("text/vtt"),
                            AssetType.CAPTION, ProviderAssetRole.CAPTION, captions), audit);
        } catch (Exception ex) {
            log.error("Falha ao recuperar acabamento preservado; jobId={}", job.id(), ex);
            throw new VideoProviderException("VIDEO_FINAL_REUSE_FAILED", "Não foi possível conferir o acabamento fonte", ex);
        }
    }

    /** Baixa somente a URL congelada e recusa conteúdo vazio ou com identidade diferente. */
    private byte[] download(JsonNode source, String urlKey, String hashKey) throws Exception {
        String url = source.path(urlKey).asText();
        String hash = source.path(hashKey).asText();
        if (!url.matches("https?://[^\\s]+") || !hash.matches("[a-f0-9]{64}")) {
            throw new IllegalStateException("URL ou hash de origem inválido.");
        }
        byte[] bytes = client.get().uri(url).retrieve().bodyToMono(byte[].class).block(Duration.ofSeconds(90));
        if (bytes == null || bytes.length == 0
                || !hash.equals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)))) {
            throw new IllegalStateException("Hash divergente no artefato " + urlKey);
        }
        return bytes;
    }
}
