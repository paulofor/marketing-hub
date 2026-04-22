package com.marketinghub.mois.web;

import com.marketinghub.mois.dto.MoisArtifactDtos;
import com.marketinghub.mois.dto.MoisDiscoveryDtos;
import com.marketinghub.mois.dto.MoisInsightDtos;
import com.marketinghub.mois.dto.MoisOfferDtos;
import com.marketinghub.mois.service.MoisApiStubService;
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

    private final MoisApiStubService service;

    @PostMapping("/discovery-requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse createDiscoveryRequest(
            @Valid @RequestBody MoisDiscoveryDtos.CreateDiscoveryRequest request
    ) {
        return service.createDiscoveryRequest(request);
    }

    @GetMapping("/discovery-requests")
    public MoisDiscoveryDtos.DiscoveryRequestListResponse listDiscoveryRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String nicheName,
            @RequestParam(required = false) String marketTheme
    ) {
        return service.listDiscoveryRequests(status, nicheName, marketTheme);
    }

    @GetMapping("/discovery-requests/{requestId}")
    public MoisDiscoveryDtos.DiscoveryRequestDetailResponse getDiscoveryRequest(@PathVariable String requestId) {
        return service.getDiscoveryRequest(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "discovery request not found"));
    }

    @PostMapping("/discovery-requests/{requestId}/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MoisDiscoveryDtos.AsyncAcceptedResponse runDiscoveryRequest(@PathVariable String requestId) {
        return service.runDiscoveryRequest(requestId);
    }

    @GetMapping("/offers")
    public MoisOfferDtos.OfferCardListResponse listOffers(
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String nicheName,
            @RequestParam(required = false) String sellerOrBrand
    ) {
        return service.listOffers(requestId, nicheName, sellerOrBrand);
    }

    @GetMapping("/offers/{offerId}")
    public MoisOfferDtos.OfferCardResponse getOffer(@PathVariable String offerId) {
        return service.getOffer(offerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "offer not found"));
    }

    @GetMapping("/insight-reports")
    public MoisInsightDtos.InsightReportListResponse listInsightReports(
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String nicheName
    ) {
        return service.listInsightReports(requestId, nicheName);
    }

    @GetMapping("/insight-reports/{reportId}")
    public MoisInsightDtos.InsightReportResponse getInsightReport(@PathVariable String reportId) {
        return service.getInsightReport(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "insight report not found"));
    }

    @GetMapping("/artifacts/{artifactId}")
    public MoisArtifactDtos.ArtifactEnvelopeResponse getArtifact(@PathVariable String artifactId) {
        return service.getArtifact(artifactId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "artifact not found"));
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "module", "mois-backend");
    }
}
