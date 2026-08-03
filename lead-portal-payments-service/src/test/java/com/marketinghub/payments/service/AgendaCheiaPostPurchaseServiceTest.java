package com.marketinghub.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.payments.dto.AgendaCheiaBriefingRequest;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPaymentDetails;
import com.marketinghub.payments.model.AgendaCheiaBriefing;
import com.marketinghub.payments.repository.AgendaCheiaBriefingRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida o gate de pagamento e a persistência do briefing pós-compra. */
@ExtendWith(MockitoExtension.class)
class AgendaCheiaPostPurchaseServiceTest {
    @Mock private CheckoutService checkoutService;
    @Mock private AgendaCheiaBriefingRepository repository;
    @Mock private DigitalProductPostPurchaseEmailService emailService;
    @Mock private AgendaCheiaKitProductionService productionService;

    /** Deve aceitar o briefing somente para pagamento aprovado do Agenda Cheia. */
    @Test
    void submitsBriefingForApprovedAgendaCheiaPayment() {
        MercadoPagoPaymentDetails payment = new MercadoPagoPaymentDetails(
                "pay-67", "approved", new BigDecimal("0.67"), "BRL", "Agenda Cheia",
                "buyer@example.com", "agenda-cheia-nail-design", Instant.now(), Map.of(), "{}");
        when(checkoutService.fetchPayment("pay-67")).thenReturn(Optional.of(payment));
        when(repository.findByPaymentId("pay-67")).thenReturn(Optional.empty());
        when(repository.save(org.mockito.ArgumentMatchers.any(AgendaCheiaBriefing.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        AgendaCheiaPostPurchaseService service =
                new AgendaCheiaPostPurchaseService(checkoutService, repository, emailService, productionService);

        var response = service.submit(new AgendaCheiaBriefingRequest(
                "pay-67", "buyer@example.com", "Studio Ana", "Campinas", "11999999999",
                "Alongamento e manutenção", "Clean e elegante", "Rosa", "Preencher horários vagos", null));

        assertThat(response.status()).isEqualTo("ENTREGUE");
        assertThat(response.paymentId()).isEqualTo("pay-67");
        verify(emailService).sendToRecipient(payment, "buyer@example.com", "Studio Ana");
        verify(productionService).produceAndDeliver(org.mockito.ArgumentMatchers.any(AgendaCheiaBriefing.class),
                org.mockito.ArgumentMatchers.eq(payment));
    }
}
