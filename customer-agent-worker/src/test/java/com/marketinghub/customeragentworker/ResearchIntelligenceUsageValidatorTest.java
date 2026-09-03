package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Comprova o gate dos cartões consultivos aplicados por Psique. */
class ResearchIntelligenceUsageValidatorTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Aceita somente evidências que cobrem as duas coleções entregues. */
  @Test
  void shouldAcceptDeliveredCardsFromEveryCollection() throws Exception {
    assertThatCode(
            () ->
                ResearchIntelligenceUsageValidator.validate(
                    task(),
                    "customer-agent",
                    List.of("RI1-AAAAAAAAAAAA para percepção", "RI1-BBBBBBBBBBBB para prazer"),
                    true))
        .doesNotThrowAnyException();
  }

  /** Bloqueia parecer que ignora uma das coleções selecionadas. */
  @Test
  void shouldRejectMissingCollection() throws Exception {
    assertThatThrownBy(
            () ->
                ResearchIntelligenceUsageValidator.validate(
                    task(), "customer-agent", List.of("RI1-AAAAAAAAAAAA"), true))
        .hasMessageContaining("cada coleção");
  }

  /** Monta a seleção tipada entregue pelo backend no contrato da tarefa. */
  private Map<String, Object> task() throws Exception {
    Map<String, Object> task = new java.util.HashMap<>();
    task.put(
        "researchIntelligence",
        json.readValue(
            """
            {"contractVersion":"HARNESS_RESEARCH_INTELLIGENCE_V1","routes":[
              {"agentKey":"customer-agent","cards":[
                {"cardId":"RI1-AAAAAAAAAAAA","collection":"neuromarketing"},
                {"cardId":"RI1-BBBBBBBBBBBB","collection":"prazer-audio-visual"}
              ]}
            ]}
            """,
            new TypeReference<>() {}));
    return task;
  }
}
