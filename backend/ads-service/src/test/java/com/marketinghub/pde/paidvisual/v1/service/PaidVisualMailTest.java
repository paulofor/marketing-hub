package com.marketinghub.pde.paidvisual.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marketinghub.settings.email.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** Responsabilidade: validar entrega do acesso sem SMTP real nos testes automatizados. */
class PaidVisualMailTest {
  /** Garante destinatário, fragmento privado e bloqueio do modo de teste antes do envio. */
  @Test
  void sendsOnlyToConfirmedBuyerAndRejectsDryRun() {
    var settings = mock(EmailSmtpSettingsService.class);
    var factory = mock(EmailSmtpMailFactory.class);
    var transport = mock(JavaMailSenderImpl.class);
    var config = config("sandbox-mail");
    when(settings.getRequiredSettings()).thenReturn(config);
    when(factory.create(config)).thenReturn(transport);
    var service = new PaidVisualMail(settings, factory);
    service.send(
        "test-payment",
        "teste+paid-visual@sandbox.local",
        "https://example.test/paid-visual.html#access=local-only");
    var sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(transport).send(sent.capture());
    assertThat(sent.getValue().getTo()).containsExactly("teste+paid-visual@sandbox.local");
    assertThat(sent.getValue().getText()).contains("#access=local-only");
    when(settings.getRequiredSettings()).thenReturn(config.toBuilder().dryRun(true).build());
    assertThatThrownBy(
            () ->
                service.send(
                    "test-payment", "teste+paid-visual@sandbox.local", "https://example.test"))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    verifyNoMoreInteractions(transport);
  }

  /** Exercita o cliente SMTP real somente no servidor descartável explicitamente selecionado. */
  @Test
  @EnabledIfEnvironmentVariable(named = "PDE_SANDBOX_MAIL_HOST", matches = "sandbox-mail")
  void sendsThroughDisposableSandboxSmtp() {
    var settings = mock(EmailSmtpSettingsService.class);
    when(settings.getRequiredSettings()).thenReturn(config("sandbox-mail"));
    new PaidVisualMail(settings, new EmailSmtpMailFactory())
        .send(
            "paid-visual-6a097098-local",
            "teste+6a097098-paid-visual@sandbox.local",
            "https://example.test/paid-visual.html#access=local-test-only");
  }

  /** Usa apenas credenciais ausentes e identidades locais descartáveis. */
  private EmailSmtpSettingsData config(String host) {
    return EmailSmtpSettingsData.builder()
        .host(host)
        .port(1025)
        .authEnabled(false)
        .fromEmail("alcyone-test@sandbox.local")
        .dryRun(false)
        .useSsl(false)
        .useStartTls(false)
        .connectionTimeoutMs(3000)
        .readTimeoutMs(3000)
        .writeTimeoutMs(3000)
        .build();
  }
}
