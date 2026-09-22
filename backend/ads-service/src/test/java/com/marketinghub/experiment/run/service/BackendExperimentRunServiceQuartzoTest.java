package com.marketinghub.experiment.run.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.marketinghub.experiment.run.service.homologation.ExperimentRunHomologationRequest;
import com.marketinghub.experiment.run.service.homologation.ExperimentRunHomologationRequest.GateEvidence;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunGateResultRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

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
