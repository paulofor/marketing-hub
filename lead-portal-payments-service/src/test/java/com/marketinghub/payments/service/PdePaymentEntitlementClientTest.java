package com.marketinghub.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marketinghub.payments.config.PdeEntitlementProperties;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPaymentDetails;
import com.sun.net.httpserver.HttpServer;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/** Valida a publicação autenticada e bloqueante do pagamento real para o entitlement PDE. */
class PdePaymentEntitlementClientTest {
    private HttpServer server;

    /** Encerra o servidor HTTP isolado após cada contrato exercitado. */
    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    /** Comprova que o Spring seleciona o construtor produtivo durante o bootstrap real do bean. */
    @Test
    void startsInSpringContextWithProductionConstructor() {
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext()) {
            context.register(PdeEntitlementProperties.class, PdePaymentEntitlementClient.class);
            context.refresh();

            assertThat(context.getBean(PdePaymentEntitlementClient.class)).isNotNull();
        }
    }

    /** Envia aprovação do Kit com segredo interno, URL exata e payload financeiro completo. */
    @Test
    void publishesSupportedPaymentWithDedicatedCredential() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/entitlements", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
        });
        server.start();
        PdeEntitlementProperties properties = properties(server.getAddress().getPort());

        new PdePaymentEntitlementClient(properties).notifyIfSupported(payment("approved"));

        assertThat(authorization.get()).isEqualTo("Bearer entitlement-test-token");
        assertThat(body.get())
                .contains("\"paymentId\":\"mp-271\"")
                .contains("\"externalReference\":\"kit-whatsapp-pronto\"")
                .contains("\"experimentId\":89")
                .doesNotContain("entitlement-test-token");
    }

    /** Ignora produto alheio sem gerar tráfego ou efeito financeiro no PDE. */
    @Test
    void ignoresUnsupportedProduct() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/entitlements", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
        });
        server.start();
        PdeEntitlementProperties properties = properties(server.getAddress().getPort());
        MercadoPagoPaymentDetails other = new MercadoPagoPaymentDetails(
                "mp-other",
                "approved",
                new BigDecimal("349.00"),
                "BRL",
                "Outro produto",
                "buyer@sandbox.local",
                "outro-produto",
                Instant.parse("2026-08-30T04:00:00Z"),
                Map.of(),
                "{}");

        new PdePaymentEntitlementClient(properties).notifyIfSupported(other);

        assertThat(requests).hasValue(0);
    }

    /** Propaga indisponibilidade do PDE para que o Mercado Pago repita o webhook. */
    @Test
    void failsWebhookWhenEntitlementCannotBePersisted() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/entitlements", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();
        PdeEntitlementProperties properties = properties(server.getAddress().getPort());

        assertThatThrownBy(() -> new PdePaymentEntitlementClient(properties)
                        .notifyIfSupported(payment("refunded")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("entitlement pago");
    }

    /** Monta as propriedades isoladas sem reutilizar credenciais externas. */
    private PdeEntitlementProperties properties(int port) {
        PdeEntitlementProperties properties = new PdeEntitlementProperties();
        properties.setBackendBaseUrl("http://127.0.0.1:" + port);
        properties.setNotificationPath("/entitlements");
        properties.setInternalToken("entitlement-test-token");
        return properties;
    }

    /** Cria uma resposta normalizada equivalente à consulta real do Mercado Pago. */
    private MercadoPagoPaymentDetails payment(String status) {
        return new MercadoPagoPaymentDetails(
                "mp-271",
                status,
                new BigDecimal("349.00"),
                "BRL",
                "Kit WhatsApp Pronto",
                "buyer@sandbox.local",
                "kit-whatsapp-pronto",
                Instant.parse("2026-08-30T04:00:00Z"),
                Map.of("productKey", "kit-whatsapp-pronto", "productId", 9, "experimentId", 89),
                "{\"id\":\"mp-271\"}");
    }
}
