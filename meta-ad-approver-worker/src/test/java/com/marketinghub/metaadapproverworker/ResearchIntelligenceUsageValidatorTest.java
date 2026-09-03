package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Comprova o gate dos cartões consultivos aplicados por Têmis. */
class ResearchIntelligenceUsageValidatorTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Aceita somente evidências que cobrem todas as coleções entregues. */
  @Test
  void shouldAcceptDeliveredCardsFromEveryCollection() throws Exception {
    assertThatCode(
            () ->
                ResearchIntelligenceUsageValidator.validate(
                    task(),
                    "meta-ad-approver",
                    List.of(
                        "RI1-AAAAAAAAAAAA",
                        "RI1-BBBBBBBBBBBB",
                        "RI1-CCCCCCCCCCCC",
                        "RI1-DDDDDDDDDDDD"),
                    true))
        .doesNotThrowAnyException();
  }

  /** Bloqueia parecer que tenta usar cartão ausente do contrato. */
  @Test
  void shouldRejectUndeliveredCard() throws Exception {
    assertThatThrownBy(
            () ->
                ResearchIntelligenceUsageValidator.validate(
                    task(),
                    "meta-ad-approver",
                    List.of("RI1-AAAAAAAAAAAA", "RI1-EEEEEEEEEEEE"),
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
              {"agentKey":"meta-ad-approver","cards":[
                {"cardId":"RI1-AAAAAAAAAAAA","collection":"video"},
                {"cardId":"RI1-BBBBBBBBBBBB","collection":"prazer-audio-visual"},
                {"cardId":"RI1-CCCCCCCCCCCC","collection":"neuromarketing"},
                {"cardId":"RI1-DDDDDDDDDDDD","collection":"momentos-de-compra-b2c"}
              ]}
            ]}
            """,
            new TypeReference<>() {}));
    return task;
  }
}
