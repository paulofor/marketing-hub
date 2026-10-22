package com.marketinghub.experimentstrategistworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Verifica que Atena recebe direção concreta e não a redefine silenciosamente. */
class BigIdeaContractTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Cria um conteúdo sintético completo. */
  private ObjectNode idea() {
    var n = json.createObjectNode();
    for (String key :
        new String[] {
          "centralIdea", "audience", "desire", "promise", "mechanism", "evidence", "limits"
        }) n.put(key, "Hipótese específica " + key);
    return n;
  }

  /** Preserva caminhos válidos e bloqueia conteúdo vazio, numérico ou divergente. */
  @Test
  void validatesNewAndPreservedIdeas() {
    var task = json.createObjectNode().put("processVersion", 13);
    var context =
        task.putObject("processContextJson")
            .putObject("productBigIdea")
            .put("contractVersion", "PRODUCT_BIG_IDEA_V1");
    var result = json.createObjectNode().put("decision", "APPROVE");
    result.set("bigIdea", idea());
    assertThat(PdeMarketStrategyBpmTaskConsumer.requiresBigIdea(task)).isTrue();
    PdeMarketStrategyBpmTaskConsumer.validateBigIdea(result, task);
    context.putObject("revision").set("content", idea());
    PdeMarketStrategyBpmTaskConsumer.validateBigIdea(result, task);
    ((ObjectNode) result.path("bigIdea")).put("desire", "Outra direção");
    assertThatThrownBy(() -> PdeMarketStrategyBpmTaskConsumer.validateBigIdea(result, task))
        .hasMessageContaining("fixada");
    ((ObjectNode) result.path("bigIdea")).put("desire", 14);
    assertThatThrownBy(() -> PdeMarketStrategyBpmTaskConsumer.validateBigIdea(result, task))
        .hasMessageContaining("inválido");
    result.set("bigIdea", idea().put("centralIdea", " "));
    assertThatThrownBy(() -> PdeMarketStrategyBpmTaskConsumer.validateBigIdea(result, task))
        .hasMessageContaining("inválido");
    assertThat(
            PdeMarketStrategyBpmTaskConsumer.requiresBigIdea(
                json.createObjectNode().put("processVersion", 12)))
        .isFalse();
  }

  /** Confere contrato versionado no prompt e schema realmente carregáveis pelo executor. */
  @Test
  void promptAndSchemaCarryTheDirection() throws Exception {
    var prompt =
        new ClassPathResource("prompts/pde-commercial-plan/v12/market-strategy.md")
            .getContentAsString(StandardCharsets.UTF_8);
    var schema =
        json.readTree(
            new ClassPathResource("prompts/pde-commercial-plan/v12/market-strategy-schema.json")
                .getInputStream());
    assertThat(prompt).contains("copie exatamente", "Não invente resultados", "{{TASK_CONTEXT}}");
    assertThat(schema.path("properties").path("bigIdea").path("required")).hasSize(7);
  }
}
