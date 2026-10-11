package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.planning.CommercialPlanVisualAsset;
import com.marketinghub.planning.CommercialPlanVisualAssetStatus;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioJob;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioStatus;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanVisualAssetReviewStatus;
import com.marketinghub.repository.jpa.planning.CommercialPlanImageStudioJobRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanVisualAssetRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Confirma proveniência legada sem transformar disponibilidade de arquivo em aprovação. */
class PlanningVideoDeliveryExampleSourceTest {
  /** Recupera hash auditado e bloqueia ausência de direitos, reprovação ou recibo inexistente. */
  @Test
  void resolvesHistoricalAuditWithoutInventingApproval() {
    var plans = mock(CommercialPlanRepository.class);
    var assets = mock(CommercialPlanVisualAssetRepository.class);
    var jobs = mock(CommercialPlanImageStudioJobRepository.class);
    when(plans.findIdsByProductId(9007L)).thenReturn(List.of(9002L));
    var asset = new CommercialPlanVisualAsset();
    asset.setId(9133L);
    asset.setMediaType("IMAGE");
    asset.setPurpose("DELIVERY");
    asset.setLabel("Amostra");
    asset.setOrigin("Gerador auditado");
    asset.setRightsStatement("Uso em teste");
    asset.setAssetUrl("https://example.invalid/source.png");
    asset.setAgentReviewStatus(CommercialPlanVisualAssetReviewStatus.APPROVED);
    when(assets.findByCommercialPlanIdAndStatusOrderByCreatedAtAsc(
            9002L, CommercialPlanVisualAssetStatus.APPROVED))
        .thenReturn(List.of(asset));
    var job = new CommercialPlanImageStudioJob();
    job.setId(909L);
    job.setStatus(CommercialPlanImageStudioStatus.COMPLETED);
    job.setResponseJson("{\"data\":[{\"image_sha256\":\"" + "a".repeat(64) + "\"}]}");
    when(jobs.findByResultVisualAssetId(9133L)).thenReturn(Optional.of(job));
    var source = new PlanningVideoDeliveryExampleSource(plans, assets, jobs, new ObjectMapper());
    assertThat(source.approved(9007L))
        .singleElement()
        .satisfies(value -> assertThat(value.sha256()).isEqualTo("a".repeat(64)));
    asset.setRightsStatement("");
    assertThat(source.approved(9007L)).isEmpty();
    asset.setRightsStatement("Uso em teste");
    asset.setAgentReviewStatus(null);
    assertThat(source.approved(9007L)).isEmpty();
    asset.setAgentReviewStatus(CommercialPlanVisualAssetReviewStatus.APPROVED);
    job.setResponseJson("{}");
    assertThat(source.approved(9007L)).isEmpty();
    asset.setContentSha256("b".repeat(64));
    assertThat(source.approved(9007L)).hasSize(1);
    assertThat(source.approved(9008L)).isEmpty();
  }
}
