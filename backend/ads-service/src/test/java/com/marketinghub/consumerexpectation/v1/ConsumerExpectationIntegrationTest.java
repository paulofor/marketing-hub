package com.marketinghub.consumerexpectation.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycleEvent;
import com.marketinghub.consumerexpectation.v1.controller.ConsumerExpectationController;
import com.marketinghub.consumerexpectation.v1.service.ConsumerExpectationService;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Exercita HTTP real, validação, planejamento e leituras segregadas com fontes locais sintéticas.
 */
@SpringBootTest(
    classes = ConsumerExpectationIntegrationTest.App.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.main.banner-mode=off", "spring.jpa.open-in-view=false"})
class ConsumerExpectationIntegrationTest {
  /** Inicializa somente a capacidade em teste, sem serviços, credenciais ou filas de produção. */
  @Configuration
  @EnableAutoConfiguration(
      exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        LiquibaseAutoConfiguration.class
      })
  @Import({ConsumerExpectationController.class, ConsumerExpectationService.class})
  static class App {}

  @MockBean LearningSalesCycleRepository cycles;
  @MockBean LearningSalesCycleEventRepository events;
  @Autowired TestRestTemplate http;
  @Autowired ObjectMapper mapper;
  @LocalServerPort int port;
  private static final String BASE = "/api/consumer-expectations/v1";

  /** Prepara dois produtos independentes e uma medição conciliada de teste, nunca comercial. */
  @BeforeEach
  void setup() {
    for (long id : List.of(501L, 701L)) {
      var cycle = new LearningSalesCycle();
      cycle.setId(id);
      cycle.setProductId(id + 1);
      cycle.setExperimentId(id + 2);
      cycle.setChainDefinitionId(800L);
      cycle.setProductVersion("synthetic-" + id);
      cycle.setVersionChangedAt(Instant.parse("2026-11-01T00:00:00Z"));
      when(cycles.findById(id)).thenReturn(Optional.of(cycle));
    }
    when(events.findFirstByCycleIdAndActionInOrderByRevisionDesc(anyLong(), anyCollection()))
        .thenReturn(Optional.empty());
  }

  /** Confere fontes científicas distintas das hipóteses e controles contra promessa causal. */
  @Test
  void protocolKeepsEvidenceAndHypothesisSeparate() {
    var p = http.getForObject(BASE + "/protocol", JsonNode.class);
    assertThat(p.path("sources")).hasSize(11);
    assertThat(p.path("axes")).hasSize(6);
    assertThat(p.path("evidenceKind").asText()).isEqualTo("SCIENTIFIC_SYNTHESIS_NOT_MARKET_PROOF");
  }

  /** Reproduz as quatro superfícies, dois públicos e estabilidade sem gravação ou cobrança. */
  @Test
  void preparesContextualVariantsWithoutSideEffects() {
    for (String surface : List.of("AD", "OFFER", "PRODUCT", "DIGITAL_EXPERIENCE")) {
      var input = input(surface);
      var result = http.postForObject(BASE + "/plans/preview", input, JsonNode.class);
      assertThat(result.path("variants")).hasSize(3);
      assertThat(result.path("status").asText()).isEqualTo("DRAFT_HYPOTHESIS");
      assertThat(result.path("gaps")).hasSize(4);
      assertThat(result.path("commercialAuthorization").asBoolean()).isFalse();
      var repeated = http.postForObject(BASE + "/plans/preview", input, JsonNode.class);
      assertThat(repeated).isEqualTo(result);
      input.put("audience", "Outro público sintético");
      var other = http.postForObject(BASE + "/plans/preview", input, JsonNode.class);
      assertThat(other.path("fingerprint")).isNotEqualTo(result.path("fingerprint"));
      assertThat(other.path("variants").get(0).path("hypothesis").asText())
          .contains("Outro público");
    }
    verifyNoInteractions(cycles, events);
  }

  /**
   * Impede valores vazios, enum desconhecido e entrada sem limites antes de preparar alternativas.
   */
  @Test
  void rejectsInvalidInputs() {
    for (var change :
        List.of(
            Map.entry("audience", " "),
            Map.entry("surface", "UNKNOWN"),
            Map.entry("context", "a".repeat(501)))) {
      var input = input("AD");
      input.put(change.getKey(), change.getValue());
      assertThat(
              http.postForEntity(BASE + "/plans/preview", input, String.class)
                  .getStatusCode()
                  .value())
          .isEqualTo(400);
    }
  }

  /** Mantém ausência desconhecida e rejeita leitura cruzada de produto/ciclo. */
  @Test
  void isolatesIdentityAndMissingMeasurements() {
    assertThat(learning(502, 501).path("metrics")).isEmpty();
    assertThat(learning(702, 701).path("status").asText()).isEqualTo("MEASUREMENT_REQUIRED");
    assertThat(
            http.getForEntity(BASE + "/products/702/cycles/501/learning", String.class)
                .getStatusCode()
                .value())
        .isEqualTo(404);
  }

  /**
   * Usa só a tentativa mais recente; bloqueio, contrato falso e versão antiga não viram sucesso.
   */
  @Test
  void doesNotReuseStaleOrInvalidSuccess() {
    for (String invalid :
        List.of(
            "blocked",
            "old",
            "wrongExperiment",
            "testIncluded",
            "unknownContract",
            "nullFingerprint",
            "numericFingerprint",
            "blankFingerprint",
            "malformed",
            "empty")) {
      var e = event(3, 100);
      if (invalid.equals("blocked")) e.setAction("MEASUREMENT_BLOCKED");
      if (invalid.equals("old")) e.setCreatedAt(Instant.parse("2026-10-01T00:00:00Z"));
      if (invalid.equals("wrongExperiment"))
        e.setEvidenceJson(e.getEvidenceJson().replace("503", "999"));
      if (invalid.equals("testIncluded"))
        e.setEvidenceJson(
            e.getEvidenceJson().replace("\"testDataExcluded\":true", "\"testDataExcluded\":false"));
      if (invalid.equals("unknownContract"))
        e.setEvidenceJson(e.getEvidenceJson().replace("MEASUREMENT_V2", "MEASUREMENT_V0"));
      if (invalid.endsWith("Fingerprint")) {
        String value =
            switch (invalid) {
              case "nullFingerprint" -> "null";
              case "numericFingerprint" -> "123";
              default -> "\" \"";
            };
        e.setEvidenceJson(e.getEvidenceJson().replace("\"synthetic-reconciled-source\"", value));
      }
      if (invalid.equals("malformed")) e.setEvidenceJson("{");
      if (invalid.equals("empty")) e.setEvidenceJson("");
      when(events.findFirstByCycleIdAndActionInOrderByRevisionDesc(eq(501L), anyCollection()))
          .thenReturn(Optional.of(e));
      var r = learning(502, 501);
      assertThat(r.path("status").asText()).isEqualTo("MEASUREMENT_REQUIRED");
      assertThat(r.path("metrics")).isEmpty();
    }
    verify(events, atLeastOnce())
        .findFirstByCycleIdAndActionInOrderByRevisionDesc(
            501L, List.of("MEASURE", "MEASUREMENT_BLOCKED"));
  }

  /**
   * Separa atenção sem venda, venda deficitária, entrega não comprovada e evidência observacional.
   */
  @Test
  void learnsWithoutEquatingAttentionToSalesOrCausality() {
    for (var scenario :
        List.of(
            Map.entry(0, "INCONCLUSIVE"),
            Map.entry(-20, "REVIEW_ECONOMICS"),
            Map.entry(100, "OBSERVED_NOT_CAUSAL"))) {
      var e = event(scenario.getKey() == 0 ? 0 : 3, scenario.getKey());
      when(events.findFirstByCycleIdAndActionInOrderByRevisionDesc(eq(501L), anyCollection()))
          .thenReturn(Optional.of(e));
      var result = learning(502, 501);
      assertThat(result.path("status").asText()).isEqualTo(scenario.getValue());
      assertThat(result.path("causalConclusion").asBoolean()).isFalse();
      assertThat(result.path("commercialAuthorization").asBoolean()).isFalse();
    }
    var e = event(3, 100);
    e.setEvidenceJson(e.getEvidenceJson().replace("\"useVerified\":true", "\"useVerified\":false"));
    when(events.findFirstByCycleIdAndActionInOrderByRevisionDesc(eq(501L), anyCollection()))
        .thenReturn(Optional.of(e));
    assertThat(learning(502, 501).path("status").asText()).isEqualTo("INCONCLUSIVE");
    verify(cycles, never()).save(any());
    verify(events, never()).save(any());
  }

  /** Um recibo incompleto nunca confirma economia positiva por ausência tratada como zero. */
  @Test
  void keepsUnknownCostsInconclusive() throws Exception {
    var e = event(3, 100);
    var evidence =
        (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(e.getEvidenceJson());
    evidence.remove("spendBrl");
    e.setEvidenceJson(evidence.toString());
    when(events.findFirstByCycleIdAndActionInOrderByRevisionDesc(eq(501L), anyCollection()))
        .thenReturn(Optional.of(e));
    var result = learning(502, 501);
    assertThat(result.path("status").asText()).isEqualTo("INCONCLUSIVE");
    assertThat(result.path("metrics").path("spendBrl").isNull()).isTrue();
  }

  /** Executa o check exclusivo do Node contra HTTP real, separado da descoberta do Vitest. */
  @Test
  void browserUsesRealLocalBackend() throws Exception {
    Assumptions.assumeTrue(Boolean.getBoolean("expectation.browser"));
    var root = java.nio.file.Path.of("../..").toAbsolutePath().normalize();
    var process =
        new ProcessBuilder(
                "node",
                "--test",
                "--test-reporter=tap",
                "frontend/tests/consumer-expectation/browser.check.cjs")
            .directory(root.toFile())
            .redirectErrorStream(true);
    process.environment().put("EXPECTATION_BACKEND_URL", "http://127.0.0.1:" + port);
    var running = process.start();
    String output =
        new String(
            running.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    java.nio.file.Files.createDirectories(root.resolve(".codex/expectation-evidence"));
    java.nio.file.Files.writeString(
        root.resolve(".codex/expectation-evidence/browser.tap"), output);
    assertThat(running.waitFor()).withFailMessage(output).isZero();
  }

  /** Monta entrada sintética sem associar testes a nenhum produto publicado. */
  private Map<String, String> input(String surface) {
    return new HashMap<>(
        Map.of(
            "audience",
            "Profissionais sintéticos",
            "context",
            "Primeira visita mobile",
            "surface",
            surface,
            "expectedExperience",
            "Preencher um modelo manualmente",
            "desiredOutcome",
            "Terminar uma proposta",
            "actualDelivery",
            "Rascunho editável com dados fornecidos",
            "evidenceBasis",
            "HYPOTHESIS"));
  }

  /** Consulta o contrato HTTP como a interface administrativa. */
  private JsonNode learning(long product, long cycle) {
    return http.getForObject(
        BASE + "/products/" + product + "/cycles/" + cycle + "/learning", JsonNode.class);
  }

  /** Representa recibo conciliado isolado para testar regras de leitura e classificação. */
  private LearningSalesCycleEvent event(long sales, int contribution) {
    var e = new LearningSalesCycleEvent();
    e.setId(900L);
    e.setCycleId(501L);
    e.setAction("MEASURE");
    e.setCreatedAt(Instant.parse("2026-11-20T10:00:00Z"));
    var data =
        mapper
            .createObjectNode()
            .put("contractVersion", "LEARNING_CYCLE_AUTOMATIC_MEASUREMENT_V2")
            .put("automatic", true)
            .put("dataValid", true)
            .put("testDataExcluded", true)
            .put("experimentId", 503)
            .put("sourceFingerprint", "synthetic-reconciled-source")
            .put("humanVisitors", 1000)
            .put("revenueBrl", 200)
            .put("spendBrl", 50)
            .put("refunds", 0)
            .put("netSales", sales)
            .put("contributionBrl", contribution)
            .put("deliveryVerified", true)
            .put("useVerified", true);
    e.setEvidenceJson(data.toString());
    return e;
  }
}
