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

@ExtendWith(MockitoExtension.class)
class MercadoPagoWebhookControllerTest {

    @Mock
    private CheckoutService checkoutService;

    @Mock
    private MercadoPagoWebhookLogRepository webhookLogRepository;

    private MercadoPagoWebhookController controller;

    @BeforeEach
    void setUp() {
        when(webhookLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        controller = new MercadoPagoWebhookController(checkoutService, new ObjectMapper(), webhookLogRepository);
    }

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
                Instant.now(),
                Map.of("packageId", 1L),
                "{\"id\":\"123\"}"
        );

        when(checkoutService.fetchPayment("123")).thenReturn(Optional.of(details));

        ResponseEntity<Void> response = controller.handleWebhook(payload, null, "payment");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(checkoutService).updateFromPayment(eq(details), any());

        ArgumentCaptor<MercadoPagoWebhookLog> logCaptor = ArgumentCaptor.forClass(MercadoPagoWebhookLog.class);
        verify(webhookLogRepository).save(logCaptor.capture());
        MercadoPagoWebhookLog log = logCaptor.getValue();
        assertThat(log.getProcessingStatus()).isEqualTo(WebhookProcessingStatus.PROCESSED);
        assertThat(log.getResourceId()).isEqualTo("123");
        assertThat(log.getMercadoPagoStatus()).isEqualTo("approved");
        assertThat(log.getMercadoPagoResponse()).isEqualTo("{\"id\":\"123\"}");
        assertThat(log.getPayload()).isNotBlank();
    }

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

        ArgumentCaptor<MercadoPagoWebhookLog> logCaptor = ArgumentCaptor.forClass(MercadoPagoWebhookLog.class);
        verify(webhookLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getProcessingStatus()).isEqualTo(WebhookProcessingStatus.PAYMENT_NOT_FOUND);
    }

    @Test
    void shouldReturnBadRequestWhenNoIdProvided() {
        ResponseEntity<Void> response = controller.handleWebhook(null, null, "payment");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(checkoutService);

        ArgumentCaptor<MercadoPagoWebhookLog> logCaptor = ArgumentCaptor.forClass(MercadoPagoWebhookLog.class);
        verify(webhookLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getProcessingStatus()).isEqualTo(WebhookProcessingStatus.INVALID_REQUEST);
    }
}
