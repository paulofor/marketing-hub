package com.marketinghub.payments.service.kit;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifica composição, origem e limites da candidata editorial sem aprovar sua qualidade comercial.
 */
class EditorialKitArtworkTest {
  @TempDir Path root;

  /**
   * Exercita seis especialidades reais e preserva fontes históricas com identidade própria de
   * layout.
   */
  @ParameterizedTest
  @ValueSource(strings = {"nails", "barber", "hair", "makeup", "brows", "aesthetics"})
  void rendersBothFormatsFromTheSameLicensedSource(String specialty) throws Exception {
    var profile = CapellaKitCatalog.byCode(specialty + "-v3");
    var origin = LicensedKitReferences.provenance(profile.code());
    var previous = LicensedKitReferences.provenance(specialty + "-v2");
    assertThat(origin.path("compositionVersion").asText()).isEqualTo("REFERENCE_EDITORIAL_V3");
    assertThat(origin.path("librarySha256")).isEqualTo(previous.path("librarySha256"));
    assertThat(previous.has("compositionVersion")).isFalse();
    for (int i = 0; i < 5; i++) {
      var asset = origin.path("assets").get(i);
      assertThat(asset.path("sha256")).isEqualTo(previous.path("assets").get(i).path("sha256"));
      assertThat(asset.path("sourceProfileCode").asText()).isEqualTo(specialty + "-v2");
      assertThat(asset.path("profileCode").asText()).isEqualTo(profile.code());
    }
    var photo = new LicensedKitReferences().generate("editorial-local-907", 0, profile);
    String service = profile.serviceExamples().split(":")[0];
    for (int height : new int[] {1080, 1920}) {
      Path output = root.resolve(profile.code() + "-" + height + ".png");
      EditorialKitArtwork.render(
          output,
          height,
          photo,
          "Studio Aurora",
          "00000000000 · Cidade Exemplo",
          service,
          profile.headlines().getFirst(),
          0,
          LicensedKitReferences.focusY(profile.code(), 0));
      var image = ImageIO.read(output.toFile());
      assertThat(image.getWidth()).isEqualTo(1080);
      assertThat(image.getHeight()).isEqualTo(height);
      String evidence = System.getProperty("capella.validation.output");
      if (evidence != null) {
        Files.createDirectories(Path.of(evidence));
        Files.copy(
            output,
            Path.of(evidence).resolve(output.getFileName()),
            StandardCopyOption.REPLACE_EXISTING);
      }
    }
    assertThatThrownBy(() -> CapellaKitCatalog.forPaymentReference(profile.code()))
        .hasMessageContaining("homologada");
  }

  /**
   * Recusa texto impossível sem cortar telefone, prometer um pacote pronto ou gravar arte
   * incompleta.
   */
  @Test
  void rejectsOverflowAndUnknownVersionsWithoutDamagingHistoricalComposition() throws Exception {
    var photo =
        new LicensedKitReferences()
            .generate("editorial-local-2112", 0, CapellaKitCatalog.byCode("hair-v3"));
    Path output = root.resolve("invalid.png");
    assertThatThrownBy(
            () ->
                EditorialKitArtwork.render(
                    output,
                    1080,
                    photo,
                    "Outro Studio",
                    "9".repeat(100),
                    "Corte e finalização",
                    "Um visual com seu estilo",
                    1,
                    .5))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("legibilidade mínima");
    assertThat(output).doesNotExist();
    assertThatThrownBy(() -> CapellaKitCatalog.byCode("hair-v4"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(CapellaKitCatalog.byCode("hair-v2").code()).isEqualTo("hair-v2");
    assertThat(CapellaKitCatalog.byCode("hair-v1").code()).isEqualTo("hair-v1");
  }

  /** Aceita nomes compostos e contato completos sem reduzir a referência abaixo do mínimo. */
  @Test
  void rendersLongNamesAndMessagesWithoutEllipsis() throws Exception {
    var profile = CapellaKitCatalog.byCode("nails-v3");
    var photo = new LicensedKitReferences().generate("editorial-local-7", 0, profile);
    for (int height : new int[] {1080, 1920}) {
      Path output = root.resolve("long-" + height + ".png");
      EditorialKitArtwork.render(
          output,
          height,
          photo,
          "Ateliê de Beleza Aurora e Horizonte",
          "00000000000 · Cidade de Demonstração",
          "Manicure e nail design",
          "Pequenos detalhes, cuidado em cada escolha",
          0,
          .5);
      assertThat(output).isRegularFile();
      String evidence = System.getProperty("capella.validation.output");
      if (evidence != null)
        Files.copy(
            output,
            Path.of(evidence).resolve(output.getFileName()),
            StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
