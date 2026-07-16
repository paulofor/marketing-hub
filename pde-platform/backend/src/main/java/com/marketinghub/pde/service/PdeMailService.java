package com.marketinghub.pde.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

/** Envia e-mails transacionais simples da experiência PDE quando SMTP estiver configurado. */
@Service
public class PdeMailService {

    private static final Logger log = LoggerFactory.getLogger(PdeMailService.class);

    private final String smtpHost;
    private final int smtpPort;
    private final String from;
    private final String username;
    private final String password;

    /** Recebe a configuração SMTP opcional da Área MUSA. */
    public PdeMailService(
            @Value("${pde.mail.host:}") String smtpHost,
            @Value("${pde.mail.port:1025}") int smtpPort,
            @Value("${pde.mail.from:area-musa@sandbox.local}") String from,
            @Value("${pde.mail.username:}") String username,
            @Value("${pde.mail.password:}") String password) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.from = from;
        this.username = username;
        this.password = password;
    }

    /** Informa se existe SMTP configurado para envio de link magico. */
    public boolean isConfigured() {
        return smtpHost != null && !smtpHost.isBlank();
    }

    /** Envia o link de acesso da cliente para o e-mail informado. */
    public void sendMagicLink(String to, String accessUrl) {
        if (!isConfigured()) {
            log.info("SMTP PDE nao configurado; link magico nao enviado para {}", to);
            return;
        }
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
            message.setSubject("Seu acesso ao Metodo MUSA");
            message.setText("""
                    Oi,

                    Use este link para entrar na sua Area MUSA e continuar sua experiencia guiada:

                    %s

                    Se voce nao pediu este acesso, ignore este e-mail.
                    """.formatted(accessUrl));
            sender.send(message);
        } catch (RuntimeException ex) {
            log.error("Falha ao enviar magic link PDE; to={}, accessUrl={}", to, accessUrl, ex);
            throw ex;
        }
    }
}
