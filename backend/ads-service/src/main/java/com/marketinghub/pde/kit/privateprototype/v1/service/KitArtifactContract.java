package com.marketinghub.pde.kit.privateprototype.v1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Responsabilidade: conferir o arquivo final do kit sem misturá-lo à auditoria técnica. */
@Component
@RequiredArgsConstructor
@Slf4j
public class KitArtifactContract {
  public static final int MAX_ZIP_BYTES = 32 * 1024 * 1024;
  public static final String PACKAGE_CONTRACT_VERSION = "PDE_PRIVATE_KIT_PACKAGE_V2";
  private final ObjectMapper json;

  /** Confere nomes, volume, dimensões, textos e hashes antes de aceitar a composição. */
  public ObjectNode validate(byte[] bytes) throws IOException {
    var contents = entries(bytes);
    var expected = new HashSet<>(List.of("calendario/calendario-7-dias.txt"));
    for (int i = 1; i <= 10; i++) {
      expected.add("posts/post-%02d.png".formatted(i));
      expected.add("stories/story-%02d.png".formatted(i));
      expected.add("legendas/legenda-%02d.txt".formatted(i));
    }
    for (int i = 1; i <= 5; i++) expected.add("mensagens/mensagem-%02d.txt".formatted(i));
    if (!contents.keySet().equals(expected))
      throw new IllegalArgumentException(
          "O ZIP precisa conter os 36 arquivos funcionais aprovados, nas pastas contratadas, sem arquivos técnicos ou extras.");
    var manifest = json.createObjectNode().put("packageContractVersion", PACKAGE_CONTRACT_VERSION);
    var files = manifest.putArray("files");
    for (var entry : contents.entrySet()) {
      var item =
          files
              .addObject()
              .put("name", entry.getKey())
              .put("sha256", hash(entry.getValue()))
              .put("bytes", entry.getValue().length);
      if (entry.getKey().endsWith(".png")) {
        var image = ImageIO.read(new ByteArrayInputStream(entry.getValue()));
        int height = entry.getKey().startsWith("posts/") ? 1080 : 1920;
        if (image == null || image.getWidth() != 1080 || image.getHeight() != height)
          throw new IllegalArgumentException("Dimensões do kit inválidas.");
        item.put("width", image.getWidth()).put("height", image.getHeight());
      } else if (text(contents, entry.getKey()).isBlank()) {
        throw new IllegalArgumentException("Texto funcional ausente no kit.");
      }
    }
    String calendar = text(contents, "calendario/calendario-7-dias.txt");
    String[] days = calendar.split("\\n");
    var references =
        java.util.regex.Pattern.compile(
                "(?:posts|stories|legendas|mensagens)/[A-Za-z0-9_-]+\\.(?:png|txt)")
            .matcher(calendar);
    int referenceCount = 0;
    while (references.find()) {
      if (!contents.containsKey(references.group()))
        throw new IllegalArgumentException("O calendário referencia arquivo ausente.");
      referenceCount++;
    }
    if (days.length != 7 || referenceCount != 28)
      throw new IllegalArgumentException(
          "Textos, quantidades ou associação da primeira aplicação incompletos.");
    for (int day = 1; day <= 7; day++) {
      if (!days[day - 1].startsWith("Dia " + day + " — "))
        throw new IllegalArgumentException("O calendário precisa identificar seus sete dias.");
    }
    var first = manifest.putObject("firstApplication");
    first
        .put("post", "post-01.png")
        .put("story", "story-01.png")
        .put("postFile", "posts/post-01.png")
        .put("storyFile", "stories/story-01.png")
        .put("captionFile", "legendas/legenda-01.txt")
        .put("messageFile", "mensagens/mensagem-01.txt")
        .put("caption", text(contents, "legendas/legenda-01.txt"))
        .put("message", text(contents, "mensagens/mensagem-01.txt"))
        .put("calendar", calendar);
    manifest
        .put("zipSha256", hash(bytes))
        .put("providerCalls", 0)
        .put("testMarker", "AGENT_VALIDATION")
        .put("commercialEvidenceEligible", false);
    return manifest;
  }

  /**
   * Lê pastas canônicas e arquivos legados preservados, recusando traversal e expansão excessiva.
   */
  public Map<String, byte[]> entries(byte[] bytes) throws IOException {
    if (bytes == null || bytes.length < 50000 || bytes.length > MAX_ZIP_BYTES)
      throw new IllegalArgumentException("Arquivo de kit ausente ou fora do limite.");
    var contents = new LinkedHashMap<String, byte[]>();
    int total = 0;
    try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        String name = entry.getName();
        if (entry.isDirectory()
            || !name.matches(
                "(?:posts/post-|stories/story-|legendas/legenda-|mensagens/mensagem-)\\d{2}\\.(?:png|txt)|calendario/calendario-7-dias\\.txt|(?:post-|story-)\\d{2}\\.png|(?:legendas-prontas|mensagens-whatsapp|calendario-7-dias|LEIA-ME)\\.txt")
            || contents.containsKey(name)
            || contents.size() >= 36)
          throw new IllegalArgumentException("Entrada do kit não permitida.");
        byte[] value = zip.readNBytes(16 * 1024 * 1024 + 1);
        total += value.length;
        if (value.length > 16 * 1024 * 1024 || total > 100 * 1024 * 1024)
          throw new IllegalArgumentException("Expansão do kit acima de 100 MB.");
        contents.put(name, value);
      }
    }
    return contents;
  }

  /** Resolve a imagem da sessão preservando acesso aos pacotes históricos imutáveis. */
  public byte[] image(byte[] bytes, String name) throws IOException {
    var contents = entries(bytes);
    byte[] previous = contents.get(name);
    return previous != null
        ? previous
        : contents.get((name.startsWith("post-") ? "posts/" : "stories/") + name);
  }

  /** Recusa UTF-8 inválido antes de comprovar um texto funcional. */
  private String text(Map<String, byte[]> contents, String name) throws IOException {
    return StandardCharsets.UTF_8
        .newDecoder()
        .decode(java.nio.ByteBuffer.wrap(contents.get(name)))
        .toString();
  }

  /** Calcula a identidade estável dos bytes efetivamente recebidos. */
  public static String hash(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (java.security.NoSuchAlgorithmException ex) {
      log.error("Falha ao calcular identidade SHA-256 do kit. bytes={}", bytes.length, ex);
      throw new IllegalStateException(ex);
    }
  }
}
