package com.marketinghub.experiment.run.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignObjective;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.run.ExperimentRun;
import com.marketinghub.experiment.run.ExperimentRunGateCodes;
import com.marketinghub.experiment.run.ExperimentRunGateGroup;
import com.marketinghub.experiment.run.ExperimentRunGateResult;
import com.marketinghub.experiment.run.ExperimentRunGateStatus;
import com.marketinghub.experiment.run.ExperimentRunMode;
import com.marketinghub.experiment.run.ExperimentRunStatus;
import com.marketinghub.experiment.run.controller.BackendExperimentRunController;
import com.marketinghub.experiment.run.service.homologation.ExperimentRunHomologationRequest;
import com.marketinghub.experiment.run.service.homologation.ExperimentRunHomologationRequest.GateEvidence;
import com.marketinghub.product.Product;
import com.marketinghub.quartzo.commercial.v1.service.QuartzoCommercialContext;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunGateResultRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: validar a integração entre homologação funcional e escopo Quartzo. */
class BackendExperimentRunServiceQuartzoTest {
  private static final long RUN_ID = 92001L;
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final ExperimentRunRepository runs = mock(ExperimentRunRepository.class);
  private final ExperimentRunGateResultRepository gates =
      mock(ExperimentRunGateResultRepository.class);
  private final MoisCommercialDossierPreflightService dossiers =
      mock(MoisCommercialDossierPreflightService.class);
  private final QuartzoPreflightEvidenceScopeService quartzo =
      mock(QuartzoPreflightEvidenceScopeService.class);
  private final BackendExperimentRunService service =
      new BackendExperimentRunService(experiments, runs, gates, dossiers, null, quartzo);

  /** Homologa HTTP em UTF-8 com validador de tokens, preservando prova antiga e recuperação. */
  @Test
  void servesStaleAndCurrentEvidenceThroughTheCanonicalEndpoint() throws Exception {
    ExperimentRun run = approvedRun();
    run.getExperiment().setProduct(Product.builder().id(92011L).build());
    var context = mock(QuartzoCommercialContext.class);
    when(context.applies(run.getExperiment().getProduct())).thenReturn(true);
    var snapshot = new ObjectMapper().createObjectNode();
    snapshot.put("publicationId", 92012L);
    snapshot.put("pageHash", "a".repeat(64));
    snapshot.put("fingerprint", "b".repeat(64));
    when(context.snapshot("experiment:92008")).thenReturn(snapshot);
    var validator = new QuartzoPreflightEvidenceScopeService(context, gates);
    var integrated =
        new BackendExperimentRunService(experiments, runs, gates, dossiers, null, validator);
    var http =
        MockMvcBuilders.standaloneSetup(new BackendExperimentRunController(integrated)).build();
    var reference = validator.requiredReference(run);
    var landing = gates.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(RUN_ID).getFirst();
    landing.setEvidenceReference(reference.replace("publication:92012", "publication:92007"));

    var staleResponse =
        http.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/experiment-runs/{id}/preflight", RUN_ID))
            .andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                        "$.hasBlockers")
                    .value(true))
            .andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                        "$.runStatus")
                    .value("COMPLETED"))
            .andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                        "$.gates[0].status")
                    .value("PASS"))
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

    landing.setEvidenceReference(reference);
    var currentResponse =
        http.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/experiment-runs/{id}/preflight", RUN_ID))
            .andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                        "$.hasBlockers")
                    .value(false))
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    String fixtureOutput = System.getProperty("preflight.fixture-output");
    if (fixtureOutput != null) {
      var mapper = new ObjectMapper();
      var fixture = mapper.createObjectNode();
      fixture.set("stale", mapper.readTree(staleResponse));
      fixture.set("current", mapper.readTree(currentResponse));
      java.nio.file.Files.writeString(java.nio.file.Path.of(fixtureOutput), fixture.toString());
    }
    verify(runs, never()).save(any());
    verify(gates, never()).saveAll(any());
  }

  /** Aprovação histórica não libera a leitura quando a publicação Quartzo já mudou. */
  @Test
  void blocksStaleApprovedQuartzoEvidenceWithoutChangingHistory() {
    ExperimentRun run = approvedRun();
    when(quartzo.applies(run)).thenReturn(true);
    when(quartzo.requiredReference(run)).thenReturn("publication:92009;page-sha256:new-hash");
    when(quartzo.hasCurrentEvidence(run)).thenReturn(false);

    var result = service.getPreflight(RUN_ID);

    assertThat(result.hasBlockers()).isTrue();
    assertThat(result.currentEvidenceBlockReason()).contains("nova tentativa", "não reativa");
    assertThat(result.requiredLandingEvidenceReference()).contains("publication:92009");
    assertThat(result.runStatus()).isEqualTo(ExperimentRunStatus.COMPLETED);
    assertThat(result.gates()).allMatch(gate -> gate.status() == ExperimentRunGateStatus.PASS);
    verify(runs, never()).save(any());
    verify(gates, never()).saveAll(any());
    verifyNoInteractions(experiments, dossiers);
  }

  /** Uma prova atual continua aprovada e não exige nova homologação ou consumo. */
  @Test
  void acceptsCurrentApprovedQuartzoEvidence() {
    ExperimentRun run = approvedRun();
    when(quartzo.applies(run)).thenReturn(true);
    when(quartzo.requiredReference(run)).thenReturn("publication:92009;page-sha256:current");
    when(quartzo.hasCurrentEvidence(run)).thenReturn(true);

    var result = service.getPreflight(RUN_ID);

    assertThat(result.hasBlockers()).isFalse();
    assertThat(result.currentEvidenceBlockReason()).isNull();
  }

  /** Identidade ausente produz bloqueio acionável, sem chamar o validador que exige publicação. */
  @Test
  void blocksMissingPublicationIdentityWithoutFailingRead() {
    ExperimentRun run = approvedRun();
    when(quartzo.applies(run)).thenReturn(true);

    var result = service.getPreflight(RUN_ID);

    assertThat(result.hasBlockers()).isTrue();
    assertThat(result.currentEvidenceBlockReason()).contains("não possui identidade verificável");
    verify(quartzo, never()).hasCurrentEvidence(any());
  }

  /** O mesmo contrato preserva o isolamento e bloqueia evidência antiga do percurso Safira. */
  @Test
  void blocksStaleApprovedSafiraEvidence() {
    ExperimentRun run = approvedRun();
    var safira = mock(SafiraPreflightEvidenceScopeService.class);
    ReflectionTestUtils.setField(service, "safiraEvidenceScope", safira);
    when(safira.applies(run)).thenReturn(true);
    when(safira.requiredReference(run)).thenReturn("slot:92010;experience-sha256:new-hash");
    when(safira.hasCurrentEvidence(run)).thenReturn(false);

    var result = service.getPreflight(RUN_ID);

    assertThat(result.hasBlockers()).isTrue();
    assertThat(result.requiredLandingEvidenceReference()).startsWith("slot:92010");
    assertThat(result.gates()).allMatch(gate -> gate.status() == ExperimentRunGateStatus.PASS);
    verify(quartzo, never()).hasCurrentEvidence(any());
  }

  /** Tipos sem identidade publicada específica continuam dependendo dos gates funcionais. */
  @Test
  void preservesOtherTypesAndFailedFunctionalGates() {
    approvedRun();
    assertThat(service.getPreflight(RUN_ID).hasBlockers()).isFalse();
    var persisted = gates.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(RUN_ID);
    persisted.get(1).setStatus(ExperimentRunGateStatus.FAIL);

    var result = service.getPreflight(RUN_ID);

    assertThat(result.hasBlockers()).isTrue();
    assertThat(result.currentEvidenceBlockReason()).isNull();
  }

  /** Cria fontes aprovadas de outra identidade sintética, sem campanha ou banco produtivo. */
  private ExperimentRun approvedRun() {
    var experiment = Experiment.builder().id(92008L).build();
    var run =
        ExperimentRun.builder()
            .id(RUN_ID)
            .experiment(experiment)
            .mode(ExperimentRunMode.PRODUCTION)
            .status(ExperimentRunStatus.COMPLETED)
            .build();
    var persisted = functionalGates(run);
    persisted.forEach(gate -> gate.setStatus(ExperimentRunGateStatus.PASS));
    when(runs.findById(RUN_ID)).thenReturn(Optional.of(run));
    when(gates.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(RUN_ID)).thenReturn(persisted);
    return run;
  }

  /** Valida a identidade Quartzo antes de persistir quatro gates aprovados e liberar o run. */
  @Test
  void validatesQuartzoScopeBeforeCompletingHomologation() {
    Experiment experiment =
        Experiment.builder()
            .id(92002L)
            .campaignObjective(ExperimentCampaignObjective.SALES)
            .platform(ExperimentPlatform.FACEBOOK)
            .build();
    ExperimentRun run =
        ExperimentRun.builder()
            .id(RUN_ID)
            .experiment(experiment)
            .mode(ExperimentRunMode.PRODUCTION)
            .status(ExperimentRunStatus.PREFLIGHT_PENDING)
            .build();
    List<ExperimentRunGateResult> persisted = functionalGates(run);
    String landingReference =
        "publication:92003;page-sha256:"
            + "a".repeat(64)
            + ";quartzo-fingerprint:"
            + "b".repeat(64);
    ExperimentRunHomologationRequest request =
        new ExperimentRunHomologationRequest(
            List.of(
                evidence(ExperimentRunGateCodes.LANDING_QUALITY_REVIEW_APPROVED, landingReference),
                evidence(
                    ExperimentRunGateCodes.CHECKOUT_AND_DELIVERY_CAN_BE_COMPLETED,
                    "sandbox://checkout-delivery/92001"),
                evidence(
                    ExperimentRunGateCodes.META_EFFECTIVE_STATUS_CONFIRMED,
                    "meta://campaign-paused/92001"),
                evidence(ExperimentRunGateCodes.DATA_FRESHNESS_VALID, "db://qa-events/92001")));
    when(runs.findById(RUN_ID)).thenReturn(Optional.of(run));
    when(gates.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(RUN_ID)).thenReturn(persisted);
    when(gates.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    when(runs.save(any(ExperimentRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
    doAnswer(
            invocation -> {
              run.setAssetBundleVersion(92003);
              return null;
            })
        .when(quartzo)
        .validateAndBind(eq(run), any(GateEvidence.class));

    var result = service.recordHomologationResults(RUN_ID, request);

    verify(quartzo)
        .validateAndBind(
            eq(run),
            org.mockito.ArgumentMatchers.argThat(
                evidence -> landingReference.equals(evidence.evidenceReference())));
    assertThat(result.runStatus()).isEqualTo(ExperimentRunStatus.READY_TO_PUBLISH);
    assertThat(run.getAssetBundleVersion()).isEqualTo(92003);
  }

  /** Monta os quatro gates funcionais do contrato de venda por mídia paga. */
  private List<ExperimentRunGateResult> functionalGates(ExperimentRun run) {
    return List.of(
        gate(run, ExperimentRunGateCodes.LANDING_QUALITY_REVIEW_APPROVED),
        gate(run, ExperimentRunGateCodes.CHECKOUT_AND_DELIVERY_CAN_BE_COMPLETED),
        gate(run, ExperimentRunGateCodes.META_EFFECTIVE_STATUS_CONFIRMED),
        gate(run, ExperimentRunGateCodes.DATA_FRESHNESS_VALID));
  }

  /** Cria um gate pendente que será atualizado pela homologação. */
  private ExperimentRunGateResult gate(ExperimentRun run, String code) {
    return ExperimentRunGateResult.builder()
        .experimentRun(run)
        .gateCode(code)
        .gateGroup(ExperimentRunGateGroup.FUNCTIONAL_E2E)
        .status(ExperimentRunGateStatus.PENDING)
        .build();
  }

  /** Cria uma evidência aprovada, auditável e não vinculada a produto real. */
  private GateEvidence evidence(String code, String reference) {
    return new GateEvidence(
        code, ExperimentRunGateStatus.PASS, "Comprovado localmente.", reference);
  }
}
