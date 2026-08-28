package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
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

  /** Carrega somente o manifesto do produto solicitado e preserva o hash da candidata atual. */
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
        {
          "contractVersion":"product-commercial-homologation.v1",
          "product":{"id":9,"slug":"produto-a","experienceVersion":"produto-a-v1"},
          "homologationEvidence":[{"path":"pde-platform/frontend/tests/product-journey.spec.ts","sha256":"%s"}]
        }
        """
            .formatted(hash));
    Path unrelated =
        tempDir.resolve("pde-platform/contracts/unrelated-commercial-homologation-v1.json");
    Files.writeString(
        unrelated,
        """
        {
          "contractVersion":"unrelated-commercial-homologation.v1",
          "product":{"id":4,"slug":"produto-b","experienceVersion":"produto-b-v1"},
          "homologationEvidence":[{"path":"arquivo-ausente.txt","sha256":"%s"}]
        }
        """
            .formatted("0".repeat(64)));

    var evidence =
        new PdeExperienceEvidenceLoader(tempDir.toString())
            .loadCommercialHomologationEvidence(
                Map.of(
                    "experimentId",
                    89L,
                    "productId",
                    9L,
                    "productSlug",
                    "produto-a",
                    "experienceVersion",
                    "produto-a-v1"));

    assertThat(evidence)
        .extracting(item -> item.get("path"))
        .containsExactly(
            "pde-platform/contracts/product-commercial-homologation-v1.json",
            "pde-platform/frontend/tests/product-journey.spec.ts")
        .doesNotContain("arquivo-ausente.txt");
    assertThat(evidence.get(1))
        .containsEntry("baselineIntegrity", "MATCH")
        .containsEntry("bundleIntegrity", "LOCAL_SOURCE");
  }

  /** Entrega uma nova candidata para reavaliação sem tratá-la como corrupção do pacote. */
  @Test
  void marksChangedBaselineAsUpdatedCandidate() throws Exception {
    Path proof = tempDir.resolve("pde-platform/frontend/src/App.tsx");
    Files.createDirectories(proof.getParent());
    Files.writeString(proof, "candidata atual");
    Path manifest =
        tempDir.resolve("pde-platform/contracts/product-commercial-homologation-v1.json");
    Files.createDirectories(manifest.getParent());
    Files.writeString(
        manifest,
        """
        {
          "contractVersion":"product-commercial-homologation.v1",
          "product":{"id":4,"slug":"produto-b","experienceVersion":"produto-b-v1"},
          "homologationEvidence":[{"path":"pde-platform/frontend/src/App.tsx","sha256":"%s"}]
        }
        """
            .formatted("0".repeat(64)));

    var evidence =
        new PdeExperienceEvidenceLoader(tempDir.toString())
            .loadCommercialHomologationEvidence(
                Map.of(
                    "experimentId",
                    90L,
                    "productId",
                    4L,
                    "productSlug",
                    "produto-b",
                    "experienceVersion",
                    "produto-b-v1"));

    assertThat(evidence.get(1))
        .containsEntry("baselineIntegrity", "UPDATED_CANDIDATE")
        .containsEntry("content", "candidata atual");
  }

  /** Bloqueia alteração posterior ao empacotamento imutável usado pelo container. */
  @Test
  void rejectsEvidenceThatDiffersFromBundleIndex() throws Exception {
    Path proof = tempDir.resolve("pde-platform/frontend/src/App.tsx");
    Files.createDirectories(proof.getParent());
    Files.writeString(proof, "arquivo alterado depois do build");
    Path manifest =
        tempDir.resolve("pde-platform/contracts/product-commercial-homologation-v1.json");
    Files.createDirectories(manifest.getParent());
    Files.writeString(
        manifest,
        """
        {
          "contractVersion":"product-commercial-homologation.v1",
          "product":{"id":4,"slug":"produto-b","experienceVersion":"produto-b-v1"},
          "homologationEvidence":[{"path":"pde-platform/frontend/src/App.tsx","sha256":"%s"}]
        }
        """
            .formatted("0".repeat(64)));
    String manifestHash =
        HexFormat.of()
            .formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(manifest)));
    Files.writeString(
        tempDir.resolve("commercial-review-bundle-index-v1.json"),
        """
        {
          "bundleVersion":"pde-commercial-review-evidence-v1",
          "files":[
            {"path":"pde-platform/contracts/product-commercial-homologation-v1.json","sha256":"%s"},
            {"path":"pde-platform/frontend/src/App.tsx","sha256":"%s"}
          ]
        }
        """
            .formatted(manifestHash, "f".repeat(64)));
    var loader = new PdeExperienceEvidenceLoader(tempDir.toString());

    assertThatThrownBy(
            () ->
                loader.loadCommercialHomologationEvidence(
                    Map.of(
                        "experimentId",
                        90L,
                        "productId",
                        4L,
                        "productSlug",
                        "produto-b",
                        "experienceVersion",
                        "produto-b-v1")))
        .isInstanceOf(java.io.IOException.class)
        .hasMessageContaining("pacote comercial")
        .hasMessageContaining("App.tsx");
  }

  /** Confirma no repositório real que Rigel e Vega recebem somente suas próprias provas. */
  @Test
  void segregatesCurrentRepositoryEvidenceByProduct() throws Exception {
    Path moduleDirectory = Path.of("").toAbsolutePath().normalize();
    Path repository =
        moduleDirectory.getFileName().toString().equals("customer-agent-worker")
            ? moduleDirectory.getParent()
            : moduleDirectory;
    var loader = new PdeExperienceEvidenceLoader(repository.toString());

    var rigel =
        loader.loadCommercialHomologationEvidence(
            Map.of(
                "experimentId",
                89L,
                "productId",
                9L,
                "productSlug",
                "kit-whatsapp-pronto",
                "experienceVersion",
                "kit-whatsapp-pronto-pde-v2"));
    var vega =
        loader.loadCommercialHomologationEvidence(
            Map.of(
                "experimentId",
                90L,
                "productId",
                4L,
                "productSlug",
                "metodo-musa-7-dias",
                "experienceVersion",
                "musa-pde-entry-v7-espelho-antes-de-sair"));

    assertThat(rigel)
        .extracting(item -> item.get("path"))
        .contains("pde-platform/contracts/kit-whatsapp-tasting-homologation-v1.json")
        .doesNotContain(
            "pde-platform/contracts/musa-v7-commercial-homologation-v1.json",
            "pde-platform/frontend/src/App.tsx");
    assertThat(vega)
        .extracting(item -> item.get("path"))
        .contains(
            "pde-platform/contracts/musa-v7-commercial-homologation-v1.json",
            "pde-platform/frontend/src/App.tsx")
        .doesNotContain("pde-platform/contracts/kit-whatsapp-tasting-homologation-v1.json");
  }
}
