package com.marketinghub.customeragentworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.CRC32;

/** Responsabilidade: carregar a experiência versionada que Psique deve revisar como cliente. */
final class PdeExperienceEvidenceLoader {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final String CONTRACT_DIRECTORY = "pde-platform/contracts";
  private static final String COMMERCIAL_HOMOLOGATION_SUFFIX = "-commercial-homologation-v1.json";
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

  /** Descobre manifestos comerciais e carrega somente as provas declaradas e íntegras. */
  List<Map<String, Object>> loadCommercialHomologationEvidence() throws IOException {
    Path directory = repositoryRoot.resolve(CONTRACT_DIRECTORY).normalize();
    if (!directory.startsWith(repositoryRoot) || !Files.isDirectory(directory)) {
      throw new IOException("Diretório de contratos PDE não encontrado");
    }
    Map<String, Map<String, Object>> evidence = new LinkedHashMap<>();
    try (Stream<Path> files = Files.list(directory)) {
      for (Path manifest :
          files
              .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
              .filter(
                  path -> path.getFileName().toString().endsWith(COMMERCIAL_HOMOLOGATION_SUFFIX))
              .sorted(Comparator.comparing(path -> path.getFileName().toString()))
              .toList()) {
        String manifestPath = repositoryRoot.relativize(manifest).toString();
        Map<String, Object> manifestEvidence = readAuthorizedEvidence(manifestPath);
        evidence.put(manifestPath, manifestEvidence);
        JsonNode contract = JSON.readTree(manifestEvidence.get("content").toString());
        JsonNode declaredEvidence = contract.path("homologationEvidence");
        if (!declaredEvidence.isArray() || declaredEvidence.isEmpty()) {
          throw new IOException("Manifesto comercial sem evidências: " + manifestPath);
        }
        for (JsonNode declared : declaredEvidence) {
          String relativePath = declared.path("path").asText("");
          Map<String, Object> artifact = readAuthorizedEvidence(relativePath);
          requireSha256(relativePath, declared.path("sha256").asText(""), artifact);
          evidence.putIfAbsent(relativePath, artifact);
        }
      }
    }
    if (evidence.isEmpty()) {
      throw new IOException("Nenhum manifesto de homologação comercial PDE foi encontrado");
    }
    return new ArrayList<>(evidence.values());
  }

  /** Lê uma evidência regular sem permitir caminho absoluto, link ou saída da raiz. */
  private Map<String, Object> readAuthorizedEvidence(String relativePath) throws IOException {
    if (relativePath == null || relativePath.isBlank() || Path.of(relativePath).isAbsolute()) {
      throw new IOException("Caminho de evidência comercial inválido");
    }
    Path artifact = repositoryRoot.resolve(relativePath).normalize();
    if (!artifact.startsWith(repositoryRoot)
        || !Files.isRegularFile(artifact, LinkOption.NOFOLLOW_LINKS)
        || !artifact.toRealPath().startsWith(repositoryRoot.toRealPath())) {
      throw new IOException("Evidência comercial fora do repositório ou ausente: " + relativePath);
    }
    String content = Files.readString(artifact, StandardCharsets.UTF_8);
    return Map.of(
        "path",
        relativePath,
        "contentLength",
        content.length(),
        "contentChecksum",
        checksum(content),
        "content",
        content,
        "sha256",
        sha256(content.getBytes(StandardCharsets.UTF_8)));
  }

  /** Bloqueia o gate quando uma prova mudou depois da criação do manifesto. */
  private void requireSha256(String relativePath, String expectedHash, Map<String, Object> artifact)
      throws IOException {
    String actualHash = artifact.get("sha256").toString();
    if (expectedHash.isBlank()
        || !MessageDigest.isEqual(
            expectedHash.getBytes(StandardCharsets.UTF_8),
            actualHash.getBytes(StandardCharsets.UTF_8))) {
      throw new IOException("SHA-256 divergente para a prova comercial: " + relativePath);
    }
  }

  /** Calcula SHA-256 hexadecimal para congelar a versão revisada por Psique. */
  private String sha256(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 indisponível na JVM", ex);
    }
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
