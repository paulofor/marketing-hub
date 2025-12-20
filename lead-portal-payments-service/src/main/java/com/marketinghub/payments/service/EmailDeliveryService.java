package com.marketinghub.payments.service;

import com.marketinghub.payments.config.EmailProperties;
import com.marketinghub.payments.dto.LeadPortalPackageSummary;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryService.class);

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    public EmailDeliveryService(JavaMailSender mailSender, EmailProperties emailProperties) {
        this.mailSender = mailSender;
        this.emailProperties = emailProperties;
    }

    public void sendOriginalsEmail(String recipient,
                                   LeadPortalPackageSummary summary,
                                   String downloadUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(emailProperties.getFrom());
            helper.setTo(recipient);
            helper.setSubject(emailProperties.getSubject());
            if (StringUtils.hasText(emailProperties.getReplyTo())) {
                helper.setReplyTo(emailProperties.getReplyTo());
            }

            String html = buildHtmlBody(summary, downloadUrl);
            String plain = buildTextBody(summary, downloadUrl);
            helper.setText(plain, html);

            mailSender.send(message);
            log.info("E-mail enviado para {} com link das imagens do pacote {}", recipient, summary.packageId());
        } catch (MessagingException ex) {
            throw new IllegalStateException("Falha ao montar ou enviar e-mail", ex);
        }
    }

    private String buildHtmlBody(LeadPortalPackageSummary summary, String downloadUrl) {
        String greeting = StringUtils.hasText(summary.submissionName())
                ? "Olá, " + summary.submissionName() + "!"
                : "Olá!";
        String link = StringUtils.hasText(downloadUrl)
                ? "<p><a href=\"" + downloadUrl + "\" style=\"background:#1d4ed8;color:white;padding:12px 18px;border-radius:6px;text-decoration:none;\">Baixar imagens originais</a></p>"
                : "<p>Não foi possível gerar o link de download automaticamente. Responda este e-mail para receber os arquivos.</p>";
        return """
                <div style="font-family:Arial,sans-serif;font-size:15px;color:#111">
                  <p>%s</p>
                  <p>Recebemos o pagamento do seu pacote de imagens #%d.</p>
                  <p>Use o link abaixo para baixar todas as variações em alta resolução, sem marca d'água.</p>
                  %s
                  <p style="margin-top:24px;color:#555">Se tiver qualquer problema com o download é só responder este e-mail.</p>
                </div>
                """.formatted(greeting, summary.packageId(), link);
    }

    private String buildTextBody(LeadPortalPackageSummary summary, String downloadUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá");
        if (StringUtils.hasText(summary.submissionName())) {
            sb.append(", ").append(summary.submissionName());
        }
        sb.append("!\n\n");
        sb.append("Recebemos o pagamento do seu pacote de imagens #").append(summary.packageId()).append(".\n");
        if (StringUtils.hasText(downloadUrl)) {
            sb.append("Baixe as imagens originais neste link: ").append(downloadUrl).append("\n\n");
        } else {
            sb.append("Não conseguimos gerar o link automaticamente. Responda este e-mail e reenviamos os arquivos.\n\n");
        }
        sb.append("Se precisar de ajuda é só responder esta mensagem.\n");
        return sb.toString();
    }
}
