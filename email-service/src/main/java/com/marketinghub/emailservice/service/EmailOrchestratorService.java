package com.marketinghub.emailservice.service;

import com.marketinghub.emailservice.config.EmailServiceProperties;
import com.marketinghub.emailservice.dto.EmailAttachmentRequest;
import com.marketinghub.emailservice.dto.EmailRequestDto;
import com.marketinghub.emailservice.dto.EmailResponseDto;
import com.marketinghub.emailservice.exception.EmailServiceException;
import com.marketinghub.emailservice.model.EmailLog;
import com.marketinghub.emailservice.service.client.CloudflareImageClient;
import com.marketinghub.emailservice.service.client.MarketingHubClient;
import com.marketinghub.emailservice.service.client.MarketingHubTemplateResponse;
import com.marketinghub.emailservice.service.client.RemoteAsset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class EmailOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(EmailOrchestratorService.class);

    private final MarketingHubClient marketingHubClient;
    private final CloudflareImageClient cloudflareImageClient;
    private final TemplateRenderingService templateRenderingService;
    private final EmailSenderService emailSenderService;
    private final EmailLogService emailLogService;
    private final EmailServiceProperties emailServiceProperties;
    private final TrackingPixelService trackingPixelService;

    public EmailOrchestratorService(MarketingHubClient marketingHubClient,
                                    CloudflareImageClient cloudflareImageClient,
                                    TemplateRenderingService templateRenderingService,
                                    EmailSenderService emailSenderService,
                                    EmailLogService emailLogService,
                                    EmailServiceProperties emailServiceProperties,
                                    TrackingPixelService trackingPixelService) {
        this.marketingHubClient = marketingHubClient;
        this.cloudflareImageClient = cloudflareImageClient;
        this.templateRenderingService = templateRenderingService;
        this.emailSenderService = emailSenderService;
        this.emailLogService = emailLogService;
        this.emailServiceProperties = emailServiceProperties;
        this.trackingPixelService = trackingPixelService;
    }

    public EmailResponseDto sendEmail(EmailRequestDto request) {
        validateRequest(request);
        EmailLog emailLog = emailLogService.createPendingLog(String.join(",", request.to()), request.subject(), request.templateId());

        try {
            MarketingHubTemplateResponse template = marketingHubClient.fetchTemplate(request.templateId());
            Map<String, Object> templateVariables = marketingHubClient.fetchDynamicVariables(request.templateId());

            Map<String, Object> mergedVariables = new HashMap<>(templateVariables != null ? templateVariables : Map.of());
            mergedVariables.putIfAbsent("requestId", emailLog.getRequestId());

            String trackingPixelUrl = trackingPixelService.buildTrackingPixelUrl(emailLog.getRequestId());
            if (StringUtils.hasText(trackingPixelUrl)) {
                mergedVariables.putIfAbsent("trackingPixelUrl", trackingPixelUrl);
            }
            if (request.variables() != null) {
                mergedVariables.putAll(request.variables());
            }

            String htmlSource = template.htmlContent();
            if (htmlSource == null || htmlSource.isBlank()) {
                htmlSource = template.textContent();
            }
            String htmlBody = templateRenderingService.render(htmlSource, mergedVariables);
            htmlBody = trackingPixelService.appendTrackingPixel(htmlBody, trackingPixelUrl);
            String textBody = template.textContent() != null ? templateRenderingService.render(template.textContent(), mergedVariables) : null;

            List<EmailAttachmentResource> attachments = resolveAttachments(request.attachments());

            EmailMessage message = new EmailMessage(
                    emailServiceProperties.defaultFromAddress(),
                    request.to(),
                    defaultList(request.cc()),
                    defaultList(request.bcc()),
                    request.subject(),
                    htmlBody,
                    textBody,
                    attachments
            );

            emailSenderService.send(message);
            EmailLog sentLog = emailLogService.markSent(emailLog.getRequestId());
            return new EmailResponseDto(sentLog.getRequestId(), sentLog.getStatus(), sentLog.getCreatedAt(), sentLog.getSentAt(),
                    "E-mail enviado com sucesso");
        } catch (Exception ex) {
            log.error("Falha ao enviar e-mail para {}", request.to(), ex);
            emailLogService.markFailed(emailLog.getRequestId(), ex.getMessage());
            throw ex;
        }
    }

    public List<EmailResponseDto> sendBulk(List<EmailRequestDto> requests) {
        if (CollectionUtils.isEmpty(requests)) {
            return List.of();
        }
        return requests.stream()
                .map(this::sendEmail)
                .collect(Collectors.toList());
    }

    public EmailResponseDto getStatus(String requestId) {
        return emailLogService.findByRequestId(requestId)
                .map(log -> new EmailResponseDto(log.getRequestId(), log.getStatus(), log.getCreatedAt(), log.getSentAt(), log.getErrorMessage()))
                .orElseThrow(() -> new EmailServiceException("Registro não encontrado para requestId " + requestId));
    }

    private void validateRequest(EmailRequestDto request) {
        if (request.to() == null || request.to().isEmpty()) {
            throw new EmailServiceException("Informe ao menos um destinatário");
        }
    }

    private List<EmailAttachmentResource> resolveAttachments(List<EmailAttachmentRequest> attachments) {
        if (CollectionUtils.isEmpty(attachments)) {
            return List.of();
        }

        List<EmailAttachmentResource> resources = new ArrayList<>();
        for (EmailAttachmentRequest attachment : attachments) {
            RemoteAsset asset = cloudflareImageClient.fetchAsset(attachment);
            resources.add(new EmailAttachmentResource(asset, attachment.inline(), attachment.contentId()));
        }
        return resources;
    }

    private List<String> defaultList(List<String> list) {
        return CollectionUtils.isEmpty(list) ? List.of() : list.stream().filter(Objects::nonNull).toList();
    }
}
