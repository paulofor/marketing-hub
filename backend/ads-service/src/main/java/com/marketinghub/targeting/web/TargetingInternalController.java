package com.marketinghub.targeting.web;

import com.marketinghub.targeting.TargetingRequest;
import com.marketinghub.targeting.dto.TargetingCandidateIngestionRequest;
import com.marketinghub.targeting.dto.TargetingCandidateResolutionUpdateRequest;
import com.marketinghub.targeting.dto.TargetingRequestDto;
import com.marketinghub.targeting.dto.TargetingMetaAdsPendingElementDto;
import com.marketinghub.targeting.dto.UpdateTargetingMetaAdsDataRequest;
import com.marketinghub.targeting.mapper.TargetingRequestMapper;
import com.marketinghub.targeting.service.TargetingRequestService;
import com.marketinghub.targeting.service.TargetingMetaAdsSyncService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/internal/targeting")
public class TargetingInternalController {
    private final TargetingRequestService service;
    private final TargetingRequestMapper mapper;
    private final TargetingMetaAdsSyncService metaAdsSyncService;

    public TargetingInternalController(TargetingRequestService service,
                                       TargetingRequestMapper mapper,
                                       TargetingMetaAdsSyncService metaAdsSyncService) {
        this.service = service;
        this.mapper = mapper;
        this.metaAdsSyncService = metaAdsSyncService;
    }

    @GetMapping("/requests/pending")
    public List<TargetingRequestDto> listPending(@RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        List<TargetingRequest> pending = service.listPendingForAi(limit != null ? limit : 10);
        return pending.stream().map(r -> mapper.toDto(r, service.etaSeconds())).toList();
    }

    @PostMapping("/{requestId}/candidates")
    public void saveCandidates(@PathVariable UUID requestId,
                               @RequestBody TargetingCandidateIngestionRequest payload) {
        service.saveCandidates(requestId, payload);
    }

    @PatchMapping("/candidates/{candidateId}")
    public void updateCandidate(@PathVariable Long candidateId,
                                @RequestBody TargetingCandidateResolutionUpdateRequest payload) {
        service.applyResolution(candidateId, payload);
    }

    @GetMapping("/elements/metaads-pending")
    public List<TargetingMetaAdsPendingElementDto> listMetaAdsPending(@RequestParam(value = "limit", defaultValue = "50") Integer limit) {
        return metaAdsSyncService.listPending(limit != null ? limit : 50);
    }

    @PatchMapping("/elements/{id}/metaads")
    public void updateMetaAdsData(@PathVariable Long id,
                                  @RequestBody UpdateTargetingMetaAdsDataRequest request) {
        metaAdsSyncService.updateMetaAdsData(id, request);
    }

}
