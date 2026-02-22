package com.marketinghub.emailservice.settings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.emailservice.config.EmailServiceProperties;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailSmtpConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(EmailSmtpConfigurationService.class);
    private static final int DEFAULT_TIMEOUT_MS = 5000;
    private static final long CACHE_TTL_MILLIS = 30_000L;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final EmailServiceProperties fallbackProperties;
    private final AtomicReference<CacheEntry> cache = new AtomicReference<>();

    public EmailSmtpConfigurationService(JdbcTemplate jdbcTemplate,
                                         ObjectMapper objectMapper,
                                         EmailServiceProperties fallbackProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.fallbackProperties = fallbackProperties;
    }

    public Optional<EmailSmtpSettingsData> fetchSettings() {
        long now = System.currentTimeMillis();
        CacheEntry entry = cache.get();
        if (entry != null && now - entry.loadedAt <= CACHE_TTL_MILLIS) {
            return Optional.ofNullable(entry.settings);
        }
        EmailSmtpSettingsData settings = loadFromDatabase().orElse(null);
        cache.set(new CacheEntry(settings, now));
        return Optional.ofNullable(settings);
    }

    public JavaMailSenderImpl buildMailSender(EmailSmtpSettingsData settings) {
        EmailSmtpSettingsData sanitized = sanitize(settings);
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setProtocol("smtp");
        sender.setHost(sanitized.host());
        sender.setPort(sanitized.port());
        sender.setDefaultEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        boolean authEnabled = Boolean.TRUE.equals(sanitized.authEnabled());
        if (authEnabled) {
            sender.setUsername(sanitized.username());
            sender.setPassword(sanitized.password());
        }
        var props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", Boolean.toString(authEnabled));
        props.put("mail.smtp.starttls.enable", Boolean.toString(Boolean.TRUE.equals(sanitized.useStartTls())));
        props.put("mail.smtp.ssl.enable", Boolean.toString(Boolean.TRUE.equals(sanitized.useSsl())));
        applyTimeout(props, "mail.smtp.connectiontimeout", sanitized.connectionTimeoutMs());
        applyTimeout(props, "mail.smtp.timeout", sanitized.readTimeoutMs());
        applyTimeout(props, "mail.smtp.writetimeout", sanitized.writeTimeoutMs());
        if (!StringUtils.hasText(sender.getUsername())) {
            props.remove("mail.smtp.auth");
        }
        return sender;
    }

    public String resolveFromAddress() {
        return fetchSettings()
                .map(EmailSmtpSettingsData::fromEmail)
                .filter(StringUtils::hasText)
                .orElse(fallbackProperties.defaultFromAddress());
    }

    public String resolveFromName() {
        return fetchSettings()
                .map(EmailSmtpSettingsData::fromName)
                .filter(StringUtils::hasText)
                .orElse(null);
    }

    public boolean isDryRun() {
        return fetchSettings()
                .map(cfg -> Boolean.TRUE.equals(cfg.dryRun()))
                .orElse(fallbackProperties.dryRun());
    }

    private void applyTimeout(java.util.Properties props, String key, Integer value) {
        if (value == null) {
            props.remove(key);
            return;
        }
        props.put(key, Integer.toString(value));
    }

    private Optional<EmailSmtpSettingsData> loadFromDatabase() {
        String sql = "SELECT setting_value FROM general_setting WHERE name = ? LIMIT 1";
        List<String> values = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("setting_value"),
                GeneralSettingNames.EMAIL_SERVICE_SMTP);
        if (values.isEmpty()) {
            return Optional.empty();
        }
        String raw = values.get(0);
        if (!StringUtils.hasText(raw)) {
            return Optional.empty();
        }
        try {
            EmailSmtpSettingsData parsed = objectMapper.readValue(raw, EmailSmtpSettingsData.class);
            return Optional.of(sanitize(parsed));
        } catch (JsonProcessingException ex) {
            log.error("Configuração SMTP inválida no banco de dados", ex);
            return Optional.empty();
        }
    }

    private EmailSmtpSettingsData sanitize(EmailSmtpSettingsData data) {
        if (data == null) {
            return null;
        }
        boolean authEnabled = data.authEnabled() == null || Boolean.TRUE.equals(data.authEnabled());
        String username = authEnabled ? normalize(data.username()) : null;
        String password = authEnabled ? normalize(data.password()) : null;
        return new EmailSmtpSettingsData(
                normalize(data.providerName()),
                normalize(data.host()),
                data.port(),
                authEnabled,
                username,
                password,
                normalize(data.fromName()),
                normalize(data.fromEmail()),
                Boolean.TRUE.equals(data.useStartTls()),
                Boolean.TRUE.equals(data.useSsl()),
                normalizeTimeout(data.connectionTimeoutMs()),
                normalizeTimeout(data.readTimeoutMs()),
                normalizeTimeout(data.writeTimeoutMs()),
                Boolean.TRUE.equals(data.dryRun())
        );
    }

    private Integer normalizeTimeout(Integer value) {
        return value == null || value <= 0 ? DEFAULT_TIMEOUT_MS : value;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record CacheEntry(EmailSmtpSettingsData settings, long loadedAt) {
    }
}
