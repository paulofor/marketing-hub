package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.run.ExperimentRun;
import com.marketinghub.experiment.run.ExperimentRunDataQualityStatus;
import com.marketinghub.experiment.run.ExperimentRunGateCodes;
import com.marketinghub.experiment.run.ExperimentRunGateResult;
import com.marketinghub.experiment.run.ExperimentRunGateStatus;
import com.marketinghub.experiment.run.ExperimentRunMode;
import com.marketinghub.experiment.run.ExperimentRunStatus;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRunGateResultRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar o vínculo entre publicação PDE e preflight produtivo auditado. */
class PublishedPdePreflightEvidenceServiceTest {
  private static final String VERSION = "musa-pde-entry-v12-primeiro-ajuste-aplicavel";
  private static final String URL = "https://v8.clubemusa.com.br";

  private final ExperimentRunRepository runs = mock(ExperimentRunRepository.class);
  private final ExperimentRunGateResultRepository gates =
      mock(ExperimentRunGateResultRepository.class);
  private final PdeProductionSlotRepository slots = mock(PdeProductionSlotRepository.class);
  private final PublishedPdePreflightEvidenceService service =
      new PublishedPdePreflightEvidenceService(runs, gates, slots);

  private Experiment experiment;
  private ExperimentRun run;
  private PdeProductionSlot slot;

  /** Monta a publicação e o run produtivo coincidentes usados pelos cenários. */
  @BeforeEach
  void setUp() {
    experiment = new Experiment();
    experiment.setId(92L);
    experiment.setProduct(Product.builder().id(4L).slug("metodo-musa-7-dias").build());
    experiment.setPlatform(ExperimentPlatform.FACEBOOK);
    experiment.setExperimentType(ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL);
    experiment.setFollowUpActionUrl(URL);
    slot =
        PdeProductionSlot.builder()
            .id(8L)
            .slotCode("v8")
            .productSlug("metodo-musa-7-dias")
            .publicUrl(URL + "/")
            .experienceVersion(VERSION)
            .sourceExperimentId(92L)
            .status(PdeProductionSlotStatus.ACTIVE)
            .validationStatus("OK")
            .publishedExperienceJson("{\"experienceVersion\":\"" + VERSION + "\"}")
            .publishedAt(Instant.parse("2026-09-19T19:30:00Z"))
            .build();
    run =
        ExperimentRun.builder()
            .id(11L)
            .experiment(experiment)
            .mode(ExperimentRunMode.PRODUCTION)
            .status(ExperimentRunStatus.READY_TO_PUBLISH)
            .dataQualityStatus(ExperimentRunDataQualityStatus.VALID)
            .build();
    when(slots.findFirstBySourceExperimentIdOrderByUpdatedAtDesc(92L))
        .thenReturn(Optional.of(slot));
    when(runs.findTopByExperimentIdAndModeOrderByRunNumberDesc(92L, ExperimentRunMode.PRODUCTION))
        .thenReturn(Optional.of(run));
    when(gates.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(11L))
        .thenReturn(approvedGates(URL, VERSION));
  }

  /** Libera somente a publicação exata com os quatro gates operacionais auditados. */
  @Test
  void recognizesPublishedVersionApprovedByCurrentProductionRun() {
    assertThat(service.isReady(experiment)).isTrue();
  }

  /** Preserva a prontidão publicada enquanto a Meta ainda não confirmou a primeira impressão. */
  @Test
  void recognizesPublishedVersionAwaitingFirstExposure() {
    run.setStatus(ExperimentRunStatus.PUBLISHED_AWAITING_EXPOSURE);

    assertThat(service.isReady(experiment)).isTrue();
  }

  /** Impede que uma candidata apenas homologada seja tratada como publicação comercial. */
  @Test
  void rejectsReadySlotWithoutPublication() {
    slot.setStatus(PdeProductionSlotStatus.READY);
    slot.setPublishedAt(null);
    slot.setPublishedExperienceJson(null);

    assertThat(service.isReady(experiment)).isFalse();
  }

  /** Impede reutilizar evidência visual de outra versão no mesmo experimento. */
  @Test
  void rejectsLandingEvidenceFromAnotherVersion() {
    when(gates.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(11L))
        .thenReturn(approvedGates("https://v7.clubemusa.com.br", "musa-pde-entry-v7"));

    assertThat(service.isReady(experiment)).isFalse();
  }

  /** Impede reaproveitar um preflight antigo depois que a tentativa produtiva atual falhou. */
  @Test
  void rejectsLatestProductionRunWithoutApproval() {
    run.setStatus(ExperimentRunStatus.FAILED);

    assertThat(service.isReady(experiment)).isFalse();
  }

  /** Impede liberar campanha quando um gate obrigatório perdeu aprovação auditável. */
  @Test
  void rejectsMissingAuditedMetaGate() {
    List<ExperimentRunGateResult> incomplete =
        approvedGates(URL, VERSION).stream()
            .filter(
                gate ->
                    !ExperimentRunGateCodes.META_EFFECTIVE_STATUS_CONFIRMED.equals(
                        gate.getGateCode()))
            .toList();
    when(gates.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(11L)).thenReturn(incomplete);

    assertThat(service.isReady(experiment)).isFalse();
  }

  /** Monta os gates mínimos aprovados e vinculados à mesma URL e versão. */
  private List<ExperimentRunGateResult> approvedGates(String url, String version) {
    return List.of(
        gate(
            ExperimentRunGateCodes.LANDING_QUALITY_REVIEW_APPROVED,
            "agent-task:460;" + url + ";version:" + version),
        gate(
            ExperimentRunGateCodes.CHECKOUT_AND_DELIVERY_CAN_BE_COMPLETED,
            "agent-task:461;checkout:owm6x"),
        gate(ExperimentRunGateCodes.META_EFFECTIVE_STATUS_CONFIRMED, "meta-graph:account-active"),
        gate(ExperimentRunGateCodes.DATA_FRESHNESS_VALID, "pde-funnel-events:v12"));
  }

  /** Cria um gate operacional aprovado com referência auditável. */
  private ExperimentRunGateResult gate(String code, String evidence) {
    return ExperimentRunGateResult.builder()
        .gateCode(code)
        .status(ExperimentRunGateStatus.PASS)
        .evidenceReference(evidence)
        .build();
  }
}
