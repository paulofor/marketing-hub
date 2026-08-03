package com.marketinghub.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.payments.config.MercadoPagoProperties;
import com.marketinghub.payments.dto.TemporaryCheckoutRequest;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoClient;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPreferenceResponse;
import com.marketinghub.payments.model.TemporaryCheckout;
import com.marketinghub.payments.repository.TemporaryCheckoutRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida ativação, resolução e restauração segura de checkouts temporários. */
@ExtendWith(MockitoExtension.class)
class TemporaryCheckoutServiceTest {
    @Mock private MercadoPagoClient mercadoPagoClient;
    @Mock private TemporaryCheckoutRepository repository;

    /** Deve criar a preferência de teste preservando o destino comercial. */
    @Test
    void activatesTemporaryCheckout() {
        MercadoPagoProperties properties = new MercadoPagoProperties();
        when(mercadoPagoClient.createPreference(any()))
                .thenReturn(new MercadoPagoPreferenceResponse("pref-test", "https://mp.test/checkout"));
        when(repository.findByProductKey("agenda-cheia-nail-design")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        TemporaryCheckoutService service = new TemporaryCheckoutService(
                mercadoPagoClient, properties, repository, "https://pagamentopalf.site");

        var response = service.activate(new TemporaryCheckoutRequest(
                "agenda-cheia-nail-design", "Agenda Cheia Nail Design", new BigDecimal("0.67"),
                "https://www.mercadopago.com.br/commercial", 60));

        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.redirectUrl()).endsWith("/agenda-cheia-nail-design/redirect");
        assertThat(response.commercialCheckoutUrl()).contains("commercial");
        verify(repository).save(any(TemporaryCheckout.class));
    }

    /** Deve voltar ao checkout comercial quando o período de teste expirou. */
    @Test
    void resolvesCommercialCheckoutAfterExpiration() {
        TemporaryCheckout checkout = new TemporaryCheckout();
        checkout.setProductKey("agenda-cheia-nail-design");
        checkout.setProductName("Agenda Cheia Nail Design");
        checkout.setCommercialCheckoutUrl("https://www.mercadopago.com.br/commercial");
        checkout.setTemporaryCheckoutUrl("https://www.mercadopago.com.br/test");
        checkout.setMercadoPagoPreferenceId("pref-test");
        checkout.setTestAmount(new BigDecimal("0.67"));
        checkout.setActivatedAt(Instant.now().minusSeconds(600));
        checkout.setExpiresAt(Instant.now().minusSeconds(60));
        when(repository.findByProductKey("agenda-cheia-nail-design")).thenReturn(Optional.of(checkout));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        TemporaryCheckoutService service = new TemporaryCheckoutService(
                mercadoPagoClient, new MercadoPagoProperties(), repository, "https://pagamentopalf.site");

        URI destination = service.resolveDestination("agenda-cheia-nail-design");

        assertThat(destination).isEqualTo(URI.create("https://www.mercadopago.com.br/commercial"));
        assertThat(checkout.getRestoredAt()).isNotNull();
    }
}
