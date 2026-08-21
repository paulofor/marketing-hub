package com.marketinghub.customeragentworker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

/** Responsabilidade: carregar a experiência versionada que Psique deve revisar como cliente. */
final class PdeExperienceEvidenceLoader {
  private static final List<String> EVIDENCE_PATHS =
      List.of(
          "pde-platform/contracts/kit-whatsapp-pronto-v1.json",
          "pde-platform/frontend/src/AssistedServiceApp.tsx",
          "pde-platform/frontend/tests/assisted-service-local.spec.ts",
          "docs/homologacao/pde-kit-whatsapp-construcao-v1.md");

  private final Path repositoryRoot;

  /** Fixa a raiz autorizada e normalizada do repositório local. */
  PdeExperienceEvidenceLoader(String repositoryPath) {
    repositoryRoot = Path.of(repositoryPath).toAbsolutePath().normalize();
  }

  /** Lê contrato, experiência e homologação sem permitir caminho fora da raiz autorizada. */
  List<Map<String, Object>> load() throws IOException {
    List<Map<String, Object>> evidence = new ArrayList<>();
    for (String relativePath : EVIDENCE_PATHS) {
      Path artifact = repositoryRoot.resolve(relativePath).normalize();
      if (!artifact.startsWith(repositoryRoot)) {
        throw new IllegalArgumentException("Evidência PDE fora do repositório autorizado");
      }
      String content = Files.readString(artifact, StandardCharsets.UTF_8);
      evidence.add(
          Map.of(
              "path",
              relativePath,
              "contentLength",
              content.length(),
              "contentChecksum",
              checksum(content),
              "content",
              content));
    }
    return evidence;
  }

  /** Expõe a lista fixa apenas para testes de contrato do carregador. */
  static List<String> evidencePaths() {
    return EVIDENCE_PATHS;
  }

  /** Calcula checksum determinístico para provar a versão efetivamente avaliada. */
  private String checksum(String content) {
    CRC32 checksum = new CRC32();
    checksum.update(content.getBytes(StandardCharsets.UTF_8));
    return Long.toHexString(checksum.getValue());
  }
}
