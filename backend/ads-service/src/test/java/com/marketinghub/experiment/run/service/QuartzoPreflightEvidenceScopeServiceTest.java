package com.marketinghub.experiment.run.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.run.ExperimentRun;
import com.marketinghub.experiment.run.ExperimentRunGateCodes;
import com.marketinghub.experiment.run.ExperimentRunGateResult;
import com.marketinghub.experiment.run.ExperimentRunGateStatus;
import com.marketinghub.experiment.run.service.homologation.ExperimentRunHomologationRequest.GateEvidence;
import com.marketinghub.product.Product;
import com.marketinghub.quartzo.commercial.v1.service.QuartzoCommercialContext;
import com.marketinghub.repository.jpa.experiment.ExperimentRunGateResultRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir que o preflight Quartzo reutilize outra publicação comercial. */
class QuartzoPreflightEvidenceScopeServiceTest {
  private static final long PRODUCT_ID = 91001L;
  private static final long EXPERIMENT_ID = 91002L;
  private static final long RUN_ID = 91003L;
  private static final long PUBLICATION_ID = 91004L;
  private static final String PAGE_HASH = "a".repeat(64);
  private static final String FINGERPRINT = "b".repeat(64);
  private final QuartzoCommercialContext context = mock(QuartzoCommercialContext.class);
  private final ExperimentRunGateResultRepository gates =
      mock(ExperimentRunGateResultRepository.class);
  private final QuartzoPreflightEvidenceScopeService service =
      new QuartzoPreflightEvidenceScopeService(context, gates);
  private final ExperimentRun run = run();

  /** Prepara uma fotografia imutável independente de produto real. */
  @BeforeEach
  void setUp() {
    when(context.applies(run.getExperiment().getProduct())).thenReturn(true);
    var snapshot = new ObjectMapper().createObjectNode();
    snapshot.put("publicationId", PUBLICATION_ID);
    snapshot.put("pageHash", PAGE_HASH);
    snapshot.put("fingerprint", FINGERPRINT);
    when(context.snapshot("experiment:" + EXPERIMENT_ID)).thenReturn(snapshot);
  }

  /** Aceita somente os três tokens exatos e registra a versão auditada no run. */
  @Test
  void validatesCurrentPublicationAndBindsAssetVersion() {
    service.validateAndBind(
        run,
        evidence(
            "publication:"
                + PUBLICATION_ID
                + ";page-sha256:"
                + PAGE_HASH
                + ";quartzo-fingerprint:"
                + FINGERPRINT
                + ";playwright:desktop,iphone15pro,pixel7"));

    assertThat(run.getAssetBundleVersion()).isEqualTo(Math.toIntExact(PUBLICATION_ID));
  }

  /** Recusa uma evidência antiga mesmo quando ela foi aprovada em outro run. */
  @Test
  void rejectsPreviouslyApprovedPublication() {
    assertThatThrownBy(
            () ->
                service.validateAndBind(
                    run,
                    evidence(
                        "publication:"
                            + (PUBLICATION_ID - 1)
                            + ";page-sha256:"
                            + "c".repeat(64)
                            + ";quartzo-fingerprint:"
                            + FINGERPRINT)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("publication:" + PUBLICATION_ID, PAGE_HASH, FINGERPRINT);
    assertThat(run.getAssetBundleVersion()).isNull();
  }

  /** Recusa token parcial para não aceitar hash ou impressão apenas por coincidência textual. */
  @Test
  void rejectsPartialScopeTokens() {
    assertThatThrownBy(
            () ->
                service.validateAndBind(
                    run,
                    evidence(
                        "publication:"
                            + PUBLICATION_ID
                            + ";page-sha256:"
                            + PAGE_HASH
                            + ";debug:quartzo-fingerprint:"
                            + FINGERPRINT)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** Confere a vigência usando o gate persistido, sem confiar apenas no estado RUNNING. */
  @Test
  void comparesPersistedGateWithCurrentScope() {
    ExperimentRunGateResult current =
        ExperimentRunGateResult.builder()
            .gateCode(ExperimentRunGateCodes.LANDING_QUALITY_REVIEW_APPROVED)
            .status(ExperimentRunGateStatus.PASS)
            .evidenceReference(
                "publication:"
                    + PUBLICATION_ID
                    + ";page-sha256:"
                    + PAGE_HASH
                    + ";quartzo-fingerprint:"
                    + FINGERPRINT)
            .build();
    when(gates.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(RUN_ID))
        .thenReturn(List.of(current));

    assertThat(service.hasCurrentEvidence(run)).isTrue();

    current.setEvidenceReference(
        "publication:"
            + (PUBLICATION_ID - 1)
            + ";page-sha256:"
            + PAGE_HASH
            + ";quartzo-fingerprint:"
            + FINGERPRINT);
    assertThat(service.hasCurrentEvidence(run)).isFalse();
  }

  /** Monta a evidência funcional do gate visual. */
  private GateEvidence evidence(String reference) {
    return new GateEvidence(
        ExperimentRunGateCodes.LANDING_QUALITY_REVIEW_APPROVED,
        ExperimentRunGateStatus.PASS,
        "Página atual conferida.",
        reference);
  }

  /** Monta uma tentativa produtiva do experimento Quartzo. */
  private ExperimentRun run() {
    Product product = Product.builder().id(PRODUCT_ID).build();
    Experiment experiment = Experiment.builder().id(EXPERIMENT_ID).product(product).build();
    return ExperimentRun.builder().id(RUN_ID).experiment(experiment).runNumber(1).build();
  }
}
