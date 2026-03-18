package com.marketinghub.leadportal.controller;

import com.marketinghub.leadportal.dto.RegisterRenderCompleteRequest;
import com.marketinghub.leadportal.service.ExperimentFunnelTrackingClient;
import com.marketinghub.leadportal.service.ExperimentFunnelTrackingClient.TrackingResult;
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

    private final ExperimentFunnelTrackingClient trackingClient;

    public FlowEngagementController(ExperimentFunnelTrackingClient trackingClient) {
        this.trackingClient = trackingClient;
    }

    @PostMapping("/{slug}/render-complete")
    public ResponseEntity<Void> registerRenderComplete(
            @PathVariable String slug,
            @RequestBody(required = false) RegisterRenderCompleteRequest request) {
        String visitorId = request != null ? request.visitorId() : null;
        TrackingResult result = trackingClient.registerRenderComplete(slug, visitorId);
        return switch (result) {
            case FORWARDED -> ResponseEntity.accepted().build();
            case SKIPPED -> ResponseEntity.noContent().build();
            case FAILED -> ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        };
    }
}
