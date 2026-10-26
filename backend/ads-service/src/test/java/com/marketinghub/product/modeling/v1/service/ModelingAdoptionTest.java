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
    when(research.findById(id + 200)).thenReturn(Optional.of(r));
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

  /** Estudo internacional completo pode orientar estratégia sem promover maturidade comercial. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(longs = {2L, 52L})
  void adoptsReferenceStudyAsHypothesisWithoutCommercialPromotion(long id) throws Exception {
    var m = referenceStudy(id);
    assertThat(adoption.options(m)).hasSize(1);
    adoption.adopt(id, id + 300);
    var saved = new ObjectMapper().readTree(m.getAdoptedContextJson());
    assertThat(saved.path("adoptionBasis").asText()).isEqualTo("REFERENCE_STUDY_FOR_STRATEGY");
    assertThat(saved.path("demandStatus").asText()).isEqualTo("HYPOTHESIS");
    assertThat(saved.path("commercialValidationRequired").asBoolean()).isTrue();
    assertThat(saved.path("commercialAuthorizationInherited").asBoolean()).isFalse();
    assertThat(saved.path("candidates").get(0).path("maturity").asText()).isEqualTo("RESEARCHABLE");
    assertThat(saved.path("candidates").get(0).path("limitations").asText())
        .contains("não comprovada");
    verify(tasks, never()).save(any());
  }

  /** Falta de referência, contraponto, data ou limite mantém a pesquisa bloqueada. */
  @Test
  void refusesIncompleteReferenceStudyAndLegacyNonReadyDossier() {
    for (int mode = 0; mode < 11; mode++) {
      long id = 100 + mode;
      var m = referenceStudy(id);
      var facts = new ArrayList<>(evidence.findByDossierIdOrderByCreatedAtAsc(id + 600));
      if (mode == 0) facts.remove(0);
      if (mode == 1) facts.remove(2);
      if (mode == 2) facts.get(0).setCreatedAt(null);
      if (mode == 3)
        dossiers.findAllByProductDiscoveryCycleIdOrderByIdAsc(id + 200).get(0).setKnownRisks(" ");
      if (mode == 4) m.setResearchScope("MARKET_REFERENCES_V1");
      if (mode == 5)
        dossiers
            .findAllByProductDiscoveryCycleIdOrderByIdAsc(id + 200)
            .get(0)
            .getProductDiscoveryOpportunity()
            .setMaturity(ProductDiscoveryOpportunityMaturity.HUMAN_REVIEW);
      if (mode == 6)
        dossiers
            .findAllByProductDiscoveryCycleIdOrderByIdAsc(id + 200)
            .get(0)
            .getProductDiscoveryOpportunity()
            .setMaturity(ProductDiscoveryOpportunityMaturity.REJECTED);
      if (mode == 7) facts.get(0).setSourceUrl("https://alpha.example.evil/");
      if (mode == 8)
        dossiers.findAllByProductDiscoveryCycleIdOrderByIdAsc(id + 200).get(0).setMainPain(null);
      if (mode == 9)
        dossiers
            .findAllByProductDiscoveryCycleIdOrderByIdAsc(id + 200)
            .get(0)
            .getProductDiscoveryOpportunity()
            .setEvidenceJson("{\"publicEvidenceAssessment\":{\"observations\":[]}}");
      if (mode == 10) facts.get(2).setSummary("Outra frase que não confirma o trecho atribuído.");
      when(evidence.findByDossierIdOrderByCreatedAtAsc(id + 600)).thenReturn(facts);
      assertThat(adoption.options(m)).isEmpty();
      assertThatThrownBy(() -> adoption.adopt(id, id + 300))
          .hasMessageContaining("apto ao planejamento");
      assertThat(m.getLearningCycleId()).isNull();
    }
  }

  /**
   * Reproduz o estudo de Vega e outra identidade com fontes primárias e contraponto independentes.
   */
  private ProductModeling referenceStudy(long id) {
    var m = fixture(id);
    m.setResearchScope("INTERNATIONAL_TO_BR_V1");
    String first = id == 2 ? "https://www.whering.co/" : "https://alpha.example/";
    String second = id == 2 ? "https://www.myindyx.com/home/" : "https://beta.example/";
    m.setReferencesJson("[\"" + first + "\",\"" + second + "\"]");
    var d = dossiers.findAllByProductDiscoveryCycleIdOrderByIdAsc(id + 200).get(0);
    d.getProductDiscoveryOpportunity()
        .setMaturity(ProductDiscoveryOpportunityMaturity.RESEARCHABLE);
    d.setMainPain("Escolher combinações com identidade pessoal");
    d.setKnownRisks("Demanda brasileira, preço e margem ainda não comprovada.");
    d.getProductDiscoveryOpportunity()
        .setEvidenceJson(
            "{\"publicEvidenceAssessment\":{\"observations\":[{\"sourceRole\":\"EDITORIAL\",\"url\":\"https://editorial.example/comparacao\",\"supportingExcerpt\":\"Fonte editorial sintética\",\"limitation\":\"Fonte de teste sem prova comercial\",\"sourceSha256\":\""
                + "0".repeat(64)
                + "\"}]}}");
    when(evidence.findByDossierIdOrderByCreatedAtAsc(id + 600))
        .thenReturn(
            List.of(
                referenceFact(
                    id + 701,
                    first,
                    "Fonte primária sintética: experiência anunciada, uso não verificado."),
                referenceFact(
                    id + 702,
                    second,
                    "Fonte primária sintética: alternativa disponível, receita desconhecida."),
                referenceFact(
                    id + 703,
                    "https://editorial.example/comparacao",
                    "Fonte editorial sintética: contraponto e limitações da experiência.")));
    return m;
  }

  /** Produz evidência persistida de teste, sem confundir seu texto com prova comercial. */
  private OpportunityEvidence referenceFact(long id, String url, String observation) {
    var f = new OpportunityEvidence();
    f.setId(id);
    f.setSourceUrl(url);
    f.setSummary(observation);
    f.setCreatedAt(Instant.parse("2026-10-26T20:00:00Z"));
    return f;
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
