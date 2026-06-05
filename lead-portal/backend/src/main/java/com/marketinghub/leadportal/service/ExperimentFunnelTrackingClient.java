package com.marketinghub.leadportal.service;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Cliente responsável por encaminhar eventos de engajamento do Lead Portal ao Marketing Hub.
 */
@Service
public class ExperimentFunnelTrackingClient {

    private static final Logger log = LoggerFactory.getLogger(ExperimentFunnelTrackingClient.class);
    private static final String SUBMISSION_CONTRACT_VERSION = "lead-portal-submission-engagement.v1";

    public enum TrackingResult {
        FORWARDED,
        SKIPPED,
        FAILED
    }

    private final RestTemplate restTemplate;
    private final URI baseUri;

    /**
     * Configura o cliente HTTP com a URL base pública de tracking do Marketing Hub.
     */
    public ExperimentFunnelTrackingClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${lead-portal.marketing-hub.funnel-tracking-base-url}") String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("lead-portal.marketing-hub.funnel-tracking-base-url não configurado");
        }
        String sanitized = baseUrl.trim();
        if (sanitized.endsWith("/")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }
        this.baseUri = URI.create(sanitized);
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Encaminha o evento de renderização completa do fluxo para o Marketing Hub.
     */
    public TrackingResult registerRenderComplete(String slug, String visitorId, String campaignCode) {
        if (!StringUtils.hasText(slug)) {
            throw new IllegalArgumentException("O slug do fluxo é obrigatório");
        }

        URI endpoint = UriComponentsBuilder.fromUri(baseUri)
                .path("/flows/{slug}/render-complete")
                .buildAndExpand(slug.trim())
                .toUri();

        HttpEntity<Map<String, String>> entity = buildRenderPayload(visitorId, campaignCode);
        return sendTrackingRequest(endpoint, entity, "render-complete", slug);
    }

    /**
     * Encaminha o evento idempotente de submissão do formulário para o Marketing Hub.
     */
    public TrackingResult registerSubmission(
            String slug,
            UUID submissionId,
            Instant submittedAt,
            String campaignCode,
            String contactName,
            String contactEmail,
            String contactPhone) {
        if (!StringUtils.hasText(slug)) {
            throw new IllegalArgumentException("O slug do fluxo é obrigatório");
        }
        if (submissionId == null) {
            throw new IllegalArgumentException("O ID da submissão é obrigatório");
        }

        URI endpoint = UriComponentsBuilder.fromUri(baseUri)
                .path("/flows/{slug}/submission")
                .buildAndExpand(slug.trim())
                .toUri();

        HttpEntity<Map<String, Object>> entity = buildSubmissionPayload(
                slug,
                submissionId,
                submittedAt,
                campaignCode,
                contactName,
                contactEmail,
                contactPhone);
        return sendTrackingRequest(endpoint, entity, "submission", slug);
    }


    /**
     * Encaminha eventos de visualização da landing standalone para o Marketing Hub.
     */
    public TrackingResult registerPageAnalytics(String slug, Map<String, Object> payload) {
        if (!StringUtils.hasText(slug)) {
            throw new IllegalArgumentException("O slug do fluxo é obrigatório");
        }

        URI endpoint = UriComponentsBuilder.fromUri(baseUri)
                .path("/flows/{slug}/page-analytics")
                .buildAndExpand(slug.trim())
                .toUri();

        HttpEntity<Map<String, Object>> entity = buildPageAnalyticsPayload(payload);
        return sendTrackingRequest(endpoint, entity, "page-analytics", slug);
    }

    /**
     * Envia uma requisição de tracking e traduz o resultado HTTP em status operacional.
     */
    private TrackingResult sendTrackingRequest(URI endpoint, HttpEntity<?> entity, String action, String slug) {
        log.info("Enviando evento de tracking ao Marketing Hub. action={}, slug={}, endpoint={}, payload={}",
                action, slug, endpoint, entity.getBody());
        try {
            ResponseEntity<Void> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Evento de tracking aceito pelo Marketing Hub. action={}, slug={}, status={}",
                        action, slug, response.getStatusCode());
                return TrackingResult.FORWARDED;
            }
            log.warn("Marketing Hub retornou status {} ao registrar {} do fluxo {}. endpoint={}, payload={}",
                    response.getStatusCode(), action, slug, endpoint, entity.getBody());
            return TrackingResult.FAILED;
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                log.info("Ignorando {} para fluxo '{}' (status {} retornado pelo Marketing Hub). endpoint={}, responseBody={}, payload={}",
                        action, slug, ex.getStatusCode(), endpoint, ex.getResponseBodyAsString(), entity.getBody(), ex);
                return TrackingResult.SKIPPED;
            }
            log.warn("Falha ao registrar {} do fluxo '{}' no Marketing Hub (status {}). endpoint={}, responseBody={}, payload={}",
                    action, slug, ex.getStatusCode(), endpoint, ex.getResponseBodyAsString(), entity.getBody(), ex);
            return TrackingResult.FAILED;
        } catch (RestClientException ex) {
            log.warn("Erro de comunicação ao registrar {} do fluxo '{}'. endpoint={}, payload={}", action, slug, endpoint, entity.getBody(), ex);
            return TrackingResult.FAILED;
        }
    }

    /**
     * Monta o payload JSON de renderização completa.
     */
    private HttpEntity<Map<String, String>> buildRenderPayload(String visitorId, String campaignCode) {
        String sanitizedVisitor = visitorId != null ? visitorId.trim() : null;
        String sanitizedCampaign = campaignCode != null ? campaignCode.trim() : null;
        Map<String, String> body = new HashMap<>();
        if (StringUtils.hasText(sanitizedVisitor)) {
            body.put("visitorId", sanitizedVisitor);
        }
        if (StringUtils.hasText(sanitizedCampaign)) {
            body.put("campaignCode", sanitizedCampaign);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    /**
     * Monta o payload JSON do contrato de submissão do Lead Portal.
     */
    private HttpEntity<Map<String, Object>> buildSubmissionPayload(
            String slug,
            UUID submissionId,
            Instant submittedAt,
            String campaignCode,
            String contactName,
            String contactEmail,
            String contactPhone) {
        Map<String, Object> body = new HashMap<>();
        body.put("contractVersion", SUBMISSION_CONTRACT_VERSION);
        body.put("slug", slug.trim());
        body.put("submissionId", submissionId.toString());
        body.put("submittedAt", (submittedAt != null ? submittedAt : Instant.now()).toString());
        body.put("idempotencyKey", submissionId.toString());
        Map<String, String> contato = new HashMap<>();
        contato.put("nome", contactName);
        contato.put("email", contactEmail);
        if (contactPhone != null && !contactPhone.trim().isEmpty()) {
            contato.put("telefone", contactPhone.trim());
        }
        body.put("contato", contato);
        if (campaignCode != null && !campaignCode.trim().isEmpty()) {
            body.put("campaignCode", campaignCode.trim());
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
    /**
     * Monta o payload JSON de analytics preservando somente o corpo recebido.
     */
    private HttpEntity<Map<String, Object>> buildPageAnalyticsPayload(Map<String, Object> payload) {
        Map<String, Object> body = payload == null ? new HashMap<>() : new HashMap<>(payload);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
