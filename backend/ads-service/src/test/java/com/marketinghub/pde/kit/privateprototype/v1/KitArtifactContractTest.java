package com.marketinghub.pde.kit.privateprototype.v1;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.kit.privateprototype.v1.service.KitArtifactContract;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: exigir os arquivos individuais aprovados sem perder acesso ao ZIP histórico.
 */
class KitArtifactContractTest {
  private final KitArtifactContract contract = new KitArtifactContract(new ObjectMapper());

  /** Confere entrega integral, dimensões, identidade e primeira aplicação nos arquivos reais. */
  @Test
  void acceptsThirtySixFilesAndTheirResolvedCalendar() throws Exception {
    byte[] zip = zip(complete());
    var manifest = contract.validate(zip);
    assertThat(manifest.path("files")).hasSize(36);
    assertThat(manifest.path("packageContractVersion").asText())
        .isEqualTo(KitArtifactContract.PACKAGE_CONTRACT_VERSION);
    assertThat(manifest.path("zipSha256").asText()).isEqualTo(KitArtifactContract.hash(zip));
    assertThat(manifest.path("firstApplication").path("captionFile").asText())
        .isEqualTo("legendas/legenda-01.txt");
    assertThat(contract.image(zip, "post-01.png")).isEqualTo(complete().get("posts/post-01.png"));
  }

  /** Recusa pacote incompleto, conteúdo extra, texto inválido e referência sem arquivo. */
  @Test
  void refusesIncompleteOrContradictoryDeliveries() throws Exception {
    var contents = complete();
    contents.remove("legendas/legenda-10.txt");
    assertThatThrownBy(() -> contract.validate(zip(contents))).hasMessageContaining("36 arquivos");
    contents.put("legendas/legenda-10.txt", new byte[] {(byte) 0xff});
    assertThatThrownBy(() -> contract.validate(zip(contents))).isInstanceOf(IOException.class);
    contents.put("legendas/legenda-10.txt", bytes("Legenda dez"));
    contents.put(
        "calendario/calendario-7-dias.txt",
        bytes(calendar().replace("posts/post-01.png", "posts/post-99.png")));
    assertThatThrownBy(() -> contract.validate(zip(contents)))
        .hasMessageContaining("referencia arquivo ausente");
    contents.put("../post-01.png", contents.get("posts/post-01.png"));
    assertThatThrownBy(() -> contract.entries(zip(contents))).hasMessageContaining("não permitida");
  }

  /** Mantém leitura do pacote anterior sem aceitá-lo como prova do novo contrato. */
  @Test
  void preservesLegacyBytesWithoutAcceptingThemAsCurrentProof() throws Exception {
    var current = complete();
    var legacy = new LinkedHashMap<String, byte[]>();
    current.forEach(
        (name, value) -> {
          if (name.endsWith(".png")) legacy.put(name.substring(name.indexOf('/') + 1), value);
        });
    for (String name :
        List.of(
            "legendas-prontas.txt",
            "mensagens-whatsapp.txt",
            "calendario-7-dias.txt",
            "LEIA-ME.txt")) legacy.put(name, bytes("Texto histórico preservado"));
    byte[] original = zip(legacy);
    assertThat(contract.entries(original)).hasSize(24);
    assertThat(contract.image(original, "post-01.png")).isEqualTo(legacy.get("post-01.png"));
    assertThatThrownBy(() -> contract.validate(original)).hasMessageContaining("36 arquivos");
  }

  /** Materializa um pacote sintético finito nas dimensões nativas, sem API externa. */
  private Map<String, byte[]> complete() throws IOException {
    var result = new LinkedHashMap<String, byte[]>();
    byte[] post = png(1080), story = png(1920);
    for (int i = 1; i <= 10; i++) {
      result.put("posts/post-%02d.png".formatted(i), post);
      result.put("stories/story-%02d.png".formatted(i), story);
      result.put("legendas/legenda-%02d.txt".formatted(i), bytes("Legenda " + i));
    }
    for (int i = 1; i <= 5; i++)
      result.put("mensagens/mensagem-%02d.txt".formatted(i), bytes("Mensagem " + i));
    result.put("calendario/calendario-7-dias.txt", bytes(calendar()));
    return result;
  }

  /** Declara quatro referências existentes por dia, conforme a aplicação aprovada. */
  private String calendar() {
    var days = new ArrayList<String>();
    for (int i = 1; i <= 7; i++)
      days.add(
          "Dia "
              + i
              + " — Aplicação | posts/post-%02d.png | stories/story-%02d.png | legendas/legenda-%02d.txt | mensagens/mensagem-%02d.txt"
                  .formatted(i, i, i, (i - 1) % 5 + 1));
    return String.join("\n", days);
  }

  /** Gera pixels sintéticos suficientes para exercitar o tamanho mínimo do ZIP real. */
  private byte[] png(int height) throws IOException {
    var image = new BufferedImage(1080, height, BufferedImage.TYPE_INT_RGB);
    var random = new SplittableRandom(11);
    for (int y = 0; y < 64; y++)
      for (int x = 0; x < 64; x++) image.setRGB(x, y, random.nextInt(0xffffff));
    var output = new ByteArrayOutputStream();
    ImageIO.write(image, "png", output);
    return output.toByteArray();
  }

  /** Codifica o texto funcional em UTF-8. */
  private byte[] bytes(String text) {
    return text.getBytes(StandardCharsets.UTF_8);
  }

  /** Compacta somente os arquivos solicitados, permitindo testar entradas inválidas. */
  private byte[] zip(Map<String, byte[]> contents) throws IOException {
    var output = new ByteArrayOutputStream();
    try (var zip = new ZipOutputStream(output)) {
      for (var entry : contents.entrySet()) {
        zip.putNextEntry(new ZipEntry(entry.getKey()));
        zip.write(entry.getValue());
        zip.closeEntry();
      }
    }
    return output.toByteArray();
  }
}
