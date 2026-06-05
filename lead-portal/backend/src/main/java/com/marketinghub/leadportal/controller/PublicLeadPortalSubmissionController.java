package com.marketinghub.leadportal.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.service.ExperimentFunnelTrackingClient;
import com.marketinghub.leadportal.service.ExperimentFunnelTrackingClient.TrackingResult;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller de compatibilidade que recebe submissões públicas publicadas no host do Lead Portal.
 */
@RestController
@RequestMapping("/api/public/lead-portal/flows")
public class PublicLeadPortalSubmissionController {

    private static final Logger log = LoggerFactory.getLogger(PublicLeadPortalSubmissionController.class);

    private final ExperimentFunnelTrackingClient trackingClient;
    private final ObjectMapper objectMapper;

    /**
     * Inicializa o controller com cliente de tracking e parser JSON.
     */
    public PublicLeadPortalSubmissionController(
            ExperimentFunnelTrackingClient trackingClient,
            ObjectMapper objectMapper) {
        this.trackingClient = trackingClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Recebe o contrato canônico de submissão gerado pelo GeraLanding e encaminha ao Marketing Hub.
     */
    @PostMapping("/{slug}/submission")
    public ResponseEntity<Void> registerSubmission(
            @PathVariable("slug") String slug,
            @RequestBody(required = false) String requestBody) {
        Map<String, Object> payload = parseSubmissionRequest(slug, requestBody);
        String payloadSlug = textValue(payload.get("slug"));
        if (payloadSlug != null && !payloadSlug.equals(slug)) {
            log.warn("Submission com slug divergente recebida no Lead Portal. routeSlug={}, payloadSlug={}", slug, payloadSlug);
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> contato = objectMap(payload.get("contato"));
        UUID submissionId = resolveSubmissionId(textValue(payload.get("submissionId")));
        Instant submittedAt = parseSubmittedAt(textValue(payload.get("submittedAt")));
        String campaignCode = textValue(payload.get("campaignCode"));
        String contactName = textValue(contato.get("nome"));
        String contactEmail = textValue(contato.get("email"));
        String contactPhone = textValue(contato.get("telefone"));

        log.info("Submission pública recebida no Lead Portal. slug={}, submissionId={}, emailPresent={}",
                slug, submissionId, StringUtils.hasText(contactEmail));
        TrackingResult result = trackingClient.registerSubmission(
                slug,
                submissionId,
                submittedAt,
                campaignCode,
                contactName,
                contactEmail,
                contactPhone);
        return toResponse(result);
    }

    /**
     * Faz o parse do payload cru de submissão preservando contexto operacional em caso de JSON inválido.
     */
    private Map<String, Object> parseSubmissionRequest(String slug, String requestBody) {
        if (!StringUtils.hasText(requestBody)) {
            throw new IllegalArgumentException("Payload de submissão é obrigatório");
        }
        try {
            return objectMapper.readValue(requestBody, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            log.warn("Payload inválido em submission pública do Lead Portal. slug={}, rawPayload={}", slug, requestBody, ex);
            throw new IllegalArgumentException("Payload de submissão inválido", ex);
        }
    }

    /**
     * Resolve o identificador idempotente da submissão aceitando UUID nativo ou texto legado determinístico.
     */
    private UUID resolveSubmissionId(String submissionId) {
        if (!StringUtils.hasText(submissionId)) {
            throw new IllegalArgumentException("submissionId é obrigatório");
        }
        String sanitized = submissionId.trim();
        try {
            return UUID.fromString(sanitized);
        } catch (IllegalArgumentException ex) {
            UUID deterministicId = UUID.nameUUIDFromBytes(sanitized.getBytes(StandardCharsets.UTF_8));
            log.warn("submissionId textual convertido para UUID determinístico no Lead Portal. submissionId={}, deterministicId={}",
                    sanitized, deterministicId, ex);
            return deterministicId;
        }
    }

    /**
     * Converte submittedAt para Instant e usa o momento atual quando a origem não envia valor válido.
     */
    private Instant parseSubmittedAt(String submittedAt) {
        if (!StringUtils.hasText(submittedAt)) {
            return Instant.now();
        }
        try {
            return Instant.parse(submittedAt.trim());
        } catch (DateTimeParseException ex) {
            log.warn("submittedAt inválido em submission pública do Lead Portal. submittedAt={}", submittedAt, ex);
            return Instant.now();
        }
    }

    /**
     * Converte valor genérico em mapa quando o JSON contém objeto aninhado.
     */
    private Map<String, Object> objectMap(Object value) {
        if (value instanceof Map<?, ?> rawMap) {
            return rawMap.entrySet().stream()
                    .filter(entry -> entry.getKey() != null)
                    .collect(java.util.stream.Collectors.toMap(
                            entry -> entry.getKey().toString(),
                            Map.Entry::getValue,
                            (left, right) -> right));
        }
        return Map.of();
    }

    /**
     * Extrai texto normalizado de valores JSON simples.
     */
    private String textValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * Converte o resultado do encaminhamento para resposta HTTP pública do Lead Portal.
     */
    private ResponseEntity<Void> toResponse(TrackingResult result) {
        return switch (result) {
            case FORWARDED -> ResponseEntity.accepted().build();
            case SKIPPED -> ResponseEntity.noContent().build();
            case FAILED -> ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        };
    }
}
