package com.marketinghub.quartzo.commercial.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.service.ExperimentCampaignDestinationPolicy;
import com.marketinghub.experiment.service.ExperimentReadinessService;
import com.marketinghub.experiment.service.ExperimentTargetingSelectionService;
import com.marketinghub.financialplan.v1.FinancialPlanRevision.Environment;
import com.marketinghub.financialplan.v1.service.FinancialPlanService;
import com.marketinghub.financialplan.v1.service.getplan.PlanView;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.service.CommercialPlanLandingAssetService;
import com.marketinghub.product.Product;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Comprova o vínculo econômico pelo contrato, preservando a versão executável de Quartzo. */
class QuartzoFinancialContractContextTest {
  final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  final ExperimentRepository experiments = mock(ExperimentRepository.class);
  final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  final FinancialPlanService finances = mock(FinancialPlanService.class);
  final CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
  final ExperimentCampaignDestinationPolicy destinations =
      mock(ExperimentCampaignDestinationPolicy.class);
  final QuartzoCommercialContext context =
      new QuartzoCommercialContext(
          experiments,
          cycles,
          destinations,
          mock(CreativeRepository.class),
          mock(CommercialPlanLandingAssetService.class),
          mock(ExperimentTargetingSelectionService.class),
          finances,
          plans,
          json);
  Product product;
  Experiment experiment;
  LearningSalesCycle cycle;
  CommercialPlan plan;

  /** Prepara a mesma referência de produto, ciclo e plano usada por uma candidata aberta. */
  @BeforeEach
  void setup() {
    product =
        Product.builder()
            .id(7L)
            .validationDefinitionVersion("v1")
            .currentPriceBrl(new BigDecimal("67"))
            .productTypeDefinition(
                ProductTypeDefinition.builder().code(QuartzoCommercialContext.TYPE).build())
            .build();
    experiment = new Experiment();
    experiment.setId(105L);
    experiment.setProduct(product);
    experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
    experiment.setStatus(ExperimentStatus.PLANNED);
    experiment.setUnitPrice(new BigDecimal("67"));
    cycle = new LearningSalesCycle();
    cycle.setId(12L);
    cycle.setProductId(7L);
    cycle.setProductVersion("capella-private-v3");
    cycle.setStatus("OPEN");
    cycle.setStage("AUTHORIZATION");
    plan = new CommercialPlan();
    plan.setId(2L);
    when(experiments.findById(anyLong())).thenAnswer(i -> Optional.of(experiment));
    when(cycles.findByExperimentId(anyLong())).thenAnswer(i -> Optional.of(cycle));
    when(plans.findByExperimentReference(anyLong())).thenReturn(List.of(plan));
  }

  /** Caso original, outra identidade e versão antes válida chegam ao gate sem novo Plutus. */
  @ParameterizedTest
  @CsvSource({
    "7,105,12,2,22,v1,capella-private-v3",
    "9007,9105,912,902,922,KIT_CONTRACT_V4,kit-candidate-v8",
    "8007,8105,812,802,822,v1,v1"
  })
  void selectsCanonicalContractAndPreservesImplementation(
      long productId,
      long experimentId,
      long cycleId,
      long planId,
      long financialId,
      String contractVersion,
      String implementationVersion)
      throws Exception {
    product.setId(productId);
    product.setValidationDefinitionVersion(contractVersion);
    experiment.setId(experimentId);
    cycle.setId(cycleId);
    cycle.setProductId(productId);
    cycle.setProductVersion(implementationVersion);
    plan.setId(planId);
    var approved = financial(financialId, planId, contractVersion);
    when(finances.list("PRODUCT", productId, Environment.LIVE)).thenReturn(List.of(approved));

    var snapshot = context.snapshot("experiment:" + experimentId);

    assertThat(snapshot.path("productVersion").asText()).isEqualTo(implementationVersion);
    assertThat(snapshot.at("/financialPlan/id").asLong()).isEqualTo(financialId);
    assertThat(snapshot.at("/financialPlan/assumptions/productVersion").asText())
        .isEqualTo(contractVersion);
    assertThat(snapshot.at("/financialPlan/analysis/executionId").asLong()).isEqualTo(88L);
    var checks = new QuartzoCommercialChecks(destinations, mock(ExperimentReadinessService.class));
    assertThatCode(
            () ->
                checks.check(
                    "economics",
                    context.scope("experiment:" + experimentId, productId, true),
                    snapshot))
        .doesNotThrowAnyException();
    verify(finances).list("PRODUCT", productId, Environment.LIVE);
    verifyNoMoreInteractions(finances);
    verify(experiments, never()).save(any());
  }

  /** O mesmo contrato econômico continua selecionável no percurso legado sem ciclo. */
  @Test
  void retainsLegacyContractWithoutCycle() throws Exception {
    when(cycles.findByExperimentId(105L)).thenReturn(Optional.empty());
    when(finances.list("PRODUCT", 7L, Environment.LIVE))
        .thenReturn(List.of(financial(22L, 2L, "v1")));
    var snapshot = context.snapshot("experiment:105");
    assertThat(snapshot.path("productVersion").asText()).isEqualTo("v1");
    assertThat(snapshot.at("/financialPlan/id").asLong()).isEqualTo(22L);
  }

  /** Parecer de outro contrato ou plano não é aproveitado pelo nome da implementação. */
  @Test
  void rejectsForeignContractAndCommercialPlan() throws Exception {
    when(finances.list("PRODUCT", 7L, Environment.LIVE))
        .thenReturn(List.of(financial(23L, 2L, "capella-private-v3"), financial(24L, 999L, "v1")));
    assertThat(context.snapshot("experiment:105").path("financialPlan").isNull()).isTrue();
  }

  /** Contrato canônico ausente não pode ser inferido da versão executável do ciclo. */
  @Test
  void rejectsMissingCanonicalContract() throws Exception {
    product.setValidationDefinitionVersion(null);
    when(finances.list("PRODUCT", 7L, Environment.LIVE))
        .thenReturn(List.of(financial(23L, 2L, "capella-private-v3")));
    assertThat(context.snapshot("experiment:105").path("financialPlan").isNull()).isTrue();
  }

  /** Monta parecer concluído e descoberta limitada sem alterar o contrato econômico original. */
  private PlanView financial(long id, long planId, String contractVersion) throws Exception {
    var node =
        (ObjectNode)
            json.readTree(
                """
                {"scope":"PRODUCT","environment":"LIVE","revision":13,
                 "commercialPlanVersion":7,"stale":false,"pendingActions":[],
                 "assumptions":{"priceBrl":67,"minimumMarginPercent":30,
                   "discoveryInvestment":{"mediaBudgetBrl":100,"durationDays":5,
                     "acceptBelowMinimumMargin":true,"decisionReference":"fixture-autorizada"}},
                 "evaluation":{"status":"DISCOVERY_REVIEW"},
                 "analysis":{"executionId":88,"status":"COMPLETED","result":{
                   "decision":"APPROVE","costCoverageAssessment":{"status":"COMPLETE_DETAILED"},
                   "scenarios":[{"name":"CONSERVATIVE"},{"name":"BASE","profitBrl":-10,
                     "averagePriceBrl":67},{"name":"OPTIMISTIC"}]}}}
                """);
    node.put("id", id).put("scopeId", product.getId()).put("commercialPlanId", planId);
    node.withObject("/assumptions").put("productVersion", contractVersion);
    node.withObject("/assumptions/discoveryInvestment")
        .put("experimentId", experiment.getId())
        .put("learningCycleId", cycle.getId());
    return json.treeToValue(node, PlanView.class);
  }
}
