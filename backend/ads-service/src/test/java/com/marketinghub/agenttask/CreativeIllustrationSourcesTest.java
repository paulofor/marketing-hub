package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.imagegenerator.dto.ImageGeneratorResponse.ImageGeneratorResult;
import com.marketinghub.imagegenerator.service.ImageGeneratorService;
import com.marketinghub.repository.jpa.imagegenerator.ImageGenerationRequestRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: proteger a origem ilustrativa sem fabricar prova nem cruzar produtos. */
class CreativeIllustrationSourcesTest {
  private final ImageGenerationRequestRepository repository =
      mock(ImageGenerationRequestRepository.class);
  private final ImageGeneratorService generator = mock(ImageGeneratorService.class);
  private final CreativeIllustrationSources service =
      new CreativeIllustrationSources(repository, generator);
  private final ObjectMapper json = new ObjectMapper();

  /** Confere segregação na consulta oficial, hash, recorte e ausência de aprovação implícita. */
  @ParameterizedTest
  @ValueSource(longs = {11, 91011})
  void reusesOnlyCompletedImagesInExactContext(long product) throws Exception {
    var context = json.createObjectNode().put("commercialPlanId", 91034L);
    context.putObject("product").put("id", product);
    var png = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(
        new java.awt.image.BufferedImage(800, 500, java.awt.image.BufferedImage.TYPE_INT_RGB),
        "png",
        png);
    when(generator.getGeneratedImage(product, 91034L, 91097L, "img-sandbox"))
        .thenReturn(
            new ImageGeneratorResult(
                "img-sandbox",
                "test",
                "flex",
                "png",
                Base64.getEncoder().encodeToString(png.toByteArray()),
                List.of(),
                java.time.Instant.EPOCH));
    var image = service.read(context, "experiment:91097", "img-sandbox");
    assertThat(image.bytes()).isEqualTo(png.toByteArray());
    assertThat(image.metadata().productProof()).isFalse();
    assertThat(image.metadata().role()).isEqualTo("GENERATED_ILLUSTRATION");
    var selection = json.valueToTree(image.metadata());
    ((com.fasterxml.jackson.databind.node.ObjectNode) selection)
        .putObject("crop")
        .put("x", 0)
        .put("y", 0)
        .put("width", 800)
        .put("height", 500);
    service.validate(context, "experiment:91097", selection);
    ((com.fasterxml.jackson.databind.node.ObjectNode) selection.path("crop")).put("x", 1);
    assertThatThrownBy(() -> service.validate(context, "experiment:91097", selection))
        .hasMessageContaining("fora dos pixels");
    ((com.fasterxml.jackson.databind.node.ObjectNode) selection).put("sha256", "0".repeat(64));
    assertThatThrownBy(() -> service.validate(context, "experiment:91097", selection))
        .hasMessageContaining("divergente");
    when(generator.getGeneratedImage(product, 91034L, 91098L, "img-sandbox"))
        .thenThrow(new IllegalArgumentException("Não encontrada neste experimento"));
    assertThatThrownBy(() -> service.read(context, "experiment:91098", "img-sandbox"))
        .hasMessageContaining("indisponível no contexto");
    verify(generator).getGeneratedImage(product, 91034L, 91098L, "img-sandbox");
    verifyNoInteractions(repository);
  }

  /** Não busca imagens globais quando o plano ou a referência não estão identificados. */
  @Test
  void incompleteContextDoesNotBroadenSearch() {
    assertThat(service.list(json.createObjectNode(), "experiment:97")).isEmpty();
    var context = json.createObjectNode().put("commercialPlanId", 34);
    context.putObject("product").put("id", 11);
    assertThat(service.list(context, "product:11")).isEmpty();
    assertThatThrownBy(() -> service.read(context, "product:11", "img-sandbox"))
        .hasMessageContaining("contexto comercial exato");
    verifyNoInteractions(repository, generator);
  }
}
