package com.marketinghub.salesvideo.service.reconcileNarrationCosts;

import static com.marketinghub.salesvideo.service.reconcileNarrationCosts.NarrationUsageEvidence.require;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.salesvideo.*;
import java.util.*;

/** Reconhece recusas explícitas anteriores à inferência e ao aceite de tarefas de vídeo. */
final class PreGenerationCostEvidence {
  static final String POLICY = "PLANNING_REJECTED_BEFORE_GENERATION_AUDITED";

  /** Impede instâncias do validador sem estado. */
  private PreGenerationCostEvidence() {}

  /** Exige respostas Flex recusadas para todas as tentativas, sem envio incerto ou resultado. */
  static boolean withoutGeneration(SalesVideoJob job, ObjectMapper json) {
    if (job.getJobType() != SalesVideoJobType.RENDER) return false;
    require(
        job.getStatus() == SalesVideoStatus.VIDEO_FAILED
            && Set.of("APOLLO_PLANNING_HTTP_ERROR", "APOLLO_PLANNING_UNAVAILABLE")
                .contains(Objects.toString(job.getFailureCode(), ""))
            && (job.getProviderJobId() == null || job.getProviderJobId().isBlank())
            && job.getAsset() == null,
        "A falha não comprova interrupção anterior à geração.");
    var requested = new HashMap<Integer, JsonNode>();
    var rejected = new HashMap<Integer, JsonNode>();
    for (var event : job.getEvents()) {
      String details = event.getDetailsJson();
      if (details == null
          || !details.stripLeading().startsWith("{")
          || !details.contains("\"eventType\"")
          || !details.contains("APOLLO_PLANNING_HTTP")) continue;
      var item = NarrationUsageEvidence.read(details, job.getId(), json);
      int attempt = item.path("attempt").asInt();
      var request = item.path("request");
      require(
          "APOLLO_PLANNING_HTTP".equals(item.path("eventType").asText())
              && item.path("jobId").asLong() == job.getId()
              && "https://api.openai.com/v1/responses".equals(item.path("endpoint").asText())
              && attempt > 0
              && attempt <= 3
              && request.isObject()
              && !item.path("model").asText().isBlank()
              && item.path("model").asText().equals(request.path("model").asText())
              && "flex".equals(item.path("serviceTier").asText())
              && "flex".equals(request.path("service_tier").asText()),
          "A auditoria do planejamento está incompleta ou pertence a outra tentativa.");
      String status = item.path("status").asText();
      if ("REQUESTED".equals(status)) {
        require(requested.put(attempt, request) == null, "Envio de planejamento duplicado.");
      } else {
        require(
            Set.of("REJECTED", "RETRY_WAIT").contains(status)
                && item.path("httpStatus").asInt() == 429
                && !item.path("requestId").asText().isBlank(),
            "Resposta aceita ou envio incerto não autoriza custo zero.");
        var response =
            NarrationUsageEvidence.read(item.path("rawResponse").asText(), job.getId(), json);
        require(
            "flex_unavailable".equals(response.at("/error/code").asText())
                && !response.has("usage")
                && !response.has("output")
                && !response.has("id"),
            "A resposta não comprova recusa anterior à inferência.");
        if ("REJECTED".equals(status))
          require(rejected.put(attempt, request) == null, "Recusa de planejamento duplicada.");
      }
    }
    require(
        !requested.isEmpty() && requested.equals(rejected),
        "Há envio sem resposta recusada comprovada; preserve o custo desconhecido.");
    return true;
  }
}
