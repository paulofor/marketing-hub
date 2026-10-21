package com.marketinghub.opala.commercial.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleCommercialReadiness;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.pde.service.PdeCommercialCheckoutContractResolver;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: reproduzir a candidata predecessora e validar a proteção antes da fila paga.
 */
class OpalaCommercialClaimPreparationTest {
  private final ObjectMapper json = new ObjectMapper();
  private final OpalaCommercialContext context = mock(OpalaCommercialContext.class);
  private final OpalaCommercialVersionContract versions =
      new OpalaCommercialVersionContract(new PdeCommercialCheckoutContractResolver(json));
  private final OpalaCommercialClaimPreparation preparation =
      new OpalaCommercialClaimPreparation(context, versions);

  /** Reproduz Vega e outra identidade sem permitir que o contrato predecessor chegue ao modelo. */
  @Test
  void waitsForCurrentCandidateForOriginalAndOtherProducts() throws Exception {
    for (Fixture fixture : List.of(fixture(4, 106, 13, "v14"), fixture(44, 710, 99, "other-v3"))) {
      fixture.contract().put("experienceVersion", "previous-version");

      var result = preparation.prepare(fixture.task());

      assertThat(result.ready()).isFalse();
      assertThat(result.blockerCategory()).isEqualTo("MISSING_EVIDENCE");
      assertThat(result.reason()).contains("Dédalo", "contrato comercial", "versão vigente");
      assertThat(fixture.task().getStatus()).isEqualTo("PENDING");
      assertThat(fixture.task().getReceivedAt()).isNull();
      assertThat(fixture.task().getEstimatedCostUsd()).isNull();
    }
  }

  /** A mesma tarefa fica elegível após corrigir sua fonte, sem redefinir status ou custo. */
  @Test
  void reusesPendingTaskAfterItsCandidateIsCorrected() throws Exception {
    Fixture fixture = fixture(4, 106, 13, "v14");
    fixture.contract().put("experienceVersion", "previous-version");
    assertThat(preparation.prepare(fixture.task()).ready()).isFalse();

    fixture.contract().put("experienceVersion", "v14");

    assertThat(preparation.prepare(fixture.task()).ready()).isTrue();
    assertThat(fixture.task().getStatus()).isEqualTo("PENDING");
    assertThat(fixture.task().getReceivedAt()).isNull();
  }

  /** Preserva o caminho antes válido e exige checkout e prazo da mesma versão. */
  @Test
  void preservesValidCandidateAndRejectsMissingCheckoutOrDivergentAccess() throws Exception {
    Fixture valid = fixture(4, 92, 2, "v12");
    assertThat(preparation.prepare(valid.task()).ready()).isTrue();

    Fixture noCheckout = fixture(14, 192, 22, "future-v2");
    noCheckout.contract().remove("commercialCheckout");
    assertThat(preparation.prepare(noCheckout.task()).reason()).contains("checkout canônico");

    Fixture wrongAccess = fixture(24, 292, 32, "future-v3");
    ((ObjectNode) wrongAccess.contract().path("commercialAccess"))
        .put("experienceVersion", "another-version");
    assertThat(preparation.prepare(wrongAccess.task()).reason()).contains("acesso pago");
  }

  /** A abertura da atividade usa o mesmo validador da fila e expõe a causa sem erro HTTP 500. */
  @Test
  void exposesSameBlockerBeforeOpeningActivity() throws Exception {
    Fixture fixture = fixture(4, 106, 13, "v14");
    fixture.contract().put("experienceVersion", "previous-version");
    var materialization = mock(OpalaCommercialMaterialization.class);
    var service =
        new OpalaCommercialService(
            context,
            materialization,
            mock(LearningCycleCommercialReadiness.class),
            mock(AgentTaskRepository.class),
            mock(OpalaCommercialRouting.class),
            preparation);
    var activity = new BusinessProcessActivityDefinition();
    activity.setActivityId("entry");

    var result =
        service.readiness(
            fixture.task().getProcessDefinition(),
            activity,
            fixture.scope().experiment().getProduct(),
            fixture.task().getSourceReference());

    assertThat(result.ready()).isFalse();
    assertThat(result.reason()).isEqualTo(preparation.prepare(fixture.task()).reason());
    verifyNoInteractions(materialization);
  }

  /** Outra identidade não recebe a candidata e revisões posteriores conservam seus gates. */
  @Test
  void rejectsCrossProductAndLimitsSupportedActivities() throws Exception {
    Fixture fixture = fixture(4, 106, 13, "v14");
    assertThat(preparation.inspect("experiment:106", 44L).ready()).isFalse();
    for (String activity : List.of("entry", "creative", "checkout", "targeting")) {
      fixture.task().setProcessActivityId(activity);
      assertThat(preparation.supports(fixture.task())).isTrue();
    }
    for (String activity :
        List.of("economics", "humanExperienceReview", "commercialIntegrityReview")) {
      fixture.task().setProcessActivityId(activity);
      assertThat(preparation.supports(fixture.task())).isFalse();
    }
    fixture.task().setProcessActivityId("entry");
    fixture.task().getProcessDefinition().setProcessCode("other-pipeline");
    assertThat(preparation.supports(fixture.task())).isFalse();
    assertThat(preparation.supports(null)).isFalse();
  }

  /** Não cobra uma avaliação para descobrir que o destino está ausente ou a candidata é ambígua. */
  @Test
  void waitsForDestinationAndSingleCandidate() throws Exception {
    Fixture fixture = fixture(4, 106, 13, "v14");
    when(context.candidate(fixture.scope()))
        .thenReturn(
            new OpalaCommercialContext.Candidate(List.of(), null, "MISSING", fixture.contract()));
    assertThat(preparation.prepare(fixture.task()).reason()).contains("destino HTTPS");

    when(context.candidate(fixture.scope()))
        .thenReturn(
            new OpalaCommercialContext.Candidate(
                List.of(
                    new com.marketinghub.pde.PdeProductionSlot(),
                    new com.marketinghub.pde.PdeProductionSlot()),
                "https://pde.sandbox.local",
                "AMBIGUOUS",
                fixture.contract()));
    assertThat(preparation.prepare(fixture.task()).reason()).contains("ambiguidade");
  }

  /** Falha de infraestrutura conserva seu diagnóstico técnico em vez de pedir outro contrato. */
  @Test
  void distinguishesInfrastructureFailureFromMissingCandidate() {
    when(context.scope("experiment:106"))
        .thenThrow(
            new org.springframework.dao.DataAccessResourceFailureException("synthetic failure"));

    var result = preparation.inspect("experiment:106", 4L);

    assertThat(result.ready()).isFalse();
    assertThat(result.blockerCategory()).isEqualTo("TECHNICAL_FAILURE");
    assertThat(result.reason()).contains("recuperar a consulta");
  }

  /** Monta somente contratos sintéticos locais, sem pagamento, publicação ou provedor externo. */
  private Fixture fixture(long productId, long experimentId, long cycleId, String version)
      throws Exception {
    Product product = Product.builder().id(productId).slug("fixture-" + productId).build();
    Experiment experiment =
        Experiment.builder()
            .id(experimentId)
            .product(product)
            .unitPrice(new BigDecimal("67"))
            .build();
    var cycle = new LearningSalesCycle();
    cycle.setId(cycleId);
    cycle.setProductId(productId);
    cycle.setExperimentId(experimentId);
    cycle.setProductVersion(version);
    var scope = new OpalaCommercialContext.Scope(cycle, experiment);
    var contract = json.createObjectNode().put("experienceVersion", version);
    contract
        .putObject("commercialBinding")
        .put("experimentId", experimentId)
        .put("priceBrl", 67)
        .put("billingModel", "ONE_TIME");
    contract
        .putObject("commercialCheckout")
        .put("provider", "LOCAL_TEST")
        .put("checkoutUrl", "https://checkout.sandbox.local/offer")
        .put("offerReference", "fixture")
        .put("priceBrl", 67)
        .put("currency", "BRL")
        .put("billingModel", "ONE_TIME");
    contract
        .putObject("commercialAccess")
        .put("experienceVersion", version)
        .put("accessDays", 90)
        .put("renewal", false)
        .put("activationTrigger", "PAYMENT_APPROVED")
        .put("scope", "SYNTHETIC_PAID_DELIVERY");
    var process = new BusinessProcessDefinition();
    process.setProcessCode(OpalaCommercialContext.CODE);
    var task = new AgentTask();
    task.setProcessDefinition(process);
    task.setProcessActivityId("entry");
    task.setSourceReference("experiment:" + experimentId);
    task.setStatus("PENDING");
    when(context.scope(task.getSourceReference())).thenReturn(scope);
    when(context.candidate(scope))
        .thenReturn(
            new OpalaCommercialContext.Candidate(
                List.of(), "https://pde.sandbox.local", "VERSION_SLOT", contract));
    return new Fixture(task, scope, contract);
  }

  /** Mantém tarefa, identidade e candidata do caso sintético sem estado global. */
  private record Fixture(AgentTask task, OpalaCommercialContext.Scope scope, ObjectNode contract) {}
}
