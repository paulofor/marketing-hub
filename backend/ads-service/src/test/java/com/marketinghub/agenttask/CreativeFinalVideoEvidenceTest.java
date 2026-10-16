package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.video.*;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.VideoProject;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Protege a identidade da revisão final e impede que um parecer estático libere outra mídia. */
class CreativeFinalVideoEvidenceTest {
  private final ObjectMapper json = new ObjectMapper();
  private final ExperimentVideoAssetRepository videos = mock(ExperimentVideoAssetRepository.class);
  private final VideoProjectRepository projects = mock(VideoProjectRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final CreativeFinalVideoEvidence service =
      new CreativeFinalVideoEvidence(videos, projects, tasks, json);

  /**
   * Preserva limites medidos e rejeita linhas do tempo parciais, sem inventar música ou duração.
   */
  @Test
  void exposesOnlyCompleteMeasuredNarration() throws Exception {
    var result =
        json.readTree(
            """
        {"audio":{"mix":"VOICE_ONLY","music":"none"},
         "captions":{"text":"Abertura | Benefício | Convite"},
         "caption_narration_sync":{"timing_status":"APPROVED",
           "timing_method":"SEGMENT_AUDIO_DURATION","segment_durations_seconds":[6.312,3.408,6.048],
           "narration_duration_seconds":15.768}}
        """);
    com.fasterxml.jackson.databind.JsonNode evidence =
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
            service, "audioProductionEvidence", result, 20);
    assertThat(evidence.path("mix").asText()).isEqualTo("VOICE_ONLY");
    assertThat(evidence.at("/segments/1/startSeconds").asDouble()).isEqualTo(6.312);
    assertThat(evidence.at("/segments/2/endSeconds").asDouble()).isCloseTo(15.768, within(.0001));
    assertThat(evidence.at("/segments/2/expectedText").asText()).isEqualTo("Convite");
    ((com.fasterxml.jackson.databind.node.ObjectNode) result.path("captions"))
        .put("text", "Uma frase");
    evidence =
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
            service, "audioProductionEvidence", result, 20);
    assertThat(evidence.has("segments")).isFalse();
  }

  /** Montagens combinadas preservam momentos das duas fontes dentro do limite de amostragem. */
  @Test
  void preservesHybridReviewMoments() {
    var result = json.createObjectNode();
    var times = result.putObject("product_reference_overlay").putArray("reviewMoments");
    for (int i = 1; i <= 17; i++) times.add(i);
    com.fasterxml.jackson.databind.JsonNode resolved =
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
            service, "reviewMoments", result, 30);
    assertThat(resolved).hasSize(17);
    assertThat(resolved.get(16).asDouble()).isEqualTo(17);
  }

  /** Mira e um produto distinto recebem somente vídeo pertencente à sua própria versão. */
  @ParameterizedTest
  @ValueSource(longs = {10, 93010})
  void projectsExactVersionAndRejectsOtherTenant(long productId) {
    var video = video(53L);
    video.setResponseJson(
        video
            .getResponseJson()
            .replace(
                "\"customerQualityContract\"",
                "\"caption_narration_sync\":{\"segment_durations_seconds\":[4,3,4]},"
                    + "\"product_reference_overlay\":{\"reviewMoments\":[8.5]},\"customerQualityContract\""));
    var job = new SalesVideoJob();
    job.setMetadataJson(
        "{\"videoProjectId\":13,\"post_production\":{\"deliveryExamples\":{\"contractVersion\":\"APPROVED_DELIVERY_EXAMPLES_V1\",\"runtimeCaptureClaimed\":false,\"shots\":[{\"visualAssetId\":91001,\"rightsStatement\":\"Fixture local\"}]}}}");
    job.setTenantId("tenant-a");
    video.setSalesVideoJob(job);
    var project = new VideoProject();
    project.setId(13L);
    project.setTenantId("tenant-a");
    project.setProductId(productId);
    project.setExperimentId(102L);
    project.setCampaignKey("candidate-v3");
    when(projects.findById(13L)).thenReturn(Optional.of(project));
    when(videos.findReadyExperimentVideosForReview(
            ExperimentVideoStatus.READY, null, productId, 102L))
        .thenReturn(List.of(video));
    when(videos.findById(53L)).thenReturn(Optional.of(video));
    var target = target(productId);
    assertThat(service.needsReview(target)).isTrue();
    var projected = service.project(task(), target);
    assertThat(projected.pdeContext().path("finalVideoReview")).hasSize(1);
    assertThat(
            projected
                .pdeContext()
                .at("/finalVideoReview/0/deliveryExamples/runtimeCaptureClaimed")
                .asBoolean(true))
        .isFalse();
    assertThat(
            projected
                .pdeContext()
                .at("/finalVideoReview/0/deliveryExamples/shots/0/rightsStatement")
                .asText())
        .isEqualTo("Fixture local");
    assertThat(projected.pdeContext().at("/finalVideoReview/0/reviewMoments").toString())
        .isEqualTo("[2.0,5.5,8.5,9.0]");
    assertThat(
            service.permitsFrame(
                task(), target, video.getAssetUrl(), "video-53-" + "a".repeat(64) + "-1000"))
        .isTrue();
    assertThat(
            service.permitsFrame(
                task(),
                target,
                "https://other.invalid/53.mp4",
                "video-53-" + "a".repeat(64) + "-1000"))
        .isFalse();
    project.setTenantId("tenant-b");
    assertThat(service.project(task(), target).pdeContext().path("finalVideoReview")).isEmpty();
  }

  /** Parecer antigo, arquivo alterado e produção própria não substituem a revisão independente. */
  @Test
  void requiresIndependentReviewForExactHash() {
    var video = video(53L);
    var review = task();
    review.setStatus("COMPLETED");
    when(tasks.findTopByAssignedAgentAgentKeyAndSourceReferenceOrderByUpdatedAtDescIdDesc(
            "customer-agent", "experiment:102"))
        .thenReturn(Optional.of(review));
    review.setResultJson("{\"decision\":\"APPROVED\",\"renderedAssetAudit\":[]}");
    assertThat(service.approvalBlockReason(video)).contains("vídeo final");
    review.setResultJson(
        "{\"decision\":\"APPROVED\",\"videoAudit\":[{\"assetId\":53,\"sha256\":\""
            + "a".repeat(64)
            + "\",\"criticalDeficit\":false}]}");
    assertThat(service.approvalBlockReason(video)).isNull();
    video.setResponseJson(video.getResponseJson().replace("a".repeat(64), "b".repeat(64)));
    assertThat(service.approvalBlockReason(video)).isNotNull();
    video.setResponseJson("{}");
    assertThat(service.approvalBlockReason(video)).isNull();
  }

  /** Monta mídia sintética com identidade e duração, sem autorização comercial. */
  private ExperimentVideoAsset video(Long id) {
    var video = new ExperimentVideoAsset();
    video.setId(id);
    video.setSlot(ExperimentVideoSlot.AD);
    video.setDurationSeconds(20);
    video.setAssetUrl("https://fixtures.invalid/53.mp4");
    var experiment = new Experiment();
    experiment.setId(102L);
    video.setExperiment(experiment);
    video.setResponseJson(
        "{\"customerQualityContract\":\"CUSTOMER_VIDEO_QUALITY_V1\",\"hls_delivery\":{\"sourceSha256\":\""
            + "a".repeat(64)
            + "\"}}");
    return video;
  }

  /** Identifica a atividade existente de revisão criativa de Psique. */
  private AgentTask task() {
    var task = new AgentTask();
    var agent = mock(com.marketinghub.agent.Agent.class);
    when(agent.getAgentKey()).thenReturn("customer-agent");
    task.setAssignedAgent(agent);
    var process = new BusinessProcessDefinition();
    process.setProcessCode("creative-production-approval");
    task.setProcessDefinition(process);
    task.setProcessActivityId("customer");
    return task;
  }

  /** Preserva o contexto do produto e sua versão no contrato entregue ao executor. */
  private AgentTaskTargetResponse target(long productId) {
    return new AgentTaskTargetResponse(
        "experiment:102",
        102L,
        productId,
        "fixture",
        "Produto",
        "Produto",
        "candidate-v3",
        "https://fixtures.invalid/",
        null,
        null,
        null,
        null,
        json.createObjectNode());
  }
}
