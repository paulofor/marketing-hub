package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Responsabilidade: garantir que Psique receba a evidência integral e versionada da experiência.
 */
class PdeExperienceEvidenceLoaderTest {
  @TempDir Path tempDir;

  /** Carrega conteúdo, comprimento e checksum dos quatro artefatos fixados. */
  @Test
  void loadsVersionedExperienceEvidence() throws Exception {
    for (String relativePath : PdeExperienceEvidenceLoader.evidencePaths()) {
      Path artifact = tempDir.resolve(relativePath);
      Files.createDirectories(artifact.getParent());
      Files.writeString(artifact, "conteúdo verificável de " + relativePath);
    }

    var evidence = new PdeExperienceEvidenceLoader(tempDir.toString()).load();

    assertThat(evidence).hasSize(4);
    assertThat(evidence)
        .allSatisfy(
            artifact -> {
              assertThat(artifact.get("content")).asString().contains("conteúdo verificável");
              assertThat(artifact.get("contentLength"))
                  .isEqualTo(artifact.get("content").toString().length());
              assertThat(artifact.get("contentChecksum")).asString().isNotBlank();
            });
  }
}
