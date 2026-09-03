package com.marketinghub.communicationagentworker;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Comprova o gate dos cartões consultivos aplicados por Íris. */
class ResearchIntelligenceUsageValidatorTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Aceita somente evidências que cobrem as duas coleções entregues. */
  @Test
  void shouldAcceptDeliveredCardsFromEveryCollection() throws Exception {
    assertThatCode(
            () ->
                ResearchIntelligenceUsageValidator.validate(
                    task(),
                    "communication-director",
                    List.of("RI1-AAAAAAAAAAAA para ângulo", "RI1-BBBBBBBBBBBB para momento"),
                    true))
        .doesNotThrowAnyException();
  }

  /** Bloqueia referência inventada antes de aceitar a saída funcional. */
  @Test
  void shouldRejectUndeliveredCard() throws Exception {
    assertThatThrownBy(
            () ->
                ResearchIntelligenceUsageValidator.validate(
                    task(),
                    "communication-director",
                    List.of("RI1-AAAAAAAAAAAA", "RI1-CCCCCCCCCCCC"),
                    true))
        .hasMessageContaining("não entregue");
  }

  /** Monta a seleção tipada entregue pelo backend no contrato da tarefa. */
  private Map<String, Object> task() throws Exception {
    Map<String, Object> task = new java.util.HashMap<>();
    task.put(
        "researchIntelligence",
        json.readValue(
            """
            {"contractVersion":"HARNESS_RESEARCH_INTELLIGENCE_V1","routes":[
              {"agentKey":"communication-director","cards":[
                {"cardId":"RI1-AAAAAAAAAAAA","collection":"neuromarketing"},
                {"cardId":"RI1-BBBBBBBBBBBB","collection":"momentos-de-compra-b2c"}
              ]}
            ]}
            """,
            new TypeReference<>() {}));
    return task;
  }
}
