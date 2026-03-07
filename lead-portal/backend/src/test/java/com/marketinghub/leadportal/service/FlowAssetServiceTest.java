package com.marketinghub.leadportal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.marketinghub.leadportal.config.LegacyAssetsProperties;
import com.marketinghub.leadportal.config.StorageProperties;
import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.model.FlowQuestion;
import com.marketinghub.leadportal.model.FlowQuestionType;
import com.marketinghub.leadportal.model.SimpleFormStyle;
import com.marketinghub.leadportal.model.SimpleFormStyleDefinition;
import com.marketinghub.leadportal.service.LegacyAssetClient.DownloadedAsset;
import com.marketinghub.leadportal.storage.FileStorageService;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlowAssetServiceTest {

    @Mock
    private LegacyAssetClient legacyAssetClient;

    @Mock
    private FileStorageService fileStorageService;

    private FlowAssetService flowAssetService;

    @BeforeEach
    void setUp() {
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setPublicBaseUrl("https://cdn.example.com");

        LegacyAssetsProperties legacyAssetsProperties = new LegacyAssetsProperties();
        legacyAssetsProperties.setBaseUrl("http://legacy.example.com");

        flowAssetService = new FlowAssetService(
                legacyAssetClient,
                fileStorageService,
                new ImageOptimizer(),
                legacyAssetsProperties,
                storageProperties);
    }

    @Test
    void shouldMigrateHeroAndProofAssetsToPublicCdn() throws IOException {
        SimpleFormStyleDefinition definition = new SimpleFormStyleDefinition(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "/uploads/hero.png",
                null);
        SimpleFormStyle style = new SimpleFormStyle("style-01", "Estilo", definition);

        FlowQuestion proofQuestion = new FlowQuestion(
                "/uploads/proof.png",
                "exemplo_real_card_1_imagem_url",
                FlowQuestionType.TEXT,
                false,
                null,
                null,
                List.of());

        Flow flow = new Flow("fluxo-01", "Fluxo", null, null, null, List.of(proofQuestion), style);

        byte[] imageBytes = createImageBytes(2000, 800);
        when(legacyAssetClient.fetch("http://legacy.example.com/uploads/hero.png"))
                .thenReturn(Optional.of(new DownloadedAsset(imageBytes, "image/png", "hero.png")));
        when(legacyAssetClient.fetch("http://legacy.example.com/uploads/proof.png"))
                .thenReturn(Optional.of(new DownloadedAsset(imageBytes, "image/png", "proof.png")));

        when(fileStorageService.store(any(byte[].class), anyString(), anyString(), anyString()))
                .thenReturn("fluxo-01-hero.jpg")
                .thenReturn("fluxo-01-exemplo_real_card_1_imagem_url.jpg");
        when(fileStorageService.resolvePublicUrl("fluxo-01-hero.jpg"))
                .thenReturn(Optional.of("https://cdn.example.com/fluxo-01-hero.jpg"));
        when(fileStorageService.resolvePublicUrl("fluxo-01-exemplo_real_card_1_imagem_url.jpg"))
                .thenReturn(Optional.of("https://cdn.example.com/fluxo-01-exemplo_real_card_1_imagem_url.jpg"));

        Flow optimized = flowAssetService.optimizeAssets(flow);

        assertThat(optimized.simpleFormStyle()).isNotNull();
        assertThat(optimized.simpleFormStyle().definition().heroImageUrl())
                .isEqualTo("https://cdn.example.com/fluxo-01-hero.jpg");
        assertThat(optimized.questions()).singleElement()
                .extracting(FlowQuestion::title)
                .isEqualTo("https://cdn.example.com/fluxo-01-exemplo_real_card_1_imagem_url.jpg");

        verify(fileStorageService, times(2)).store(any(byte[].class), anyString(), anyString(), anyString());
    }

    @Test
    void shouldSkipAssetsAlreadyOnCdn() {
        SimpleFormStyleDefinition definition = new SimpleFormStyleDefinition(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "https://cdn.example.com/existing-hero.jpg",
                null);
        SimpleFormStyle style = new SimpleFormStyle("style-02", "Estilo", definition);
        Flow flow = new Flow("fluxo-02", "Fluxo", null, null, null, List.of(), style);

        Flow optimized = flowAssetService.optimizeAssets(flow);

        assertThat(optimized.simpleFormStyle().definition().heroImageUrl())
                .isEqualTo("https://cdn.example.com/existing-hero.jpg");
        verifyNoInteractions(legacyAssetClient);
        verify(fileStorageService, never()).store(any(byte[].class), anyString(), anyString(), anyString());
    }

    private byte[] createImageBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }
}
