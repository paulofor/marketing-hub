package com.marketinghub.product.bigidea.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.experimentstrategist.*;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.product.bigidea.v1.service.research.StartBigIdeaResearchRequest;
import com.marketinghub.repository.jpa.experimentstrategist.ExperimentStrategistExecutionRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import jakarta.validation.Validation;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.*;

/** Valida fila isolada, contexto congelado, fontes e aplicação para produtos distintos. */
class BigIdeaResearchTest {
  final ObjectMapper json = new ObjectMapper();
  final ExperimentStrategistExecutionRepository executions =
      mock(ExperimentStrategistExecutionRepository.class);
  final CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
  final BigIdeaResearch research =
      new BigIdeaResearch(
          executions, plans, json, Validation.buildDefaultValidatorFactory().getValidator());

  /** Monta somente um cadastro sintético em PLAY. */
  private Product product(long id) {
    var p = new Product();
    p.setId(id);
    p.setName("Produto sintético " + id);
    p.setAutomaticExecutionEnabled(true);
    return p;
  }

  /** Reenvio do pedido reutiliza a execução, sem contaminar estratégia ou outro produto. */
  @Test
  void startsOneIsolatedResearchPerProduct() throws Exception {
    var stored = new HashMap<Long, ExperimentStrategistExecution>();
    when(executions.findLatestBigIdea(anyLong()))
        .thenAnswer(i -> Optional.ofNullable(stored.get(i.getArgument(0))));
    when(executions.saveAndFlush(any()))
        .thenAnswer(
            i -> {
              ExperimentStrategistExecution e = i.getArgument(0);
              long id =
                  json.readTree(e.getEvidenceSnapshot())
                      .path("bigIdeaResearch")
                      .path("productId")
                      .asLong();
              e.setId(id + 500);
              stored.put(id, e);
              return e;
            });
    for (long id : new long[] {99101, 99288}) {
      var plan = new CommercialPlan();
      plan.setId(id + 10);
      when(plans.findByProductId(id)).thenReturn(List.of(plan));
      var first =
          research.start(
              product(id), new StartBigIdeaResearchRequest("Fontes públicas sintéticas"));
      assertThat(research.start(product(id), new StartBigIdeaResearchRequest("duplo clique")))
          .isEqualTo(first);
      assertThat(first.status()).isEqualTo("PENDING");
      assertThat(stored.get(id).getAuthorityMode()).isEqualTo(BigIdeaResearch.MODE);
      assertThat(json.readTree(stored.get(id).getEvidenceSnapshot()).has("experiment")).isFalse();
    }
    assertThat(stored).hasSize(2);
    verify(executions, times(2)).saveAndFlush(any());
  }

  /** Identidade e fontes são verificadas também no backend, independentemente do executor. */
  @Test
  void validatesResultAndRefusesCrossedAdoption() throws Exception {
    var result =
        (ObjectNode)
            json.readTree(
                Path.of("../../infra/testing/product-big-idea/research-result.json").toFile());
    var e = new ExperimentStrategistExecution();
    e.setId(501L);
    e.setAuthorityMode(BigIdeaResearch.MODE);
    e.setEvidenceSnapshot("{\"bigIdeaResearch\":{\"productId\":99101}}");
    e.setStatus(ExperimentStrategistExecutionStatus.COMPLETED);
    e.setRawModelResponse(result.toString());
    when(executions.findById(501L)).thenReturn(Optional.of(e));
    assertThat(research.proposal(product(99101), 501L).centralIdea()).contains("ocasião");
    assertThatThrownBy(() -> research.proposal(product(99288), 501L)).hasMessageContaining("409");
    for (String field : List.of("evidence", "mechanism", "limits")) {
      var invalid = result.deepCopy();
      ((ObjectNode) invalid.path("bigIdea")).put(field, " ");
      assertThatThrownBy(() -> research.validateResult(e, invalid.toString()))
          .hasMessageContaining("400");
    }
    var numeric = result.deepCopy();
    ((ObjectNode) numeric.path("bigIdea")).put("centralIdea", 123);
    assertThatThrownBy(() -> research.validateResult(e, numeric.toString()))
        .hasMessageContaining("400");
    var invalid = result.deepCopy();
    ((ObjectNode) invalid.path("demandAssessment")).put("status", "PROVEN");
    assertThatThrownBy(() -> research.validateResult(e, invalid.toString()))
        .hasMessageContaining("400");
    ((ObjectNode) result.path("sources").get(1)).put("evidenceClass", "INDUSTRY_RESEARCH");
    assertThatThrownBy(() -> research.validateResult(e, result.toString()))
        .hasMessageContaining("400");
  }

  /** STOP preserva a decisão operacional e impede inferência não autorizada. */
  @Test
  void refusesStoppedProductBeforeQueueing() {
    var p = product(99101);
    p.setAutomaticExecutionEnabled(false);
    assertThat(research.current(p).canRequest()).isFalse();
    assertThatThrownBy(() -> research.start(p, null)).hasMessageContaining("STOP");
    verify(executions, never()).saveAndFlush(any());
  }
}
