package com.marketinghub.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.payments.dto.AgendaCheiaBriefingRequest;
import com.marketinghub.payments.integration.image.AgendaCheiaPhotoGenerator;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPaymentDetails;
import com.marketinghub.payments.repository.AgendaCheiaBriefingRepository;
import com.marketinghub.payments.repository.AgendaCheiaDeliveryRepository;
import com.marketinghub.payments.service.CheckoutService;
import com.sun.net.httpserver.HttpServer;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Homologa HTTP, persistência, ZIP e contrato de entrega com provedores externos simulados. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:delivery-integration;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.profiles.active=test", "delivery.enabled=false",
    "digital-product.delivery.email.enabled=true", "product-ai.delivery.enabled=false",
    "pde.entitlement.enabled=false", "mercado-pago.access-token=local-test",
    "mercado-pago.base-url=http://127.0.0.1:9", "agenda-cheia.production.openai-api-key=",
    "agenda-cheia.production.openai-base-url=http://127.0.0.1:9",
    "logging.file.name=target/delivery-integration.log"
})
class AgendaCheiaDeliveryIntegrationTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<JsonNode> EMAILS = new CopyOnWriteArrayList<>();
    private static final AtomicBoolean FAIL_DELIVERY = new AtomicBoolean();
    private static HttpServer emailServer;
    private static Path storage;
    @Autowired private TestRestTemplate http;
    @Autowired private AgendaCheiaBriefingRepository briefings;
    @Autowired private AgendaCheiaDeliveryRepository deliveries;
    @MockBean private CheckoutService checkout;
    @MockBean private AgendaCheiaPhotoGenerator photos;

    /** Isola armazenamento e transporte de e-mail sem qualquer credencial real. */
    @DynamicPropertySource
    static void dependencies(DynamicPropertyRegistry properties) throws Exception {
        storage = Files.createTempDirectory("agenda-cheia-integration-");
        emailServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        emailServer.createContext("/api/v1/product-deliveries/send", exchange -> {
            JsonNode body = JSON.readTree(exchange.getRequestBody());
            boolean fail = FAIL_DELIVERY.get() && body.path("downloadUrl").isTextual();
            if (!fail) EMAILS.add(body);
            byte[] response = (fail ? "{}" : "{\"requestId\":\"local-email-" + EMAILS.size() + "\"}").getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(fail ? 503 : 200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        emailServer.start();
        properties.add("digital-product.delivery.email.email-service-base-url",
                () -> "http://127.0.0.1:" + emailServer.getAddress().getPort());
        properties.add("agenda-cheia.production.storage-root", storage::toString);
    }

    /** Prepara fotografias sintéticas detalhadas, sem modelo pago e sem dados de clientes. */
    @BeforeEach
    void photographs() {
        EMAILS.clear();
        FAIL_DELIVERY.set(false);
        when(photos.generate(anyString(), anyInt())).thenAnswer(call -> {
            int variant = call.getArgument(1);
            BufferedImage image = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < 1024; y++) for (int x = 0; x < 1024; x++) {
                int noise = (x * 17 + y * 31 + variant * 47) & 63;
                image.setRGB(x, y, new Color((80 + variant * 25 + noise) % 255,
                        (40 + y / 5 + noise) % 255, (90 + x / 6) % 255).getRGB());
            }
            return image;
        });
    }

    /** Disponibiliza o aviso e seu link antes da coleta, sem exigir compra nem disparar produção. */
    @Test
    void exposesPrivacyNoticeAtTheBriefingCollectionPoint() {
        var notice = http.getForEntity("/agenda-cheia/privacidade.html", String.class);
        assertThat(notice.getStatusCode().value()).isEqualTo(200);
        assertThat(notice.getBody()).contains("agenda-cheia-privacy-v1-2026-09-21",
                "25.215.414/0001-69", "contato@digicomdigital.com.br", "O que pedimos",
                "Uso e compartilhamento", "Por quanto tempo", "Como exercer seus direitos");
        String page = http.getForObject("/agenda-cheia/obrigado.html", String.class);
        assertThat(page).contains("id=\"briefing-privacy\"", "href=\"/agenda-cheia/privacidade.html\"");
        assertThat(page.indexOf("id=\"briefing-privacy\"")).isLessThan(page.indexOf("name=\"buyerEmail\""));
        org.mockito.Mockito.verifyNoInteractions(photos);
        assertThat(EMAILS).isEmpty();
    }

    /** Confirma briefing, composição, dois e-mails, download e idempotência após conclusão. */
    @Test
    void deliversRealArchiveWithoutDuplicatingCompletedPurchase() throws Exception {
        String paymentId = payment("approved");
        assertThat(http.getForObject(statusUrl(paymentId), JsonNode.class).path("status").asText())
                .isEqualTo("AGUARDANDO_BRIEFING");
        var response = http.postForEntity("/api/v1/agenda-cheia/post-purchase/briefing", request(paymentId), JsonNode.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().path("status").asText()).isEqualTo("ENTREGUE");
        var briefing = briefings.findByPaymentId(paymentId).orElseThrow();
        var delivery = deliveries.findByBriefingId(briefing.getId()).orElseThrow();
        assertThat(delivery.getStatus()).isEqualTo("ENTREGUE");
        assertThat(EMAILS).hasSize(2);
        assertThat(EMAILS.get(1).path("downloadUrl").asText()).contains(delivery.getDownloadToken());
        byte[] zip = http.getForObject("/api/v1/agenda-cheia/post-purchase/deliveries/"
                + delivery.getDownloadToken() + "/download", byte[].class);
        assertThat(zip).isEqualTo(Files.readAllBytes(Path.of(delivery.getArtifactPath())));
        int entries = 0;
        try (var input = new ZipInputStream(new ByteArrayInputStream(zip))) {
            while (input.getNextEntry() != null) entries++;
        }
        assertThat(entries).isEqualTo(24);
        http.postForEntity("/api/v1/agenda-cheia/post-purchase/briefing", request(paymentId), JsonNode.class);
        assertThat(EMAILS).hasSize(2);
        assertThat(deliveries.findByBriefingId(briefing.getId()).orElseThrow().getId()).isEqualTo(delivery.getId());
    }

    /** Falha de e-mail permanece técnica e permite recuperar o mesmo briefing sem falsa entrega. */
    @Test
    void recoversEmailFailureWithoutClaimingDeliveryBeforeSuccess() {
        String paymentId = payment("approved");
        FAIL_DELIVERY.set(true);
        assertThat(http.postForEntity("/api/v1/agenda-cheia/post-purchase/briefing", request(paymentId), String.class)
                .getStatusCode().isError()).isTrue();
        var briefing = briefings.findByPaymentId(paymentId).orElseThrow();
        var delivery = deliveries.findByBriefingId(briefing.getId()).orElseThrow();
        assertThat(delivery.getStatus()).isEqualTo("FALHA_TECNICA");
        assertThat(delivery.getDeliveredAt()).isNull();
        assertThat(http.getForObject(statusUrl(paymentId), JsonNode.class).path("status").asText())
                .isNotEqualTo("ENTREGUE");
        FAIL_DELIVERY.set(false);
        assertThat(http.postForEntity("/api/v1/agenda-cheia/post-purchase/briefing", request(paymentId), JsonNode.class)
                .getBody().path("status").asText()).isEqualTo("ENTREGUE");
        assertThat(deliveries.findByBriefingId(briefing.getId()).orElseThrow().getId()).isEqualTo(delivery.getId());
    }

    /** Pagamento pendente nunca inicia produção, envio ou persistência de briefing. */
    @Test
    void refusesUnapprovedPayment() {
        String paymentId = payment("pending");
        assertThat(http.postForEntity("/api/v1/agenda-cheia/post-purchase/briefing", request(paymentId), String.class)
                .getStatusCode().isError()).isTrue();
        assertThat(briefings.findByPaymentId(paymentId)).isEmpty();
        assertThat(EMAILS).isEmpty();
    }

    /** Simula somente pagamentos desta execução local, sem alterar provider ou ambiente publicado. */
    private String payment(String status) {
        String id = "local-" + UUID.randomUUID();
        when(checkout.fetchPayment(id)).thenReturn(Optional.of(new MercadoPagoPaymentDetails(id, status,
                new BigDecimal("67.00"), "BRL", "Kit personalizado", "teste+" + id + "@sandbox.local",
                "agenda-cheia-nail-design", Instant.now(), Map.of(), "{}")));
        return id;
    }

    /** Monta entradas sintéticas completas com identificador independente a cada caso. */
    private AgendaCheiaBriefingRequest request(String id) {
        return new AgendaCheiaBriefingRequest(id, "teste+" + id + "@sandbox.local", "Studio Teste",
                "Cidade Teste", "11999999999", "Alongamento e manutenção", "Clean", "Rosa",
                "Organizar conteúdo semanal", "Homologação local isolada");
    }

    /** Retorna o endpoint público de consulta do próximo passo do pagamento sintético. */
    private String statusUrl(String id) {
        return "/api/v1/agenda-cheia/post-purchase?payment_id=" + id;
    }

    /** Remove servidor e arquivos temporários para não compartilhar dados entre execuções. */
    @AfterAll
    static void cleanup() throws Exception {
        if (emailServer != null) emailServer.stop(0);
        if (storage != null) try (var files = Files.walk(storage)) {
            for (Path path : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
