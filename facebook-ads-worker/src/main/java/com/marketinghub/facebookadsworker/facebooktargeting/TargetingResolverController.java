package com.marketinghub.facebookadsworker.facebooktargeting;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpoint interno acionado pelo backend para resolver os candidatos de targeting.
 */
@RestController
@RequestMapping("/internal/targeting")
public class TargetingResolverController {
    private final TargetingResolverService service;

    public TargetingResolverController(TargetingResolverService service) {
        this.service = service;
    }

    @PostMapping("/{requestId}/resolve")
    public ResponseEntity<TargetingResolutionResponse> resolve(@PathVariable UUID requestId,
                                                               @RequestBody TargetingResolutionRequest request) {
        TargetingResolutionResponse response = service.resolve(requestId, request, null);
        return ResponseEntity.ok(response);
    }
}
