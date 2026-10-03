package com.marketinghub.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.payments.integration.image.AgendaCheiaPhotoGenerator;
import com.marketinghub.payments.integration.image.ApprovedAgendaCheiaPhotoLibrary;
import com.marketinghub.payments.model.AgendaCheiaBriefing;
import com.marketinghub.payments.repository.AgendaCheiaDeliveryRepository;
import com.marketinghub.payments.service.kit.CapellaKitCatalog;
import com.marketinghub.payments.service.kit.CapellaKitText;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Homologa candidatas por profissão com acervo sintético segregado e sem venda ou chamada paga. */
class CapellaKitCandidateTest {
    @TempDir Path storage;

    /** Compõe todo o pacote de barbearia usando a biblioteca do perfil, sem persistir nem enviar. */
    @Test
    void preparesBarberKitWithoutNailCopySalesOrEmail() throws Exception {
        Path library = storage.resolve("approved");
        syntheticLibrary(library.resolve("barber-v1"), "barber-v1");
        var repository = mock(AgendaCheiaDeliveryRepository.class);
        var email = mock(DigitalProductPostPurchaseEmailService.class);
        var service = new AgendaCheiaKitProductionService(repository, email, new ObjectMapper(),
                new ApprovedAgendaCheiaPhotoLibrary(library.toString()), storage.resolve("kits").toString(),
                "http://localhost");
        var result = service.prepareCandidate(briefing("Barbearia QA Horizonte", "Corte degradê e barba"), "barber-v1");
        assertThat(result.qualityScore()).isEqualTo(100);
        assertThat(result.profileCode()).isEqualTo("barber-v1");
        try (ZipFile zip = new ZipFile(result.zipPath().toFile())) {
            assertThat(zip.size()).isEqualTo(24);
            String captions = content(zip, "legendas-prontas.txt");
            assertThat(captions).contains("Corte degradê e barba", "Cidade QA", "Um visual com seu estilo")
                    .doesNotContain("unhas", "nail", "alongamento");
            assertThat(content(zip, "calendario-7-dias.txt")).contains("corte ou barba", "autorizaram contato");
            assertThat(content(zip, "LEIA-ME.txt")).contains("CAPELLA", "BARBEARIA", "não garante clientes", "7 dias corridos");
            for (int index = 1; index <= 10; index++) {
                assertThat(ImageIO.read(zip.getInputStream(zip.getEntry("post-%02d.png".formatted(index)))).getWidth()).isEqualTo(1080);
                assertThat(ImageIO.read(zip.getInputStream(zip.getEntry("story-%02d.png".formatted(index)))).getHeight()).isEqualTo(1920);
            }
        }
        verifyNoInteractions(repository, email);
        preserveEvidence(result.zipPath(), library);
    }

    /** Mantém unhas disponíveis sem transformar a candidata em referência aceita no pagamento. */
    @Test
    void preservesLegacyIdentityAndRefusesUnapprovedCommercialProfile() {
        assertThat(CapellaKitCatalog.forPaymentReference("agenda-cheia-nail-design").code()).isEqualTo("nails-v1");
        assertThatThrownBy(() -> CapellaKitCatalog.forPaymentReference("capella-barbearia-v1"))
                .hasMessageContaining("homologada");
        assertThatThrownBy(() -> CapellaKitCatalog.byCode("../nails-v1"))
                .hasMessageContaining("não suportado");
    }

    /** Um acervo de unhas completo nunca serve como fallback para uma candidata de barbearia. */
    @Test
    void blocksBeforeProducingWhenOnlyNailLibraryExists() throws Exception {
        syntheticLibrary(storage, "nails-v1");
        var library = new ApprovedAgendaCheiaPhotoLibrary(storage.toString());
        assertThatThrownBy(() -> library.generate("qa-independent", 0, CapellaKitCatalog.byCode("barber-v1")))
                .hasMessageContaining("não está disponível para barber-v1");
    }

    /** A porta antiga de geração recusa outro perfil antes de executar qualquer implementação. */
    @Test
    void refusesLegacyGeneratorForNewProfessionBeforeAnyPaidFallback() {
        AgendaCheiaPhotoGenerator generator = (id, variant) -> { throw new AssertionError("Geração não autorizada"); };
        assertThatThrownBy(() -> generator.generate("qa-other", 0, CapellaKitCatalog.byCode("barber-v1")))
                .hasMessageContaining("não homologada");
    }

    /** Outra entrada independente recebe conteúdo do serviço correto e mantém sanitização pública. */
    @Test
    void personalizesIndependentBriefingWithoutTechnicalMetadata() {
        var texts = new CapellaKitText();
        var input = briefing("Outro QA", "Barba; corte");
        input.setCityRegion("Cidade debug payload QA");
        assertThat(String.join("\n", texts.captions(input, CapellaKitCatalog.byCode("barber-v1"))))
                .contains("Barba em Cidade QA").doesNotContain("debug", "payload", "unhas", "corte");
        assertThat(texts.instructions(input, CapellaKitCatalog.byCode("nails-v1")))
                .startsWith("AGENDA CHEIA NAIL DESIGN");
    }

    /** Cria uma entrada fictícia, sem dado de cliente ou identificador produtivo. */
    private AgendaCheiaBriefing briefing(String name, String services) {
        var briefing = new AgendaCheiaBriefing();
        briefing.setProfessionalName(name);
        briefing.setBuyerEmail("teste+capella-barber@sandbox.local");
        briefing.setCityRegion("Cidade QA");
        briefing.setWhatsapp("11999999999");
        briefing.setServices(services);
        briefing.setVisualStyle("Moderno e marcante");
        briefing.setPreferredColors("Preto e dourado");
        briefing.setWeeklyGoal("Divulgar um serviço específico");
        return briefing;
    }

    /** Cria dez imagens de QA com variação, sem retratar trabalho ou cliente real. */
    private void syntheticLibrary(Path root, String profile) throws Exception {
        Files.createDirectories(root);
        var manifest = new StringBuilder("# profile=" + profile + "\n");
        for (int variant = 0; variant < 10; variant++) {
            var image = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < 1024; y++) for (int x = 0; x < 1024; x++) {
                int noise = (x * 17 + y * 31 + variant * 47) & 63;
                image.setRGB(x, y, new Color((80 + variant * 25 + noise) % 255,
                        (40 + y / 5 + noise) % 255, (90 + x / 6) % 255).getRGB());
            }
            var graphics = image.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.drawString("QA SINTETICO — NAO E FOTOGRAFIA COMERCIAL", 100, 250);
            graphics.dispose();
            Path file = root.resolve("qa-%02d.png".formatted(variant));
            ImageIO.write(image, "png", file.toFile());
            String hash = HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
            manifest.append(file.getFileName()).append('\t').append(hash).append("\tQA_FIXTURE\t9.5\tAPPROVED\tfalse\tQA segregado\n");
        }
        Files.writeString(root.resolve("approved-manifest.tsv"), manifest);
    }

    /** Lê o texto funcional de um arquivo do pacote composto. */
    private String content(ZipFile zip, String filename) throws Exception {
        return new String(zip.getInputStream(zip.getEntry(filename)).readAllBytes(), StandardCharsets.UTF_8);
    }

    /** Guarda evidência somente quando o runner local informa explicitamente um destino de QA. */
    private void preserveEvidence(Path kit, Path library) throws Exception {
        String directory = System.getProperty("capella.validation.output");
        if (directory == null) return;
        Path output = Path.of(directory);
        Files.createDirectories(output);
        Files.copy(kit, output.resolve("barber-qa-kit.zip"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        try (var files = Files.list(library.resolve("barber-v1"))) {
            for (Path file : files.toList()) {
                Path target = output.resolve("approved/barber-v1").resolve(file.getFileName());
                Files.createDirectories(target.getParent());
                Files.copy(file, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
