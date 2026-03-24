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

@RestController
@RequestMapping("/api/flows")
public class FlowEngagementController {

    private static final Logger log = LoggerFactory.getLogger(FlowEngagementController.class);

    private final ExperimentFunnelTrackingClient trackingClient;
    private final ObjectMapper objectMapper;

    public FlowEngagementController(ExperimentFunnelTrackingClient trackingClient, ObjectMapper objectMapper) {
        this.trackingClient = trackingClient;
        this.objectMapper = objectMapper;
    }

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
        return switch (result) {
            case FORWARDED -> ResponseEntity.accepted().build();
            case SKIPPED -> ResponseEntity.noContent().build();
            case FAILED -> ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        };
    }

    private RegisterRenderCompleteRequest parseRenderCompleteRequest(String requestBody) {
        if (requestBody == null || requestBody.isBlank()) {
            return new RegisterRenderCompleteRequest(null, null);
        }

        try {
            RegisterRenderCompleteRequest request = objectMapper.readValue(requestBody, RegisterRenderCompleteRequest.class);
            return request == null ? new RegisterRenderCompleteRequest(null, null) : request;
        } catch (JsonProcessingException ex) {
            log.warn("Payload inválido em render-complete. requestBody será ignorado.");
            return new RegisterRenderCompleteRequest(null, null);
        }
    }
}
