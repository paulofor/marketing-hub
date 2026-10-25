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
}
