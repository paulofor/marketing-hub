package com.marketinghub.pde.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

/** Envia e-mails transacionais simples da experiência PDE por SMTP local ou Amazon SES. */
@Service
public class PdeMailService {

    private static final Logger log = LoggerFactory.getLogger(PdeMailService.class);

    private final String transport;
    private final String awsRegion;
    private final String smtpHost;
    private final int smtpPort;
    private final String from;
    private final String username;
    private final String password;

    /** Recebe a configuração opcional de envio da Área MUSA. */
    public PdeMailService(
            @Value("${pde.mail.transport:smtp}") String transport,
            @Value("${pde.mail.aws-region:us-east-1}") String awsRegion,
            @Value("${pde.mail.host:}") String smtpHost,
            @Value("${pde.mail.port:1025}") int smtpPort,
            @Value("${pde.mail.from:area-musa@sandbox.local}") String from,
            @Value("${pde.mail.username:}") String username,
            @Value("${pde.mail.password:}") String password) {
        this.transport = transport;
        this.awsRegion = awsRegion;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.from = from;
        this.username = username;
        this.password = password;
    }

    /** Informa se existe transporte configurado para envio de link mágico. */
    public boolean isConfigured() {
        if ("ses".equalsIgnoreCase(transport)) {
            return from != null && !from.isBlank();
        }
        return smtpHost != null && !smtpHost.isBlank();
    }

    /** Envia o link de acesso da cliente para o e-mail informado. */
    public void sendMagicLink(String to, String accessUrl) {
        if (!isConfigured()) {
            log.info("Envio de e-mail PDE não configurado; link mágico não enviado para {}", to);
            return;
        }
        if ("ses".equalsIgnoreCase(transport)) {
            sendMagicLinkWithSes(to, accessUrl);
            return;
        }
        sendMagicLinkWithSmtp(to, accessUrl);
    }

    /** Envia o link de acesso usando SMTP, inclusive no sandbox local. */
    private void sendMagicLinkWithSmtp(String to, String accessUrl) {
        try {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(smtpHost);
            sender.setPort(smtpPort);
            if (username != null && !username.isBlank()) {
                sender.setUsername(username);
                sender.setPassword(password);
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject("Seu acesso ao Método MUSA");
            message.setText(buildMagicLinkText(accessUrl));
            sender.send(message);
        } catch (RuntimeException ex) {
            log.error("Falha ao enviar magic link PDE por SMTP; to={}, accessUrl={}", to, accessUrl, ex);
            throw ex;
        }
    }

    /** Envia o link de acesso usando Amazon SES API e credenciais AWS do ambiente. */
    private void sendMagicLinkWithSes(String to, String accessUrl) {
        try (SesV2Client client = SesV2Client.builder()
                .region(Region.of(awsRegion))
                .build()) {
            client.sendEmail(SendEmailRequest.builder()
                    .fromEmailAddress(from)
                    .destination(Destination.builder().toAddresses(to).build())
                    .content(EmailContent.builder()
                            .simple(Message.builder()
                                    .subject(Content.builder()
                                            .charset("UTF-8")
                                            .data("Seu acesso ao Método MUSA")
                                            .build())
                                    .body(Body.builder()
                                            .text(Content.builder()
                                                    .charset("UTF-8")
                                                    .data(buildMagicLinkText(accessUrl))
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build());
        } catch (RuntimeException ex) {
            log.error(
                    "Falha ao enviar magic link PDE por SES; to={}, accessUrl={}, from={}, region={}",
                    to,
                    accessUrl,
                    from,
                    awsRegion,
                    ex);
            throw ex;
        }
    }

    /** Monta o texto comercial do link de acesso da Área MUSA. */
    String buildMagicLinkText(String accessUrl) {
        return """
                Oi,

                Seu acesso ao Clube MUSA está pronto.

                Entre por este link seguro para liberar seu diagnóstico inicial e continuar o Dia 1 do Método MUSA:

                %s

                Se você não pediu este acesso, ignore este e-mail.
                """.formatted(accessUrl);
    }
}
