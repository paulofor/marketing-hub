package com.marketinghub.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.payments.config.MercadoPagoProperties;
import com.marketinghub.payments.dto.CommercialProductCheckoutRequest;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoClient;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPreferenceRequest;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPreferenceResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida a criação segura do checkout comercial de um produto digital. */
@ExtendWith(MockitoExtension.class)
class CommercialProductCheckoutServiceTest {

    @Mock private MercadoPagoClient mercadoPagoClient;
    private CommercialProductCheckoutService service;

    /** Configura o serviço isolado antes de cada cenário. */
    @BeforeEach
    void setUp() {
        service = new CommercialProductCheckoutService(mercadoPagoClient, new MercadoPagoProperties());
    }

    /** Preserva preço, entrega e correlação no payload enviado ao Mercado Pago. */
    @Test
    void createsCheckoutFromValidatedCommercialContract() {
        when(mercadoPagoClient.createPreference(any(), anyString()))
                .thenReturn(new MercadoPagoPreferenceResponse("pref-kit", "https://mercadopago.com/kit"));

        var response = service.create(new CommercialProductCheckoutRequest(
                "kit-whatsapp-pronto",
                "Kit WhatsApp Pronto",
                9L,
                89L,
                new BigDecimal("349.00"),
                "https://kit-whatsapp-pronto.digicomdigital.com.br"));

        assertThat(response.checkoutUrl()).isEqualTo("https://mercadopago.com/kit");
        assertThat(response.amount()).isEqualByComparingTo("349.00");
        ArgumentCaptor<MercadoPagoPreferenceRequest> captor =
                ArgumentCaptor.forClass(MercadoPagoPreferenceRequest.class);
        ArgumentCaptor<String> idempotencyKey = ArgumentCaptor.forClass(String.class);
        verify(mercadoPagoClient).createPreference(captor.capture(), idempotencyKey.capture());
        assertThat(idempotencyKey.getValue()).isEqualTo("a80b2c6f-d0e3-3adc-b491-6784edcf6aa4");
        assertThat(captor.getValue().externalReference()).isEqualTo("kit-whatsapp-pronto");
        assertThat(captor.getValue().metadata())
                .containsEntry("productId", 9L)
                .containsEntry("experimentId", 89L)
                .containsEntry("delivery_url", "https://kit-whatsapp-pronto.digicomdigital.com.br");
    }

    /** Bloqueia página de entrega insegura antes de chamar o provedor. */
    @Test
    void rejectsNonHttpsDeliveryPage() {
        assertThatThrownBy(() -> service.create(new CommercialProductCheckoutRequest(
                        "kit-whatsapp-pronto",
                        "Kit WhatsApp Pronto",
                        9L,
                        89L,
                        new BigDecimal("349.00"),
                        "http://localhost/acesso")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTPS");
    }
}
