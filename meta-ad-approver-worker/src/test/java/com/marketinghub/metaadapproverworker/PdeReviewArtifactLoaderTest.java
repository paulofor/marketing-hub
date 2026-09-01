package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
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

    assertThat(evidence).hasSize(PdeReviewArtifactLoader.artifactPaths().size());
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

  /** Impede que contratos gerais silenciem uma alteração em prova executável declarada. */
  @Test
  void rejectsHomologationManifestWithStaleEvidenceHash() throws Exception {
    Path contracts = tempDir.resolve("pde-platform/contracts");
    Files.createDirectories(contracts);
    Files.writeString(
        contracts.resolve("produto-homologation-v1.json"),
        """
        {
          "productSlug": "produto-a",
          "evidenceVersion": "produto-homologation-v1",
          "implementationEvidence": [
            {
              "path": "pde-platform/frontend/src/AssistedServiceApp.tsx",
              "sha256": "0000000000000000000000000000000000000000000000000000000000000000"
            }
          ]
        }
        """);
    for (String relativePath : PdeReviewArtifactLoader.communicationImplementationEvidencePaths()) {
      Path artifact = tempDir.resolve(relativePath);
      Files.createDirectories(artifact.getParent());
      Files.writeString(artifact, "prova executável de " + relativePath);
    }

    var loader = new PdeReviewArtifactLoader(tempDir.toString());

    assertThatThrownBy(loader::loadCommunicationContracts)
        .isInstanceOf(IOException.class)
        .hasMessageContaining("SHA-256 divergente")
        .hasMessageContaining("AssistedServiceApp.tsx");
  }

  /** Preserva manifesto histórico divergente quando a revisão atual comprova o código candidato. */
  @Test
  void validatesOnlyLatestHomologationManifestForEachProduct() throws Exception {
    Path contracts = tempDir.resolve("pde-platform/contracts");
    Path proof = tempDir.resolve("pde-platform/frontend/src/AssistedServiceApp.tsx");
    Files.createDirectories(contracts);
    Files.createDirectories(proof.getParent());
    Files.writeString(proof, "candidata comercial atual");
    String currentHash =
        HexFormat.of()
            .formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(proof)));
    Files.writeString(
        contracts.resolve("produto-homologation-v1.json"),
        """
        {
          "evidenceVersion":"produto-homologation-v1",
          "productSlug":"produto-a",
          "implementationEvidence":[{"path":"pde-platform/frontend/src/AssistedServiceApp.tsx","sha256":"%s"}]
        }
        """
            .formatted("0".repeat(64)));
    Files.writeString(
        contracts.resolve("produto-homologation-v2.json"),
        """
        {
          "evidenceVersion":"produto-homologation-v2",
          "productSlug":"produto-a",
          "implementationEvidence":[{"path":"pde-platform/frontend/src/AssistedServiceApp.tsx","sha256":"%s"}]
        }
        """
            .formatted(currentHash));
    for (String relativePath : PdeReviewArtifactLoader.communicationImplementationEvidencePaths()) {
      Path artifact = tempDir.resolve(relativePath);
      Files.createDirectories(artifact.getParent());
      if (!Files.exists(artifact)) {
        Files.writeString(artifact, "prova executável de " + relativePath);
      }
    }

    var evidence = new PdeReviewArtifactLoader(tempDir.toString()).loadCommunicationContracts();

    assertThat(evidence)
        .extracting(item -> item.get("path"))
        .contains(
            "pde-platform/contracts/produto-homologation-v1.json",
            "pde-platform/contracts/produto-homologation-v2.json");
  }

  /**
   * Confirma que o manifesto versionado do repositório aponta para a revisão posterior à tarefa
   * 272.
   */
  @Test
  void validatesCurrentRepositoryHomologationManifest() throws Exception {
    Path moduleDirectory = Path.of("").toAbsolutePath().normalize();
    Path repository =
        moduleDirectory.getFileName().toString().equals("meta-ad-approver-worker")
            ? moduleDirectory.getParent()
            : moduleDirectory;

    var evidence = new PdeReviewArtifactLoader(repository.toString()).loadCommunicationContracts();

    assertThat(evidence)
        .extracting(item -> item.get("path"))
        .contains(
            "pde-platform/contracts/kit-whatsapp-tasting-homologation-v2.json",
            "pde-platform/contracts/kit-whatsapp-tasting-homologation-v3.json");
  }

  /** Entrega à revisão independente somente a prova comercial declarada e íntegra. */
  @Test
  void loadsCommercialHomologationManifestAndEvidence() throws Exception {
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
          "homologationEvidence":[{
            "path":"pde-platform/frontend/tests/product-journey.spec.ts",
            "sha256":"%s",
            "promptMode":"ATTESTED_REFERENCE",
            "reviewSummary":"Jornada comercial validada pelo teste específico."
          }]
        }
        """
            .formatted(hash));
    Files.writeString(
        manifest.getParent().resolve("unrelated-commercial-homologation-v1.json"),
        """
        {
          "contractVersion":"unrelated-commercial-homologation.v1",
          "product":{"id":4,"slug":"produto-b","experienceVersion":"produto-b-v1"},
          "homologationEvidence":[{"path":"arquivo-ausente.txt","sha256":"%s"}]
        }
        """
            .formatted("0".repeat(64)));

    var evidence =
        new PdeReviewArtifactLoader(tempDir.toString())
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
        .containsEntry("bundleIntegrity", "LOCAL_SOURCE")
        .containsEntry("promptMode", "ATTESTED_REFERENCE")
        .containsEntry("reviewSummary", "Jornada comercial validada pelo teste específico.")
        .doesNotContainKey("content");
  }

  /** Entrega a candidata modificada para nova revisão em vez de reutilizar parecer antigo. */
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
        new PdeReviewArtifactLoader(tempDir.toString())
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

  /** Impede que a compactação esconda conteúdo sem uma síntese explícita e auditável. */
  @Test
  void rejectsAttestedReferenceWithoutReviewSummary() throws Exception {
    Path proof = tempDir.resolve("pde-platform/frontend/src/App.tsx");
    Files.createDirectories(proof.getParent());
    Files.writeString(proof, "conteúdo amplo");
    String hash =
        HexFormat.of()
            .formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(proof)));
    Path manifest =
        tempDir.resolve("pde-platform/contracts/product-commercial-homologation-v1.json");
    Files.createDirectories(manifest.getParent());
    Files.writeString(
        manifest,
        """
        {
          "contractVersion":"product-commercial-homologation.v1",
          "product":{"id":4,"slug":"produto-b","experienceVersion":"produto-b-v1"},
          "homologationEvidence":[{
            "path":"pde-platform/frontend/src/App.tsx",
            "sha256":"%s",
            "promptMode":"ATTESTED_REFERENCE"
          }]
        }
        """
            .formatted(hash));

    assertThatThrownBy(
            () ->
                new PdeReviewArtifactLoader(tempDir.toString())
                    .loadCommercialHomologationEvidence(
                        Map.of(
                            "experimentId",
                            90L,
                            "productId",
                            4L,
                            "productSlug",
                            "produto-b",
                            "experienceVersion",
                            "produto-b-v1")))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("sem resumo verificável");
  }

  /** Bloqueia alteração posterior ao pacote imutável entregue ao container de Têmis. */
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
    var loader = new PdeReviewArtifactLoader(tempDir.toString());

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
        .isInstanceOf(IOException.class)
        .hasMessageContaining("pacote comercial")
        .hasMessageContaining("App.tsx");
  }

  /** Confirma no repositório real que Têmis não mistura provas comerciais entre PDEs. */
  @Test
  void segregatesCurrentRepositoryEvidenceByProduct() throws Exception {
    Path moduleDirectory = Path.of("").toAbsolutePath().normalize();
    Path repository =
        moduleDirectory.getFileName().toString().equals("meta-ad-approver-worker")
            ? moduleDirectory.getParent()
            : moduleDirectory;
    var loader = new PdeReviewArtifactLoader(repository.toString());

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
        .contains("pde-platform/contracts/kit-whatsapp-tasting-homologation-v3.json")
        .doesNotContain("pde-platform/contracts/kit-whatsapp-tasting-homologation-v2.json")
        .doesNotContain("pde-platform/contracts/musa-v7-commercial-homologation-v1.json");
    assertThat(vega)
        .extracting(item -> item.get("path"))
        .contains(
            "pde-platform/contracts/musa-v7-commercial-homologation-v3.json",
            "pde-platform/frontend/src/App.tsx")
        .doesNotContain(
            "pde-platform/contracts/musa-v7-commercial-homologation-v1.json",
            "pde-platform/contracts/musa-v7-commercial-homologation-v2.json",
            "pde-platform/contracts/kit-whatsapp-tasting-homologation-v1.json",
            "pde-platform/contracts/kit-whatsapp-tasting-homologation-v2.json",
            "pde-platform/contracts/kit-whatsapp-tasting-homologation-v3.json");
  }
}
