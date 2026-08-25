package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Responsabilidade: garantir que Psique receba a evidência integral e versionada da experiência.
 */
class PdeExperienceEvidenceLoaderTest {
  @TempDir Path tempDir;

  /** Carrega conteúdo, comprimento e checksum de todos os artefatos fixados. */
  @Test
  void loadsVersionedExperienceEvidence() throws Exception {
    for (String relativePath : PdeExperienceEvidenceLoader.evidencePaths()) {
      Path artifact = tempDir.resolve(relativePath);
      Files.createDirectories(artifact.getParent());
      Files.writeString(artifact, "conteúdo verificável de " + relativePath);
    }

    var evidence = new PdeExperienceEvidenceLoader(tempDir.toString()).load();

    assertThat(evidence).hasSize(PdeExperienceEvidenceLoader.evidencePaths().size());
    assertThat(evidence)
        .allSatisfy(
            artifact -> {
              assertThat(artifact.get("content")).asString().contains("conteúdo verificável");
              assertThat(artifact.get("contentLength"))
                  .isEqualTo(artifact.get("content").toString().length());
              assertThat(artifact.get("contentChecksum")).asString().isNotBlank();
            });
  }

  /** Carrega manifesto comercial descoberto e bloqueia provas que não correspondam ao hash. */
  @Test
  void loadsCommercialHomologationEvidenceFromManifest() throws Exception {
    Path proof = tempDir.resolve("pde-platform/frontend/tests/product-journey.spec.ts");
    Files.createDirectories(proof.getParent());
    Files.writeString(proof, "prova comercial íntegra");
    String hash =
        HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(Files.readString(proof).getBytes(StandardCharsets.UTF_8)));
    Path manifest =
        tempDir.resolve("pde-platform/contracts/product-commercial-homologation-v1.json");
    Files.createDirectories(manifest.getParent());
    Files.writeString(
        manifest,
        """
        {"homologationEvidence":[{"path":"pde-platform/frontend/tests/product-journey.spec.ts","sha256":"%s"}]}
        """
            .formatted(hash));

    var evidence =
        new PdeExperienceEvidenceLoader(tempDir.toString()).loadCommercialHomologationEvidence();

    assertThat(evidence)
        .extracting(item -> item.get("path"))
        .containsExactly(
            "pde-platform/contracts/product-commercial-homologation-v1.json",
            "pde-platform/frontend/tests/product-journey.spec.ts");
  }
}
