package com.marketinghub.salesvideo.service.reconcileNarrationCosts;

import static com.marketinghub.salesvideo.service.reconcileNarrationCosts.NarrationUsageEvidence.require;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoStatus;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/**
 * Reconhece a recusa de autenticação do primeiro segmento sem confundir ausência de recibo com custo
 * zero.
 */
@Slf4j
final class RejectedNarrationEvidence {
  static final String POLICY = "FIRST_TTS_AUTHENTICATION_REJECTED_AUDITED";

  /** Impede instâncias do verificador sem estado. */
  private RejectedNarrationEvidence() {}

  /**
   * Confere o callback completo de HTTP 401 antes de qualquer áudio, mantendo falhas incertas
   * bloqueadas.
   */
  static boolean withoutSpeech(
      SalesVideoJob job, JsonNode audit, JsonNode metadata, ObjectMapper json) {
    if (!"OPENAI_TTS_FAILED".equals(job.getFailureCode())
        || audit.path("tts_interactions").size() > 0) return false;
    require(
        job.getStatus() == SalesVideoStatus.VIDEO_FAILED
            && job.getAsset() == null
            && job.getPosterAsset() == null
            && job.getVttAsset() == null
            && (job.getProviderJobId() == null || job.getProviderJobId().isBlank())
            && !metadata.has("preservedNarration")
            && (!audit.has("tts_interactions")
                || (audit.path("tts_interactions").isArray()
                    && audit.path("tts_interactions").isEmpty())),
        "Há áudio recebido, fonte reutilizada ou estado incerto; preserve o custo da locução.");
    String detail = Objects.toString(job.getFailureDetail(), "");
    String wrapper = "retryable=false;code=OPENAI_TTS_FAILED;message=";
    if (detail.startsWith(wrapper)) detail = detail.substring(wrapper.length());
    String prefix =
        "OpenAI TTS recusou a locução; jobId="
            + job.getId()
            + "; segment=1; url=https://api.openai.com/v1/audio/speech; status=401; rawRequest=";
    require(
        detail.startsWith(prefix),
        "A falha não comprova recusa do primeiro segmento por autenticação.");
    String body = detail.substring(prefix.length());
    int separator = body.indexOf("; rawResponse=");
    require(
        separator > 0, "Request e resposta da recusa precisam estar preservados integralmente.");
    JsonNode request = read(body.substring(0, separator), job.getId(), json);
    JsonNode response =
        read(body.substring(separator + "; rawResponse=".length()), job.getId(), json);
    String firstCaption = metadata.path("captionText").asText().split("\\|", -1)[0].trim();
    String input = request.path("input").asText();
    require(
        request.isObject()
            && !request.path("model").asText().isBlank()
            && !request.path("voice").asText().isBlank()
            && !request.path("response_format").asText().isBlank()
            && !input.isBlank()
            && input.equals(firstCaption)
            && metadata.path("voiceOverScript").asText().startsWith(input)
            && response.isObject()
            && response.path("error").isObject()
            && "invalid_api_key".equals(response.at("/error/code").asText())
            && (!response.has("status") || response.path("status").asInt() == 401)
            && !response.has("usage")
            && !response.has("output")
            && !response.has("audio")
            && !response.has("id"),
        "A resposta não comprova recusa sem geração da entrada desta tentativa.");
    return true;
  }

  /**
   * Rejeita JSON truncado, duplicado ou seguido de outra resposta sem descartar a evidência
   * original.
   */
  private static JsonNode read(String value, Long jobId, ObjectMapper json) {
    try {
      return json.reader()
          .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
          .readTree(value);
    } catch (Exception ex) {
      log.warn("Recusa de locução sem JSON íntegro; jobId={}", jobId, ex);
      throw NarrationUsageEvidence.invalid("A resposta da locução está incompleta ou ambígua.");
    }
  }
}
