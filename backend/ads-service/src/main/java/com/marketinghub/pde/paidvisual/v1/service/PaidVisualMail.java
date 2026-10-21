package com.marketinghub.pde.paidvisual.v1.service;

import static com.marketinghub.pde.visualpersonalization.v1.service.VisualPreparationService.*;

import com.marketinghub.settings.email.EmailSmtpMailFactory;
import com.marketinghub.settings.email.EmailSmtpSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

/** Responsabilidade: enviar acesso exclusivamente ao e-mail da compra com o SMTP já configurado. */
@Component
@Slf4j
public class PaidVisualMail {
  private final EmailSmtpSettingsService settings;
  private final EmailSmtpMailFactory factory;

  /** Reutiliza configuração e transporte de e-mail do backend. */
  public PaidVisualMail(EmailSmtpSettingsService settings, EmailSmtpMailFactory factory) {
    this.settings = settings;
    this.factory = factory;
  }

  /** Confere configuração de envio sem expor credenciais nem disparar mensagens. */
  public boolean ready() {
    var config = settings.getSettings();
    return config.host() != null
        && !config.host().isBlank()
        && config.port() != null
        && config.fromEmail() != null
        && config.fromEmail().contains("@")
        && !config.dryRun()
        && (!config.authEnabled() || config.hasPassword());
  }

  /** Envia um link com bearer no fragmento, sem registrar endereço, corpo ou credencial em logs. */
  public void send(String paymentId, String email, String url) {
    var config = settings.getRequiredSettings();
    require(
        !Boolean.TRUE.equals(config.dryRun()), "O envio de acesso ainda está em modo de teste.");
    try {
      var message = new SimpleMailMessage();
      message.setFrom(config.fromEmail());
      message.setTo(email);
      message.setSubject("Seu acesso à entrega personalizada");
      message.setText(
          "Abra seu acesso para informar a ocasião e as peças ou recuperar suas combinações:\n"
              + url
              + "\n\nO acesso permanece disponível pelo prazo informado na compra. Não compartilhe este link.");
      factory.create(config).send(message);
      log.info("Acesso da entrega visual enviado paymentId={}", paymentId);
    } catch (RuntimeException ex) {
      log.error("Falha no envio do acesso visual paymentId={}", paymentId, ex);
      throw fail(503, "Não foi possível enviar o acesso agora.");
    }
  }
}
