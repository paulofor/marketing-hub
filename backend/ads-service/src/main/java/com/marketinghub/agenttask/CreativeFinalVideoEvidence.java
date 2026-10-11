package com.marketinghub.agenttask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import java.util.HashSet;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Vincula a revisão de Psique aos bytes finais dos vídeos da mesma versão do produto. */
@Component
@RequiredArgsConstructor
@Slf4j
public class CreativeFinalVideoEvidence {
  private final ExperimentVideoAssetRepository videos;
  private final VideoProjectRepository projects;
  private final AgentTaskRepository tasks;
  private final ObjectMapper json;

  /** Acrescenta as mídias finais sem substituir o contexto ou as peças estáticas da tarefa. */
  public AgentTaskTargetResponse project(AgentTask task, AgentTaskTargetResponse target) {
    if (!supports(task) || target == null || target.experimentId() == null) return target;
    ObjectNode context =
        target.pdeContext() instanceof ObjectNode node ? node.deepCopy() : json.createObjectNode();
    context.set("finalVideoReview", inputs(target));
    return new AgentTaskTargetResponse(
        target.sourceReference(),
        target.experimentId(),
        target.productId(),
        target.productSlug(),
        target.productName(),
        target.productInternalName(),
        target.experienceVersion(),
        target.publicUrl(),
        target.commercialCheckoutProvider(),
        target.commercialCheckoutReference(),
        target.commercialCheckoutUrl(),
        target.unitPriceBrl(),
        context);
  }

  /**
   * Resolve os vídeos atuais e suas fontes de entrega pela identidade exata, sem herdar pareceres.
   */
  public ArrayNode inputs(AgentTaskTargetResponse target) {
    ArrayNode inputs = json.createArrayNode();
    if (target == null || target.experimentId() == null || target.productId() == null)
      return inputs;
    var roles = new HashSet<String>();
    for (var video :
        videos.findReadyExperimentVideosForReview(
            ExperimentVideoStatus.READY, null, target.productId(), target.experimentId())) {
      var job = video.getSalesVideoJob();
      if (job == null || video.getSlot() == null || roles.contains(video.getSlot().name()))
        continue;
      JsonNode metadata = read(job.getMetadataJson(), video.getId());
      var project = projects.findById(metadata.path("videoProjectId").asLong()).orElse(null);
      if (project == null
          || !Objects.equals(project.getCampaignKey(), target.experienceVersion())
          || !Objects.equals(project.getTenantId(), job.getTenantId())
          || !Objects.equals(project.getProductId(), target.productId())
          || !Objects.equals(project.getExperimentId(), target.experimentId())) continue;
      JsonNode result = read(video.getResponseJson(), video.getId());
      String sha = result.at("/hls_delivery/sourceSha256").asText();
      if (!sha.matches("[a-f0-9]{64}") || video.getAssetUrl() == null) continue;
      roles.add(video.getSlot().name());
      var input = inputs.addObject();
      input
          .put("assetId", video.getId())
          .put("sha256", sha)
          .put("url", video.getAssetUrl())
          .put("role", video.getSlot().name())
          .put("durationSeconds", video.getDurationSeconds())
          .put(
              "narration",
              result.path("voice_over_script").asText(result.at("/captions/text").asText()))
          .put("rejectionReason", Objects.toString(video.getRejectionReason(), ""))
          .set("reviewMoments", reviewMoments(result, video.getDurationSeconds()));
      var examples = metadata.at("/post_production/deliveryExamples");
      if ("APPROVED_DELIVERY_EXAMPLES_V1".equals(examples.path("contractVersion").asText()))
        input.set("deliveryExamples", examples.deepCopy());
    }
    return inputs;
  }

  /** Inclui cada trecho narrado e a prova curta para não deixá-los fora da amostragem visual. */
  private ArrayNode reviewMoments(JsonNode result, Integer duration) {
    if (duration == null || duration <= 0) return json.createArrayNode();
    var moments = new java.util.TreeSet<Double>();
    double cursor = 0;
    var segments = result.at("/caption_narration_sync/segment_durations_seconds");
    if (segments.isArray() && segments.size() <= 12) {
      for (var segment : segments) {
        double seconds = segment.asDouble(-1);
        if (!Double.isFinite(seconds) || seconds <= 0 || cursor + seconds > duration + .1) break;
        moments.add(cursor + seconds / 2);
        cursor += seconds;
      }
    }
    var proof = result.at("/product_reference_overlay/reviewMoments");
    if (proof.isArray() && proof.size() <= 12) {
      for (var value : proof) {
        double second = value.asDouble(-1);
        if (Double.isFinite(second) && second > 0 && second < duration) moments.add(second);
      }
    }
    var output = json.createArrayNode();
    moments.stream().limit(24).forEach(output::add);
    return output;
  }

  /** Informa se uma edição final nova exige reabrir a atividade de revisão já existente. */
  public boolean needsReview(AgentTaskTargetResponse target) {
    for (var input : inputs(target)) {
      var video = videos.findById(input.path("assetId").asLong()).orElse(null);
      if (video != null && approvalBlockReason(video) != null) return true;
    }
    return false;
  }

  /** Autoriza somente frames de mídias da própria tarefa, com identidade na chave da evidência. */
  public boolean permitsFrame(
      AgentTask task, AgentTaskTargetResponse target, String url, String key) {
    var projected = project(task, target);
    if (projected == null || projected.pdeContext() == null) return false;
    for (var input : projected.pdeContext().path("finalVideoReview")) {
      if (url.equals(input.path("url").asText())
          && key.matches(
              "video-"
                  + input.path("assetId").asLong()
                  + "-"
                  + input.path("sha256").asText()
                  + "-[0-9]+")) return true;
    }
    return false;
  }

  /** Exige parecer independente do hash atual para novas produções regidas por este contrato. */
  public String approvalBlockReason(ExperimentVideoAsset video) {
    JsonNode result = read(video.getResponseJson(), video.getId());
    if (!"CUSTOMER_VIDEO_QUALITY_V1".equals(result.path("customerQualityContract").asText()))
      return null;
    String sha = result.at("/hls_delivery/sourceSha256").asText();
    if (!sha.matches("[a-f0-9]{64}"))
      return "O vídeo final ainda não tem hash verificável para Psique.";
    var latest =
        tasks
            .findTopByAssignedAgentAgentKeyAndSourceReferenceOrderByUpdatedAtDescIdDesc(
                "customer-agent", "experiment:" + video.getExperiment().getId())
            .orElse(null);
    if (latest != null && "COMPLETED".equals(latest.getStatus()) && supports(latest)) {
      JsonNode review = read(latest.getResultJson(), video.getId());
      if ("APPROVED".equals(review.path("decision").asText())) {
        for (var audit : review.path("videoAudit")) {
          if (audit.path("assetId").asLong() == video.getId()
              && sha.equals(audit.path("sha256").asText())
              && !audit.path("criticalDeficit").asBoolean(true)) return null;
        }
      }
    }
    return "Psique precisa revisar o vídeo final, sua clareza e progressão visual antes da seleção de uso. Abra a atividade de revisão da comunicação.";
  }

  /** Restringe o contrato à revisão criativa independente, nunca à produção do próprio ativo. */
  private boolean supports(AgentTask task) {
    return task != null
        && task.getProcessDefinition() != null
        && "customer-agent".equals(task.getAssignedAgent().getAgentKey())
        && "creative-production-approval".equals(task.getProcessDefinition().getProcessCode())
        && "customer".equals(task.getProcessActivityId());
  }

  /** Mantém JSON inválido sem evidência e registra contexto suficiente para diagnóstico. */
  private JsonNode read(String value, Long videoId) {
    try {
      JsonNode parsed = json.readTree(value == null || value.isBlank() ? "{}" : value);
      return parsed != null && parsed.isObject() ? parsed : json.createObjectNode();
    } catch (Exception ex) {
      log.error("Contrato audiovisual inválido na revisão criativa. videoId={}", videoId, ex);
      return json.createObjectNode();
    }
  }
}
