package com.marketinghub.pde.kit.privateprototype.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.product.service.agentvalidation.PdeAgentValidationGateActivityExecutor;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Responsabilidade: impedir que a passagem privada aceite matriz incompleta ou prova de outro
 * contrato.
 */
class KitPrivateProofContractTest {
  private final ObjectMapper json = new ObjectMapper();

  /**
   * Aceita o relatório realmente executado ou a fixture estrutural, preservando os contratos
   * históricos.
   */
  @Test
  void acceptsNineKitCases() throws Exception {
    assertThat(valid(report())).isTrue();
  }

  /**
   * Recusa contagem correta que duplica um caso e perde uma combinação de segurança e dispositivo.
   */
  @Test
  void rejectsDuplicateDeviceCoverage() throws Exception {
    var result = report();
    var cases = (com.fasterxml.jackson.databind.node.ArrayNode) result.path("scenarios");
    cases.set(8, cases.get(0).deepCopy());
    assertThat(valid(result)).isFalse();
  }

  /** Impede aceitar integração paga, arquivo em SAFETY ou perda do pacote recuperado. */
  @Test
  void rejectsPaidCallOrUnsafePackage() throws Exception {
    var result = report();
    result.put("providerCalls", 1);
    assertThat(valid(result)).isFalse();
    result = report();
    for (var c : result.path("scenarios"))
      if ("SAFETY".equals(c.path("scenarioCode").asText()))
        ((ObjectNode) c).put("zipSha256", "a".repeat(64));
    assertThat(valid(result)).isFalse();
  }

  /** Recusa o pacote que a matriz antiga aprovava apesar de não cumprir os arquivos individuais. */
  @Test
  void rejectsOldPackageFormatAndMissingFiles() throws Exception {
    var result = report();
    result.remove("packageContractVersion");
    assertThat(valid(result)).isFalse();
    result = report();
    ((ObjectNode) result.path("scenarios").get(0)).put("packageFileCount", 24);
    assertThat(valid(result)).isFalse();
  }

  /** Mantém matriz histórica de cinco provas válida e recusa contrato desconhecido. */
  @Test
  void preservesExistingContractAndRejectsUnknown() {
    var result = json.createObjectNode();
    var cases = result.putArray("scenarios");
    for (int i = 0; i < 5; i++) cases.addObject();
    assertThat(valid(result)).isTrue();
    result.put("fixtureContract", "UNKNOWN");
    assertThat(valid(result)).isFalse();
  }

  /** Executa o mesmo gate de conclusão utilizado pelo processo da cadeia. */
  private boolean valid(JsonNode result) {
    var gate =
        mock(PdeAgentValidationGateActivityExecutor.class, org.mockito.Mockito.CALLS_REAL_METHODS);
    return Boolean.TRUE.equals(
        ReflectionTestUtils.invokeMethod(gate, "validTechnicalScenarioMatrix", result));
  }

  /**
   * Usa a prova de navegador local quando disponível e uma fixture segregada nos demais testes
   * unitários.
   */
  private ObjectNode report() throws Exception {
    String path = System.getenv("KIT_LOCAL_REPORT");
    if (path != null) return (ObjectNode) json.readTree(Files.readString(Path.of(path)));
    var report =
        json.createObjectNode()
            .put("fixtureContract", "PDE_PRIVATE_KIT_FIXTURES_V1")
            .put("packageContractVersion", "PDE_PRIVATE_KIT_PACKAGE_V2")
            .put("profileCode", "nails-v1")
            .put("providerCalls", 0);
    var cases = report.putArray("scenarios");
    for (String s : List.of("ADHERENT", "RECOVERY", "SAFETY"))
      for (String d : List.of("DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7")) {
        var c =
            cases
                .addObject()
                .put("scenarioCode", s)
                .put("deviceProfile", d)
                .put("status", "PASS")
                .put("trafficClass", "AGENT_VALIDATION")
                .put("mhInternalTest", true)
                .put("providerCalls", 0)
                .put("humanEvidenceClaimed", false)
                .put("commercialEvidenceClaimed", false)
                .put("resumed", true)
                .put("safetyBlocked", true);
        c.put("packageFileCount", s.equals("SAFETY") ? 0 : 36);
        if (s.equals("SAFETY")) c.putNull("zipSha256");
        else c.put("zipSha256", "a".repeat(64));
        c.putObject("sideEffects")
            .put("paymentEnabled", false)
            .put("published", false)
            .put("campaignCreated", false)
            .put("mediaSpendBrl", 0);
      }
    return report;
  }
}
