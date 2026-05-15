package com.marketinghub.leadportal.web;

import com.marketinghub.experiment.funnel.ExperimentFunnelService;
import com.marketinghub.leadportal.dto.RegisterLandingPageAnalyticsEventRequest;
import com.marketinghub.leadportal.dto.LeadPortalSubmissionEngagementContractV1;
import com.marketinghub.leadportal.dto.RegisterLeadPortalRenderCompleteRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
        String campaignCode = request == null ? null : request.campaignCode();
        experimentFunnelService.registerFormRenderCompleted(slug, visitorId, campaignCode);
    }

    @PostMapping("/{slug}/submission")
    public ResponseEntity<LeadPortalEngagementAckResponse> registerSubmission(@PathVariable String slug,
                                                                               @RequestBody @Valid LeadPortalSubmissionEngagementContractV1 request) {
        if (!slug.equals(request.slug())) {
            throw new IllegalArgumentException("Slug da rota diverge do payload de submissão");
        }
        boolean accepted = experimentFunnelService.registerFormSubmission(slug, request);
        if (!accepted) {
            return ResponseEntity.ok(new LeadPortalEngagementAckResponse(
                    LeadPortalSubmissionEngagementContractV1.VERSION,
                    slug,
                    request.submissionId(),
                    "duplicate",
                    "Evento de submissão já recebido anteriormente (idempotente)."));
        }
        return ResponseEntity.ok(new LeadPortalEngagementAckResponse(
                LeadPortalSubmissionEngagementContractV1.VERSION,
                slug,
                request.submissionId(),
                "accepted",
                "Evento de submissão registrado com sucesso."));
    }

    public record LeadPortalEngagementAckResponse(
            String contractVersion,
            String slug,
            String submissionId,
            String status,
            String message) {
    }

    @PostMapping("/{slug}/page-analytics")
    public ResponseEntity<LeadPortalEngagementAckResponse> registerPageAnalytics(
            @PathVariable String slug,
            @RequestBody @Valid RegisterLandingPageAnalyticsEventRequest request) {
        experimentFunnelService.registerLandingPageAnalyticsEvent(slug, request);
        return ResponseEntity.ok(new LeadPortalEngagementAckResponse(
                LeadPortalSubmissionEngagementContractV1.VERSION,
                slug,
                request.eventId(),
                "accepted",
                "Evento de analytics da landing registrado com sucesso."));
    }

}
