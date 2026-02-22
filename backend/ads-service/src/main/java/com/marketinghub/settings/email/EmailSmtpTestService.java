package com.marketinghub.settings.email;

import com.marketinghub.settings.dto.TestEmailRequest;
import com.marketinghub.settings.dto.TestEmailResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmailSmtpTestService {

    private static final Logger log = LoggerFactory.getLogger(EmailSmtpTestService.class);
    private static final String DEFAULT_SUBJECT = "Teste de SMTP do Marketing Hub";
    private static final String DEFAULT_BODY = "Este e-mail confirma que o servidor SMTP configurado no Marketing Hub está funcionando.";

    private final EmailSmtpSettingsService settingsService;
    private final EmailSmtpMailFactory mailFactory;

    public EmailSmtpTestService(EmailSmtpSettingsService settingsService, EmailSmtpMailFactory mailFactory) {
        this.settingsService = settingsService;
        this.mailFactory = mailFactory;
    }

    public TestEmailResponse sendTestEmail(TestEmailRequest request) {
        EmailSmtpSettingsData settings = settingsService.getRequiredSettings();
        if (Boolean.TRUE.equals(settings.dryRun())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O modo dry-run está ativado. Desative-o para enviar testes reais.");
        }
        JavaMailSenderImpl sender = mailFactory.create(settings);
        try {
            var mimeMessage = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
            helper.setTo(request.recipient().trim());
            helper.setSubject(resolveSubject(request));
            helper.setText(resolveBody(settings, request), false);
            if (StringUtils.hasText(settings.fromName())) {
                helper.setFrom(new InternetAddress(settings.fromEmail(), settings.fromName(), StandardCharsets.UTF_8.name()));
            } else {
                helper.setFrom(settings.fromEmail());
            }
            sender.send(mimeMessage);
            log.info("E-mail de teste enviado para {} usando host {}:{}", request.recipient(), settings.host(), settings.port());
            return new TestEmailResponse(true,
                    "E-mail enviado para " + request.recipient().trim(),
                    Instant.now());
        } catch (MailException | MessagingException | UnsupportedEncodingException ex) {
            log.error("Falha ao enviar e-mail de teste", ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Falha ao enviar e-mail de teste: " + ex.getMessage(), ex);
        }
    }

    private String resolveSubject(TestEmailRequest request) {
        return StringUtils.hasText(request.subject()) ? request.subject().trim() : DEFAULT_SUBJECT;
    }

    private String resolveBody(EmailSmtpSettingsData settings, TestEmailRequest request) {
        if (StringUtils.hasText(request.message())) {
            return request.message().trim();
        }
        String provider = StringUtils.hasText(settings.providerName()) ? settings.providerName() : "o provedor configurado";
        return DEFAULT_BODY + "\n\n" + "Remetente: " + settings.fromEmail() + " (" + provider + ")";
    }
}
