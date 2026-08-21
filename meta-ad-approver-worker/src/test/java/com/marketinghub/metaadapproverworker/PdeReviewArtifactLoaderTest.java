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
}
