package com.marketinghub.leadportal.web;

import com.marketinghub.leadportal.dto.LeadPortalEmailTemplateDto;
import com.marketinghub.leadportal.dto.LeadPortalEmailTemplatePlaceholderDto;
import com.marketinghub.leadportal.dto.UpdateLeadPortalEmailTemplateRequest;
import com.marketinghub.leadportal.email.LeadPortalEmailTemplatePlaceholder;
import com.marketinghub.leadportal.service.LeadPortalEmailTemplateService;
import com.marketinghub.leadportal.service.LeadPortalEmailTemplateService.LeadPortalEmailTemplate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lead-portal/email-template")
public class LeadPortalEmailTemplateController {

    private final LeadPortalEmailTemplateService templateService;

    public LeadPortalEmailTemplateController(LeadPortalEmailTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public LeadPortalEmailTemplateDto getTemplate() {
        return toDto(templateService.findTemplate().orElse(null));
    }

    @PutMapping
    public LeadPortalEmailTemplateDto saveTemplate(@RequestBody(required = false) UpdateLeadPortalEmailTemplateRequest request) {
        String subject = request != null ? request.subject() : null;
        String html = request != null ? request.html() : null;
        LeadPortalEmailTemplate saved = templateService.saveTemplate(subject, html);
        return toDto(saved);
    }

    private LeadPortalEmailTemplateDto toDto(LeadPortalEmailTemplate template) {
        return new LeadPortalEmailTemplateDto(
                template != null ? template.subject() : null,
                template != null ? template.html() : null,
                template != null ? template.updatedAt() : null,
                buildPlaceholderDtos());
    }

    private List<LeadPortalEmailTemplatePlaceholderDto> buildPlaceholderDtos() {
        return templateService.getPlaceholders().stream()
                .map(this::toPlaceholderDto)
                .toList();
    }

    private LeadPortalEmailTemplatePlaceholderDto toPlaceholderDto(LeadPortalEmailTemplatePlaceholder placeholder) {
        return new LeadPortalEmailTemplatePlaceholderDto(
                placeholder.key(),
                placeholder.token(),
                placeholder.label(),
                placeholder.description());
    }
}
