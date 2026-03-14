package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.email.LeadPortalEmailTemplatePlaceholder;
import com.marketinghub.settings.GeneralSettingKeys;
import com.marketinghub.settings.GeneralSettingService;
import com.marketinghub.settings.dto.GeneralSettingDto;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
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
        Optional<GeneralSettingDto> htmlSetting = generalSettingService
                .findByName(GeneralSettingKeys.LEAD_PORTAL_EMAIL_TEMPLATE_HTML);
        Optional<GeneralSettingDto> subjectSetting = generalSettingService
                .findByName(GeneralSettingKeys.LEAD_PORTAL_EMAIL_TEMPLATE_SUBJECT);
        if (htmlSetting.isEmpty() && subjectSetting.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toTemplate(subjectSetting.orElse(null), htmlSetting.orElse(null)));
    }

    public LeadPortalEmailTemplate saveTemplate(String subject, String html) {
        String sanitizedHtml = sanitizeHtml(html);
        String sanitizedSubject = sanitizeSubject(subject);
        GeneralSettingDto savedSubject = generalSettingService.upsert(
                GeneralSettingKeys.LEAD_PORTAL_EMAIL_TEMPLATE_SUBJECT,
                sanitizedSubject);
        GeneralSettingDto savedHtml = generalSettingService.upsert(
                GeneralSettingKeys.LEAD_PORTAL_EMAIL_TEMPLATE_HTML,
                sanitizedHtml);
        return toTemplate(savedSubject, savedHtml);
    }

    public List<LeadPortalEmailTemplatePlaceholder> getPlaceholders() {
        return Arrays.asList(LeadPortalEmailTemplatePlaceholder.values());
    }

    private LeadPortalEmailTemplate toTemplate(GeneralSettingDto subjectDto, GeneralSettingDto htmlDto) {
        Instant updatedAt = Stream.of(subjectDto, htmlDto)
                .filter(Objects::nonNull)
                .map(GeneralSettingDto::updatedAt)
                .filter(Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);
        return new LeadPortalEmailTemplate(
                subjectDto != null ? subjectDto.value() : null,
                htmlDto != null ? htmlDto.value() : null,
                updatedAt);
    }

    private String sanitizeHtml(String html) {
        if (!StringUtils.hasText(html)) {
            return null;
        }
        return html.trim();
    }

    private String sanitizeSubject(String subject) {
        if (!StringUtils.hasText(subject)) {
            return null;
        }
        String normalized = subject.replaceAll("[\\r\\n]+", " ").trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public record LeadPortalEmailTemplate(String subject, String html, Instant updatedAt) {
    }
}
