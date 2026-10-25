package com.marketinghub.product.modeling.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.experiment.*;
import com.marketinghub.opportunitydossier.*;
import com.marketinghub.product.Product;
import com.marketinghub.product.modeling.v1.ProductModeling;
import com.marketinghub.productdiscovery.v1.*;
import com.marketinghub.repository.jpa.agenttask.*;
import com.marketinghub.repository.jpa.experiment.*;
import com.marketinghub.repository.jpa.learningcycle.*;
import com.marketinghub.repository.jpa.opportunitydossier.*;
import com.marketinghub.repository.jpa.processautomation.*;
import com.marketinghub.repository.jpa.product.*;
import com.marketinghub.repository.jpa.productdiscovery.*;
import com.marketinghub.repository.jpa.productmodeling.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Exercita a passagem real de fontes ao contexto dos agentes sem gerar consumo externo. */
class ModelingAdoptionTest {
  final ProductModelingRepository models = mock(ProductModelingRepository.class);
  final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  final ProductDiscoveryCycleRepository research = mock(ProductDiscoveryCycleRepository.class);
  final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  final ExperimentRepository experiments = mock(ExperimentRepository.class);
  final ProductRepository products = mock(ProductRepository.class);
  final ProcessRunRepository runs = mock(ProcessRunRepository.class);
  final OpportunityDossierRepository dossiers = mock(OpportunityDossierRepository.class);
  final OpportunityEvidenceRepository evidence = mock(OpportunityEvidenceRepository.class);
  final ModelingHandoff handoff =
      new ModelingHandoff(models, new ObjectMapper(), dossiers, evidence, tasks);
  final ModelingAdoption adoption =
      new ModelingAdoption(models, cycles, research, tasks, experiments, handoff, products, runs);

  /** Monta uma preparação com fatos sintéticos segregados por identidade. */
  private ProductModeling fixture(long id) {
    var m = new ProductModeling();
    m.setId(id);
    m.setProductId(id + 100);
    m.setDiscoveryCycleId(id + 200);
    m.setReferencesJson("[\"https://reference.example/\"]");
    m.setDesiredOutcome("Resultado próprio");
    when(models.findById(id)).thenReturn(Optional.of(m));
    when(models.findLockedById(id)).thenReturn(Optional.of(m));
    when(models.findByDiscoveryCycleId(id + 200)).thenReturn(Optional.of(m));
    when(products.findLockedById(id + 100)).thenReturn(Optional.of(new Product()));
    var c = new LearningSalesCycle();
    c.setId(id + 300);
    c.setProductId(id + 100);
    c.setExperimentId(id + 400);
    c.setChainDefinitionId(999L);
    c.setStatus("OPEN");
    c.setStage("PLANNING");
    when(cycles.findLocked(id + 100, id + 300)).thenReturn(Optional.of(c));
    when(cycles.findById(id + 300)).thenReturn(Optional.of(c));
    when(cycles.findByProductIdAndOpenSlot(id + 100, 1)).thenReturn(List.of(c));
    var e = new Experiment();
    e.setStatus(ExperimentStatus.PLANNED);
    when(experiments.findById(id + 400)).thenReturn(Optional.of(e));
    var r = new ProductDiscoveryCycle();
    r.setStatus(ProductDiscoveryCycleStatus.COMPLETED);
    when(research.findByIdForUpdate(id + 200)).thenReturn(Optional.of(r));
    var opportunity = new ProductDiscoveryOpportunity();
    org.springframework.test.util.ReflectionTestUtils.setField(opportunity, "id", id + 500);
    opportunity.setMaturity(ProductDiscoveryOpportunityMaturity.DOSSIER_READY);
    var d = new OpportunityDossier();
    d.setId(id + 600);
    d.setTitle("Referência sintética");
    d.setKnownRisks("Sinal público, não venda");
    d.setProductDiscoveryOpportunity(opportunity);
    when(dossiers.findAllByProductDiscoveryCycleIdOrderByIdAsc(id + 200)).thenReturn(List.of(d));
    var fact = new OpportunityEvidence();
    fact.setId(id + 700);
    fact.setSourceUrl("https://reference.example/");
    fact.setSummary("Entrada gratuita observada");
    fact.setCreatedAt(Instant.now());
    when(evidence.findByDossierIdOrderByCreatedAtAsc(id + 600)).thenReturn(List.of(fact));
    return m;
  }

  /**
   * Dois produtos recebem a própria pesquisa; replay, construção e histórico permanecem isolados.
   */
  @Test
  void handsOffFrozenFactsToEveryStageWithoutRepeatingWork() {
    for (long id : List.of(1L, 50L)) {
      var m = fixture(id);
      assertThat(adoption.options(m)).hasSize(1);
      adoption.adopt(id, id + 300);
      adoption.adopt(id, id + 300);
      verify(models, times(1)).save(m);
      assertThat(adoption.activityQuery(m)).isEqualTo("&chainId=999&learningCycleId=" + (id + 300));
      when(models.findByExperimentId(id + 400)).thenReturn(Optional.of(m));
      when(models.findByLearningCycleId(id + 300)).thenReturn(Optional.of(m));
      var at = m.getAdoptedAt();
      var planning = handoff.snapshot("experiment:" + (id + 400), at.plusSeconds(1));
      assertThat(planning.path("factualSources").get(0).path("evidenceId").asLong())
          .isEqualTo(id + 700);
      assertThat(handoff.forLearningCycle(id + 300, id + 100, at.plusSeconds(1)))
          .isEqualTo(planning);
      assertThat(handoff.snapshot("experiment:" + (id + 400), at.minusSeconds(1)).isNull())
          .isTrue();
      assertThat(handoff.forLearningCycle(id + 300, 9999L, at.plusSeconds(1)).isNull()).isTrue();
      assertThat(handoff.snapshot("experiment:9999", at).isNull()).isTrue();
      when(evidence.findByDossierIdOrderByCreatedAtAsc(id + 600)).thenReturn(List.of());
      assertThat(handoff.snapshot("experiment:" + (id + 400), at.plusSeconds(2)))
          .isEqualTo(planning);
      assertThatThrownBy(() -> adoption.adopt(id, 9999L))
          .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
    verify(tasks, never()).save(any());
  }

  /** Entrega a decisão rastreável às etapas seguintes sem ressuscitar aprovação superada. */
  @Test
  void propagatesDecisionAndPreservesIndependentFailure() {
    var m = fixture(8L);
    adoption.adopt(8L, 308L);
    when(models.findByExperimentId(408L)).thenReturn(Optional.of(m));
    var strategy = new com.marketinghub.agenttask.AgentTask();
    strategy.setId(900L);
    strategy.setProcessActivityId("marketStrategy");
    strategy.setStatus("COMPLETED");
    strategy.setResultJson(
        "{\"decision\":\"APPROVE\",\"referenceModelingTrace\":[{\"deliverable\":\"Primeiro valor utilizável\"}],\"internationalAssessment\":{\"brazilAdaptation\":{\"firstValue\":\"Uma combinação própria\"}}}");
    when(tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc("experiment:408"))
        .thenReturn(List.of(strategy));
    assertThat(
            handoff
                .snapshot("experiment:408", m.getAdoptedAt())
                .path("strategicDecision")
                .path("trace")
                .get(0)
                .path("deliverable")
                .asText())
        .isEqualTo("Primeiro valor utilizável");
    assertThat(
            handoff
                .snapshot("experiment:408", m.getAdoptedAt())
                .path("strategicDecision")
                .path("internationalAssessment")
                .path("brazilAdaptation")
                .path("firstValue")
                .asText())
        .isEqualTo("Uma combinação própria");
    var retry = new com.marketinghub.agenttask.AgentTask();
    retry.setId(901L);
    retry.setProcessActivityId("marketStrategy");
    retry.setStatus("BLOCKED");
    when(tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc("experiment:408"))
        .thenReturn(List.of(strategy, retry));
    var current = handoff.snapshot("experiment:408", m.getAdoptedAt()).path("strategicDecision");
    assertThat(current.path("status").asText()).isEqualTo("BLOCKED");
    assertThat(current.has("trace")).isFalse();
    assertThat(current.has("internationalAssessment")).isFalse();
  }

  /** Rejeita histórico, ausência de fatos e produto alheio sem alterar qualquer limite. */
  @Test
  void rejectsUnsafeAdoptionAndMissingEvidence() {
    var m = fixture(5L);
    assertThatThrownBy(() -> adoption.adopt(5L, 999L))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    when(runs.existsByProductIdAndSourceReference(105L, "experiment:405")).thenReturn(true);
    assertThatThrownBy(() -> adoption.adopt(5L, 305L)).hasMessageContaining("histórico");
    when(runs.existsByProductIdAndSourceReference(105L, "experiment:405")).thenReturn(false);
    when(evidence.findByDossierIdOrderByCreatedAtAsc(605L)).thenReturn(List.of());
    assertThatThrownBy(() -> adoption.adopt(5L, 305L)).hasMessageContaining("fontes factuais");
    assertThat(m.getExperimentId()).isNull();
    verify(models, never()).save(any());
  }
}
