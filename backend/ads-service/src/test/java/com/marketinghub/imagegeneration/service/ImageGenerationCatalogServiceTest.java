package com.marketinghub.imagegeneration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.imagegeneration.ImageGenerationModel;
import com.marketinghub.imagegeneration.ImageGenerationProvider;
import com.marketinghub.repository.jpa.imagegeneration.ImageGenerationModelRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar os modelos oferecidos para novas gerações de imagem. */
class ImageGenerationCatalogServiceTest {

  /**
   * Garante que somente o Sunburst apareça para novas gerações e todo modelo anterior fique
   * histórico.
   */
  @Test
  void shouldExposeOnlyCanonicalSunburstModel() {
    ImageGenerationModelRepository repository = mock(ImageGenerationModelRepository.class);
    when(repository.findAll(any(org.springframework.data.domain.Sort.class)))
        .thenReturn(
            List.of(
                model("gpt-image-1", "gpt-image-1"),
                model("gpt-image-1.5", "gpt-image-1.5"),
                model("gpt-image-2", "gpt-image-2"),
                model("dall-e-3", "dall-e-3"),
                model("gpt-image-2.5-sunburst", "gpt-image-2.5-sunburst")));

    List<String> availableModels =
        new ImageGenerationCatalogService(repository)
            .listModels().stream().map(model -> model.apiModel()).toList();

    assertThat(availableModels).containsExactly("gpt-image-2.5-sunburst");
  }

  /** Cria um modelo mínimo para o cenário de catálogo. */
  private ImageGenerationModel model(String code, String apiModel) {
    ImageGenerationModel model = new ImageGenerationModel();
    model.setId((long) code.hashCode());
    model.setCode(code);
    model.setDisplayName(code);
    model.setProvider(ImageGenerationProvider.OPENAI);
    model.setApiModel(apiModel);
    return model;
  }
}
