package com.marketinghub.metaadapproverworker;

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
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.CRC32;

/**
 * Responsabilidade: carregar evidências versionadas do PDE para a revisão independente de Têmis.
 */
final class PdeReviewArtifactLoader {
  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
  private static final String COMMUNICATION_CONTRACT_DIRECTORY = "pde-platform/contracts";
  private static final List<String> EVIDENCE_COLLECTIONS =
      List.of("implementationEvidence", "executableEvidence");
  private static final List<String> COMMUNICATION_IMPLEMENTATION_EVIDENCE_PATHS =
      List.of(
          "pde-platform/frontend/src/AssistedServiceApp.tsx",
          "pde-platform/frontend/src/assistedServiceTastingContracts.ts",
          "pde-platform/backend/src/main/java/com/marketinghub/pde/service/AccessService.java",
          "pde-platform/frontend/tests/assisted-service-local.spec.ts",
          "pde-platform/frontend/tests/assisted-service-public-analytics.spec.ts",
          "pde-platform/frontend/playwright.assisted-service-public-analytics.config.ts",
          "pde-platform/frontend/tests/public-health.spec.ts",
          "pde-platform/backend/src/test/java/com/marketinghub/pde/service/AccessServiceTest.java");
  private static final List<String> ARTIFACT_PATHS =
      List.of(
          "pde-platform/contracts/kit-whatsapp-pronto-v1.json",
          "pde-platform/frontend/public/materials/kit-whatsapp-v1/01-comece-aqui.md",
          "pde-platform/frontend/public/materials/kit-whatsapp-v1/02-roteiro-de-briefing.md",
          "pde-platform/frontend/public/materials/kit-whatsapp-v1/03-biblioteca-de-respostas.md",
          "pde-platform/frontend/public/materials/kit-whatsapp-v1/04-qualificacao-e-followups.md",
          "pde-platform/frontend/public/materials/kit-whatsapp-v1/05-regras-de-escalonamento.md",
          "pde-platform/frontend/public/materials/kit-whatsapp-v1/06-modelo-microentrega-12h.md",
          "pde-platform/frontend/public/materials/kit-whatsapp-v1/07-guia-e-atualizacao.md");

  private final Path repositoryRoot;

  /** Fixa a raiz autorizada e normalizada do repositório local. */
  PdeReviewArtifactLoader(String repositoryPath) {
    repositoryRoot = Path.of(repositoryPath).toAbsolutePath().normalize();
  }

  /** Lê contrato e materiais integrais sem permitir caminho fora do repositório autorizado. */
  List<Map<String, Object>> load() throws IOException {
    List<Map<String, Object>> evidence = new ArrayList<>();
    for (String relativePath : ARTIFACT_PATHS) {
      Path artifact = repositoryRoot.resolve(relativePath).normalize();
      if (!artifact.startsWith(repositoryRoot)) {
        throw new IllegalArgumentException("Artefato PDE fora do repositório autorizado");
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

  /** Lê todos os contratos comerciais JSON versionados para produtos atuais e futuros. */
  List<Map<String, Object>> loadCommunicationContracts() throws IOException {
    Path contractsDirectory = repositoryRoot.resolve(COMMUNICATION_CONTRACT_DIRECTORY).normalize();
    if (!contractsDirectory.startsWith(repositoryRoot) || !Files.isDirectory(contractsDirectory)) {
      throw new IOException("Diretório de contratos comerciais PDE não encontrado");
    }
    List<Map<String, Object>> evidence = new ArrayList<>();
    try (Stream<Path> files = Files.list(contractsDirectory)) {
      for (Path artifact :
          files
              .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
              .filter(path -> path.getFileName().toString().endsWith(".json"))
              .sorted(Comparator.comparing(path -> path.getFileName().toString()))
              .toList()) {
        if (!artifact.normalize().startsWith(contractsDirectory)) {
          throw new IllegalArgumentException("Contrato comercial fora do diretório autorizado");
        }
        String content = Files.readString(artifact, StandardCharsets.UTF_8);
        validateDeclaredEvidenceHashes(content, artifact);
        evidence.add(
            Map.of(
                "path",
                repositoryRoot.relativize(artifact).toString(),
                "contentLength",
                content.length(),
                "contentChecksum",
                checksum(content),
                "content",
                content));
      }
    }
    if (evidence.isEmpty()) {
      throw new IOException("Nenhum contrato comercial PDE versionado foi encontrado");
    }
    for (String relativePath : COMMUNICATION_IMPLEMENTATION_EVIDENCE_PATHS) {
      evidence.add(readAuthorizedEvidence(relativePath));
    }
    return evidence;
  }

  /** Expõe a lista fixa apenas para testes de contrato do carregador. */
  static List<String> artifactPaths() {
    return ARTIFACT_PATHS;
  }

  /** Expõe as provas de implementação entregues ao gate apenas para teste de contrato. */
  static List<String> communicationImplementationEvidencePaths() {
    return COMMUNICATION_IMPLEMENTATION_EVIDENCE_PATHS;
  }

  /** Lê uma prova de implementação somente dentro da raiz autorizada do repositório. */
  private Map<String, Object> readAuthorizedEvidence(String relativePath) throws IOException {
    Path artifact = repositoryRoot.resolve(relativePath).normalize();
    if (!artifact.startsWith(repositoryRoot)
        || !Files.isRegularFile(artifact, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("Prova de implementação PDE não encontrada: " + relativePath);
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
        content);
  }

  /** Bloqueia a revisão quando um manifesto aponta para uma prova ausente ou alterada. */
  private void validateDeclaredEvidenceHashes(String contractContent, Path contractPath)
      throws IOException {
    JsonNode contract = JSON_MAPPER.readTree(contractContent);
    for (String collectionName : EVIDENCE_COLLECTIONS) {
      JsonNode collection = contract.path(collectionName);
      if (!collection.isMissingNode() && !collection.isArray()) {
        throw new IOException(
            "Coleção de evidências inválida em " + repositoryRoot.relativize(contractPath));
      }
      for (JsonNode evidence : collection) {
        validateDeclaredEvidenceHash(evidence, contractPath);
      }
    }
  }

  /** Confere caminho e SHA-256 de uma prova declarada pelo manifesto de homologação. */
  private void validateDeclaredEvidenceHash(JsonNode evidence, Path contractPath)
      throws IOException {
    String relativePath = evidence.path("path").asText("");
    String expectedHash = evidence.path("sha256").asText("");
    if (relativePath.isBlank() || expectedHash.isBlank()) {
      throw new IOException(
          "Evidência sem path ou sha256 em " + repositoryRoot.relativize(contractPath));
    }
    Path artifact = repositoryRoot.resolve(relativePath).normalize();
    if (!artifact.startsWith(repositoryRoot)
        || !Files.isRegularFile(artifact, LinkOption.NOFOLLOW_LINKS)
        || !artifact.toRealPath().startsWith(repositoryRoot.toRealPath())) {
      throw new IOException("Prova de homologação fora do repositório ou ausente: " + relativePath);
    }
    String actualHash = sha256(Files.readAllBytes(artifact));
    if (!MessageDigest.isEqual(
        expectedHash.getBytes(StandardCharsets.UTF_8),
        actualHash.getBytes(StandardCharsets.UTF_8))) {
      throw new IOException("SHA-256 divergente para a prova de homologação: " + relativePath);
    }
  }

  /** Calcula o SHA-256 hexadecimal usado pelos manifestos de homologação. */
  private String sha256(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 indisponível na JVM", ex);
    }
  }

  /** Calcula checksum determinístico para comprovar qual conteúdo foi revisado. */
  private String checksum(String content) {
    CRC32 checksum = new CRC32();
    checksum.update(content.getBytes(StandardCharsets.UTF_8));
    return Long.toHexString(checksum.getValue());
  }
}
