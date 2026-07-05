package com.marketinghub.leadportal.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.funnel.ExperimentFunnelService;
import com.marketinghub.leadportal.dto.RegisterLandingPageAnalyticsEventRequest;
import com.marketinghub.leadportal.dto.LeadPortalSubmissionEngagementContractV1;
import com.marketinghub.leadportal.dto.RegisterLeadPortalRenderCompleteRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints internos para receber sinais de engajamento enviados pelo backend do Lead Portal.
 */
@RestController
@RequestMapping("/api/internal/lead-portal/flows")
public class LeadPortalFlowEngagementController {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalFlowEngagementController.class);

    private final ExperimentFunnelService experimentFunnelService;
    private final ObjectMapper objectMapper;

    /**
     * Inicializa o controller com o serviço de funil e parser JSON para logs de payload cru.
     */
    public LeadPortalFlowEngagementController(
            ExperimentFunnelService experimentFunnelService,
            ObjectMapper objectMapper) {
        this.experimentFunnelService = experimentFunnelService;
        this.objectMapper = objectMapper;
    }

    /**
     * Registra que o formulário público renderizou e encaminha o sinal ao funil do experimento.
     */
    @PostMapping("/{slug}/render-complete")
    public void registerRenderComplete(@PathVariable String slug,
                                       @RequestBody(required = false) RegisterLeadPortalRenderCompleteRequest request) {
        String visitorId = request == null ? null : request.visitorId();
        String campaignCode = request == null ? null : request.campaignCode();
        log.info("Lead Portal render-complete recebido no backend. slug={}, visitorIdPresent={}, campaignCodePresent={}",
                slug, visitorId != null && !visitorId.isBlank(), campaignCode != null && !campaignCode.isBlank());
        experimentFunnelService.registerFormRenderCompleted(slug, visitorId, campaignCode);
    }

    /**
     * Registra submissão pública do formulário e retorna confirmação idempotente ao Lead Portal.
     */
    @PostMapping("/{slug}/submission")
    public ResponseEntity<LeadPortalEngagementAckResponse> registerSubmission(@PathVariable String slug,
                                                                               @RequestBody @Valid LeadPortalSubmissionEngagementContractV1 request) {
        if (!slug.equals(request.slug())) {
            throw new IllegalArgumentException("Slug da rota diverge do payload de submissão");
        }
        log.info("Lead Portal submission recebido no backend. slug={}, submissionId={}, contractVersion={}",
                slug, request.submissionId(), request.contractVersion());
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

    /**
     * Resposta pública que confirma o status de processamento do evento de engajamento.
     */
    public record LeadPortalEngagementAckResponse(
            String contractVersion,
            String slug,
            String submissionId,
            String status,
            String message) {
    }

    /**
     * Registra evento de analytics emitido pelos scripts da landing e loga o payload cru recebido.
     */
    @PostMapping("/{slug}/page-analytics")
    public ResponseEntity<LeadPortalEngagementAckResponse> registerPageAnalytics(
            @PathVariable String slug,
            @RequestBody(required = false) String requestBody) {
        log.info("Lead Portal page-analytics raw recebido no backend. slug={}, rawPayload={}", slug, requestBody);
        RegisterLandingPageAnalyticsEventRequest request = parsePageAnalyticsRequest(slug, requestBody);
        log.info("Lead Portal page-analytics parseado no backend. slug={}, eventId={}, visitorId={}, eventType={}, sectionId={}, sessionId={}, pageUrl={}, deviceType={}",
                slug, request.eventId(), request.visitorId(), request.eventType(), request.sectionId(), request.sessionId(),
                request.pageUrl(), request.deviceType());
        experimentFunnelService.registerLandingPageAnalyticsEvent(slug, request);
        return ResponseEntity.ok(new LeadPortalEngagementAckResponse(
                LeadPortalSubmissionEngagementContractV1.VERSION,
                slug,
                request.eventId(),
                "accepted",
                "Evento de analytics da landing registrado com sucesso."));
    }

    /**
     * Converte o corpo cru de analytics para o contrato do backend preservando logs em caso de payload inválido.
     */
    private RegisterLandingPageAnalyticsEventRequest parsePageAnalyticsRequest(String slug, String requestBody) {
        if (requestBody == null || requestBody.isBlank()) {
            throw new IllegalArgumentException("Payload de analytics da landing é obrigatório");
        }
        try {
            return objectMapper.readValue(requestBody, RegisterLandingPageAnalyticsEventRequest.class);
        } catch (JsonProcessingException ex) {
            log.warn("Payload inválido em Lead Portal page-analytics. slug={}, rawPayload={}", slug, requestBody, ex);
            throw new IllegalArgumentException("Payload de analytics da landing é inválido", ex);
        }
    }

}
