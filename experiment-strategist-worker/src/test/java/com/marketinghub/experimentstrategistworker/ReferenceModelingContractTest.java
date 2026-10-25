package com.marketinghub.experimentstrategistworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;

/** Protege a rastreabilidade executável sem transformar referência em prova comercial. */
class ReferenceModelingContractTest {
  final ObjectMapper json = new ObjectMapper();

  /** Rejeita aprovação sem ligação e preserva revisão histórica válida. */
  @Test
  void requiresTraceOnlyForModeling() throws Exception {
    var result = json.readTree("{\"decision\":\"APPROVE\",\"alternatives\":[{},{},{}]}");
    var task =
        json.readTree(
            "{\"processContextJson\":{\"referenceModeling\":{\"contractVersion\":\"MARKET_REFERENCE_MODELING_V1\"}}}");
    assertThatThrownBy(() -> ReferenceModelingContract.validate(result, task))
        .isInstanceOf(IllegalArgumentException.class);
    ReferenceModelingContract.validate(result, json.readTree("{}"));
  }

  /** Duas identidades independentes preservam fontes e recusam citação inventada. */
  @Test
  void acceptsGroundedTraceAndRejectsMissingSource() throws Exception {
    for (int id : new int[] {4, 99122}) {
      var task =
          json.readTree(
              "{\"processContextJson\":{\"referenceModeling\":{\"contractVersion\":\"MARKET_REFERENCE_MODELING_V1\",\"productId\":"
                  + id
                  + "}}}");
      ((com.fasterxml.jackson.databind.node.ObjectNode)
              task.path("processContextJson").path("referenceModeling"))
          .putArray("factualSources")
          .addObject()
          .put("url", "https://reference.example/");
      var result = json.createObjectNode();
      result.put("decision", "APPROVE");
      result.putArray("alternatives").addObject();
      result.withArray("alternatives").addObject();
      result.withArray("alternatives").addObject();
      result.putArray("sources").addObject().put("url", "https://reference.example/");
      var trace = result.putArray("referenceModelingTrace").addObject();
      trace.put("sourceUrl", "https://reference.example/");
      for (String field :
          new String[] {
            "observedFact", "limitation", "learning", "decision", "deliverable", "acceptanceMetric"
          }) trace.put(field, "Evidência e hipótese explícitas");
      ReferenceModelingContract.validate(result, task);
      trace.put("sourceUrl", "https://invented.example/");
      assertThatThrownBy(() -> ReferenceModelingContract.validate(result, task))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  /** Aprovação internacional exige evidência literal e entrega brasileira em duas identidades. */
  @Test
  void requiresBrazilAdaptationAndGroundedInternationalSignals() throws Exception {
    for (int id : new int[] {4, 99122}) {
      var task =
          json.readTree(
              """
          {"processContextJson":{"referenceModeling":{
            "contractVersion":"MARKET_REFERENCE_MODELING_V1","researchScope":"INTERNATIONAL_TO_BR_V1",
            "referenceUrls":["https://reference.example/"],
            "factualSources":[{"url":"https://reference.example/pricing", "observation":"O vendedor anuncia plano pago; não há receita auditada."}]}}}
          """);
      ((com.fasterxml.jackson.databind.node.ObjectNode)
              task.path("processContextJson").path("referenceModeling"))
          .put("productId", id);
      var result =
          json.readTree(
              """
          {"decision":"APPROVE","alternatives":[{},{},{}],
            "referenceModelingTrace":[{"sourceUrl":"https://reference.example/pricing",
            "observedFact":"Plano anunciado", "limitation":"Alegação do vendedor", "learning":"Entrada gratuita",
            "decision":"Valor antes do compromisso", "deliverable":"Orientação própria", "acceptanceMetric":"Compra e contribuição"}],
            "internationalAssessment":{"references":[{"referenceUrl":"https://reference.example/",
              "sourceUrl":"https://reference.example/pricing", "observedEvidence":"O vendedor anuncia plano pago",
              "originMarket":"Mercado exterior, país não confirmado", "signalType":"PAID_OFFER", "evidenceLevel":"SELLER_CLAIM",
              "limitation":"Não comprova receita nem resultado no Brasil"}],
              "brazilAdaptation":{"recognizedDesire":"Encontrar combinações próprias", "firstValue":"Uma combinação antes do cadastro",
                "localization":"pt-BR e rotina local", "localAlternatives":"Comparação pública, preços ainda desconhecidos",
                "paidValue":"Jornada personalizada de sete dias", "economicsHypotheses":"Custos projetados, sem demanda comprovada",
                "mainRisk":"Não converter diante das alternativas gratuitas", "validationPlan":"Uso, compra líquida e contribuição no teste autorizado"}}}
          """);
      ReferenceModelingContract.validate(result, task);
      var missing = result.deepCopy();
      ((com.fasterxml.jackson.databind.node.ObjectNode) missing).remove("internationalAssessment");
      assertThatThrownBy(() -> ReferenceModelingContract.validate(missing, task))
          .hasMessageContaining("adaptação");
      var invented = result.deepCopy();
      ((com.fasterxml.jackson.databind.node.ObjectNode)
              invented.path("internationalAssessment").path("references").get(0))
          .put("observedEvidence", "Este concorrente faturou um milhão de reais");
      assertThatThrownBy(() -> ReferenceModelingContract.validate(invented, task))
          .hasMessageContaining("trecho literal");
      for (String field :
          new String[] {"firstValue", "paidValue", "economicsHypotheses", "localAlternatives"}) {
        var incomplete = result.deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode)
                incomplete.path("internationalAssessment").path("brazilAdaptation"))
            .remove(field);
        assertThatThrownBy(() -> ReferenceModelingContract.validate(incomplete, task))
            .hasMessageContaining(field);
      }
      var other = result.deepCopy();
      ((com.fasterxml.jackson.databind.node.ObjectNode)
              other.path("internationalAssessment").path("references").get(0))
          .put("referenceUrl", "https://other.example/");
      assertThatThrownBy(() -> ReferenceModelingContract.validate(other, task))
          .hasMessageContaining("ausente");
      var unknown = result.deepCopy();
      ((com.fasterxml.jackson.databind.node.ObjectNode)
              unknown.path("internationalAssessment").path("references").get(0))
          .put("signalType", "UNKNOWN")
          .put("evidenceLevel", "UNKNOWN");
      ReferenceModelingContract.validate(unknown, task);
      ((com.fasterxml.jackson.databind.node.ObjectNode) missing).put("decision", "ADJUST");
      ReferenceModelingContract.validate(missing, task);
    }
  }
}
