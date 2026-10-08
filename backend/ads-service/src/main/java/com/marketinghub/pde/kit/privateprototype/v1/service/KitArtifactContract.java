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
import org.springframework.stereotype.Component;

/** Responsabilidade: conferir o arquivo final do kit sem misturá-lo à auditoria técnica. */
@Component
@RequiredArgsConstructor
public class KitArtifactContract {
  public static final int MAX_ZIP_BYTES = 32 * 1024 * 1024;
  private final ObjectMapper json;

  /** Confere nomes, volume, dimensões, textos e hashes antes de aceitar a composição. */
  public ObjectNode validate(byte[] bytes) throws IOException {
    var contents = entries(bytes);
    var expected =
        new HashSet<>(
            List.of(
                "legendas-prontas.txt",
                "mensagens-whatsapp.txt",
                "calendario-7-dias.txt",
                "LEIA-ME.txt"));
    for (int i = 1; i <= 10; i++) {
      expected.add("post-%02d.png".formatted(i));
      expected.add("story-%02d.png".formatted(i));
    }
    if (!contents.keySet().equals(expected))
      throw new IllegalArgumentException(
          "O ZIP precisa conter os 24 arquivos contratados, sem arquivos técnicos ou caminhos extras.");
    var manifest = json.createObjectNode();
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
        int height = entry.getKey().startsWith("post-") ? 1080 : 1920;
        if (image == null || image.getWidth() != 1080 || image.getHeight() != height)
          throw new IllegalArgumentException("Dimensões do kit inválidas.");
        item.put("width", image.getWidth()).put("height", image.getHeight());
      }
    }
    String captions = text(contents, "legendas-prontas.txt");
    String messages = text(contents, "mensagens-whatsapp.txt");
    String calendar = text(contents, "calendario-7-dias.txt");
    if (captions.split("\\n\\n---\\n\\n").length != 10
        || !messages.contains("5. ")
        || !calendar.contains("Dia 7")
        || !calendar.contains("post-01.png")
        || !calendar.contains("story-01.png"))
      throw new IllegalArgumentException(
          "Textos, quantidades ou associação da primeira aplicação incompletos.");
    var first = manifest.putObject("firstApplication");
    first
        .put("post", "post-01.png")
        .put("story", "story-01.png")
        .put("caption", captions.split("\\n\\n---\\n\\n")[0])
        .put("message", messages.split("\\n\\n")[0])
        .put("calendar", calendar);
    manifest
        .put("zipSha256", hash(bytes))
        .put("providerCalls", 0)
        .put("testMarker", "AGENT_VALIDATION")
        .put("commercialEvidenceEligible", false);
    return manifest;
  }

  /** Extrai apenas entradas finitas, sem caminhos, duplicatas ou expansão acima do limite. */
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
            || !name.matches("[A-Za-z0-9_-]+\\.(png|txt)")
            || contents.containsKey(name)
            || contents.size() >= 24)
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

  /** Lê o texto contratado sem inserir conteúdo técnico adicional. */
  private String text(Map<String, byte[]> contents, String name) {
    return new String(contents.get(name), StandardCharsets.UTF_8);
  }

  /** Calcula a identidade estável dos bytes efetivamente recebidos. */
  public static String hash(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (java.security.NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
