package com.marketinghub.product.service.agentvalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ResourceLoader;

/**
 * Responsabilidade: impedir uso de prova adulterada, incompleta ou pertencente a outra candidata.
 */
class PdeOperationalControlEvidenceTest {
  private final ObjectMapper json = new ObjectMapper();
  private final ResourceLoader resources = mock(ResourceLoader.class);
  private final PdeOperationalControlEvidence provider =
      new PdeOperationalControlEvidence(json, resources);
  private ObjectNode report;

  /** Monta prova determinística mínima com origem e limites de interpretação explícitos. */
  @BeforeEach
  void setup() throws Exception {
    report =
        (ObjectNode)
            json.readTree(
                """
      {"contractVersion":"PDE_OPERATIONAL_CONTROLS_EVIDENCE_V1","productSlug":"original","prototypeVersion":"v3",
       "status":"PASS","origin":"LOCAL_MYSQL57_WITH_CONTEXT_TEST_DOUBLES","providerCalls":0,"commercialSideEffects":false,
       "requiredCriteria":["CONCURRENT_SINGLE_CONSUMPTION"],
       "criteria":[{"code":"CONCURRENT_SINGLE_CONSUMPTION","status":"PASS","testMethod":"concurrentGenerationConsumesOnce"}]}
      """);
    report.put("generatedAt", Instant.now().minusSeconds(60).toString());
    report.put("frontendSourceFingerprint", "a".repeat(64));
    report.put("testReceiptSha256", "b".repeat(64));
    ((ObjectNode) report.path("criteria").get(0)).put("resultSha256", "c".repeat(64));
  }

  /** Exige identidade e hash exatos, sem atribuir aprovação independente ao teste local. */
  @Test
  void returnsExactEvidenceWithoutApproval() throws Exception {
    catalog(false);
    var evidence = provider.resolve("original", "v3").orElseThrow();
    assertThat(evidence.path("reportSha256").asText()).hasSize(64);
    assertThat(evidence.path("agentApprovalClaimed").asBoolean()).isFalse();
    assertThat(evidence.path("commercialEvidenceClaimed").asBoolean()).isFalse();
    assertThat(provider.resolve("foreign", "v3")).isEmpty();
    assertThat(provider.resolve("original", "v4")).isEmpty();
  }

  /** Demonstra o mesmo mecanismo para outro produto e versão, sem exceção por nome ou ID. */
  @Test
  void acceptsAnotherIdentityWithItsOwnProof() throws Exception {
    report.put("productSlug", "another-product");
    report.put("prototypeVersion", "candidate-seven");
    catalog(false);
    assertThat(provider.resolve("another-product", "candidate-seven")).isPresent();
    assertThat(provider.resolve("original", "v3")).isEmpty();
  }

  /** Recusa divergência entre bytes publicados e o hash declarado no índice. */
  @Test
  void rejectsChangedHash() throws Exception {
    catalog(true);
    assertThat(provider.resolve("original", "v3")).isEmpty();
  }

  /** Um relatório sem todos os critérios declarados nunca abre reavaliação paga. */
  @Test
  void rejectsMissingCriterion() throws Exception {
    report.withArray("requiredCriteria").add("EXPIRED_CREDENTIAL");
    catalog(false);
    assertThat(provider.resolve("original", "v3")).isEmpty();
  }

  /**
   * Mantém resultados reprovados como evidência de bloqueio, sem apresentá-los como cobertura
   * aceita.
   */
  @Test
  void rejectsFailedCriterion() throws Exception {
    ((ObjectNode) report.path("criteria").get(0)).put("status", "FAIL");
    catalog(false);
    assertThat(provider.resolve("original", "v3")).isEmpty();
  }

  /** Recusa custo externo ou origem diferente da prova determinística declarada. */
  @Test
  void rejectsExternalEffectsAndUnidentifiedOrigin() throws Exception {
    report.put("providerCalls", 1);
    catalog(false);
    assertThat(provider.resolve("original", "v3")).isEmpty();
    report.put("providerCalls", 0);
    report.put("origin", "UNREPORTED");
    catalog(false);
    assertThat(provider.resolve("original", "v3")).isEmpty();
  }

  /** Recusa timestamps inválidos e conserva diagnóstico com stack trace. */
  @Test
  void rejectsInvalidDate() throws Exception {
    report.put("generatedAt", "invalid-date");
    catalog(false);
    assertThat(provider.resolve("original", "v3")).isEmpty();
  }

  /** Impede data futura de manter reavaliações repetidas com a mesma prova. */
  @Test
  void rejectsFutureDate() throws Exception {
    report.put("generatedAt", Instant.now().plusSeconds(3600).toString());
    catalog(false);
    assertThat(provider.resolve("original", "v3")).isEmpty();
  }

  /**
   * Confere a prova realmente emitida e impede que alteração da implementação use recibo antigo.
   */
  @Test
  void publishedReceiptMatchesActualSourceFiles() throws Exception {
    var real =
        new PdeOperationalControlEvidence(
            json, new org.springframework.core.io.DefaultResourceLoader());
    var evidence = real.resolve("pde-planejado-36", "mira-private-candidate-v3").orElseThrow();
    assertThat(evidence.path("criteria").size()).isEqualTo(7);
    assertThat(evidence.path("testedProductIds").size()).isEqualTo(2);
    for (var source : evidence.path("sourceFiles")) {
      var path = java.nio.file.Path.of("../..").resolve(source.path("path").asText());
      String actual =
          HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(java.nio.file.Files.readAllBytes(path)));
      assertThat(actual).as(source.path("path").asText()).isEqualTo(source.path("sha256").asText());
    }
  }

  /** Gera índice e conteúdo coerentes exclusivamente em memória, preservando recursos reais. */
  private void catalog(boolean wrongHash) throws Exception {
    byte[] bytes = json.writeValueAsBytes(report);
    String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    var index = json.createObjectNode();
    index
        .putArray("reports")
        .addObject()
        .put("productSlug", report.path("productSlug").asText())
        .put("prototypeVersion", report.path("prototypeVersion").asText())
        .put("resource", "contracts/operational-evidence/fixture-v1.json")
        .put("sha256", wrongHash ? "d".repeat(64) : digest);
    when(resources.getResource("classpath:contracts/pde-operational-controls-catalog-v1.json"))
        .thenReturn(new ByteArrayResource(json.writeValueAsBytes(index)));
    when(resources.getResource("classpath:contracts/operational-evidence/fixture-v1.json"))
        .thenReturn(new ByteArrayResource(bytes));
  }
}
