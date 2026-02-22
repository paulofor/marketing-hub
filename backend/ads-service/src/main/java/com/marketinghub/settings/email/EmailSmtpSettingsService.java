package com.marketinghub.settings.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.settings.GeneralSettingKeys;
import com.marketinghub.settings.GeneralSettingService;
import com.marketinghub.settings.dto.GeneralSettingDto;
import com.marketinghub.settings.dto.EmailSmtpSettingsResponse;
import com.marketinghub.settings.dto.UpdateEmailSmtpSettingsRequest;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmailSmtpSettingsService {

    private static final Logger log = LoggerFactory.getLogger(EmailSmtpSettingsService.class);
    private static final int DEFAULT_TIMEOUT_MS = 5000;

    private final GeneralSettingService generalSettingService;
    private final ObjectMapper objectMapper;

    public EmailSmtpSettingsService(GeneralSettingService generalSettingService, ObjectMapper objectMapper) {
        this.generalSettingService = generalSettingService;
        this.objectMapper = objectMapper;
    }

    public EmailSmtpSettingsResponse getSettings() {
        Optional<GeneralSettingDto> dto = generalSettingService.findByName(GeneralSettingKeys.EMAIL_SERVICE_SMTP);
        EmailSmtpSettingsData data = dto.flatMap(value -> parse(value.value())).orElse(null);
        EmailSmtpSettingsData sanitized = data != null ? sanitize(data) : null;
        return buildResponse(sanitized, dto.map(GeneralSettingDto::updatedAt).orElse(null));
    }

    @Transactional
    public EmailSmtpSettingsResponse update(UpdateEmailSmtpSettingsRequest request) {
        EmailSmtpSettingsData current = generalSettingService.findByName(GeneralSettingKeys.EMAIL_SERVICE_SMTP)
                .flatMap(value -> parse(value.value()))
                .map(this::sanitize)
                .orElse(null);

        EmailSmtpSettingsData merged = merge(current, request);
        validate(merged);
        String serialized = serialize(merged);
        GeneralSettingDto saved = generalSettingService.upsert(GeneralSettingKeys.EMAIL_SERVICE_SMTP, serialized);
        return buildResponse(merged, saved.updatedAt());
    }

    public EmailSmtpSettingsData getRequiredSettings() {
        return generalSettingService.findByName(GeneralSettingKeys.EMAIL_SERVICE_SMTP)
                .flatMap(value -> parse(value.value()))
                .map(this::sanitize)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Configure o servidor SMTP antes de enviar e-mails de teste."));
    }

    private EmailSmtpSettingsResponse buildResponse(EmailSmtpSettingsData data, Instant updatedAt) {
        if (data == null) {
            return new EmailSmtpSettingsResponse(null, null, null, true, null, null, null,
                    false, true, DEFAULT_TIMEOUT_MS, DEFAULT_TIMEOUT_MS, DEFAULT_TIMEOUT_MS,
                    false, false, null);
        }
        boolean hasPassword = StringUtils.hasText(data.password());
        return new EmailSmtpSettingsResponse(
                data.providerName(),
                data.host(),
                data.port(),
                Boolean.TRUE.equals(data.authEnabled()),
                data.username(),
                data.fromName(),
                data.fromEmail(),
                Boolean.TRUE.equals(data.useStartTls()),
                Boolean.TRUE.equals(data.useSsl()),
                data.connectionTimeoutMs(),
                data.readTimeoutMs(),
                data.writeTimeoutMs(),
                Boolean.TRUE.equals(data.dryRun()),
                hasPassword,
                updatedAt
        );
    }

    private EmailSmtpSettingsData merge(EmailSmtpSettingsData current, UpdateEmailSmtpSettingsRequest request) {
        boolean authEnabled = request.authEnabled() == null
                ? current == null || !Boolean.FALSE.equals(current.authEnabled())
                : request.authEnabled();

        String providerName = normalize(request.providerName());
        String host = normalize(request.host());
        Integer port = request.port();
        String username = authEnabled ? normalize(request.username()) : null;
        String password;
        if (!authEnabled) {
            password = null;
        } else if (request.password() != null) {
            password = normalize(request.password());
        } else {
            password = current != null ? current.password() : null;
        }

        String fromEmail = normalize(request.fromEmail());
        String fromName = normalize(request.fromName());
        boolean useStartTls = request.useStartTls() != null ? request.useStartTls() : current != null && Boolean.TRUE.equals(current.useStartTls());
        boolean useSsl = request.useSsl() != null ? request.useSsl() : current != null && Boolean.TRUE.equals(current.useSsl());
        int connectionTimeout = request.connectionTimeoutMs() != null ? request.connectionTimeoutMs()
                : current != null && current.connectionTimeoutMs() != null ? current.connectionTimeoutMs() : DEFAULT_TIMEOUT_MS;
        int readTimeout = request.readTimeoutMs() != null ? request.readTimeoutMs()
                : current != null && current.readTimeoutMs() != null ? current.readTimeoutMs() : DEFAULT_TIMEOUT_MS;
        int writeTimeout = request.writeTimeoutMs() != null ? request.writeTimeoutMs()
                : current != null && current.writeTimeoutMs() != null ? current.writeTimeoutMs() : DEFAULT_TIMEOUT_MS;
        boolean dryRun = request.dryRun() != null ? request.dryRun() : current != null && Boolean.TRUE.equals(current.dryRun());

        return EmailSmtpSettingsData.builder()
                .providerName(providerName)
                .host(host)
                .port(port)
                .authEnabled(authEnabled)
                .username(username)
                .password(password)
                .fromName(fromName)
                .fromEmail(fromEmail)
                .useStartTls(useStartTls)
                .useSsl(useSsl)
                .connectionTimeoutMs(connectionTimeout)
                .readTimeoutMs(readTimeout)
                .writeTimeoutMs(writeTimeout)
                .dryRun(dryRun)
                .build();
    }

    private void validate(EmailSmtpSettingsData data) {
        if (!StringUtils.hasText(data.host())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o host SMTP.");
        }
        if (data.port() == null || data.port() < 1 || data.port() > 65535) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe uma porta válida entre 1 e 65535.");
        }
        if (!StringUtils.hasText(data.fromEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o e-mail do remetente.");
        }
        if (Boolean.TRUE.equals(data.authEnabled())) {
            if (!StringUtils.hasText(data.username())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário SMTP é obrigatório quando a autenticação está ativa.");
            }
            if (!StringUtils.hasText(data.password())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Defina a senha SMTP para autenticar no provedor.");
            }
        }
        if (data.connectionTimeoutMs() == null || data.connectionTimeoutMs() <= 0
                || data.readTimeoutMs() == null || data.readTimeoutMs() <= 0
                || data.writeTimeoutMs() == null || data.writeTimeoutMs() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Timeouts devem ser maiores que zero.");
        }
    }

    private Optional<EmailSmtpSettingsData> parse(String value) {
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, EmailSmtpSettingsData.class));
        } catch (JsonProcessingException ex) {
            log.error("Falha ao converter configuração SMTP", ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível ler a configuração atual. Edite e salve novamente.");
        }
    }

    private EmailSmtpSettingsData sanitize(EmailSmtpSettingsData data) {
        if (data == null) {
            return null;
        }
        boolean authEnabled = data.authEnabled() == null || Boolean.TRUE.equals(data.authEnabled());
        String username = authEnabled ? normalize(data.username()) : null;
        String password = authEnabled ? normalize(data.password()) : null;
        return data.toBuilder()
                .providerName(normalize(data.providerName()))
                .host(normalize(data.host()))
                .port(data.port())
                .authEnabled(authEnabled)
                .username(username)
                .password(password)
                .fromName(normalize(data.fromName()))
                .fromEmail(normalize(data.fromEmail()))
                .useStartTls(Boolean.TRUE.equals(data.useStartTls()))
                .useSsl(Boolean.TRUE.equals(data.useSsl()))
                .connectionTimeoutMs(normalizeTimeout(data.connectionTimeoutMs()))
                .readTimeoutMs(normalizeTimeout(data.readTimeoutMs()))
                .writeTimeoutMs(normalizeTimeout(data.writeTimeoutMs()))
                .dryRun(Boolean.TRUE.equals(data.dryRun()))
                .build();
    }

    private Integer normalizeTimeout(Integer value) {
        return value == null || value <= 0 ? DEFAULT_TIMEOUT_MS : value;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String serialize(EmailSmtpSettingsData data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível salvar a configuração de e-mail", ex);
        }
    }
}
