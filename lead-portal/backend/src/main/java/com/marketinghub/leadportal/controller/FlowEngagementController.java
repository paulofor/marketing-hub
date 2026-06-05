package com.marketinghub.leadportal.controller;

import com.marketinghub.leadportal.dto.RegisterRenderCompleteRequest;
import com.marketinghub.leadportal.service.ExperimentFunnelTrackingClient;
import com.marketinghub.leadportal.service.ExperimentFunnelTrackingClient.TrackingResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/**
 * Controller que recebe sinais públicos da landing e encaminha ao funil do Marketing Hub.
 */
@RestController
@RequestMapping("/api/flows")
public class FlowEngagementController {

    private static final Logger log = LoggerFactory.getLogger(FlowEngagementController.class);

    private final ExperimentFunnelTrackingClient trackingClient;
    private final ObjectMapper objectMapper;

    /**
     * Inicializa o controller com cliente de tracking e parser JSON.
     */
    public FlowEngagementController(ExperimentFunnelTrackingClient trackingClient, ObjectMapper objectMapper) {
        this.trackingClient = trackingClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Registra que o formulário do fluxo foi renderizado e encaminha o evento ao Marketing Hub.
     */
    @PostMapping("/{slug}/render-complete")
    public ResponseEntity<Void> registerRenderComplete(
            @PathVariable("slug") String slug,
            @RequestBody(required = false) String requestBody) {
        RegisterRenderCompleteRequest payload = parseRenderCompleteRequest(requestBody);
        String visitorId = payload.visitorId();
        String campaignCode = payload.campaignCode();
        log.info("Render-complete recebido. slug={}, visitorIdPresent={}, campaignCodePresent={}",
                slug, visitorId != null && !visitorId.isBlank(), campaignCode != null && !campaignCode.isBlank());

        TrackingResult result = trackingClient.registerRenderComplete(slug, visitorId, campaignCode);
        return toResponse(result);
    }


    /**
     * Registra analytics da landing standalone e encaminha o evento ao Marketing Hub.
     */
    @PostMapping("/{slug}/page-analytics")
    public ResponseEntity<Void> registerPageAnalytics(
            @PathVariable("slug") String slug,
            @RequestBody(required = false) String requestBody) {
        log.info("Page-analytics raw recebido no Lead Portal. slug={}, rawPayload={}", slug, requestBody);
        Map<String, Object> payload = parsePageAnalyticsRequest(slug, requestBody);
        Object eventType = payload == null ? null : payload.get("eventType");
        Object eventId = payload == null ? null : payload.get("eventId");
        log.info("Page-analytics parseado no Lead Portal. slug={}, eventType={}, eventIdPresent={}",
                slug, eventType, eventId != null && !eventId.toString().isBlank());

        TrackingResult result = trackingClient.registerPageAnalytics(slug, payload);
        log.info("Page-analytics encaminhado ao Marketing Hub. slug={}, eventType={}, eventId={}, result={}",
                slug, eventType, eventId, result);
        return toResponse(result);
    }

    /**
     * Faz o parse tolerante do payload de analytics preservando log do corpo cru recebido.
     */
    private Map<String, Object> parsePageAnalyticsRequest(String slug, String requestBody) {
        if (requestBody == null || requestBody.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(requestBody, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            log.warn("Payload inválido em page-analytics. slug={}, rawPayload={}", slug, requestBody, ex);
            return Map.of();
        }
    }

    /**
     * Converte o resultado do cliente de tracking para a resposta HTTP pública do Lead Portal.
     */
    private ResponseEntity<Void> toResponse(TrackingResult result) {
        return switch (result) {
            case FORWARDED -> ResponseEntity.accepted().build();
            case SKIPPED -> ResponseEntity.noContent().build();
            case FAILED -> ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        };
    }

    /**
     * Faz o parse tolerante do payload legado de render-complete.
     */
    private RegisterRenderCompleteRequest parseRenderCompleteRequest(String requestBody) {
        if (requestBody == null || requestBody.isBlank()) {
            return new RegisterRenderCompleteRequest(null, null);
        }

        try {
            RegisterRenderCompleteRequest request = objectMapper.readValue(requestBody, RegisterRenderCompleteRequest.class);
            return request == null ? new RegisterRenderCompleteRequest(null, null) : request;
        } catch (JsonProcessingException ex) {
            log.warn("Payload inválido em render-complete. requestBody será ignorado.", ex);
            return new RegisterRenderCompleteRequest(null, null);
        }
    }
}
