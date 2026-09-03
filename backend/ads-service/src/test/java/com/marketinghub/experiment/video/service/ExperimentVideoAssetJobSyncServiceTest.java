package com.marketinghub.experiment.video.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.media.Asset;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoJobType;
import com.marketinghub.salesvideo.SalesVideoProfile;
import com.marketinghub.salesvideo.SalesVideoProviderFamily;
import com.marketinghub.salesvideo.SalesVideoStatus;
import com.marketinghub.salesvideo.VideoProductionCycle;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.salesvideo.VideoProjectStatus;
import com.marketinghub.salesvideo.dto.JobCompletionRequest;
import com.marketinghub.salesvideo.service.SalesVideoProductionCostCalculator;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida o retorno automático do vídeo final do Estúdio ao experimento comercial de origem. */
@ExtendWith(MockitoExtension.class)
class ExperimentVideoAssetJobSyncServiceTest {
  @Mock private ExperimentVideoAssetRepository repository;
  @Mock private SalesVideoProductionCostCalculator costCalculator;
  @Mock private ExperimentRepository experimentRepository;
  @Mock private VideoProductionCycleRepository cycleRepository;
  @Mock private VideoProjectRepository projectRepository;

  /** Vincula o acabamento ao experimento e move ciclo e projeto para revisão sem autoaprovação. */
  @Test
  void shouldCreateExperimentAssetFromGovernedPostProductionLineage() {
    VideoProductionCycle cycle = new VideoProductionCycle();
    cycle.setId(7L);
    cycle.setVideoProjectId(3L);
    cycle.setExperimentId(91L);
    cycle.setStatus("APOLLO_BLOCKED");
    VideoProject project =
        VideoProject.builder()
            .id(3L)
            .tenantId("default")
            .experimentId(91L)
            .targetChannel("INSTAGRAM_REELS_STORIES")
            .format("VERTICAL_9_16")
            .title("Vega #91")
            .objective("Transformar reconhecimento no espelho em clique qualificado.")
            .primaryMetric("VIDEO_75;CTA_CLICK;PURCHASE")
            .scriptText("Você se arruma e ainda sente que falta presença.")
            .scenePlan("DOR -> RESULTADO -> MECANISMO -> CTA")
            .status(VideoProjectStatus.IN_PRODUCTION)
            .build();
    SalesVideoProfile profile = SalesVideoProfile.builder().id(57L).tenantId("default").build();
    SalesVideoJob source =
        SalesVideoJob.builder()
            .id(21225L)
            .tenantId("default")
            .metadataJson("{\"videoProductionCycleId\":7,\"videoProjectId\":3,\"experimentId\":91}")
            .build();
    Asset video = Asset.builder().id(2769L).url("https://cdn.test/vega-91.mp4").build();
    Asset poster = Asset.builder().id(2770L).url("https://cdn.test/vega-91.png").build();
    SalesVideoJob finalJob =
        SalesVideoJob.builder()
            .id(21231L)
            .tenantId("default")
            .profile(profile)
            .retryOfJob(source)
            .jobType(SalesVideoJobType.POST_PRODUCTION)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .providerName("MUSA_POST_PRODUCTION")
            .status(SalesVideoStatus.VIDEO_READY)
            .asset(video)
            .posterAsset(poster)
            .auditSnapshotJson("{\"sourceJobId\":21225}")
            .build();
    JobCompletionRequest request = new JobCompletionRequest();
    request.setMetadataJson(
        "{\"has_audio\":true,\"audio_streams\":1,"
            + "\"audio\":{\"review\":{\"model\":\"gpt-4o-mini-tts\"}}}");
    request.setCostUsd(new BigDecimal("0.08"));

    given(repository.findBySalesVideoJobId(21231L)).willReturn(List.of());
    given(cycleRepository.findById(7L)).willReturn(Optional.of(cycle));
    given(projectRepository.findById(3L)).willReturn(Optional.of(project));
    given(experimentRepository.findById(91L))
        .willReturn(Optional.of(Experiment.builder().id(91L).name("MUSA-H003-E002").build()));

    ExperimentVideoAssetJobSyncService service =
        new ExperimentVideoAssetJobSyncService(
            repository, costCalculator, experimentRepository, cycleRepository, projectRepository);
    service.syncCompletedRender(finalJob, request, 24, "720x1280");

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<ExperimentVideoAsset>> captor = ArgumentCaptor.forClass(List.class);
    verify(repository).saveAll(captor.capture());
    ExperimentVideoAsset created = captor.getValue().get(0);
    assertThat(created.getExperiment().getId()).isEqualTo(91L);
    assertThat(created.getSlot()).isEqualTo(ExperimentVideoSlot.AD);
    assertThat(created.getReviewStatus()).isEqualTo(ExperimentVideoReviewStatus.PENDING);
    assertThat(created.isRequiredForRelease()).isTrue();
    assertThat(created.getHasAudio()).isTrue();
    assertThat(created.getAssetUrl()).isEqualTo("https://cdn.test/vega-91.mp4");
    assertThat(created.getAspectRatio()).isEqualTo("9:16");
    assertThat(created.getSalesVideoJob().getId()).isEqualTo(21231L);
    assertThat(cycle.getSalesVideoJobId()).isEqualTo(21231L);
    assertThat(cycle.getStatus()).isEqualTo("VIDEO_READY_FOR_REVIEW");
    assertThat(project.getStatus()).isEqualTo(VideoProjectStatus.READY_FOR_REVIEW);
    verify(cycleRepository).save(cycle);
    verify(projectRepository).save(project);
  }
}
