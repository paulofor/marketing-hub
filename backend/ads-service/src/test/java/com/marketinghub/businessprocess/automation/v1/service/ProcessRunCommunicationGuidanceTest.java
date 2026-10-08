package com.marketinghub.businessprocess.automation.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.financialplan.v1.FinancialPlanRevision;
import com.marketinghub.financialplan.v1.FinancialPlanRevision.Environment;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.financialplan.FinancialPlanRevisionRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: preservar contexto, história e autoridade ao explicar a meta econômica. */
class ProcessRunCommunicationGuidanceTest {
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
  private final FinancialPlanRevisionRepository revisions =
      mock(FinancialPlanRevisionRepository.class);
  private final ObjectMapper json = new ObjectMapper();
  private final ProcessRunCommunicationGuidance guidance =
      new ProcessRunCommunicationGuidance(processes, experiments, plans, revisions, json);
  private ProcessRun run;
  private Product product;
  private Experiment experiment;
  private CommercialPlan plan;
  private FinancialPlanRevision revision;

  /** Monta contexto sintético da regressão sem contato com banco ou agentes externos. */
  @BeforeEach
  void setup() {
    context(11L, 97L, 34L, 12L);
  }

  /** Configura identidades distintas usando os mesmos contratos e métodos de consulta. */
  private void context(long productId, long experimentId, long planId, long revisionId) {
    run = new ProcessRun();
    run.setId(44L);
    run.setStatus("WAITING_INPUT");
    run.setProductId(productId);
    run.setProcessDefinitionId(113L);
    run.setSourceReference("experiment:" + experimentId);
    run.setCurrentActivityId("communicationContract");
    product = new Product();
    product.setId(productId);
    product.setAutomaticExecutionEnabled(true);
    experiment = new Experiment();
    experiment.setId(experimentId);
    experiment.setStatus(ExperimentStatus.PLANNED);
    experiment.setProduct(product);
    plan = new CommercialPlan();
    plan.setId(planId);
    revision = new FinancialPlanRevision();
    revision.setId(revisionId);
    revision.setScopeKind("PRODUCT");
    revision.setScopeId(productId);
    revision.setProductId(productId);
    revision.setEnvironment(Environment.LIVE);
    revision.setCommercialPlanId(planId);
    revision.setAssumptionsJson("{\"minimumMarginPercent\":null}");
    var process = new BusinessProcessDefinition();
    process.setProcessCode("pde-communication-sales-journey");
    when(processes.findById(113L)).thenReturn(Optional.of(process));
    when(experiments.findById(experimentId)).thenReturn(Optional.of(experiment));
    when(plans.findByExperimentReference(experimentId)).thenReturn(List.of(plan));
    when(revisions.findByScopeKindAndScopeIdAndEnvironmentOrderByRevisionNumberDesc(
            "PRODUCT", productId, Environment.LIVE))
        .thenReturn(List.of(revision));
  }

  /** Explica base e limites da decisão em Alcyone e outro produto, sem gravar ou consumir. */
  @ParameterizedTest
  @CsvSource({"11,97,34,12", "710,893,934,912"})
  void missingTargetHasExactContextAndNeverWrites(
      long productId, long experimentId, long planId, long revisionId) throws Exception {
    context(productId, experimentId, planId, revisionId);
    run.setReason(
        "Dédalo: falta comprovar a geração personalizada integrada; Plutus: custos incompletos.");
    var action = guidance.resolve(run);
    assertThat(action.code()).isEqualTo("DEFINE_CONTRIBUTION_TARGET");
    assertThat(action.waitingStatus()).isEqualTo("WAITING_HUMAN");
    assertThat(action.reason())
        .contains(
            "#" + revisionId,
            "taxas/impostos",
            "aquisição",
            "não é lucro líquido",
            "não resolve",
            "receita líquida positiva",
            "sem descontá-las duas vezes",
            run.getReason());
    assertThat(action.actionUrl())
        .isEqualTo(
            "/financial/plans?productId="
                + productId
                + "&commercialPlanId="
                + planId
                + "&revisionId="
                + revisionId
                + "&edit=contribution-target");
    assertThat(action.afterAction())
        .contains(
            "Exemplo fictício",
            "40% do valor cobrado",
            "44,44%",
            "custos estiverem completos",
            "não autoriza");
    assertThat(action.evidenceReference()).endsWith("/" + productId + "/revisions/" + revisionId);
    assertThat(run.getStatus()).isEqualTo("WAITING_INPUT");
    verify(revisions, never()).save(any());
    verify(revisions, never()).saveAndFlush(any());
    String fixture = System.getProperty("communication-guidance.fixtureOutput");
    if (fixture != null && productId == 11)
      Files.writeString(Path.of(fixture), json.writeValueAsString(action));
  }

  /** Conserva o caminho antes válido e qualquer meta já informada, sem escolher outra. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"minimumMarginPercent\":40}",
        "{\"minimumMarginPercent\":0}",
        "{\"minimumMarginPercent\":\"40\"}"
      })
  void registeredTargetIsNotRequestedAgain(String assumptions) {
    revision.setAssumptionsJson(assumptions);
    assertThat(guidance.resolve(run)).isNull();
  }

  /** Pausa, encerramento, fila, execução e falha técnica não viram decisão econômica. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "PAUSED",
        "PAUSING",
        "COMPLETED",
        "CLOSED",
        "FAILED",
        "QUEUED",
        "RUNNING",
        "WAITING_ACTIVITY"
      })
  void preservesRunAuthority(String status) {
    run.setStatus(status);
    assertThat(guidance.resolve(run)).isNull();
    verifyNoInteractions(experiments, plans, revisions);
  }

  /** Não reutiliza revisão ou plano de outra identidade e conserva segregação de testes. */
  @Test
  void rejectsOtherContextAndTestData() {
    revision.setEnvironment(Environment.TEST);
    assertThat(guidance.resolve(run)).isNull();
    revision.setEnvironment(Environment.LIVE);
    revision.setProductId(99L);
    assertThat(guidance.resolve(run)).isNull();
    revision.setProductId(11L);
    revision.setScopeId(99L);
    assertThat(guidance.resolve(run)).isNull();
    revision.setScopeId(11L);
    revision.setCommercialPlanId(99L);
    assertThat(guidance.resolve(run)).isNull();
    revision.setCommercialPlanId(34L);
    experiment.setProduct(new Product());
    assertThat(guidance.resolve(run)).isNull();
  }

  /** Preserva decisões de ciclos e ignora entidades encerradas, STOP e planos ambíguos. */
  @Test
  void excludesCyclesStoppedProductsAndAmbiguousPlans() {
    run.setLearningCycleId(5L);
    assertThat(guidance.resolve(run)).isNull();
    run.setLearningCycleId(null);
    product.setAutomaticExecutionEnabled(false);
    assertThat(guidance.resolve(run)).isNull();
    product.setAutomaticExecutionEnabled(true);
    experiment.setStatus(ExperimentStatus.INVALIDATED);
    assertThat(guidance.resolve(run)).isNull();
    experiment.setStatus(ExperimentStatus.PLANNED);
    when(plans.findByExperimentReference(97L)).thenReturn(List.of(plan, new CommercialPlan()));
    assertThat(guidance.resolve(run)).isNull();
  }

  /** Corrupção, fonte ausente, atividade diferente e falha real não justificam meta inferida. */
  @Test
  void invalidOrMissingSourcesDoNotFabricateDecision() {
    revision.setAssumptionsJson("{invalid}");
    assertThat(guidance.resolve(run)).isNull();
    revision.setAssumptionsJson("null");
    assertThat(guidance.resolve(run)).isNull();
    revision.setAssumptionsJson("[]");
    assertThat(guidance.resolve(run)).isNull();
    when(revisions.findByScopeKindAndScopeIdAndEnvironmentOrderByRevisionNumberDesc(
            "PRODUCT", 11L, Environment.LIVE))
        .thenReturn(List.of());
    assertThat(guidance.resolve(run)).isNull();
    run.setCurrentActivityId("destination");
    assertThat(guidance.resolve(run)).isNull();
    run.setCurrentActivityId("communicationContract");
    run.setFailureCount(1);
    assertThat(guidance.resolve(run)).isNull();
  }

  /** Referências inválidas não derrubam a tela nem consultam outra ocorrência. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "experiment:999999999999999999999999999",
        "experiment:0",
        "experiment:-1",
        "product:97",
        "experiment:x"
      })
  void invalidReferenceDoesNotBreakStatus(String reference) {
    run.setSourceReference(reference);
    assertThat(guidance.resolve(run)).isNull();
    verifyNoInteractions(experiments, plans, revisions);
  }
}
