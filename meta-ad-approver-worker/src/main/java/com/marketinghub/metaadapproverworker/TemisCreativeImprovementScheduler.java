package com.marketinghub.metaadapproverworker;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/** Responsabilidade: materializar pelo recurso de Dédalo as correções visuais requeridas. */
@Component
@ConditionalOnProperty(name = "meta-ad-approver.execution-role", havingValue = "image-studio")
class TemisCreativeImprovementProcessor {
  private static final Logger log =
      LoggerFactory.getLogger(TemisCreativeImprovementProcessor.class);
  private final RestClient backend;
  private final TemisImageStudioOpenAiClient openAi;
  private final MetaAdApproverProperties properties;

  /** Inicializa o consumidor usando o backend como fila e autoridade de avanço. */
  TemisCreativeImprovementProcessor(
      MetaAdApproverProperties properties, TemisImageStudioOpenAiClient openAi) {
    this.backend =
        BackendRestClientFactory.create(properties, properties.getImageStudioBackendReadTimeout());
    this.openAi = openAi;
    this.properties = properties;
  }

  /** Consome correções após a produção da Biblioteca para evitar concorrência com a revisão. */
  public void processPending() {
    List<Map<String, Object>> pending =
        backend
            .get()
            .uri(
                "/api/internal/creatives/agent-improvement/stage-executions/pending?limit={limit}",
                properties.getImageStudioPendingLimit())
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    List<Map<String, Object>> jobs = pending == null ? List.of() : pending;
    jobs.forEach(this::process);
  }

  /** Edita o produto real, envia a nova versão e nunca aprova o próprio resultado. */
  private void process(Map<String, Object> correction) {
    Long creativeId = number(correction.get("creativeId"));
    try {
      TemisImageStudioJob job = toImageJob(correction, creativeId);
      TemisImageStudioOpenAiClient.Result result = openAi.execute(job);
      upload(creativeId, job.producerExecutionId(), result);
      log.info("Correção visual materializada pelo recurso de Dédalo. creativeId={}", creativeId);
    } catch (RuntimeException ex) {
      log.error(
          "Falha na correção visual materializada pelo recurso de Dédalo. creativeId={}",
          creativeId,
          ex);
      backend
          .post()
          .uri("/api/internal/creatives/{id}/agent-improvement/result", creativeId)
          .body(Map.of("error", rootMessage(ex)))
          .retrieve()
          .toBodilessEntity();
    }
  }

  /** Adapta o contrato do gate de criativo ao mesmo estúdio GPT Image 2. */
  private TemisImageStudioJob toImageJob(Map<String, Object> value, Long creativeId) {
    String prompt = text(value.get("revisedImagePrompt"));
    prompt += section("REQUISITOS OBRIGATÓRIOS", strings(value.get("mandatoryVisualRequirements")));
    prompt += section("ELEMENTOS PROIBIDOS", strings(value.get("forbiddenVisualElements")));
    prompt += section("CRITÉRIOS DE ACEITAÇÃO", strings(value.get("visualAcceptanceCriteria")));
    TemisVisualPlaybook playbook = TemisVisualPlaybook.from(value.get("visualPlaybook"));
    List<String> references = new java.util.ArrayList<>(strings(value.get("referenceImageUrls")));
    playbook.exampleUrls().stream()
        .filter(url -> !references.contains(url))
        .forEach(references::add);
    String format = text(value.get("format"));
    String size =
        format.toLowerCase(java.util.Locale.ROOT).contains("story")
                || format.toLowerCase(java.util.Locale.ROOT).contains("9:16")
            ? "1152x2048"
            : "2048x2048";
    return new TemisImageStudioJob(
        creativeId,
        number(value.get("experimentId")),
        references.isEmpty() ? "CREATE" : "EDIT",
        prompt,
        "Correção " + (format.isBlank() ? "" : format + " ") + "do criativo #" + creativeId,
        List.of("DELIVERY", "LANDING", "ADS", "SOCIAL"),
        size,
        "high",
        references.stream().limit(4).toList(),
        UUID.randomUUID().toString(),
        playbook);
  }

  /** Envia o binário ao endpoint canônico de materialização de Dédalo. */
  private void upload(
      Long creativeId, String producerExecutionId, TemisImageStudioOpenAiClient.Result result) {
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("model", result.model());
    body.add("producerExecutionId", producerExecutionId);
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
            return "dedalo-creative-improvement-" + creativeId + ".png";
          }
        });
    backend
        .post()
        .uri("/api/internal/creatives/{id}/agent-improvement/artifact", creativeId)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(body)
        .retrieve()
        .toBodilessEntity();
  }

  /** Monta uma seção enumerada para manter todos os critérios no prompt executado. */
  private String section(String title, List<String> items) {
    if (items.isEmpty()) return "";
    StringBuilder value = new StringBuilder("\n\n").append(title).append(':');
    for (int index = 0; index < items.size(); index++) {
      value.append("\n").append(index + 1).append(". ").append(items.get(index));
    }
    return value.toString();
  }

  /** Converte identificador numérico obrigatório. */
  private Long number(Object value) {
    if (!(value instanceof Number number)) {
      throw new IllegalArgumentException("Correção sem identificador");
    }
    return number.longValue();
  }

  /** Normaliza uma lista textual do contrato estruturado. */
  private List<String> strings(Object value) {
    if (!(value instanceof java.util.Collection<?> collection)) return List.of();
    return collection.stream().map(this::text).filter(item -> !item.isBlank()).distinct().toList();
  }

  /** Normaliza texto recebido do backend. */
  private String text(Object value) {
    return value == null ? "" : value.toString().trim();
  }

  /** Extrai causa específica preservada no log com stack trace. */
  private String rootMessage(Throwable error) {
    Throwable current = error;
    while (current.getCause() != null) current = current.getCause();
    return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
  }
}
