package com.marketinghub.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.payments.config.DigitalProductEmailDeliveryProperties;
import com.marketinghub.payments.integration.email.DigitalProductDeliveryEmailClient;
import com.marketinghub.payments.integration.email.DigitalProductDeliveryEmailResponse;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPaymentDetails;
import com.marketinghub.payments.model.DigitalProductDeliveryEmail;
import com.marketinghub.payments.model.DigitalProductDeliveryEmailStatus;
import com.marketinghub.payments.repository.DigitalProductDeliveryEmailRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida envio de entrega digital pós-compra por referência do Mercado Pago. */
@ExtendWith(MockitoExtension.class)
class DigitalProductPostPurchaseEmailServiceTest {

    @Mock
    private DigitalProductDeliveryEmailRepository repository;

    @Mock
    private DigitalProductDeliveryEmailClient emailClient;

    private DigitalProductPostPurchaseEmailService service;

    /** Inicializa o service com propriedades reais para validar os produtos suportados. */
    @BeforeEach
    void setUp() {
        DigitalProductEmailDeliveryProperties properties = new DigitalProductEmailDeliveryProperties();
        service = new DigitalProductPostPurchaseEmailService(properties, repository, emailClient);
    }

    /** Deve enviar a entrega do experimento 66 quando o Mercado Pago confirmar pagamento aprovado. */
    @Test
    void sendIfSupportedShouldSendExperiment66DeliveryEmail() {
        MercadoPagoPaymentDetails payment = new MercadoPagoPaymentDetails(
                "pay-66",
                "approved",
                BigDecimal.valueOf(47),
                "BRL",
                "Método MUSA - Presença Elegante em 7 Dias",
                "compradora@example.com",
                "marketinghub-experiment-66",
                Instant.now(),
                Map.of(),
                "{\"id\":\"pay-66\"}");

        when(repository.findByPaymentId("pay-66")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailClient.send(any())).thenReturn(new DigitalProductDeliveryEmailResponse(
                "req-66",
                "SENT",
                "ok"));

        service.sendIfSupported(payment);

        ArgumentCaptor<DigitalProductDeliveryEmail> deliveryCaptor =
                ArgumentCaptor.forClass(DigitalProductDeliveryEmail.class);
        verify(repository, org.mockito.Mockito.atLeastOnce()).save(deliveryCaptor.capture());
        DigitalProductDeliveryEmail lastSaved = deliveryCaptor.getAllValues().get(deliveryCaptor.getAllValues().size() - 1);
        assertThat(lastSaved.getExternalReference()).isEqualTo("marketinghub-experiment-66");
        assertThat(lastSaved.getDeliveryPageUrl()).isEqualTo("https://pagamentopalf.site/obrigado-exp66-metodo-musa.html");
        assertThat(lastSaved.getDownloadUrl()).isEqualTo("https://pagamentopalf.site/downloads/experimento-66-entregaveis.zip");
        assertThat(lastSaved.getStatus()).isEqualTo(DigitalProductDeliveryEmailStatus.SENT);
        verify(emailClient).send(argThat(request ->
                request.to().equals("compradora@example.com")
                        && request.deliveryPageUrl().contains("obrigado-exp66-metodo-musa.html")
                        && request.deliveryPageUrl().contains("payment_id=pay-66")
                        && request.downloadUrl().endsWith("/experimento-66-entregaveis.zip")));
    }

    /** Deve ignorar pagamentos que não pertencem a produto digital direto suportado. */
    @Test
    void sendIfSupportedShouldIgnoreUnsupportedExternalReference() {
        MercadoPagoPaymentDetails payment = new MercadoPagoPaymentDetails(
                "pay-other",
                "approved",
                BigDecimal.TEN,
                "BRL",
                "Outro produto",
                "compradora@example.com",
                "outro-produto",
                Instant.now(),
                Map.of(),
                "{}");

        service.sendIfSupported(payment);

        verify(repository, never()).save(any());
        verify(emailClient, never()).send(any());
    }
}
