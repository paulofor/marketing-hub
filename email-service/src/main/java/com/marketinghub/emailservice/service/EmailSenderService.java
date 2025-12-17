package com.marketinghub.emailservice.service;

import com.marketinghub.emailservice.config.EmailServiceProperties;
import com.marketinghub.emailservice.exception.EmailSendingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class EmailSenderService {

    private static final Logger log = LoggerFactory.getLogger(EmailSenderService.class);

    private final JavaMailSender javaMailSender;
    private final EmailServiceProperties properties;

    public EmailSenderService(JavaMailSender javaMailSender, EmailServiceProperties properties) {
        this.javaMailSender = javaMailSender;
        this.properties = properties;
    }

    public void send(EmailMessage message) {
        if (properties.dryRun()) {
            log.info("[DRY-RUN] Envio de e-mail simulado para {} com assunto '{}'", message.to(), message.subject());
            return;
        }

        String mailConfigDescription = describeMailConfiguration();
        log.info("Preparando envio de e-mail: de={}, para={}, cc={}, bcc={}, assunto='{}', anexos={}, tamanhoAnexos={} bytes, config={}",
                message.from(),
                formatList(message.to()),
                formatList(message.cc()),
                formatList(message.bcc()),
                message.subject(),
                CollectionUtils.isEmpty(message.attachments()) ? 0 : message.attachments().size(),
                totalAttachmentBytes(message.attachments()),
                mailConfigDescription);

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

            helper.setFrom(message.from());
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

            javaMailSender.send(mimeMessage);
            log.info("E-mail enviado com sucesso para {} (configuração: {})", String.join(",", message.to()), mailConfigDescription);
        } catch (Exception ex) {
            log.error("Erro ao enviar e-mail para {}. Configuração SMTP: {}", formatList(message.to()), mailConfigDescription, ex);
            throw new EmailSendingException("Falha ao enviar e-mail (" + mailConfigDescription + ")", ex);
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

    private String describeMailConfiguration() {
        if (javaMailSender instanceof JavaMailSenderImpl sender) {
            String protocol = sender.getProtocol() != null ? sender.getProtocol() : "smtp";
            var properties = sender.getJavaMailProperties();
            return String.format("host=%s:%d, protocol=%s, username=%s, tls=%s, ssl=%s, auth=%s, timeouts=[connect=%sms, read=%sms, write=%sms]",
                    sender.getHost(),
                    sender.getPort(),
                    protocol,
                    sender.getUsername(),
                    properties.getProperty("mail.smtp.starttls.enable", "n/a"),
                    properties.getProperty("mail.smtp.ssl.enable", "n/a"),
                    properties.getProperty("mail.smtp.auth", "n/a"),
                    properties.getProperty("mail.smtp.connectiontimeout", "n/a"),
                    properties.getProperty("mail.smtp.timeout", "n/a"),
                    properties.getProperty("mail.smtp.writetimeout", "n/a"));
        }
        return "JavaMailSender=" + javaMailSender.getClass().getSimpleName();
    }

    private String formatList(List<String> values) {
        return CollectionUtils.isEmpty(values) ? "[]" : String.join(",", values);
    }
}
