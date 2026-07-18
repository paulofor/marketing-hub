package com.marketinghub.imagegenerator.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.imagegenerator.dto.ImageGeneratorResponse.ImageGeneratorVariant;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ImageDerivativeServiceTest {
    private final ImageDerivativeService service = new ImageDerivativeService();

    /** Garante que uma imagem original gere versões web e mobile leves para uso em PDEs. */
    @Test
    void createsWebAndMobileVariants() throws Exception {
        String imageBase64 = createPngBase64(1800, 1200);

        List<ImageGeneratorVariant> variants = service.createVariants("png", imageBase64);

        assertThat(variants).extracting(ImageGeneratorVariant::role).containsExactly("original", "web", "mobile");
        assertThat(variants.get(0).format()).isEqualTo("png");
        assertThat(variants.get(1).format()).isEqualTo("jpeg");
        assertThat(variants.get(1).width()).isEqualTo(1600);
        assertThat(variants.get(2).width()).isEqualTo(900);
        assertThat(variants.get(1).byteSize()).isPositive();
        assertThat(variants.get(2).byteSize()).isLessThan(variants.get(1).byteSize());
    }

    /** Cria uma imagem PNG sintética para validar o processamento sem chamar provedor externo. */
    private String createPngBase64(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(120, 40, 180));
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(Color.WHITE);
            graphics.fillOval(120, 120, 480, 480);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return Base64.getEncoder().encodeToString(output.toByteArray());
    }
}
