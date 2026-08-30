package com.marketinghub.payments.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.payments.dto.MercadoPagoWebhookPayload;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPaymentDetails;
import com.marketinghub.payments.model.MercadoPagoWebhookLog;
import com.marketinghub.payments.model.WebhookProcessingStatus;
import com.marketinghub.payments.repository.MercadoPagoWebhookLogRepository;
import com.marketinghub.payments.service.CheckoutService;
import com.marketinghub.payments.service.PaymentAuditPayloadSanitizer;
import com.marketinghub.payments.service.PdePaymentEntitlementClient;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Valida auditoria e efeitos do webhook após a confirmação autoritativa do pagamento. */
@ExtendWith(MockitoExtension.class)
class MercadoPagoWebhookControllerTest {

    @Mock
    private CheckoutService checkoutService;

    @Mock
    private MercadoPagoWebhookLogRepository webhookLogRepository;

    @Mock
    private PdePaymentEntitlementClient pdePaymentEntitlementClient;

    private MercadoPagoWebhookController controller;

    /** Configura dependências isoladas e persistência de auditoria observável. */
    @BeforeEach
    void setUp() {
        when(webhookLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ObjectMapper objectMapper = new ObjectMapper();
        controller = new MercadoPagoWebhookController(
                checkoutService,
                objectMapper,
                webhookLogRepository,
                pdePaymentEntitlementClient,
                new PaymentAuditPayloadSanitizer(objectMapper));
    }

    /** Aplica compra, entitlement e auditoria quando o pagamento existe. */
    @Test
    void shouldProcessWebhookWhenPaymentExists() {
        MercadoPagoWebhookPayload payload = new MercadoPagoWebhookPayload();
        MercadoPagoWebhookPayload.Data data = new MercadoPagoWebhookPayload.Data();
        data.setId("123");
        payload.setData(data);

        MercadoPagoPaymentDetails details = new MercadoPagoPaymentDetails(
                "123",
                "approved",
                BigDecimal.TEN,
                "BRL",
                "Pagamento teste",
                "client@example.com",
                null,
                Instant.now(),
                Map.of("packageId", 1L),
                "{\"id\":\"123\",\"payer\":{\"email\":\"client@example.com\"}}"
        );

        when(checkoutService.fetchPayment("123")).thenReturn(Optional.of(details));

        ResponseEntity<Void> response = controller.handleWebhook(payload, null, "payment");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        ArgumentCaptor<String> notificationPayloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(checkoutService).updateFromPayment(eq(details), notificationPayloadCaptor.capture());
        verify(pdePaymentEntitlementClient).notifyIfSupported(details);
        assertThat(notificationPayloadCaptor.getValue()).doesNotContain("client@example.com");

        ArgumentCaptor<MercadoPagoWebhookLog> logCaptor = ArgumentCaptor.forClass(MercadoPagoWebhookLog.class);
        verify(webhookLogRepository).save(logCaptor.capture());
        MercadoPagoWebhookLog log = logCaptor.getValue();
        assertThat(log.getProcessingStatus()).isEqualTo(WebhookProcessingStatus.PROCESSED);
        assertThat(log.getResourceId()).isEqualTo("123");
        assertThat(log.getMercadoPagoStatus()).isEqualTo("approved");
        assertThat(log.getMercadoPagoResponse())
                .contains("[REDACTED]")
                .doesNotContain("client@example.com");
        assertThat(log.getPayload()).isNotBlank();
    }

    /** Confirma sem efeito quando o provedor não encontra o pagamento informado. */
    @Test
    void shouldAckWebhookWhenPaymentNotFound() {
        MercadoPagoWebhookPayload payload = new MercadoPagoWebhookPayload();
        MercadoPagoWebhookPayload.Data data = new MercadoPagoWebhookPayload.Data();
        data.setId("999");
        payload.setData(data);

        when(checkoutService.fetchPayment("999")).thenReturn(Optional.empty());

        ResponseEntity<Void> response = controller.handleWebhook(payload, null, "payment");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(checkoutService, never()).updateFromPayment(any(), any());
        verifyNoInteractions(pdePaymentEntitlementClient);

        ArgumentCaptor<MercadoPagoWebhookLog> logCaptor = ArgumentCaptor.forClass(MercadoPagoWebhookLog.class);
        verify(webhookLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getProcessingStatus()).isEqualTo(WebhookProcessingStatus.PAYMENT_NOT_FOUND);
    }

    /** Rejeita payload sem identificador antes de chamar integrações. */
    @Test
    void shouldReturnBadRequestWhenNoIdProvided() {
        ResponseEntity<Void> response = controller.handleWebhook(null, null, "payment");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(checkoutService);
        verifyNoInteractions(pdePaymentEntitlementClient);

        ArgumentCaptor<MercadoPagoWebhookLog> logCaptor = ArgumentCaptor.forClass(MercadoPagoWebhookLog.class);
        verify(webhookLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getProcessingStatus()).isEqualTo(WebhookProcessingStatus.INVALID_REQUEST);
    }
}
