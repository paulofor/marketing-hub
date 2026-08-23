package com.marketinghub.metaadapproverworker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.CRC32;

/**
 * Responsabilidade: carregar evidências versionadas do PDE para a revisão independente de Têmis.
 */
final class PdeReviewArtifactLoader {
  private static final String COMMUNICATION_CONTRACT_DIRECTORY = "pde-platform/contracts";
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
    return evidence;
  }

  /** Expõe a lista fixa apenas para testes de contrato do carregador. */
  static List<String> artifactPaths() {
    return ARTIFACT_PATHS;
  }

  /** Calcula checksum determinístico para comprovar qual conteúdo foi revisado. */
  private String checksum(String content) {
    CRC32 checksum = new CRC32();
    checksum.update(content.getBytes(StandardCharsets.UTF_8));
    return Long.toHexString(checksum.getValue());
  }
}
