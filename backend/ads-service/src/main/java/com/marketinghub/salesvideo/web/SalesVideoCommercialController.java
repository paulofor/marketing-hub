package com.marketinghub.salesvideo.web;

import com.marketinghub.salesvideo.dto.*;
import com.marketinghub.salesvideo.service.SalesVideoCommercialInsightsService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Endpoints comerciais da Sprint V7 para playbooks e leitura de conversão por perfil.
 */
@RestController
@RequestMapping("/api/sales-videos/profiles/{profileId}")
public class SalesVideoCommercialController {
    private final SalesVideoCommercialInsightsService commercialInsightsService;

    public SalesVideoCommercialController(SalesVideoCommercialInsightsService commercialInsightsService) {
        this.commercialInsightsService = commercialInsightsService;
    }

    @PostMapping("/commercial-playbooks")
    public SalesVideoCommercialPlaybookDto createPlaybook(@PathVariable Long profileId,
                                                          @Valid @RequestBody CreateSalesVideoCommercialPlaybookRequest request) {
        return commercialInsightsService.createPlaybook(profileId, request);
    }

    @GetMapping("/commercial-playbooks")
    public List<SalesVideoCommercialPlaybookDto> listPlaybooks(@PathVariable Long profileId) {
        return commercialInsightsService.listPlaybooks(profileId);
    }

    @PostMapping("/conversion-events")
    public SalesVideoConversionEventDto createConversionEvent(@PathVariable Long profileId,
                                                              @Valid @RequestBody CreateSalesVideoConversionEventRequest request) {
        return commercialInsightsService.createConversionEvent(profileId, request);
    }

    @GetMapping("/performance-summary")
    public SalesVideoPerformanceSummaryDto getPerformanceSummary(@PathVariable Long profileId,
                                                                 @RequestParam(required = false)
                                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                                 Instant from,
                                                                 @RequestParam(required = false)
                                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                                 Instant to) {
        return commercialInsightsService.summarizePerformance(profileId, from, to);
    }
}
