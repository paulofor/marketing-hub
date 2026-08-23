package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Responsabilidade: validar o carregamento auditável dos artefatos entregues ao gate de Têmis. */
class PdeReviewArtifactLoaderTest {
  @TempDir Path tempDir;

  /** Entrega ao gate todos os arquivos autorizados com conteúdo e checksum determinístico. */
  @Test
  void loadsAllVersionedArtifactsWithoutShellAccess() throws Exception {
    for (String relativePath : PdeReviewArtifactLoader.artifactPaths()) {
      Path artifact = tempDir.resolve(relativePath);
      Files.createDirectories(artifact.getParent());
      Files.writeString(artifact, "conteúdo auditável de " + relativePath);
    }

    var evidence = new PdeReviewArtifactLoader(tempDir.toString()).load();

    assertThat(evidence).hasSize(8);
    assertThat(evidence)
        .allSatisfy(
            artifact -> {
              assertThat(artifact.get("content").toString()).contains("conteúdo auditável");
              assertThat(artifact.get("contentChecksum").toString()).isNotBlank();
            });
  }

  /** Descobre contratos comerciais novos sem exigir lista manual no executor de Têmis. */
  @Test
  void loadsAllCommunicationContractsForCurrentAndFutureProducts() throws Exception {
    Path contracts = tempDir.resolve("pde-platform/contracts");
    Files.createDirectories(contracts);
    Files.writeString(contracts.resolve("produto-b-v1.json"), "{\"slug\":\"produto-b\"}");
    Files.writeString(contracts.resolve("produto-a-v1.json"), "{\"slug\":\"produto-a\"}");
    Files.writeString(contracts.resolve("nota.md"), "não é contrato");
    Path outsideContract = tempDir.resolve("fora-do-diretorio.json");
    Files.writeString(outsideContract, "{\"slug\":\"externo\"}");
    Files.createSymbolicLink(contracts.resolve("atalho-externo.json"), outsideContract);
    for (String relativePath : PdeReviewArtifactLoader.communicationImplementationEvidencePaths()) {
      Path artifact = tempDir.resolve(relativePath);
      Files.createDirectories(artifact.getParent());
      Files.writeString(artifact, "prova executável de " + relativePath);
    }

    var evidence = new PdeReviewArtifactLoader(tempDir.toString()).loadCommunicationContracts();

    assertThat(evidence)
        .extracting(item -> item.get("path"))
        .containsExactlyElementsOf(
            java.util.stream.Stream.concat(
                    java.util.stream.Stream.of(
                        "pde-platform/contracts/produto-a-v1.json",
                        "pde-platform/contracts/produto-b-v1.json"),
                    PdeReviewArtifactLoader.communicationImplementationEvidencePaths().stream())
                .toList());
    assertThat(evidence)
        .allSatisfy(
            artifact -> {
              assertThat(artifact.get("content").toString())
                  .containsAnyOf("slug", "prova executável");
              assertThat(artifact.get("contentChecksum").toString()).isNotBlank();
            });
  }
}
