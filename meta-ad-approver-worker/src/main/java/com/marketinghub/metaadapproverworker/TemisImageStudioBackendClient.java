package com.marketinghub.metaadapproverworker;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/** Responsabilidade: consumir e concluir filas visuais de Dédalo exclusivamente pelo backend. */
@Component
public class TemisImageStudioBackendClient {
  private static final Logger log = LoggerFactory.getLogger(TemisImageStudioBackendClient.class);
  private final RestClient client;

  /** Inicializa a porta com timeout próprio para persistir artefatos visuais grandes. */
  public TemisImageStudioBackendClient(MetaAdApproverProperties properties) {
    client =
        BackendRestClientFactory.create(properties, properties.getImageStudioBackendReadTimeout());
  }

  /** Reserva produções de imagem pelo endpoint pending canônico. */
  public List<TemisImageStudioJob> claimPending(int limit) {
    List<Map<String, Object>> values =
        client
            .get()
            .uri(
                "/api/internal/planning/image-studio/v1/stage-executions/pending?limit={limit}",
                limit)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    return values == null ? List.of() : values.stream().map(TemisImageStudioJob::from).toList();
  }

  /** Reserva revisões independentes depois que a produção já foi persistida. */
  public List<TemisLibraryReviewJob> claimReviews(int limit) {
    List<Map<String, Object>> values =
        client
            .get()
            .uri("/api/internal/planning/image-studio/v1/reviews/pending?limit={limit}", limit)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    return values == null ? List.of() : values.stream().map(TemisLibraryReviewJob::from).toList();
  }

  /** Persiste o parecer de uma execução que não produziu o arquivo avaliado. */
  public void reportReview(Long assetId, Map<String, Object> result) {
    client
        .post()
        .uri("/api/internal/planning/image-studio/v1/reviews/{assetId}/result", assetId)
        .body(result)
        .retrieve()
        .toBodilessEntity();
  }

  /** Envia o arquivo final e a auditoria integral ao backend. */
  public void complete(TemisImageStudioJob job, TemisImageStudioOpenAiClient.Result result) {
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("producerExecutionId", job.producerExecutionId());
    body.add("model", result.model());
    body.add("requestJson", result.requestJson());
    body.add("responseJson", result.responseJson());
    if (result.usageJson() != null) {
      body.add("usageJson", result.usageJson());
    }
    if (result.costUsd() != null) {
      body.add("costUsd", result.costUsd());
    }
    body.add(
        "file",
        new ByteArrayResource(result.imageBytes()) {
          @Override
          public String getFilename() {
            return "dedalo-image-studio-" + job.jobId() + ".png";
          }
        });
    client
        .post()
        .uri(
            "/api/internal/planning/image-studio/v1/stage-executions/{jobId}/artifact", job.jobId())
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(body)
        .retrieve()
        .toBodilessEntity();
  }

  /** Persiste falha com correlação da reserva e sem criar mídia parcial. */
  public void fail(
      TemisImageStudioJob job, RuntimeException ex, String requestJson, String responseJson) {
    log.error(
        "Falha no recurso de imagens de Dédalo. jobId={} commercialPlanId={}",
        job.jobId(),
        job.commercialPlanId(),
        ex);
    client
        .post()
        .uri("/api/internal/planning/image-studio/v1/stage-executions/{jobId}/failure", job.jobId())
        .body(
            Map.of(
                "producerExecutionId",
                job.producerExecutionId(),
                "error",
                rootMessage(ex),
                "requestJson",
                requestJson == null ? "" : requestJson,
                "responseJson",
                responseJson == null ? "" : responseJson))
        .retrieve()
        .toBodilessEntity();
  }

  /** Extrai a causa específica já preservada no log completo. */
  private String rootMessage(Throwable error) {
    Throwable current = error;
    while (current.getCause() != null) {
      current = current.getCause();
    }
    return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
  }
}
