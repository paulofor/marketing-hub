package com.marketinghub.product.modeling.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleJson;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleTaskContext;
import com.marketinghub.product.modeling.v1.ProductModeling;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.productmodeling.ProductModelingRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

/** Comprova transporte de fontes históricas sem pesquisar, gastar ou herdar aprovações. */
class ModelingHistoricalResearchTest {
  private final ObjectMapper json = new ObjectMapper();
  private final ProductModelingRepository models = mock(ProductModelingRepository.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final ModelingHistoricalResearch history =
      new ModelingHistoricalResearch(models, cycles, json);
  private final Instant before = Instant.parse("2026-10-26T10:00:00Z");
  private final Instant created = Instant.parse("2026-10-27T10:00:00Z");

  /** Reproduz 16→18 de Vega e outra identidade na memória real enviada aos especialistas. */
  @ParameterizedTest
  @ValueSource(longs = {4L, 97004L})
  void taskContextPreservesFrozenFactsWithoutAdoptingThem(long productId) throws Exception {
    var source = fixture(productId);
    String original = source.getAdoptedContextJson();
    var target = cycles.findById(18L).orElseThrow();
    var result = history.forCycle(target, productId, created.plusSeconds(1));
    assertThat(result.path("factualSources"))
        .isEqualTo(json.readTree(original).path("factualSources"));
    assertThat(result.path("researchReuseOnly").asBoolean()).isTrue();
    assertThat(result.path("historicalContextOnly").asBoolean()).isTrue();
    assertThat(result.path("adoptionInherited").asBoolean()).isFalse();
    assertThat(result.path("inheritedFromCycleId").asLong()).isEqualTo(16L);
    assertThat(result.path("inheritedFromExperimentId").asLong()).isEqualTo(109L);
    assertThat(result.path("targetCycleId").asLong()).isEqualTo(18L);
    assertThat(result.has("strategicDecision")).isFalse();
    assertThat(result.path("commercialAuthorizationInherited").asBoolean()).isFalse();
    assertThat(result.path("commercialValidationRequired").asBoolean()).isTrue();
    var context =
        new LearningCycleTaskContext(
            cycles, mock(LearningSalesCycleEventRepository.class), new LearningCycleJson(json));
    ReflectionTestUtils.setField(context, "historicalResearch", history);
    var task = context.resolve("experiment:111", created.plusSeconds(1)).orElseThrow();
    assertThat(task.get("inheritedReferenceResearch")).isEqualTo(result);
    assertThat(task).doesNotContainKey("referenceModeling");
    assertThat(task.get("cycleId")).isEqualTo(18L);
    assertThat(source.getLearningCycleId()).isEqualTo(16L);
    assertThat(source.getAdoptedContextJson()).isEqualTo(original);
    verify(models, never()).save(any());
    verify(cycles, never()).save(any());
  }

  /** Uma adoção própria não é substituída pelas fontes do ancestral. */
  @Test
  void ownAdoptionKeepsExistingContract() {
    fixture(4L);
    when(models.findByLearningCycleId(18L)).thenReturn(Optional.of(new ProductModeling()));
    assertThat(history.forCycle(cycles.findById(18L).orElseThrow(), 4L, created).isNull()).isTrue();
    verify(models, never()).findByLearningCycleId(16L);
  }

  /** Rejeita fonte futura, tarefa anterior ao ciclo e identidade de outro produto. */
  @Test
  void rejectsFutureResearchAndForeignLineage() {
    var source = fixture(4L);
    var target = cycles.findById(18L).orElseThrow();
    assertThat(history.forCycle(target, 4L, created.minusSeconds(1)).isNull()).isTrue();
    assertThat(history.forCycle(target, 4L, null).isNull()).isTrue();
    assertThat(history.forCycle(target, 99L, created).isNull()).isTrue();
    source.setAdoptedAt(created.plusSeconds(1));
    assertThat(history.forCycle(target, 4L, created.plusSeconds(20)).isNull()).isTrue();
    source.setAdoptedAt(before);
    source.setProductId(99L);
    assertThat(history.forCycle(target, 4L, created).isNull()).isTrue();
    source.setProductId(4L);
    cycles.findById(16L).orElseThrow().setProductId(99L);
    assertThat(history.forCycle(target, 4L, created).isNull()).isTrue();
  }

  /** Percorre ancestrais intermediários e termina sem loop em linhagens inválidas. */
  @Test
  void traversesIntermediateSuccessorAndStopsCircularLineage() {
    fixture(4L);
    var newer = cycle(20L, 113L, 4L, 18L, created.plusSeconds(20));
    var result = history.forCycle(newer, 4L, created.plusSeconds(30));
    assertThat(result.path("targetCycleId").asLong()).isEqualTo(20L);
    assertThat(result.path("inheritedFromCycleId").asLong()).isEqualTo(16L);
    when(models.findByLearningCycleId(16L)).thenReturn(Optional.empty());
    cycles.findById(16L).orElseThrow().setPreviousCycleId(18L);
    assertThat(history.forCycle(newer, 4L, created.plusSeconds(30)).isNull()).isTrue();
  }

  /** Contextos anteriores continuam válidos e fontes vazias não se tornam evidência. */
  @Test
  void preservesNoResearchPathAndRejectsEmptyFrozenFacts() {
    var source = fixture(4L);
    var target = cycles.findById(18L).orElseThrow();
    source.setAdoptedContextJson("{\"factualSources\":[]}");
    assertThat(history.forCycle(target, 4L, created).isNull()).isTrue();
    when(models.findByLearningCycleId(16L)).thenReturn(Optional.empty());
    var context =
        new LearningCycleTaskContext(
            cycles, mock(LearningSalesCycleEventRepository.class), new LearningCycleJson(json));
    ReflectionTestUtils.setField(context, "historicalResearch", history);
    assertThat(context.resolve("experiment:111", created).orElseThrow())
        .doesNotContainKey("inheritedReferenceResearch");
    assertThat(context.resolve("experiment:111", created.minusSeconds(1))).isEmpty();
  }

  /** Recusa corrupção da adoção sem entregar uma memória inventada ou iniciar recuperação paga. */
  @Test
  void reportsCorruptAdoptedResearchWithoutChangingHistory() {
    var source = fixture(4L);
    var target = cycles.findById(18L).orElseThrow();
    for (String payload : new String[] {null, " ", "{invalid"}) {
      source.setAdoptedContextJson(payload);
      assertThatThrownBy(() -> history.forCycle(target, 4L, created))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Pesquisa histórica inválida.");
    }
    verify(models, never()).save(any());
    verify(cycles, never()).save(any());
  }

  /** Cria fontes sintéticas com IDs do caso real, preservando a identidade da adoção original. */
  private ProductModeling fixture(long productId) {
    cycle(16L, 109L, productId, null, before.minusSeconds(10));
    cycle(18L, 111L, productId, 16L, created);
    var source = new ProductModeling();
    source.setLearningCycleId(16L);
    source.setExperimentId(109L);
    source.setProductId(productId);
    source.setAdoptedAt(before);
    source.setAdoptedContextJson(
        "{\"contractVersion\":\"MARKET_REFERENCE_MODELING_V1\",\"factualSources\":[{\"evidenceId\":7001,\"url\":\"https://first.example/\",\"observation\":\"Fonte sintética, não prova de venda\"},{\"evidenceId\":7002,\"url\":\"https://second.example/\",\"observation\":\"Limite conhecido\"}],\"guidance\":\"Separar fato de hipótese.\",\"strategicDecision\":{\"taskId\":835,\"decision\":\"APPROVE\"}}");
    when(models.findByLearningCycleId(16L)).thenReturn(Optional.of(source));
    return source;
  }

  /** Vincula o ciclo de teste ao próprio produto e experimento para exercitar a consulta real. */
  private LearningSalesCycle cycle(
      long id, long experiment, long product, Long previous, Instant at) {
    var c = new LearningSalesCycle();
    c.setId(id);
    c.setExperimentId(experiment);
    c.setProductId(product);
    c.setPreviousCycleId(previous);
    c.setCreatedAt(at);
    c.setProductVersion("versao-sintetica-" + id);
    c.setStage("PLANNING");
    c.setBriefJson("{}");
    c.setInheritedLearningJson("{}");
    when(cycles.findById(id)).thenReturn(Optional.of(c));
    when(cycles.findByExperimentId(experiment)).thenReturn(Optional.of(c));
    return c;
  }
}
