package com.marketinghub.videomanagement.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

/** Restaura segmentos de fala auditados sem chamar novamente o serviço de síntese. */
final class PreservedNarrationSegments {
    private static final Logger log = LoggerFactory.getLogger(PreservedNarrationSegments.class);

    /** Transporta apenas arquivo e identidade da fala restaurada para o compositor. */
    record Segment(Path file, String model, String voice, Map<String, Object> interaction) {}

    /** Impede criar um restaurador sem dependências explícitas. */
    private PreservedNarrationSegments() {}

    /** Confere texto, ordem, tenant e hash antes de usar qualquer binário preservado. */
    static List<Segment> restore(JsonNode metadata, List<String> texts, Long jobId,
                                 WebClient client, ObjectMapper mapper) throws IOException {
        JsonNode contract = metadata.path("preservedNarration");
        require("PRESERVED_TTS_NARRATION_V1".equals(contract.path("contractVersion").asText())
                && !contract.path("newTtsAuthorized").asBoolean(true)
                && contract.path("sourceJobId").asLong() > 0
                && contract.path("tenantId").asText().equals(metadata.path("tenantId").asText())
                && contract.path("captionText").asText().equals(metadata.path("captionText").asText()),
                "Contrato da fala preservada divergente.");
        JsonNode segments = contract.path("segments");
        require(segments.isArray() && segments.size() == texts.size(), "Segmentos preservados incompletos.");
        List<Segment> result = new ArrayList<>();
        try {
            for (int index = 0; index < texts.size(); index++) {
                JsonNode segment = segments.get(index);
                require(segment.path("segmentIndex").asInt() == index + 1
                        && segment.path("text").asText().equals(texts.get(index))
                        && segment.path("assetId").asLong() > 0,
                        "Texto ou identidade do segmento preservado divergente.");
                URI uri = URI.create(segment.path("url").asText());
                require("https".equals(uri.getScheme()) || ("http".equals(uri.getScheme())
                        && List.of("127.0.0.1", "localhost").contains(uri.getHost())),
                        "Origem do áudio preservado inválida.");
                byte[] content = client.get().uri(uri)
                        .retrieve().bodyToMono(byte[].class).block();
                require(content != null && content.length > 0 && content.length <= 10_000_000
                        && sha256(content, jobId).equals(segment.path("sha256").asText()),
                        "Hash do áudio preservado divergente.");
                Path file = Files.createTempFile("sales-video-" + jobId + "-voiceover-reused-", ".mp3");
                Map<String, Object> interaction = new LinkedHashMap<>();
                interaction.put("interaction_type", "TEXT_TO_SPEECH_REUSE");
                interaction.put("provider", "OPENAI");
                interaction.put("status", "REUSED");
                interaction.put("job_id", jobId);
                interaction.put("source_job_id", contract.path("sourceJobId").asLong());
                interaction.put("source_asset_id", segment.path("assetId").asLong());
                interaction.put("source_sha256", segment.path("sha256").asText());
                interaction.put("segment_index", index + 1);
                interaction.put("model", segment.path("model").asText());
                interaction.put("source_pricing", mapper.convertValue(segment.path("sourcePricing"), Map.class));
                interaction.put("incremental_cost_usd", 0);
                result.add(new Segment(file, segment.path("model").asText(), segment.path("voice").asText(), interaction));
                Files.write(file, content);
            }
            return result;
        } catch (IOException | RuntimeException ex) {
            log.error("Falha ao recuperar voz preservada; jobId={} sourceJobId={}",
                    jobId, contract.path("sourceJobId").asLong(), ex);
            for (Segment segment : result) Files.deleteIfExists(segment.file());
            throw ex;
        }
    }

    /** Calcula a identidade do binário recebido, preservando contexto em falha criptográfica. */
    private static String sha256(byte[] content, Long jobId) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException ex) {
            log.error("SHA-256 indisponível na recuperação da fala; jobId={}", jobId, ex);
            throw new IllegalStateException("SHA-256 indisponível", ex);
        }
    }

    /** Recusa recuperação insegura sem cair silenciosamente em nova síntese paga. */
    private static void require(boolean valid, String message) {
        if (!valid) throw new VideoProviderException("PRESERVED_NARRATION_INVALID", message);
    }
}
