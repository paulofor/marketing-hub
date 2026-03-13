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
        String sql = "SELECT setting_value, updated_at FROM general_setting WHERE name = ? LIMIT 1";
        return jdbcTemplate
                .query(sql, (rs, rowNum) -> new LeadPortalEmailTemplate(
                        sanitizeHtml(rs.getString("setting_value")),
                        toInstant(rs.getTimestamp("updated_at"))),
                        GeneralSettingNames.LEAD_PORTAL_EMAIL_TEMPLATE_HTML)
                .stream()
                .filter(template -> StringUtils.hasText(template.html()))
                .findFirst();
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

    public record LeadPortalEmailTemplate(String html, Instant updatedAt) {
    }
}
