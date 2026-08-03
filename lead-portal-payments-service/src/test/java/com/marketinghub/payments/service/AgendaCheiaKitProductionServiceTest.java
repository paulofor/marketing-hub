package com.marketinghub.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.payments.integration.mercadopago.MercadoPagoPaymentDetails;
import com.marketinghub.payments.integration.image.AgendaCheiaPhotoGenerator;
import java.awt.Color;
import java.awt.image.BufferedImage;
import com.marketinghub.payments.model.AgendaCheiaBriefing;
import com.marketinghub.payments.model.AgendaCheiaDelivery;
import com.marketinghub.payments.repository.AgendaCheiaDeliveryRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/** Valida a produção, o gate visual e a entrega do kit personalizado. */
class AgendaCheiaKitProductionServiceTest {
    @TempDir Path storage;

    /** Deve produzir o pacote completo e somente então marcá-lo como entregue. */
    @Test
    void producesReviewsAndDeliversCompleteKit() throws Exception {
        AgendaCheiaDeliveryRepository repository = org.mockito.Mockito.mock(AgendaCheiaDeliveryRepository.class);
        DigitalProductPostPurchaseEmailService emailService =
                org.mockito.Mockito.mock(DigitalProductPostPurchaseEmailService.class);
        when(repository.findByBriefingId(7L)).thenReturn(Optional.empty());
        when(repository.save(any(AgendaCheiaDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AgendaCheiaKitProductionService service = new AgendaCheiaKitProductionService(
                repository, emailService, new ObjectMapper(), photographicGenerator(), storage.toString(), "https://pagamentos.example");
        AgendaCheiaBriefing briefing = briefing();
        MercadoPagoPaymentDetails payment = new MercadoPagoPaymentDetails(
                "pay-67", "approved", new BigDecimal("0.67"), "BRL", "Agenda Cheia",
                "buyer@example.com", "agenda-cheia-nail-design", Instant.now(), Map.of(), "{}");

        var response = service.produceAndDeliver(briefing, payment);

        assertThat(response.status()).isEqualTo("ENTREGUE");
        assertThat(response.qualityScore()).isEqualTo(100);
        assertThat(response.downloadUrl()).contains("/deliveries/").endsWith("/download");
        ArgumentCaptor<AgendaCheiaDelivery> captor = ArgumentCaptor.forClass(AgendaCheiaDelivery.class);
        verify(repository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        Path zipPath = Path.of(captor.getValue().getArtifactPath());
        assertThat(Files.isRegularFile(zipPath)).isTrue();
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            assertThat(zip.size()).isEqualTo(24);
            assertThat(zip.getEntry("post-01.png")).isNotNull();
            assertThat(zip.getEntry("story-10.png")).isNotNull();
            assertThat(zip.getEntry("legendas-prontas.txt")).isNotNull();
            assertThat(zip.getEntry("mensagens-whatsapp.txt")).isNotNull();
        }
        verify(emailService).sendCompletedKit(
                org.mockito.ArgumentMatchers.eq(payment),
                org.mockito.ArgumentMatchers.eq("buyer@example.com"),
                org.mockito.ArgumentMatchers.eq("Studio Ana"),
                org.mockito.ArgumentMatchers.contains("/download"));
    }

    /** Deve bloquear um kit composto por imagens planas e visualmente repetidas. */
    @Test
    void rejectsFlatAndRepeatedImages() {
        AgendaCheiaDeliveryRepository repository = org.mockito.Mockito.mock(AgendaCheiaDeliveryRepository.class);
        DigitalProductPostPurchaseEmailService emailService = org.mockito.Mockito.mock(DigitalProductPostPurchaseEmailService.class);
        when(repository.findByBriefingId(7L)).thenReturn(Optional.empty());
        when(repository.save(any(AgendaCheiaDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AgendaCheiaPhotoGenerator flat = (executionId, variant) -> {
            BufferedImage image = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics();
            graphics.setColor(Color.PINK);
            graphics.fillRect(0, 0, 1024, 1024);
            graphics.dispose();
            return image;
        };
        AgendaCheiaKitProductionService service = new AgendaCheiaKitProductionService(
                repository, emailService, new ObjectMapper(), flat, storage.toString(), "https://pagamentos.example");
        MercadoPagoPaymentDetails payment = new MercadoPagoPaymentDetails(
                "pay-67", "approved", new BigDecimal("0.67"), "BRL", "Agenda Cheia",
                "buyer@example.com", "agenda-cheia-nail-design", Instant.now(), Map.of(), "{}");

        assertThatThrownBy(() -> service.produceAndDeliver(briefing(), payment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Não foi possível concluir o kit personalizado");
    }

    /** Deve manter no prompt o protagonismo das unhas e a diversidade visual exigida para venda. */
    @Test
    void requiresNailsAsThePhotographicHeroAndRealVariation() throws Exception {
        String prompt = new String(getClass().getResourceAsStream(
                "/prompts/agenda-cheia/nail-photo.md").readAllBytes(), StandardCharsets.UTF_8);

        assertThat(prompt).contains("35% a 55% do enquadramento")
                .contains("imediatamente reconhecíveis no celular")
                .contains("Varie de verdade pose, ângulo de câmera, cor dominante, fundo e contexto")
                .contains("sem reduzir o protagonismo das unhas");
    }

    /** Simula fotografias variadas e detalhadas sem chamar provedor externo no teste. */
    private AgendaCheiaPhotoGenerator photographicGenerator() {
        return (executionId, variant) -> {
            BufferedImage image = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < 1024; y++) for (int x = 0; x < 1024; x++) {
                int noise = (x * 17 + y * 31 + variant * 47) & 63;
                image.setRGB(x, y, new Color((80 + variant * 25 + noise) % 255, (40 + y / 5 + noise) % 255, (90 + x / 6) % 255).getRGB());
            }
            return image;
        };
    }

    /** Monta um briefing completo para o cenário de produção. */
    private AgendaCheiaBriefing briefing() {
        AgendaCheiaBriefing briefing = new AgendaCheiaBriefing();
        ReflectionTestUtils.setField(briefing, "id", 7L);
        briefing.setPaymentId("pay-67");
        briefing.setBuyerEmail("buyer@example.com");
        briefing.setProfessionalName("Studio Ana");
        briefing.setCityRegion("Campinas");
        briefing.setWhatsapp("11999999999");
        briefing.setServices("Alongamento em gel; manutenção");
        briefing.setVisualStyle("Clean e elegante");
        briefing.setPreferredColors("Rosa e vinho");
        briefing.setWeeklyGoal("Preencher horários vagos");
        briefing.setStatus("BRIEFING_RECEBIDO");
        briefing.setSubmittedAt(Instant.now());
        return briefing;
    }
}
