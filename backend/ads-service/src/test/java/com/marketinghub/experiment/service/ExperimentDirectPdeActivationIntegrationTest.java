package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityReadiness;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.run.ExperimentEvidenceValidity;
import com.marketinghub.experiment.run.ExperimentRun;
import com.marketinghub.experiment.run.ExperimentRunDataQualityStatus;
import com.marketinghub.experiment.run.ExperimentRunGateCodes;
import com.marketinghub.experiment.run.ExperimentRunGateEvaluatorType;
import com.marketinghub.experiment.run.ExperimentRunGateGroup;
import com.marketinghub.experiment.run.ExperimentRunGateResult;
import com.marketinghub.experiment.run.ExperimentRunGateSeverity;
import com.marketinghub.experiment.run.ExperimentRunGateStatus;
import com.marketinghub.experiment.run.ExperimentRunMode;
import com.marketinghub.experiment.run.ExperimentRunStatus;
import com.marketinghub.experiment.run.ExperimentRunStopPolicy;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.OfferType;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.valuechainposition.ProductProcessPeriodService;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.producttype.ProductTypeStatus;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunGateResultRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentStatusChangeRepository;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.producttype.ProductTypeDefinitionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

/** Responsabilidade: validar a ativação transacional do low-ticket PDE com persistência real. */
@SpringBootTest(classes = com.marketinghub.ads.AdsServiceApplication.class)
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:rigel-activation;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
      "spring.datasource.driverClassName=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create",
      "spring.liquibase.enabled=false"
    })
class ExperimentDirectPdeActivationIntegrationTest {
  @Autowired private ExperimentService experimentService;
  @Autowired private PdeCommercialActivationHumanActivityHandler activationHandler;
  @Autowired private ExperimentRepository experimentRepository;
  @Autowired private ExperimentRunRepository experimentRunRepository;
  @Autowired private ExperimentRunGateResultRepository gateResultRepository;
  @Autowired private ExperimentStatusChangeRepository statusChangeRepository;
  @Autowired private ProductRepository productRepository;
  @Autowired private ProductTypeDefinitionRepository productTypeRepository;
  @Autowired private MarketNicheRepository nicheRepository;
  @Autowired private HypothesisRepository hypothesisRepository;
  @Autowired private CommercialPlanRepository commercialPlanRepository;

  @MockBean private ProductProcessPeriodService productProcessPeriodService;

  /** Limpa o comportamento do mock transacional sem apagar as evidências de cada cenário. */
  @AfterEach
  void resetProcessPeriodService() {
    reset(productProcessPeriodService);
  }

  /** Persiste em conjunto experimento, run, janela e produto depois da autorização válida. */
  @Test
  void shouldPersistAllCommercialActivationStatesTogether() {
    ActivationFixture fixture = readyRigelFixture();

    experimentService.updateStatus(fixture.experimentId(), ExperimentStatus.RUNNING);

    assertThat(experimentRepository.findById(fixture.experimentId()).orElseThrow().getStatus())
        .isEqualTo(ExperimentStatus.RUNNING);
    ExperimentRun activatedRun = experimentRunRepository.findById(fixture.runId()).orElseThrow();
    assertThat(activatedRun.getStatus()).isEqualTo(ExperimentRunStatus.RUNNING);
    assertThat(activatedRun.getPublicationRequestedAt()).isNotNull();
    assertThat(activatedRun.getPublishedAt()).isNotNull();
    assertThat(activatedRun.getCommercialWindowStartedAt()).isNotNull();
    assertThat(productRepository.findById(fixture.productId()).orElseThrow().getCommercialStatus())
        .isEqualTo("ATIVO");
    assertThat(statusChangeRepository.findAll())
        .anySatisfy(
            change -> {
              assertThat(change.getExperiment().getId()).isEqualTo(fixture.experimentId());
              assertThat(change.getAction()).isEqualTo("START");
              assertThat(change.getChangedBy()).isEqualTo("ADMIN_UI");
            });
  }

  /** Libera o formulário humano com os requisitos factuais e o teto do mesmo experimento. */
  @Test
  void shouldExposeHumanApprovalFromAuditedProductionRun() {
    ActivationFixture fixture = readyRigelFixture();
    Product product = productRepository.findById(fixture.productId()).orElseThrow();

    HumanProductProcessActivityReadiness readiness =
        activationHandler.readiness(
            activationProcess(),
            activationActivity(),
            product,
            "experiment:" + fixture.experimentId());

    assertThat(readiness.ready()).isTrue();
    assertThat(readiness.requirements()).allMatch(requirement -> requirement.satisfied());
    assertThat(readiness.requirements())
        .extracting(requirement -> requirement.detail())
        .anyMatch(detail -> detail.contains("desktop e mobile"))
        .anyMatch(detail -> detail.contains("pagamento de teste"))
        .anyMatch(detail -> detail.contains("ausência de gasto"));
    assertThat(readiness.confirmationMessage().replace('\u00a0', ' '))
        .contains("15 contatos", "R$ 540,00");
    assertThat(readiness.decisionMode()).isEqualTo("REVIEW_AND_ACCEPT");
    assertThat(readiness.auditEvidenceReference())
        .contains(
            "experiment:" + fixture.experimentId(),
            "experiment-run:" + fixture.runId(),
            "commercial-plan:");
  }

  /** Mantém o formulário bloqueado quando READY_TO_PUBLISH não possui toda a prova funcional. */
  @Test
  void shouldHideHumanApprovalWhenOneAuditedGateIsMissing() {
    ActivationFixture fixture = readyRigelFixture();
    gateResultRepository
        .findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(fixture.runId())
        .stream()
        .filter(gate -> ExperimentRunGateCodes.DATA_FRESHNESS_VALID.equals(gate.getGateCode()))
        .forEach(gateResultRepository::delete);
    Product product = productRepository.findById(fixture.productId()).orElseThrow();

    HumanProductProcessActivityReadiness readiness =
        activationHandler.readiness(
            activationProcess(),
            activationActivity(),
            product,
            "experiment:" + fixture.experimentId());

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.requirements()).anyMatch(requirement -> !requirement.satisfied());
    assertThat(experimentRepository.findById(fixture.experimentId()).orElseThrow().getStatus())
        .isEqualTo(ExperimentStatus.PLANNED);
  }

  /** Reverte todos os estados quando a persistência do avanço do produto falha. */
  @Test
  void shouldRollbackEveryCommercialActivationStateOnFailure() {
    ActivationFixture fixture = readyRigelFixture();
    doThrow(new IllegalStateException("falha simulada ao registrar período"))
        .when(productProcessPeriodService)
        .recordAuditedPreflightTransition(any(Product.class), any());

    assertThatThrownBy(
            () -> experimentService.updateStatus(fixture.experimentId(), ExperimentStatus.RUNNING))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("falha simulada");

    assertThat(experimentRepository.findById(fixture.experimentId()).orElseThrow().getStatus())
        .isEqualTo(ExperimentStatus.PLANNED);
    ExperimentRun preservedRun = experimentRunRepository.findById(fixture.runId()).orElseThrow();
    assertThat(preservedRun.getStatus()).isEqualTo(ExperimentRunStatus.READY_TO_PUBLISH);
    assertThat(preservedRun.getCommercialWindowStartedAt()).isNull();
    assertThat(productRepository.findById(fixture.productId()).orElseThrow().getCommercialStatus())
        .isEqualTo("COMUNICACAO_E_JORNADA");
    assertThat(statusChangeRepository.findAll())
        .noneMatch(change -> change.getExperiment().getId().equals(fixture.experimentId()));
  }

  /** Cria dados independentes equivalentes ao Rigel, incluindo os quatro gates auditáveis. */
  private ActivationFixture readyRigelFixture() {
    String suffix = UUID.randomUUID().toString();
    ProductTypeDefinition pdeType =
        productTypeRepository.findAll().stream()
            .filter(type -> "PDE".equals(type.getCode()))
            .findFirst()
            .orElseGet(
                () ->
                    productTypeRepository.save(
                        ProductTypeDefinition.builder()
                            .code("PDE")
                            .name("Produto de experiência digital")
                            .status(ProductTypeStatus.ACTIVE)
                            .build()));
    Product product =
        productRepository.save(
            Product.builder()
                .slug("rigel-activation-" + suffix)
                .name("Rigel de homologação")
                .productTypeDefinition(pdeType)
                .commercialStatus("COMUNICACAO_E_JORNADA")
                .build());
    MarketNiche niche =
        nicheRepository.save(MarketNiche.builder().name("Nicho Rigel " + suffix).build());
    Hypothesis hypothesis =
        hypothesisRepository.save(
            Hypothesis.builder()
                .marketNiche(niche)
                .product(product)
                .title("Hipótese Rigel " + suffix)
                .problem("Prestadores perdem clientes por mensagens improvisadas.")
                .promise("Responder com clareza e consistência.")
                .persona("Prestadores de serviço pelo WhatsApp")
                .offerType(OfferType.TRIPWIRE)
                .build());
    Experiment experiment =
        experimentRepository.save(
            Experiment.builder()
                .niche(niche)
                .product(product)
                .hypothesisRef(hypothesis)
                .name("Experimento Rigel " + suffix)
                .experimentType(ExperimentType.LOW_TICKET_PRODUCT)
                .platform(ExperimentPlatform.DIRECT_ONE_TO_ONE)
                .sampleSize(15)
                .status(ExperimentStatus.PLANNED)
                .build());
    commercialPlanRepository.save(
        CommercialPlan.builder()
            .name("Plano Rigel " + suffix)
            .hypothesis(hypothesis)
            .experiment(experiment)
            .maxBudget(new BigDecimal("540.00"))
            .build());
    ExperimentRun run =
        experimentRunRepository.save(
            ExperimentRun.builder()
                .experiment(experiment)
                .runNumber(2)
                .mode(ExperimentRunMode.PRODUCTION)
                .status(ExperimentRunStatus.READY_TO_PUBLISH)
                .evidenceValidity(ExperimentEvidenceValidity.NOT_EVALUATED)
                .stopPolicy(ExperimentRunStopPolicy.MANUAL_ONLY)
                .dataQualityStatus(ExperimentRunDataQualityStatus.VALID)
                .requestedAt(Instant.now())
                .build());
    gateResultRepository.saveAll(
        List.of(
            auditedGate(run, ExperimentRunGateCodes.LANDING_QUALITY_REVIEW_APPROVED),
            auditedGate(run, ExperimentRunGateCodes.CHECKOUT_AND_DELIVERY_CAN_BE_COMPLETED),
            auditedGate(run, ExperimentRunGateCodes.DIRECT_CHANNEL_READINESS_CONFIRMED),
            auditedGate(run, ExperimentRunGateCodes.DATA_FRESHNESS_VALID)));
    return new ActivationFixture(product.getId(), experiment.getId(), run.getId());
  }

  /** Cria um gate humano aprovado e ligado ao run produtivo da fixture. */
  private ExperimentRunGateResult auditedGate(ExperimentRun run, String code) {
    return ExperimentRunGateResult.builder()
        .experimentRun(run)
        .gateCode(code)
        .gateGroup(ExperimentRunGateGroup.FUNCTIONAL_E2E)
        .status(ExperimentRunGateStatus.PASS)
        .severity(ExperimentRunGateSeverity.BLOCKER)
        .summary("Gate homologado localmente")
        .evidenceReference("e2e://rigel/" + code.toLowerCase())
        .evaluatedAt(Instant.now())
        .evaluatorType(ExperimentRunGateEvaluatorType.HUMAN)
        .evaluatorVersion("rigel-activation-test.v1")
        .build();
  }

  /** Monta a definição reconhecida pelo handler de ativação comercial. */
  private BusinessProcessDefinition activationProcess() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setProcessCode("pde-commercial-homologation-activation");
    return process;
  }

  /** Monta a atividade humana reconhecida pelo handler de ativação comercial. */
  private BusinessProcessActivityDefinition activationActivity() {
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setActivityId("authorization");
    return activity;
  }

  /** Agrupa os identificadores persistidos necessários para validar cada cenário. */
  private record ActivationFixture(Long productId, Long experimentId, Long runId) {}
}
