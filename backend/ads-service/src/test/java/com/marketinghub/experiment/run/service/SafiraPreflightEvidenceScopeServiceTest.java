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
import com.marketinghub.repository.jpa.experiment.ExperimentRunGateResultRepository;
import com.marketinghub.safira.commercial.v1.service.SafiraCommercialContext;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar o vínculo imutável entre preflight e publicação Safira. */
class SafiraPreflightEvidenceScopeServiceTest {
  private final SafiraCommercialContext context = mock(SafiraCommercialContext.class);
  private final ExperimentRunGateResultRepository gates =
      mock(ExperimentRunGateResultRepository.class);
  private final SafiraPreflightEvidenceScopeService service =
      new SafiraPreflightEvidenceScopeService(context, gates);

  /** Aceita apenas slot, SHA-256 e fingerprint completos e grava a versão material no run. */
  @Test
  void bindsOnlyExactCurrentPublicationIdentity() throws Exception {
    var product = Product.builder().id(10L).build();
    var experiment = new Experiment();
    experiment.setId(301L);
    experiment.setProduct(product);
    var run = ExperimentRun.builder().id(701L).experiment(experiment).build();
    when(context.applies(product)).thenReturn(true);
    when(context.snapshot("experiment:301"))
        .thenReturn(
            (com.fasterxml.jackson.databind.node.ObjectNode)
                new ObjectMapper()
                    .readTree(
                        "{\"slotId\":901,\"experienceHash\":\""
                            + "a".repeat(64)
                            + "\",\"fingerprint\":\""
                            + "b".repeat(64)
                            + "\"}"));
    String reference = service.requiredReference(run);

    assertThat(reference)
        .isEqualTo(
            "slot:901;experience-sha256:"
                + "a".repeat(64)
                + ";safira-fingerprint:"
                + "b".repeat(64));
    service.validateAndBind(
        run,
        new GateEvidence(
            ExperimentRunGateCodes.LANDING_QUALITY_REVIEW_APPROVED,
            ExperimentRunGateStatus.PASS,
            "Experiência conferida",
            reference));
    assertThat(run.getAssetBundleVersion()).isEqualTo(901);

    assertThatThrownBy(
            () ->
                service.validateAndBind(
                    run,
                    new GateEvidence(
                        ExperimentRunGateCodes.LANDING_QUALITY_REVIEW_APPROVED,
                        ExperimentRunGateStatus.PASS,
                        "Outra experiência",
                        "slot:901;experience-sha256:" + "a".repeat(64))))
        .hasMessageContaining("safira-fingerprint");
  }

  /** Detecta quando um run aprovado contém prova de outra versão comercial. */
  @Test
  void rejectsStaleGateEvidence() throws Exception {
    var product = Product.builder().id(20L).build();
    var experiment = new Experiment();
    experiment.setId(401L);
    experiment.setProduct(product);
    var run = ExperimentRun.builder().id(702L).experiment(experiment).build();
    when(context.applies(product)).thenReturn(true);
    when(context.snapshot("experiment:401"))
        .thenReturn(
            (com.fasterxml.jackson.databind.node.ObjectNode)
                new ObjectMapper()
                    .readTree(
                        "{\"slotId\":902,\"experienceHash\":\""
                            + "c".repeat(64)
                            + "\",\"fingerprint\":\""
                            + "d".repeat(64)
                            + "\"}"));
    var stale = new ExperimentRunGateResult();
    stale.setGateCode(ExperimentRunGateCodes.LANDING_QUALITY_REVIEW_APPROVED);
    stale.setStatus(ExperimentRunGateStatus.PASS);
    stale.setEvidenceReference(
        "slot:901;experience-sha256:" + "c".repeat(64) + ";safira-fingerprint:" + "d".repeat(64));
    when(gates.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(702L))
        .thenReturn(List.of(stale));

    assertThat(service.hasCurrentEvidence(run)).isFalse();
  }
}
