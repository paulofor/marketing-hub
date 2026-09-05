package com.marketinghub.financialagentworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o contrato de seguranca do executor financeiro. */
class FinancialCodexRunnerTest {
  /** Confirma pesquisa web, sandbox somente leitura e modelo configurado no comando Codex. */
  @Test
  void deveExecutarSomenteLeitura() {
    FinancialAgentProperties properties = new FinancialAgentProperties();
    properties.setCodexCommand("codex");
    properties.setRepositoryPath("/workspace/marketing-hub");
    properties.setModel("gpt-5.6-sol");
    FinancialCodexRunner runner = new FinancialCodexRunner(properties, new ObjectMapper());

    var command =
        runner.buildCommand(
            Path.of("/tmp/out"), Path.of("/tmp/schema"), Path.of("/tmp/financial-agent.mjs"));

    assertThat(command).containsSubsequence("codex", "--search", "exec", "-");
    assertThat(command).containsSubsequence("--sandbox", "read-only");
    assertThat(command).containsSubsequence("--model", "gpt-5.6-sol");
    assertThat(command).contains("--json", "approval_policy=\"never\"");
    assertThat(command)
        .contains(
            "service_tier=\"default\"",
            "model_reasoning_effort=\"high\"",
            "mcp_servers.financial_agent.command=\"node\"",
            "mcp_servers.financial_agent.args=[\"/tmp/financial-agent.mjs\"]",
            "mcp_servers.financial_agent.env_vars=[\"MCP_BACKEND_URL\",\"MCP_EXECUTION_ID\"]");
    assertThat(command).doesNotContain("--dangerously-bypass-approvals-and-sandbox");
  }

  /** Bloqueia Plutus antes do modelo quando o tipo de raciocínio não está configurado. */
  @Test
  void deveRejeitarRaciocinioAusenteAntesDoModelo() {
    FinancialAgentProperties properties = new FinancialAgentProperties();
    properties.setReasoningEffort(" ");
    FinancialCodexRunner runner = new FinancialCodexRunner(properties, new ObjectMapper());

    assertThatThrownBy(() -> runner.buildCommand(Path.of("/tmp/out"), Path.of("/tmp/schema")))
        .hasMessageContaining("obrigatório para auditar Plutus");
  }

  /** Confirma o limite operacional padrão de quarenta minutos. */
  @Test
  void deveLimitarExecucaoCodexAQuarentaMinutos() {
    FinancialAgentProperties properties = new FinancialAgentProperties();

    assertThat(properties.getCodexTimeout()).isEqualTo(Duration.ofMinutes(40));
    assertThat(properties.getReasoningEffort()).isEqualTo("high");
    assertThat(properties.getServiceTier()).isEqualTo("default");
    assertThat(properties.getServiceTierExceptionReason()).contains("Flex", "Codex OAuth");
  }

  /** Lê o último total de tokens do JSONL sem inventar custo quando o runtime nada informa. */
  @Test
  void deveExtrairTelemetriaRealDoJsonl() throws Exception {
    Path processLog = Files.createTempFile("financial-codex-test-", ".jsonl");
    try {
      Files.writeString(
          processLog,
          "{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":100,"
              + "\"cached_input_tokens\":25,\"output_tokens\":30}}\n"
              + "{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":140,"
              + "\"cached_input_tokens\":40,\"output_tokens\":50}}\n");
      FinancialCodexRunner runner =
          new FinancialCodexRunner(new FinancialAgentProperties(), new ObjectMapper());

      FinancialCodexRunner.TokenUsage usage = runner.readTokenUsage(processLog);

      assertThat(usage.informed()).isTrue();
      assertThat(usage.inputTokens()).isEqualTo(140);
      assertThat(usage.cachedInputTokens()).isEqualTo(40);
      assertThat(usage.outputTokens()).isEqualTo(50);
    } finally {
      Files.deleteIfExists(processLog);
    }
  }

  /** Protege o schema de projeção contra palavras incompatíveis com Structured Outputs. */
  @Test
  void schemaDeProjecaoDevePermanecerCompativel() throws Exception {
    String schema =
        Files.readString(
            Path.of(
                "src/main/resources/prompts/financial-agent/v1/revenue-projection-schema.json"));

    assertThat(schema).doesNotContain("uniqueItems", "anyOf", "oneOf", "allOf");
    assertThat(schema).contains("CONSERVATIVE", "BASE", "OPTIMISTIC", "learningCandidate");
  }

  /** Protege a separação entre hipótese aprovada e autorização de gasto. */
  @Test
  void contratoDePremissasNaoDeveAutorizarGasto() throws Exception {
    String prompt =
        Files.readString(
            Path.of("src/main/resources/prompts/financial-agent/v1/commercial-assumptions.md"));
    String schema =
        Files.readString(
            Path.of(
                "src/main/resources/prompts/financial-agent/v1/commercial-assumptions-schema.json"));

    assertThat(prompt).contains("não libera orçamento", "APPROVE", "Plutus");
    assertThat(schema)
        .contains("validatedAssumptions", "offerPriceBrl", "expectedCacBrl", "REJECT");
  }

  /** Protege o investimento controlado em materiais durante a fase inicial de descoberta. */
  @Test
  void contratoDeVideoDevePermitirDescobertaSemRetornoAnterior() throws Exception {
    String prompt =
        Files.readString(
            Path.of("src/main/resources/prompts/financial-agent/v1/video-cycle-review.md"));

    assertThat(prompt)
        .contains(
            "Não exija retorno, venda ou ROI anterior",
            "custos históricos irrecuperavelmente desconhecidos são USD 0",
            "custos históricos conhecidos sem plano são gastos passados",
            "não compare o total histórico sem plano com o teto incremental",
            "US$ 20 no total",
            "no máximo US$ 10 por vídeo",
            "rastreabilidade do custo incremental novo",
            "preflight READY e reserva preventiva ainda vigente",
            "RUNWAY_ROUTER:<routerConfigId>",
            "RUNWAY_PRODUCT_UGC:<routerConfigId>",
            "batchRouteId",
            "recarga mínima");
    String schema =
        Files.readString(
            Path.of(
                "src/main/resources/prompts/financial-agent/v1/video-cycle-review-schema.json"));
    assertThat(schema)
        .contains(
            "recommendedAggregator",
            "recommendedRoute",
            "batchRouteId",
            "estimatedCostUsd",
            "costBenefitBasis",
            "RECHARGE_REQUIRED",
            "recommendedRechargeCredits",
            "rechargeUrl");
  }

  /** Mantém o gate de vídeo sem web e com tier, raciocínio e telemetria declarados. */
  @Test
  void comandoDeVideoDeveSerAuditavelESomenteSnapshot() {
    FinancialAgentProperties properties = new FinancialAgentProperties();
    properties.setCodexCommand("codex");
    properties.setRepositoryPath("/workspace/marketing-hub");
    properties.setModel("gpt-5.6-sol");
    FinancialCodexRunner runner = new FinancialCodexRunner(properties, new ObjectMapper());

    var command = runner.buildVideoReviewCommand(Path.of("/tmp/out"), Path.of("/tmp/schema"));

    assertThat(command).contains("--json", "service_tier=\"default\"");
    assertThat(command).contains("model_reasoning_effort=\"high\"");
    assertThat(command).doesNotContain("--search");
  }

  /** Separa regra e atividade sem perder o snapshot nem duplicar chamada de modelo. */
  @Test
  void promptDeVideoDevePreservarComposicaoAuditavel() throws Exception {
    FinancialCodexRunner runner =
        new FinancialCodexRunner(new FinancialAgentProperties(), new ObjectMapper());
    VideoProductionCycleReview cycle =
        new VideoProductionCycleReview(
            91L,
            37L,
            4L,
            3L,
            91L,
            "PENDING_FINANCIAL_REVIEW",
            new BigDecimal("10.00"),
            BigDecimal.ZERO,
            "{\"providerPreflight\":{\"status\":\"READY\"}}",
            322L,
            null);

    FinancialCodexRunner.PromptComposition prompt = runner.buildVideoPromptComposition(cycle);

    assertThat(prompt.fullPrompt())
        .contains(prompt.agentPromptPart(), prompt.activityPromptPart(), "providerPreflight");
    assertThat(prompt.fullPrompt().indexOf(prompt.agentPromptPart()))
        .isLessThan(prompt.fullPrompt().indexOf(prompt.activityPromptPart()));
  }

  /** Reconstrói a decisão persistida para retomar sem novo consumo de modelo. */
  @Test
  void deveReutilizarRespostaFinanceiraJaAuditada() throws Exception {
    FinancialCodexRunner runner =
        new FinancialCodexRunner(new FinancialAgentProperties(), new ObjectMapper());

    var decision =
        runner.videoDecision(
            "{\"decision\":\"APPROVED\",\"reason\":\"Saldo e teto válidos.\","
                + "\"recommendedAggregator\":\"Runway\","
                + "\"recommendedRoute\":\"RUNWAY_ROUTER:final\","
                + "\"estimatedCostUsd\":1.25,"
                + "\"costBenefitBasis\":\"Dry run oficial dentro do teto.\","
                + "\"creditAction\":\"NO_PURCHASE\","
                + "\"recommendedRechargeCredits\":null,\"rechargeUrl\":null}");

    assertThat(decision)
        .containsEntry("decision", "APPROVED")
        .containsEntry("recommendedAggregator", "Runway")
        .containsEntry("estimatedCostUsd", new BigDecimal("1.25"));
  }

  /** Corrige deterministicamente o prefixo legado usando a rota exata do preflight Product UGC. */
  @Test
  void deveVincularParecerPersistidoARotaProductUgcDoPreflight() throws Exception {
    FinancialCodexRunner runner =
        new FinancialCodexRunner(new FinancialAgentProperties(), new ObjectMapper());
    VideoProductionCycleReview cycle =
        new VideoProductionCycleReview(
            10L,
            3L,
            4L,
            null,
            91L,
            "PENDING_FINANCIAL_REVIEW",
            new BigDecimal("6.48"),
            BigDecimal.ZERO,
            "{\"providerPreflight\":{\"aggregatorName\":\"Runway\","
                + "\"selectedRoutesJson\":\"[{\\\"batchRouteId\\\":\\\"RUNWAY_PRODUCT_UGC:product_ugc@2026-06\\\"}]\"}}",
            338L,
            null);

    var decision =
        runner.videoDecision(
            "{\"decision\":\"APPROVED\",\"reason\":\"Saldo e teto válidos.\","
                + "\"recommendedAggregator\":\"Runway\","
                + "\"recommendedRoute\":\"RUNWAY_ROUTER:product_ugc@2026-06\","
                + "\"estimatedCostUsd\":6.48,"
                + "\"costBenefitBasis\":\"Dry run oficial dentro do teto.\","
                + "\"creditAction\":\"NO_PURCHASE\","
                + "\"recommendedRechargeCredits\":null,\"rechargeUrl\":null}",
            cycle);

    assertThat(decision)
        .containsEntry("recommendedAggregator", "Runway")
        .containsEntry("recommendedRoute", "RUNWAY_PRODUCT_UGC:product_ugc@2026-06")
        .containsEntry("estimatedCostUsd", new BigDecimal("6.48"));
  }
}
