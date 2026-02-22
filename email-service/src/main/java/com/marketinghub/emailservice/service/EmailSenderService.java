package com.marketinghub.emailservice.service;

import com.marketinghub.emailservice.config.EmailServiceProperties;
import com.marketinghub.emailservice.exception.EmailSendingException;
import com.marketinghub.emailservice.settings.EmailSmtpConfigurationService;
import com.marketinghub.emailservice.settings.EmailSmtpSettingsData;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class EmailSenderService {

    private static final Logger log = LoggerFactory.getLogger(EmailSenderService.class);

    private final JavaMailSender defaultMailSender;
    private final EmailServiceProperties fallbackProperties;
    private final EmailSmtpConfigurationService smtpConfigurationService;

    public EmailSenderService(JavaMailSender defaultMailSender,
                              EmailServiceProperties fallbackProperties,
                              EmailSmtpConfigurationService smtpConfigurationService) {
        this.defaultMailSender = defaultMailSender;
        this.fallbackProperties = fallbackProperties;
        this.smtpConfigurationService = smtpConfigurationService;
    }

    public void send(EmailMessage message) {
        Optional<EmailSmtpSettingsData> customConfig = smtpConfigurationService.fetchSettings();
        if (customConfig.isPresent()) {
            EmailSmtpSettingsData config = customConfig.get();
            if (Boolean.TRUE.equals(config.dryRun())) {
                log.info("[DRY-RUN] SMTP configurado em modo simulação. Ignorando envio para {}", message.to());
                return;
            }
            JavaMailSenderImpl customSender = smtpConfigurationService.buildMailSender(config);
            dispatch(customSender, message, "custom");
            return;
        }

        if (fallbackProperties.dryRun()) {
            log.info("[DRY-RUN] Envio de e-mail simulado (configuração padrão) para {}", message.to());
            return;
        }
        dispatch(defaultMailSender, message, "default");
    }

    private void dispatch(JavaMailSender mailSender, EmailMessage message, String configLabel) {
        String mailConfigDescription = describeMailConfiguration(mailSender, configLabel);
        log.info("Preparando envio de e-mail: de={} (nome='{}'), para={}, cc={}, bcc={}, assunto='{}', anexos={}, tamanhoAnexos={} bytes, config={}",
                message.from(),
                message.fromName(),
                formatList(message.to()),
                formatList(message.cc()),
                formatList(message.bcc()),
                message.subject(),
                CollectionUtils.isEmpty(message.attachments()) ? 0 : message.attachments().size(),
                totalAttachmentBytes(message.attachments()),
                mailConfigDescription);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

            applyFrom(helper, message);
            helper.setTo(message.to().toArray(String[]::new));

            if (!CollectionUtils.isEmpty(message.cc())) {
                helper.setCc(message.cc().toArray(String[]::new));
            }
            if (!CollectionUtils.isEmpty(message.bcc())) {
                helper.setBcc(message.bcc().toArray(String[]::new));
            }

            helper.setSubject(message.subject());

            if (message.textBody() != null && !message.textBody().isBlank()) {
                helper.setText(message.textBody(), message.htmlBody());
            } else {
                helper.setText(message.htmlBody(), true);
            }

            addAttachments(helper, message.attachments());

            mailSender.send(mimeMessage);
            log.info("E-mail enviado com sucesso para {} (configuração: {})", String.join(",", message.to()), mailConfigDescription);
        } catch (Exception ex) {
            log.error("Erro ao enviar e-mail para {}. Configuração SMTP: {}", formatList(message.to()), mailConfigDescription, ex);
            throw new EmailSendingException("Falha ao enviar e-mail (" + mailConfigDescription + ")", ex);
        }
    }

    private void applyFrom(MimeMessageHelper helper, EmailMessage message)
            throws UnsupportedEncodingException, jakarta.mail.MessagingException {
        if (StringUtils.hasText(message.fromName())) {
            helper.setFrom(new InternetAddress(message.from(), message.fromName(), StandardCharsets.UTF_8.name()));
        } else {
            helper.setFrom(message.from());
        }
    }

    private void addAttachments(MimeMessageHelper helper, List<EmailAttachmentResource> attachments) throws Exception {
        if (CollectionUtils.isEmpty(attachments)) {
            return;
        }

        for (EmailAttachmentResource attachment : attachments) {
            if (attachment.inline()) {
                if (attachment.contentId() == null || attachment.contentId().isBlank()) {
                    throw new IllegalArgumentException("Attachments inline precisam informar contentId");
                }
                helper.addInline(attachment.contentId(), new ByteArrayResource(attachment.asset().content()),
                        attachment.asset().mediaType() != null ? attachment.asset().mediaType().toString() : null);
            } else {
                helper.addAttachment(attachment.asset().fileName(),
                        new ByteArrayResource(attachment.asset().content()),
                        attachment.asset().mediaType() != null ? attachment.asset().mediaType().toString() : null);
            }
        }
    }

    private long totalAttachmentBytes(List<EmailAttachmentResource> attachments) {
        if (CollectionUtils.isEmpty(attachments)) {
            return 0;
        }
        long total = 0;
        for (EmailAttachmentResource attachment : attachments) {
            if (attachment != null && attachment.asset() != null && attachment.asset().content() != null) {
                total += attachment.asset().content().length;
            }
        }
        return total;
    }

    private String describeMailConfiguration(JavaMailSender sender, String label) {
        if (sender instanceof JavaMailSenderImpl impl) {
            String protocol = impl.getProtocol() != null ? impl.getProtocol() : "smtp";
            var properties = impl.getJavaMailProperties();
            return String.format("%s: host=%s:%d, protocol=%s, username=%s, tls=%s, ssl=%s, auth=%s, timeouts=[connect=%sms, read=%sms, write=%sms]",
                    label,
                    impl.getHost(),
                    impl.getPort(),
                    protocol,
                    impl.getUsername(),
                    properties.getProperty("mail.smtp.starttls.enable", "n/a"),
                    properties.getProperty("mail.smtp.ssl.enable", "n/a"),
                    properties.getProperty("mail.smtp.auth", "n/a"),
                    properties.getProperty("mail.smtp.connectiontimeout", "n/a"),
                    properties.getProperty("mail.smtp.timeout", "n/a"),
                    properties.getProperty("mail.smtp.writetimeout", "n/a"));
        }
        return label + ": JavaMailSender=" + sender.getClass().getSimpleName();
    }

    private String formatList(List<String> values) {
        return CollectionUtils.isEmpty(values) ? "[]" : String.join(",", values);
    }
}
