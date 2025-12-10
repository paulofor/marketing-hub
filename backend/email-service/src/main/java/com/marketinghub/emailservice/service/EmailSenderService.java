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
            log.info("E-mail enviado com sucesso para {}", String.join(",", message.to()));
        } catch (Exception ex) {
            throw new EmailSendingException("Falha ao enviar e-mail", ex);
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
}
