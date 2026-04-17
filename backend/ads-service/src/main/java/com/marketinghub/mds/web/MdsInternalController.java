package com.marketinghub.mds.web;

import com.marketinghub.mds.dto.*;
import com.marketinghub.mds.service.MdsArtifactService;
import com.marketinghub.mds.service.MdsRequestService;
import com.marketinghub.mds.service.MdsSourceAccessService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/internal/mds")
public class MdsInternalController {
    private final MdsRequestService requestService;
    private final MdsArtifactService artifactService;
    private final MdsSourceAccessService sourceAccessService;

    public MdsInternalController(MdsRequestService requestService,
                                 MdsArtifactService artifactService,
                                 MdsSourceAccessService sourceAccessService) {
        this.requestService = requestService;
        this.artifactService = artifactService;
        this.sourceAccessService = sourceAccessService;
    }

    @PostMapping("/requests")
    public MdsRequestStatusResponse createRequest(@Valid @RequestBody MdsRequestCreateRequest request) {
        return requestService.createRequest(request);
    }

    @GetMapping("/requests/pending")
    public List<MdsRequestStatusResponse> pending() {
        return requestService.pendingRequests();
    }

    @PostMapping("/requests/{id}/claim")
    public MdsRequestStatusResponse claim(@PathVariable Long id,
                                          @Valid @RequestBody MdsClaimRequest request) {
        return requestService.claim(id, request);
    }

    @PostMapping("/requests/{id}/heartbeat")
    public MdsRequestStatusResponse heartbeat(@PathVariable Long id,
                                              @RequestBody MdsHeartbeatRequest request) {
        return requestService.heartbeat(id, request);
    }

    @PostMapping("/requests/{id}/complete")
    public MdsRequestStatusResponse complete(@PathVariable Long id,
                                             @RequestBody MdsCompleteRequest request) {
        return requestService.complete(id, request);
    }

    @PostMapping("/requests/{id}/fail")
    public MdsRequestStatusResponse fail(@PathVariable Long id,
                                         @Valid @RequestBody MdsFailRequest request) {
        return requestService.fail(id, request);
    }

    @GetMapping("/requests/{id}")
    public MdsRequestStatusResponse getRequest(@PathVariable Long id) {
        return requestService.getById(id);
    }

    @PostMapping("/artifacts/publish-batch")
    public MdsArtifactPublishBatchResponse publishBatch(@Valid @RequestBody MdsArtifactPublishBatchRequest request) {
        return artifactService.publishBatch(request);
    }

    @PostMapping("/source-access/publish-batch")
    public MdsSourceAccessPublishBatchResponse publishSourceAccessBatch(
            @Valid @RequestBody MdsSourceAccessPublishBatchRequest request
    ) {
        return sourceAccessService.publishBatch(request);
    }

    @PostMapping("/artifacts/{id}/lineage")
    public MdsLineageResponse createLineage(@PathVariable Long id,
                                            @Valid @RequestBody MdsLineageCreateRequest request) {
        if (!id.equals(request.childArtifactId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                    "path artifact id must match childArtifactId"
            );
        }
        return artifactService.createLineage(request);
    }

    @GetMapping("/requests/{id}/recommended-mechanism")
    public MdsRecommendedMechanismResponse getRecommendedMechanism(@PathVariable Long id) {
        return artifactService.getRecommendedMechanismByRequest(id);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "module", "mds-backend-orchestration");
    }
}
