package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.email.LeadPortalEmailTemplatePlaceholder;
import com.marketinghub.settings.GeneralSettingKeys;
import com.marketinghub.settings.GeneralSettingService;
import com.marketinghub.settings.dto.GeneralSettingDto;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Mantém o template HTML utilizado no envio do e-mail de amostras do Lead Portal.
 */
@Service
public class LeadPortalEmailTemplateService {

    private final GeneralSettingService generalSettingService;

    public LeadPortalEmailTemplateService(GeneralSettingService generalSettingService) {
        this.generalSettingService = generalSettingService;
    }

    public Optional<LeadPortalEmailTemplate> findTemplate() {
        return generalSettingService.findByName(GeneralSettingKeys.LEAD_PORTAL_EMAIL_TEMPLATE_HTML)
                .map(this::toTemplate);
    }

    public LeadPortalEmailTemplate saveTemplate(String html) {
        String sanitized = sanitizeHtml(html);
        GeneralSettingDto saved = generalSettingService.upsert(
                GeneralSettingKeys.LEAD_PORTAL_EMAIL_TEMPLATE_HTML,
                sanitized);
        return toTemplate(saved);
    }

    public List<LeadPortalEmailTemplatePlaceholder> getPlaceholders() {
        return Arrays.asList(LeadPortalEmailTemplatePlaceholder.values());
    }

    private LeadPortalEmailTemplate toTemplate(GeneralSettingDto dto) {
        return new LeadPortalEmailTemplate(dto.value(), dto.updatedAt());
    }

    private String sanitizeHtml(String html) {
        if (!StringUtils.hasText(html)) {
            return null;
        }
        return html.trim();
    }

    public record LeadPortalEmailTemplate(String html, Instant updatedAt) {
    }
}
