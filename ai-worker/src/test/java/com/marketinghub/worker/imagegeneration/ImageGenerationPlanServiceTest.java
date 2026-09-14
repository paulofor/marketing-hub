package com.marketinghub.worker.imagegeneration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.worker.leadportal.image.LeadPortalImagePackageClient;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a seleção canônica do plano de geração de imagens. */
class ImageGenerationPlanServiceTest {

    /** Confirma que pacotes legados migram para Sunburst, nunca para a ordem do catálogo. */
    @Test
    void resolvesPersistedModelBeforeCatalogFallback() {
        ImageGenerationCatalogService catalog = mock(ImageGenerationCatalogService.class);
        ImageGenerationModelDto dalle = model(4L, "dall-e-2", 9L);
        ImageGenerationModelDto legacyImage = model(1L, "gpt-image-1", 2L);
        ImageGenerationModelDto previousImage = model(5L, "gpt-image-2", 6L);
        ImageGenerationModelDto sunburst = model(7L, "gpt-image-2.5-sunburst", 8L);
        when(catalog.getCatalog()).thenReturn(List.of(dalle, legacyImage, previousImage, sunburst));

        var imagePackage = new LeadPortalImagePackageClient.ImagePackage(
                163L,
                UUID.randomUUID(),
                null,
                6,
                1,
                " gpt-image-1 ",
                "prompt",
                null,
                null,
                null);

        ImageGenerationPlan plan = new ImageGenerationPlanService(catalog)
                .resolvePlan(imagePackage, ImageOrientation.SQUARE);

        assertThat(plan).isNotNull();
        assertThat(plan.modelId()).isEqualTo(7L);
        assertThat(plan.apiModel()).isEqualTo("gpt-image-2.5-sunburst");
    }

    /** Monta um modelo mínimo com qualidade e preço quadrados para o cenário de seleção. */
    private ImageGenerationModelDto model(long modelId, String apiModel, long qualityId) {
        var price = new ImageGenerationPriceDto(
                qualityId, ImageOrientation.SQUARE, 1024, 1024, "1024x1024", BigDecimal.ONE, true);
        var quality = new ImageGenerationQualityDto(
                qualityId, modelId, "standard", "Standard", "standard", true, List.of(price));
        return new ImageGenerationModelDto(
                modelId, apiModel, apiModel, "OPENAI", apiModel, "Modelo de teste", List.of(quality));
    }
}
