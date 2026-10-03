package com.marketinghub.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    /** Recusa a profissão divergente antes de alterar o briefing ou produzir qualquer arquivo. */
    @Test
    void refusesDifferentProfessionBeforePersistenceAndProduction() {
        var payment = new MercadoPagoPaymentDetails("qa-mismatch", "approved", new BigDecimal("67"),
                "BRL", "Agenda Cheia", "teste+mismatch@sandbox.local", "agenda-cheia-nail-design",
                Instant.now(), Map.of(), "{}");
        when(checkoutService.fetchPayment("qa-mismatch")).thenReturn(Optional.of(payment));
        var service = new AgendaCheiaPostPurchaseService(checkoutService, repository, emailService, productionService);
        assertThatThrownBy(() -> service.submit(new AgendaCheiaBriefingRequest("qa-mismatch",
                "teste+mismatch@sandbox.local", "Barbearia QA", "Cidade QA", "11999999999", "Corte",
                "Moderno", "Preto", "Divulgar corte", null, "barber-v1")))
                .hasMessageContaining("difere do kit comprado");
        verifyNoInteractions(repository, emailService, productionService);
    }

    /** A consulta informa a profissão da oferta confirmada, sem inferir pelo texto livre. */
    @Test
    void exposesPurchasedProfessionAndServiceExamples() {
        var payment = new MercadoPagoPaymentDetails("qa-options", "approved", new BigDecimal("67"),
                "BRL", "Descrição livre de barbearia", "teste+options@sandbox.local", "agenda-cheia-nail-design",
                Instant.now(), Map.of(), "{}");
        when(checkoutService.fetchPayment("qa-options")).thenReturn(Optional.of(payment));
        when(repository.findByPaymentId("qa-options")).thenReturn(Optional.empty());
        var service = new AgendaCheiaPostPurchaseService(checkoutService, repository, emailService, productionService);
        assertThat(service.paymentStatus("qa-options").profile().code()).isEqualTo("nails-v1");
        assertThat(service.paymentStatus("qa-options").profile().serviceExamples()).contains("Alongamento");
        verifyNoInteractions(emailService, productionService);
    }

    /** Deve retornar a entrega existente sem alterar briefing nem reenviar emails. */
    @Test
    void preservesCompletedDeliveryOnDuplicateBriefing() {
        MercadoPagoPaymentDetails payment = new MercadoPagoPaymentDetails(
                "pay-67", "approved", new BigDecimal("0.67"), "BRL", "Agenda Cheia",
                "buyer@example.com", "agenda-cheia-nail-design", Instant.now(), Map.of(), "{}");
        AgendaCheiaBriefing delivered = new AgendaCheiaBriefing();
        delivered.setPaymentId("pay-67");
        delivered.setStatus("ENTREGUE");
        delivered.setSubmittedAt(Instant.parse("2026-08-20T12:00:00Z"));
        when(checkoutService.fetchPayment("pay-67")).thenReturn(Optional.of(payment));
        when(repository.findByPaymentId("pay-67")).thenReturn(Optional.of(delivered));
        AgendaCheiaPostPurchaseService service =
                new AgendaCheiaPostPurchaseService(checkoutService, repository, emailService, productionService);

        var response = service.submit(new AgendaCheiaBriefingRequest(
                "pay-67", "buyer@example.com", "Studio alterado", "Campinas", "11999999999",
                "Alongamento", "Clean", "Rosa", "Preencher horários", null));

        assertThat(response.status()).isEqualTo("ENTREGUE");
        assertThat(response.submittedAt()).isEqualTo(Instant.parse("2026-08-20T12:00:00Z"));
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any(AgendaCheiaBriefing.class));
        verify(emailService, never()).sendToRecipient(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
        verify(productionService, never()).produceAndDeliver(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
