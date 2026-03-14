package com.marketinghub.emailservice.leadportal.service;

import com.marketinghub.emailservice.settings.GeneralSettingNames;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Mantém o template HTML utilizado no envio do e-mail de amostras do Lead Portal.
 */
@Service
public class LeadPortalEmailTemplateService {

    private final JdbcTemplate jdbcTemplate;

    public LeadPortalEmailTemplateService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<LeadPortalEmailTemplate> findTemplate() {
        TemplateSetting subjectSetting = findSetting(GeneralSettingNames.LEAD_PORTAL_EMAIL_TEMPLATE_SUBJECT, this::sanitizeSubject);
        TemplateSetting htmlSetting = findSetting(GeneralSettingNames.LEAD_PORTAL_EMAIL_TEMPLATE_HTML, this::sanitizeHtml);

        if (subjectSetting == null && htmlSetting == null) {
            return Optional.empty();
        }

        return Optional.of(new LeadPortalEmailTemplate(
                subjectSetting != null ? subjectSetting.value() : null,
                htmlSetting != null ? htmlSetting.value() : null,
                maxInstant(subjectSetting != null ? subjectSetting.updatedAt() : null, htmlSetting != null ? htmlSetting.updatedAt() : null)));
    }

    private TemplateSetting findSetting(String name, java.util.function.Function<String, String> sanitizer) {
        String sql = "SELECT setting_value, updated_at FROM general_setting WHERE name = ? LIMIT 1";
        return jdbcTemplate
                .query(sql, (rs, rowNum) -> new TemplateSetting(
                                sanitizer.apply(rs.getString("setting_value")),
                                toInstant(rs.getTimestamp("updated_at"))),
                        name)
                .stream()
                .filter(setting -> StringUtils.hasText(setting.value()))
                .findFirst()
                .orElse(null);
    }

    private Instant maxInstant(Instant first, Instant second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isAfter(second) ? first : second;
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
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

    private record TemplateSetting(String value, Instant updatedAt) {
    }
}
