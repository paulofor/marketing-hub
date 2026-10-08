package com.marketinghub.payments.service.kit.privateprototype.v1;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Responsabilidade: organizar os 36 arquivos privados conforme o contrato aprovado de Dédalo. */
public final class PrivateKitPackageLayout {
  /** Separa textos e associa arquivos já compostos, sem gerar novas imagens ou alterar a oferta. */
  public static void materialize(Path directory, List<String> captions, String messages, String calendar)
      throws IOException {
    String[] individualMessages = messages.split("\\n\\n");
    if (captions.size() != 10 || individualMessages.length != 5) {
      throw new IllegalArgumentException("O pacote privado exige dez legendas e cinco mensagens.");
    }
    for (String folder : List.of("posts", "stories", "legendas", "mensagens", "calendario")) {
      Files.createDirectories(directory.resolve(folder));
    }
    for (int index = 1; index <= 10; index++) {
      Files.move(directory.resolve("post-%02d.png".formatted(index)),
          directory.resolve("posts/post-%02d.png".formatted(index)));
      Files.move(directory.resolve("story-%02d.png".formatted(index)),
          directory.resolve("stories/story-%02d.png".formatted(index)));
      Files.writeString(directory.resolve("legendas/legenda-%02d.txt".formatted(index)),
          captions.get(index - 1), StandardCharsets.UTF_8);
    }
    for (int index = 1; index <= 5; index++) {
      Files.writeString(directory.resolve("mensagens/mensagem-%02d.txt".formatted(index)),
          individualMessages[index - 1], StandardCharsets.UTF_8);
    }
    Files.writeString(directory.resolve("calendario/calendario-7-dias.txt"), calendar,
        StandardCharsets.UTF_8);
    for (String previous : List.of("legendas-prontas.txt", "mensagens-whatsapp.txt",
        "calendario-7-dias.txt", "LEIA-ME.txt")) {
      Files.deleteIfExists(directory.resolve(previous));
    }
  }
}
