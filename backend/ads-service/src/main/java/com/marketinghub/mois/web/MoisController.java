package com.marketinghub.mois.web;

import com.marketinghub.mois.dto.MoisArtifactDtos;
import com.marketinghub.mois.dto.MoisDiscoveryDtos;
import com.marketinghub.mois.dto.MoisInsightDtos;
import com.marketinghub.mois.dto.MoisOfferDtos;
import com.marketinghub.mois.dto.MoisWorkspaceDtos;
import com.marketinghub.mois.service.MoisModuleGateway;
import com.marketinghub.mois.service.MoisSprintOneService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/mois")
@RequiredArgsConstructor
public class MoisController {

    private final MoisModuleGateway gateway;
    private final MoisSprintOneService sprintOneService;

    @PostMapping("/discovery-requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse createDiscoveryRequest(
            @Valid @RequestBody MoisDiscoveryDtos.CreateDiscoveryRequest request
    ) {
        return gateway.createDiscoveryRequest(request);
    }

    @GetMapping("/discovery-requests")
    public MoisDiscoveryDtos.DiscoveryRequestListResponse listDiscoveryRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String nicheName,
            @RequestParam(required = false) String marketTheme
    ) {
        return gateway.listDiscoveryRequests(status, nicheName, marketTheme);
    }

    @GetMapping("/discovery-requests/{requestId}")
    public MoisDiscoveryDtos.DiscoveryRequestDetailResponse getDiscoveryRequest(@PathVariable String requestId) {
        return gateway.getDiscoveryRequest(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "discovery request not found"));
    }

    @PostMapping("/discovery-requests/{requestId}/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MoisDiscoveryDtos.AsyncAcceptedResponse runDiscoveryRequest(@PathVariable String requestId) {
        return gateway.runDiscoveryRequest(requestId);
    }


    @GetMapping("/workspaces/{workspaceId}/dashboard")
    public MoisWorkspaceDtos.WorkspaceDashboardResponse getWorkspaceDashboard(@PathVariable String workspaceId) {
        return sprintOneService.getDashboard(workspaceId);
    }

    @PostMapping("/references")
    @ResponseStatus(HttpStatus.CREATED)
    public MoisWorkspaceDtos.ReferenceResponse createReference(
            @Valid @RequestBody MoisWorkspaceDtos.CreateReferenceRequest request
    ) {
        return sprintOneService.createReference(request);
    }

    @GetMapping("/references")
    public MoisWorkspaceDtos.ReferenceListResponse listReferences(@RequestParam String workspaceId) {
        return sprintOneService.listReferences(workspaceId);
    }

    @PostMapping("/references/{referenceId}/extractions")
    public MoisWorkspaceDtos.ExtractionDraftResponse upsertExtractionDraft(
            @PathVariable String referenceId,
            @RequestBody MoisWorkspaceDtos.UpsertExtractionDraftRequest request
    ) {
        try {
            return sprintOneService.upsertExtractionDraft(referenceId, request);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @GetMapping("/library/blocks")
    public MoisWorkspaceDtos.LibraryBlockListResponse listLibraryBlocks(
            @RequestParam(required = false) String workspaceId,
            @RequestParam(required = false) String niche,
            @RequestParam(required = false) String formatType
    ) {
        return sprintOneService.listLibraryBlocks(workspaceId, niche, formatType);
    }

    @PostMapping("/library/blocks/{blockId}/favorite")
    public MoisWorkspaceDtos.LibraryBlockActionResponse favoriteLibraryBlock(@PathVariable String blockId) {
        try {
            return sprintOneService.favoriteLibraryBlock(blockId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PostMapping("/library/blocks/{blockId}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public MoisWorkspaceDtos.LibraryBlockActionResponse duplicateLibraryBlock(@PathVariable String blockId) {
        try {
            return sprintOneService.duplicateLibraryBlock(blockId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PostMapping("/comparisons")
    public MoisWorkspaceDtos.ComparisonResponse createComparison(
            @Valid @RequestBody MoisWorkspaceDtos.CreateComparisonRequest request
    ) {
        return sprintOneService.createComparison(request);
    }

    @PostMapping("/offers/build")
    public MoisWorkspaceDtos.BuildOfferResponse buildOffer(@Valid @RequestBody MoisWorkspaceDtos.BuildOfferRequest request) {
        return sprintOneService.buildOffer(request);
    }

    @PostMapping("/collection-jobs")
    @ResponseStatus(HttpStatus.CREATED)
    public MoisWorkspaceDtos.CollectionJobResponse createCollectionJob(
            @Valid @RequestBody MoisWorkspaceDtos.CreateCollectionJobRequest request
    ) {
        return gateway.createCollectionJob(request);
    }

    @GetMapping("/collection-jobs")
    public MoisWorkspaceDtos.CollectionJobListResponse listCollectionJobs(
            @RequestParam(required = false) String workspaceId,
            @RequestParam(required = false) String status
    ) {
        return gateway.listCollectionJobs(workspaceId, status);
    }

    @GetMapping("/collection-jobs/{jobId}/references")
    public MoisWorkspaceDtos.CollectedReferenceListResponse listCollectedReferencesByJob(
            @PathVariable String jobId
    ) {
        return gateway.listCollectedReferencesByJob(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "collection job not found"));
    }

    @GetMapping("/offers")
    public MoisOfferDtos.OfferCardListResponse listOffers(
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String nicheName,
            @RequestParam(required = false) String sellerOrBrand
    ) {
        return gateway.listOffers(requestId, nicheName, sellerOrBrand);
    }

    @GetMapping("/offers/{offerId}")
    public MoisOfferDtos.OfferCardResponse getOffer(@PathVariable String offerId) {
        return gateway.getOffer(offerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "offer not found"));
    }

    @GetMapping("/insight-reports")
    public MoisInsightDtos.InsightReportListResponse listInsightReports(
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String nicheName,
            @RequestParam(required = false) String category
    ) {
        return gateway.listInsightReports(requestId, nicheName, category);
    }

    @GetMapping("/insight-reports/{reportId}")
    public MoisInsightDtos.InsightReportResponse getInsightReport(@PathVariable String reportId) {
        return gateway.getInsightReport(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "insight report not found"));
    }

    @GetMapping("/insight-reports/{reportId}/executive-summary")
    public MoisInsightDtos.InsightExecutiveSummaryResponse getInsightExecutiveSummary(@PathVariable String reportId) {
        return gateway.getInsightExecutiveSummary(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "insight report not found"));
    }

    @GetMapping("/artifacts/{artifactId}")
    public MoisArtifactDtos.ArtifactEnvelopeResponse getArtifact(@PathVariable String artifactId) {
        return gateway.getArtifact(artifactId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "artifact not found"));
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return gateway.health();
    }
}
