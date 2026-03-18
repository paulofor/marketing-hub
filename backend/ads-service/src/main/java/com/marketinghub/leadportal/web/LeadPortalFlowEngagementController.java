package com.marketinghub.leadportal.web;

import com.marketinghub.experiment.funnel.ExperimentFunnelService;
import com.marketinghub.leadportal.dto.RegisterLeadPortalRenderCompleteRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos para registrar sinais de engajamento do formulário no Lead Portal.
 */
@RestController
@RequestMapping("/api/public/lead-portal/flows")
public class LeadPortalFlowEngagementController {

    private final ExperimentFunnelService experimentFunnelService;

    public LeadPortalFlowEngagementController(ExperimentFunnelService experimentFunnelService) {
        this.experimentFunnelService = experimentFunnelService;
    }

    @PostMapping("/{slug}/render-complete")
    public void registerRenderComplete(@PathVariable String slug,
                                       @RequestBody(required = false) RegisterLeadPortalRenderCompleteRequest request) {
        String visitorId = request == null ? null : request.visitorId();
        experimentFunnelService.registerFormRenderCompleted(slug, visitorId);
    }
}
