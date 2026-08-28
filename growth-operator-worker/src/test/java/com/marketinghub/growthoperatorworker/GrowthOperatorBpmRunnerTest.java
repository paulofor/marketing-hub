package com.marketinghub.growthoperatorworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Responsabilidade: validar o contrato somente leitura do executor BPM de Hermes. */
class GrowthOperatorBpmRunnerTest {
  /** Exige referência segregada de experimento antes de executar o modelo. */
  @Test
  void shouldRequireCanonicalExperimentReference() {
    assertThat(
            GrowthOperatorBpmRunner.experimentId(
                Map.of(
                    "processCode",
                    "operacao-otimizacao-experimento",
                    "sourceReference",
                    "experiment:88")))
        .isEqualTo(88L);
    assertThatThrownBy(
            () ->
                GrowthOperatorBpmRunner.experimentId(
                    Map.of(
                        "processCode",
                        "operacao-otimizacao-experimento",
                        "sourceReference",
                        "commercial-plan:2")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("escopo canônico");
  }

  /** Aceita o planejamento versionado somente no processo de comunicação do PDE. */
  @Test
  void shouldResolveCommercialPlanScopeForPdeCommunication() {
    GrowthOperatorBpmRunner.ExecutionScope scope =
        GrowthOperatorBpmRunner.executionScope(
            Map.of(
                "processCode",
                "pde-communication-sales-journey",
                "sourceReference",
                "commercial-plan:4@v2"));

    assertThat(scope.environmentName()).isEqualTo("MCP_COMMERCIAL_PLAN_ID");
    assertThat(scope.id()).isEqualTo(4L);
    assertThat(scope.reference()).isEqualTo("commercial-plan:4@v2");
  }

  /** Bloqueia contrato sem referência estratégica, atribuição e eventos correlacionados. */
  @Test
  void shouldRejectIncompletePdeCommunicationContract() throws Exception {
    var incomplete =
        new ObjectMapper()
            .readTree(
                """
                {
                  "executionStatus":"COMPLETED",
                  "activityOutcome":"Contrato antigo sem campos comerciais novos.",
                  "observedFacts":["Produto aprovado"],
                  "alternatives":[{"name":"A"},{"name":"B"},{"name":"C"}],
                  "selectedAlternative":"A",
                  "strategicContractReference":{},
                  "growthOperationContract":{},
                  "expectedMetric":"Três vendas",
                  "continueCriteria":"Entrega íntegra",
                  "adjustCriteria":"Sem checkout",
                  "stopCriteria":"Falha de entrega",
                  "recommendedAction":"Corrigir o contrato"
                }
                """);

    assertThatThrownBy(
            () -> GrowthOperatorBpmRunner.validate(incomplete, "pde-communication-sales-journey"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Contrato operacional");
  }

  /** Mantém o Codex em sandbox somente leitura com saída estruturada e MCP local. */
  @Test
  void shouldBuildReadOnlyAuditableCommand() {
    WorkerProperties properties = properties();
    GrowthOperatorBpmRunner runner = new GrowthOperatorBpmRunner(properties, new ObjectMapper());

    var command =
        runner.buildCommand(
            Path.of("/tmp/answer.json"), Path.of("/tmp/schema.json"), Path.of("/tmp/mcp.mjs"));

    assertThat(command)
        .containsSubsequence("--sandbox", "read-only")
        .contains(
            "--json",
            "--search",
            "--output-schema",
            "/tmp/schema.json",
            "approval_policy=\"never\"")
        .doesNotContain("--dangerously-bypass-approvals-and-sandbox");
    assertThat(command)
        .contains("mcp_servers.marketing_hub_readonly.command=\"node\"")
        .anyMatch(value -> value.contains("/tmp/mcp.mjs"));
    assertThat(command)
        .contains(
            "mcp_servers.marketing_hub_readonly.env_vars=[\"MCP_MARKETING_HUB_URL\",\"MCP_COMMERCIAL_PLAN_ID\",\"MCP_EXPERIMENT_ID\",\"MCP_SOURCE_EXECUTION_ID\"]");
  }

  /** Preserva os últimos contadores cumulativos informados no JSONL do Codex. */
  @Test
  void shouldReadRealTokenUsage() throws Exception {
    Path log = Files.createTempFile("hermes-bpm-token-", ".log");
    try {
      Files.writeString(
          log,
          """
          {"usage":{"input_tokens":100,"cached_input_tokens":20,"output_tokens":30}}
          {"usage":{"input_tokens":180,"input_tokens_details":{"cached_tokens":45},"output_tokens":60}}
          """,
          StandardCharsets.UTF_8);

      GrowthOperatorBpmRunner.TokenUsage usage =
          new GrowthOperatorBpmRunner(properties(), new ObjectMapper()).readTokenUsage(log);

      assertThat(usage).isEqualTo(new GrowthOperatorBpmRunner.TokenUsage(180L, 45L, 60L));
    } finally {
      Files.deleteIfExists(log);
    }
  }

  /** Mantém as ferramentas observadas disponíveis para o callback mesmo após falha técnica. */
  @Test
  void shouldPreserveToolUsageInFailure() throws Exception {
    var tool = new ObjectMapper().readTree("{\"tool\":\"consultar_funil\",\"status\":200}");
    var failure =
        new GrowthOperatorBpmRunner.BpmExecutionException(
            "Falha técnica", GrowthOperatorBpmRunner.TokenUsage.empty(), List.of(tool));

    assertThat(failure.toolUsage()).containsExactly(tool);
  }

  /** Reconstrói ferramenta, fonte e horário consultado a partir do JSONL oficial do Codex. */
  @Test
  void shouldExtractAuditableMcpUsageFromCodexJsonl() throws Exception {
    Path log = Files.createTempFile("hermes-bpm-mcp-", ".log");
    try {
      Files.writeString(
          log,
          """
          {"type":"item.completed","item":{"type":"mcp_tool_call","server":"marketing_hub_readonly","tool":"consultar_funil","status":"completed","result":{"content":[{"type":"text","text":"{\\"audit\\":{\\"source\\":\\"/api/experiments/88/funnel\\",\\"consultedAt\\":\\"2026-08-22T14:42:53Z\\",\\"readOnly\\":true},\\"data\\":{}}"}]}}}
          """,
          StandardCharsets.UTF_8);

      List<com.fasterxml.jackson.databind.JsonNode> usage =
          new GrowthOperatorBpmRunner(properties(), new ObjectMapper()).extractToolUsage(log);

      assertThat(usage).hasSize(1);
      assertThat(usage.getFirst().path("tool").asText()).isEqualTo("consultar_funil");
      assertThat(usage.getFirst().path("status").asText()).isEqualTo("completed");
      assertThat(usage.getFirst().path("audit").path("source").asText())
          .isEqualTo("/api/experiments/88/funnel");
    } finally {
      Files.deleteIfExists(log);
    }
  }

  /** Executa o fluxo local completo com processo simulado e escopo exclusivo do experimento. */
  @Test
  void shouldRunBpmContractWithExperimentEnvironmentAndTelemetry() throws Exception {
    Path fakeCodex = Files.createTempFile("fake-codex-hermes-", ".sh");
    try {
      Files.writeString(
          fakeCodex,
          """
          #!/bin/sh
          [ "$MCP_EXPERIMENT_ID" = "88" ] || exit 21
          [ -z "$MCP_COMMERCIAL_PLAN_ID" ] || exit 22
          [ "$MCP_SOURCE_EXECUTION_ID" = "bpm-task-1" ] || exit 23
          answer=""
          while [ "$#" -gt 0 ]; do
            if [ "$1" = "--output-last-message" ]; then
              shift
              answer="$1"
            fi
            shift
          done
          cat >/dev/null
          printf '%s' '{"executionStatus":"BLOCKED","activityOutcome":"Instrumentação ainda sem amostra comercial válida.","observedFacts":["Meta com zero impressões"],"inferences":[],"contradictoryEvidence":[],"evidenceGaps":["Primeira impressão"],"strategicContractReference":{"strategistExecutionId":41,"contractVersion":"MARKET_STRATEGY_V2","contentHash":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","strategyPreserved":true,"revisionRequired":false,"revisionReason":null},"alternatives":[{"name":"A","benefit":"B","risk":"R","effort":"E","fit":"F"},{"name":"B","benefit":"B","risk":"R","effort":"E","fit":"F"},{"name":"C","benefit":"B","risk":"R","effort":"E","fit":"F"}],"selectedAlternative":"A","expectedMetric":"Primeira impressão real","continueCriteria":"Eventos íntegros","adjustCriteria":"Divergência persistente","stopCriteria":"Gasto sem evento","recommendedAction":"Aguardar exposição e preservar a configuração atual."}' > "$answer"
          printf '%s\n' '{"usage":{"input_tokens":90,"cached_input_tokens":10,"output_tokens":25}}'
          """,
          StandardCharsets.UTF_8);
      assertThat(fakeCodex.toFile().setExecutable(true)).isTrue();
      WorkerProperties properties = properties();
      properties.setCodexCommand(fakeCodex.toString());

      GrowthOperatorBpmRunner.BpmExecution execution =
          new GrowthOperatorBpmRunner(properties, new ObjectMapper())
              .run(
                  Map.of(
                      "taskId", 1,
                      "activityId", "task-1",
                      "processCode", "operacao-otimizacao-experimento",
                      "sourceReference", "experiment:88",
                      "marketStrategicContract",
                          Map.of(
                              "contentHash",
                              "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")));

      assertThat(execution.result().path("executionStatus").asText()).isEqualTo("BLOCKED");
      assertThat(execution.usage())
          .isEqualTo(new GrowthOperatorBpmRunner.TokenUsage(90L, 10L, 25L));
    } finally {
      Files.deleteIfExists(fakeCodex);
    }
  }

  /** Executa o contrato do PDE com planejamento segregado e sem expor outro experimento. */
  @Test
  void shouldRunPdeCommunicationWithCommercialPlanEnvironment() throws Exception {
    Path fakeCodex = Files.createTempFile("fake-codex-hermes-plan-", ".sh");
    try {
      Files.writeString(
          fakeCodex,
          """
          #!/bin/sh
          [ "$MCP_COMMERCIAL_PLAN_ID" = "4" ] || exit 31
          [ -z "$MCP_EXPERIMENT_ID" ] || exit 32
          answer=""
          while [ "$#" -gt 0 ]; do
            if [ "$1" = "--output-last-message" ]; then
              shift
              answer="$1"
            fi
            shift
          done
          cat >/dev/null
          printf '%s' '{"executionStatus":"COMPLETED","activityOutcome":"Contrato operacional pronto para homologação.","observedFacts":["Canal autorizado no plano"],"inferences":[],"contradictoryEvidence":[],"evidenceGaps":[],"strategicContractReference":{"strategistExecutionId":41,"contractVersion":"MARKET_STRATEGY_V2","contentHash":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","strategyPreserved":true,"revisionRequired":false,"revisionReason":null},"alternatives":[{"name":"A","benefit":"B","risk":"R","effort":"E","fit":"F"},{"name":"B","benefit":"B","risk":"R","effort":"E","fit":"F"},{"name":"C","benefit":"B","risk":"R","effort":"E","fit":"F"}],"selectedAlternative":"A","growthOperationContract":{"selectedDistributionRoute":"Abordagem individual consentida","channelExecutionBoundary":"Sem contato ou mídia antes de aprovação humana","funnelStages":["visita","checkout","compra"],"attributionPlan":"Correlacionar origem, versão e pagamento aprovado.","eventContracts":[{"eventName":"PURCHASE_COMPLETED","trigger":"Pagamento aprovado","requiredMetadata":["paymentId"],"correlationKeys":["paymentId"],"authoritativeSource":"Pagamento","commercialMeaning":"Venda real"},{"eventName":"ACCESS_RELEASED","trigger":"Acesso liberado","requiredMetadata":["accessToken"],"correlationKeys":["accessToken"],"authoritativeSource":"PDE","commercialMeaning":"Acesso real"},{"eventName":"DELIVERY_COMPLETED","trigger":"Entrega concluída","requiredMetadata":["missionId"],"correlationKeys":["missionId"],"authoritativeSource":"PDE","commercialMeaning":"Entrega real"},{"eventName":"FIRST_USE","trigger":"Primeiro uso","requiredMetadata":["accessToken"],"correlationKeys":["accessToken"],"authoritativeSource":"PDE","commercialMeaning":"Uso real"},{"eventName":"REFUND_CONFIRMED","trigger":"Reembolso confirmado","requiredMetadata":["paymentId"],"correlationKeys":["paymentId"],"authoritativeSource":"Pagamento","commercialMeaning":"Receita revertida"}],"instrumentationGate":"Todos os eventos canônicos persistidos e correlacionados.","checkoutAndAccessVerification":"Validar checkout e acesso no preflight segregado.","samplePlan":"Quinze contatos qualificados após autorização.","testTrafficSegregation":"mh_test","refundGuardrail":"Qualquer reembolso pausa a primeira coorte.","consentAndPrivacy":"Somente contato consentido e dados mínimos.","humanApprovalGates":["contato","mídia","gasto"]},"expectedMetric":"Três vendas","continueCriteria":"Compromisso comercial","adjustCriteria":"Sem checkout","stopCriteria":"Sem entrega","recommendedAction":"Homologar instrumentação antes de ativar distribuição."}' > "$answer"
          printf '%s\n' '{"usage":{"input_tokens":110,"cached_input_tokens":20,"output_tokens":35}}'
          """,
          StandardCharsets.UTF_8);
      assertThat(fakeCodex.toFile().setExecutable(true)).isTrue();
      WorkerProperties properties = properties();
      properties.setCodexCommand(fakeCodex.toString());

      GrowthOperatorBpmRunner.BpmExecution execution =
          new GrowthOperatorBpmRunner(properties, new ObjectMapper())
              .run(
                  Map.of(
                      "taskId", 2,
                      "activityId", "contract",
                      "processCode", "pde-communication-sales-journey",
                      "sourceReference", "commercial-plan:4@v2",
                      "marketStrategicContract",
                          Map.of(
                              "contentHash",
                              "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")));

      assertThat(
              execution
                  .result()
                  .path("growthOperationContract")
                  .path("selectedDistributionRoute")
                  .asText())
          .contains("Abordagem individual");
      assertThat(execution.usage())
          .isEqualTo(new GrowthOperatorBpmRunner.TokenUsage(110L, 20L, 35L));
    } finally {
      Files.deleteIfExists(fakeCodex);
    }
  }

  /** Protege gates comerciais, três alternativas e memória ligada à ferramenta. */
  @Test
  void shouldVersionExperimentOptimizationContract() throws Exception {
    String prompt = read("prompts/bpm/v2/experiment-optimization.md");
    String schema = read("prompts/bpm/v2/experiment-optimization-schema.json");
    String mcp = read("mcp/marketing-hub-readonly.mjs");

    assertThat(prompt)
        .contains(
            "Meta → landing → checkout",
            "pelo menos 100 visitas humanas válidas",
            "p95 de carregamento abaixo de 4",
            "zero erros de recurso",
            "gasto Meta acumulado maior ou igual a R$ 25,00",
            "Orçamento global ou diário maior não",
            "substitui nem revoga essa trava",
            "exatamente três alternativas",
            "marketStrategicContract",
            "Não proponha uma nova estratégia");
    assertThat(schema)
        .contains(
            "executionStatus",
            "COMPLETED",
            "BLOCKED",
            "selectedAlternative",
            "strategicContractReference");
    assertThat(mcp)
        .contains(
            "MCP_EXPERIMENT_ID",
            "consultar_cockpit",
            "funnel/analytics",
            "experiment:${await resolvedExperimentId()}",
            "MCP_TOOL",
            "recuperar_memoria_just_in_time",
            "readOnlyHint: !writable",
            "openWorldHint: true",
            "destructiveHint: false");
  }

  /** Protege mensuração e impede autoria estratégica no contrato operacional do PDE. */
  @Test
  void shouldVersionPdeCommunicationContract() throws Exception {
    String prompt = read("prompts/bpm/v2/pde-growth-operation-contract.md");
    String schema = read("prompts/bpm/v2/pde-growth-operation-contract-schema.json");

    assertThat(prompt)
        .contains(
            "exatamente três alternativas",
            "Têmis transforma a estratégia",
            "não devolva campos",
            "instrumentação",
            "eventContracts",
            "não execute o preflight");
    assertThat(schema)
        .contains(
            "strategicContractReference",
            "growthOperationContract",
            "attributionPlan",
            "checkoutAndAccessVerification",
            "eventContracts",
            "refundGuardrail",
            "continueCriteria",
            "stopCriteria");
    assertThat(GrowthOperatorBpmRunner.promptResourceFor("pde-communication-sales-journey"))
        .isEqualTo("prompts/bpm/v2/pde-growth-operation-contract.md");
    assertThat(GrowthOperatorBpmRunner.schemaResourceFor("pde-communication-sales-journey"))
        .isEqualTo("prompts/bpm/v2/pde-growth-operation-contract-schema.json");
  }

  /** Rejeita qualquer tentativa de Hermes devolver campos estratégicos. */
  @Test
  void shouldRejectStrategicRedefinitionByHermes() throws Exception {
    var result =
        new ObjectMapper()
            .readTree(
                """
                {
                  "executionStatus":"COMPLETED",
                  "activityOutcome":"Otimização pronta com estratégia alterada indevidamente.",
                  "observedFacts":["Funil consultado"],
                  "inferences":[],"contradictoryEvidence":[],"evidenceGaps":[],
                  "strategicContractReference":{"strategistExecutionId":41,"contractVersion":"MARKET_STRATEGY_V2","contentHash":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","strategyPreserved":true,"revisionRequired":false,"revisionReason":null},
                  "alternatives":[{"name":"A","benefit":"B","risk":"R","effort":"E","fit":"F"},{"name":"B","benefit":"B","risk":"R","effort":"E","fit":"F"},{"name":"C","benefit":"B","risk":"R","effort":"E","fit":"F"}],
                  "selectedAlternative":"A",
                  "positioning":"Novo posicionamento criado por Hermes",
                  "expectedMetric":"Venda","continueCriteria":"Uma venda","adjustCriteria":"Sem venda","stopCriteria":"Risco","recommendedAction":"Executar teste operacional autorizado."
                }
                """);

    assertThatThrownBy(
            () -> GrowthOperatorBpmRunner.validate(result, "operacao-otimizacao-experimento"))
        .hasMessageContaining("não preserva");
  }

  /** Rejeita resposta que troque silenciosamente a identidade do contrato de Atena. */
  @Test
  void shouldRejectDifferentAtenaContractHash() throws Exception {
    var result =
        new ObjectMapper()
            .readTree(
                """
                {
                  "executionStatus":"BLOCKED",
                  "activityOutcome":"Estratégia preservada, mas instrumentação ausente.",
                  "observedFacts":["Sem eventos"],"inferences":[],"contradictoryEvidence":[],"evidenceGaps":["Eventos"],
                  "strategicContractReference":{"strategistExecutionId":41,"contractVersion":"MARKET_STRATEGY_V2","contentHash":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","strategyPreserved":true,"revisionRequired":false,"revisionReason":null},
                  "alternatives":[{"name":"A","benefit":"B","risk":"R","effort":"E","fit":"F"},{"name":"B","benefit":"B","risk":"R","effort":"E","fit":"F"},{"name":"C","benefit":"B","risk":"R","effort":"E","fit":"F"}],
                  "selectedAlternative":"A",
                  "expectedMetric":"Evento real","continueCriteria":"Evento íntegro","adjustCriteria":"Sem evento","stopCriteria":"Falha","recommendedAction":"Corrigir instrumentação."
                }
                """);

    assertThatThrownBy(
            () ->
                GrowthOperatorBpmRunner.validate(
                    result,
                    "operacao-otimizacao-experimento",
                    "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"))
        .hasMessageContaining("hash", "Atena");
  }

  /** Lê integralmente um recurso usado pelo contrato. */
  private String read(String resource) throws Exception {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Configura os limites mínimos do comando testado. */
  private WorkerProperties properties() {
    WorkerProperties properties = new WorkerProperties();
    properties.setCodexCommand("codex");
    properties.setRepositoryPath("/workspace/repository");
    properties.setModel("gpt-5.6-sol");
    properties.setMarketingHubUrl("http://backend:8000");
    return properties;
  }
}
