package com.marketinghub.payments.integration.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Protege o contrato de seleção da biblioteca fotográfica aprovada. */
class ApprovedAgendaCheiaPhotoLibraryTest {
    @TempDir Path storage;

    /** Deve selecionar dez fotografias distintas sem gerar imagens durante a compra. */
    @Test
    void selectsTenDistinctApprovedPhotos() throws Exception {
        for (int index = 0; index < 10; index++) writeImage(index);
        writeManifest(10);
        ApprovedAgendaCheiaPhotoLibrary library = new ApprovedAgendaCheiaPhotoLibrary(storage.toString());

        java.util.Set<Integer> colors = new java.util.HashSet<>();
        for (int variant = 0; variant < 10; variant++) {
            colors.add(library.generate("purchase-123", variant).getRGB(0, 0));
        }

        assertThat(colors).hasSize(10);
    }

    /** Deve bloquear a entrega quando o acervo aprovado não sustenta um kit completo. */
    @Test
    void rejectsInsufficientApprovedLibrary() throws Exception {
        for (int index = 0; index < 9; index++) writeImage(index);
        writeManifest(9);
        ApprovedAgendaCheiaPhotoLibrary library = new ApprovedAgendaCheiaPhotoLibrary(storage.toString());

        assertThatThrownBy(() -> library.generate("purchase-123", 0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ao menos 10 imagens");
    }

    /** Deve bloquear arquivo alterado depois da revisão humana. */
    @Test
    void rejectsPhotoChangedAfterApproval() throws Exception {
        for (int index = 0; index < 10; index++) writeImage(index);
        writeManifest(10);
        Files.writeString(storage.resolve("approved-00.png"), "alterada");

        ApprovedAgendaCheiaPhotoLibrary library = new ApprovedAgendaCheiaPhotoLibrary(storage.toString());

        assertThatThrownBy(() -> library.generate("purchase-123", 0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sem aprovação auditável");
    }

    /** Cria uma fotografia raster válida e identificável para o teste. */
    private void writeImage(int index) throws Exception {
        BufferedImage image = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(new Color(index * 20, 30 + index, 80 + index));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();
        ImageIO.write(image, "png", storage.resolve("approved-%02d.png".formatted(index)).toFile());
    }

    /** Registra o manifesto humano auditável das fotografias do teste. */
    private void writeManifest(int count) throws Exception {
        StringBuilder manifest = new StringBuilder();
        for (int index = 0; index < count; index++) {
            Path image = storage.resolve("approved-%02d.png".formatted(index));
            String hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(Files.readAllBytes(image)));
            manifest.append(image.getFileName()).append('\t').append(hash)
                    .append("\tgpt-image-2-2026-04-21\t9.5\tAPPROVED\tfalse\tteste\n");
        }
        Files.writeString(storage.resolve("approved-manifest.tsv"), manifest.toString());
    }
}
