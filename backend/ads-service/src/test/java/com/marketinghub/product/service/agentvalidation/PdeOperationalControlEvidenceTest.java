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

  /** Aceita os controles executados em identidades independentes para qualquer candidata. */
  @Test
  void acceptsExecutedControlsAcrossIndependentContexts() throws Exception {
    operationalReport();
    catalog(false);
    assertThat(provider.resolve("original", "v3")).isPresent();
    report.put("productSlug", "another-product");
    report.put("prototypeVersion", "candidate-seven");
    catalog(false);
    assertThat(provider.resolve("another-product", "candidate-seven")).isPresent();
  }

  /** Impede nova revisão paga quando um controle obrigatório não foi executado. */
  @Test
  void rejectsPartialOperationalCoverage() throws Exception {
    operationalReport();
    report.withArray("criteria").remove(0);
    report.withArray("requiredCriteria").remove(0);
    catalog(false);
    assertThat(provider.resolve("original", "v3")).isEmpty();
  }

  /** Uma contagem observada não equivale a executar concorrência, limites ou acesso inválido. */
  @Test
  void rejectsObservationsDisguisedAsExecutedControls() throws Exception {
    operationalReport();
    ((ObjectNode) report.path("criteria").get(0)).put("verificationType", "OBSERVATION");
    catalog(false);
    assertThat(provider.resolve("original", "v3")).isEmpty();
  }

  /** A cobertura deve preservar regressão em outra identidade, sem duplicar a mesma execução. */
  @Test
  void rejectsMissingOrDuplicatedIndependentContext() throws Exception {
    operationalReport();
    var contexts =
        (com.fasterxml.jackson.databind.node.ArrayNode)
            report.path("criteria").get(0).path("result").path("contexts");
    ((ObjectNode) contexts.get(1)).put("productId", contexts.get(0).path("productId").asLong());
    catalog(false);
    assertThat(provider.resolve("original", "v3")).isEmpty();
    contexts.remove(1);
    catalog(false);
    assertThat(provider.resolve("original", "v3")).isEmpty();
  }

  /** Resultados vazios ou reprovados não podem satisfazer o contrato de execução. */
  @Test
  void rejectsMissingObservedResultOrFailedContext() throws Exception {
    operationalReport();
    var context =
        (ObjectNode) report.path("criteria").get(0).path("result").path("contexts").get(0);
    context.put("status", "FAIL");
    catalog(false);
    assertThat(provider.resolve("original", "v3")).isEmpty();
    context.put("status", "PASS");
    context.putObject("observed");
    catalog(false);
    assertThat(provider.resolve("original", "v3")).isEmpty();
  }

  /** Contratos desconhecidos não abrem reavaliação com regras silenciosamente incompletas. */
  @Test
  void rejectsUnknownOperationalContract() throws Exception {
    operationalReport();
    report.put("operationalControlsContract", "UNKNOWN_V1");
    catalog(false);
    assertThat(provider.resolve("original", "v3")).isEmpty();
  }

  /** Monta recibo sintético do contrato compartilhado sem exceções por produto ou versão. */
  private void operationalReport() throws Exception {
    byte[] raw;
    try (var input =
        getClass().getResourceAsStream("/contracts/pde-private-execution-controls-v1.json")) {
      raw = input.readAllBytes();
    }
    var policy = json.readTree(raw);
    when(resources.getResource("classpath:contracts/pde-private-execution-controls-v1.json"))
        .thenReturn(new ByteArrayResource(raw));
    report.put("operationalControlsContract", policy.path("contractVersion").asText());
    report.set("requiredCriteria", policy.path("requiredCriteria").deepCopy());
    var criteria = report.putArray("criteria");
    for (var code : policy.path("requiredCriteria")) {
      var criterion =
          criteria
              .addObject()
              .put("code", code.asText())
              .put("status", "PASS")
              .put("verificationType", "EXECUTION")
              .put("testMethod", "executedControl")
              .put("resultSha256", "c".repeat(64));
      var contexts = criterion.putObject("result").put("status", "PASS").putArray("contexts");
      for (long id : new long[] {91004L, 97004L}) {
        contexts
            .addObject()
            .put("productId", id)
            .put("status", "PASS")
            .putObject("observed")
            .put("requestStatus", 200);
      }
    }
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

  /** A consulta por hash preserva o relatório aprovado sem substituir a seleção atual. */
  @Test
  void resolvesHistoricalHashWithoutPromotingItToCurrent() throws Exception {
    catalog(false);
    byte[] previous = json.writeValueAsBytes(report);
    String previousHash =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(previous));
    report.put("generatedAt", Instant.now().minusSeconds(30).toString());
    byte[] current = json.writeValueAsBytes(report);
    String currentHash =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(current));
    var index = json.createObjectNode();
    for (String section : java.util.List.of("reports", "historicalReports")) {
      boolean historical = section.equals("historicalReports");
      index
          .putArray(section)
          .addObject()
          .put("productSlug", "original")
          .put("prototypeVersion", "v3")
          .put(
              "resource",
              "contracts/operational-evidence/" + (historical ? "previous" : "current") + ".json")
          .put("sha256", historical ? previousHash : currentHash);
    }
    when(resources.getResource("classpath:contracts/pde-operational-controls-catalog-v1.json"))
        .thenReturn(new ByteArrayResource(json.writeValueAsBytes(index)));
    when(resources.getResource("classpath:contracts/operational-evidence/previous.json"))
        .thenReturn(new ByteArrayResource(previous));
    when(resources.getResource("classpath:contracts/operational-evidence/current.json"))
        .thenReturn(new ByteArrayResource(current));
    assertThat(provider.resolve("original", "v3").orElseThrow().path("reportSha256").asText())
        .isEqualTo(currentHash);
    assertThat(
            provider
                .resolveReviewed("original", "v3", previousHash)
                .orElseThrow()
                .path("reportSha256")
                .asText())
        .isEqualTo(previousHash);
    assertThat(provider.resolveReviewed("original", "v3", currentHash)).isPresent();
    assertThat(provider.resolveReviewed("foreign", "v3", previousHash)).isEmpty();
    assertThat(provider.resolveReviewed("original", "v4", previousHash)).isEmpty();
    assertThat(provider.resolveReviewed("original", "v3", "d".repeat(64))).isEmpty();
    assertThat(provider.resolveReviewed("original", "v3", null)).isEmpty();
    when(resources.getResource("classpath:contracts/operational-evidence/previous.json"))
        .thenReturn(new ByteArrayResource(current));
    assertThat(provider.resolveReviewed("original", "v3", previousHash)).isEmpty();
  }

  /**
   * Reproduz a prova de Mira aceita no gate sem confundi-la com o relatório posterior da
   * integração.
   */
  @Test
  void preservesPublishedReviewedReportBesideCurrentIntegrationProof() {
    var real =
        new PdeOperationalControlEvidence(
            json, new org.springframework.core.io.DefaultResourceLoader());
    String previous = "de6c61092ce54485dfac732eb8afc3c81d7dba65ca7814abf412777ed42536a4";
    var old =
        real.resolveReviewed("pde-planejado-36", "mira-private-candidate-v3", previous)
            .orElseThrow();
    var current = real.resolve("pde-planejado-36", "mira-private-candidate-v3").orElseThrow();
    assertThat(old.path("generatedAt").asText()).startsWith("2026-10-07");
    assertThat(current.path("reportSha256").asText()).isNotEqualTo(previous);
    assertThat(old.path("agentApprovalClaimed").asBoolean()).isFalse();
    assertThat(old.path("commercialEvidenceClaimed").asBoolean()).isFalse();
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

  /** Confere a compatibilidade v6, preservando os controles e recibos históricos anteriores. */
  @Test
  void vegaTerminalSupplementPreservesExactPublishedImplementation() throws Exception {
    var real =
        new PdeOperationalControlEvidence(
            json, new org.springframework.core.io.DefaultResourceLoader());
    var evidence =
        real.resolve("metodo-musa-7-dias", "musa-pde-entry-v14-primeiro-ajuste-aplicavel")
            .orElseThrow();
    String previousHash = "fdf3783c20a06004f979b6153040e43ae15fc34cd3fe56ca19529a6bc07930ed";
    var previous =
        real.resolveReviewed(
                "metodo-musa-7-dias", "musa-pde-entry-v14-primeiro-ajuste-aplicavel", previousHash)
            .orElseThrow();
    assertThat(evidence.path("criteria")).isEqualTo(previous.path("criteria"));
    assertThat(evidence.path("generatedAt")).isEqualTo(previous.path("generatedAt"));
    assertThat(evidence.path("testReceiptSha256")).isEqualTo(previous.path("testReceiptSha256"));
    var historicalV2 =
        real.resolveReviewed(
                "metodo-musa-7-dias",
                "musa-pde-entry-v14-primeiro-ajuste-aplicavel",
                "87b8d3d08032dd54b86b4ed5400f7b3792746d95a5e102376ab9914eb026102e")
            .orElseThrow();
    assertThat(historicalV2.path("criteria")).isEqualTo(previous.path("criteria"));
    assertThat(historicalV2.path("testReceiptSha256"))
        .isEqualTo(previous.path("testReceiptSha256"));
    var compatibility = evidence.path("compatibilityAttestation");
    assertThat(compatibility.path("previousReportSha256").asText()).isEqualTo(previousHash);
    assertThat(fileHash(compatibility.path("evidencePath").asText()))
        .isEqualTo(compatibility.path("evidenceSha256").asText());
    var receipt =
        json.readTree(
            java.nio.file.Path.of("../..")
                .resolve(compatibility.path("evidencePath").asText())
                .toFile());
    assertThat(receipt.path("executedCases").size()).isEqualTo(27);
    assertThat(receipt.path("additionalValidation").size()).isEqualTo(17);
    assertThat(receipt.path("providerCalls").asInt()).isZero();
    assertThat(fileHash(receipt.path("testSource").asText()))
        .isEqualTo(receipt.path("testSourceSha256").asText());
    receipt
        .path("executedCases")
        .forEach(item -> assertThat(item.path("status").asText()).isEqualTo("PASS"));
    assertThat(evidence.path("criteria").size()).isEqualTo(25);
    assertThat(evidence.path("testedProductIds").get(0).asLong()).isEqualTo(91004L);
    assertThat(evidence.path("testedProductIds").get(1).asLong()).isEqualTo(97004L);
    assertThat(evidence.path("operationalControlsContract").asText())
        .isEqualTo("PDE_PRIVATE_EXECUTION_CONTROLS_V1");
    int executed = 0;
    var cases = new java.util.HashSet<String>();
    for (var criterion : evidence.path("criteria")) {
      if ("EXECUTION".equals(criterion.path("verificationType").asText())) {
        executed++;
        assertThat(criterion.path("result").path("contexts").size()).isEqualTo(2);
      }
      if (!criterion.path("code").asText().startsWith("TERMINAL_")) continue;
      var result = criterion.path("result");
      cases.add(
          result.path("generationStatus").asText() + "|" + result.path("deviceProfile").asText());
      assertThat(result.path("sessionState").asText()).isEqualTo("FINISHED");
      assertThat(result.path("reloadedMessage")).isEqualTo(result.path("terminalMessage"));
      var path =
          java.nio.file.Path.of("../..").resolve(result.path("screenshot").path("path").asText());
      assertThat(
              HexFormat.of()
                  .formatHex(
                      MessageDigest.getInstance("SHA-256")
                          .digest(java.nio.file.Files.readAllBytes(path))))
          .isEqualTo(result.path("screenshot").path("sha256").asText());
    }
    assertThat(cases).hasSize(12);
    assertThat(executed).isEqualTo(10);
    for (var source : evidence.path("sourceFiles")) {
      assertThat(fileHash(source.path("path").asText()))
          .as(source.path("path").asText())
          .isEqualTo(source.path("sha256").asText());
    }
    assertThat(fileHash(evidence.path("testReceiptPath").asText()))
        .isEqualTo(evidence.path("testReceiptSha256").asText());
    String reviewedHash = "afe0057408ef9e8372256419509a4fc4621f2665beb72eeeab0a24faa82aa42a";
    var reviewed =
        real.resolveReviewed(
                "metodo-musa-7-dias", "musa-pde-entry-v14-primeiro-ajuste-aplicavel", reviewedHash)
            .orElseThrow();
    assertThat(reviewed.path("criteria").size()).isEqualTo(15);
    assertThat(evidence.path("reusedTerminalEvidence").path("reportSha256").asText())
        .isEqualTo(reviewedHash);
    assertThat(evidence.path("reusedTerminalEvidence").path("generatedAt"))
        .isEqualTo(reviewed.path("generatedAt"));
    assertThat(real.resolve("metodo-musa-7-dias", "musa-pde-entry-v13-primeiro-ajuste-aplicavel"))
        .isEmpty();
    assertThat(evidence.path("agentApprovalClaimed").asBoolean()).isFalse();
  }

  /** Calcula hash dos bytes versionados para impedir recibo incompatível com fonte alterada. */
  private String fileHash(String relativePath) throws Exception {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256")
                .digest(
                    java.nio.file.Files.readAllBytes(
                        java.nio.file.Path.of("../..").resolve(relativePath))));
  }
}
