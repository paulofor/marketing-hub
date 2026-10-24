package com.marketinghub.agenttask.audioreview.v1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

/** Responsabilidade: validar identidade, escuta temporal e consumo por modalidade. */
@Slf4j
public final class AudioReviewContract {
  public static final String VERSION = "PSIQUE_AUDIO_REVIEW_V1";
  public static final String MODEL = "gpt-audio-1.5";
  public static final BigDecimal RESERVATION = new BigDecimal("4.116000");

  /** Impede instanciação de regras sem estado. */
  private AudioReviewContract() {}

  /** Lê um único objeto sem descartar campos duplicados nem texto posterior ao JSON. */
  public static JsonNode object(ObjectMapper json, String value) throws JsonProcessingException {
    JsonNode result =
        json.reader()
            .with(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .readTree(value);
    require(result != null && result.isObject(), "Resposta auditiva não é objeto JSON.");
    return result;
  }

  /**
   * Confirma o envelope de uma resposta textual antes de interpretar o parecer orientado por
   * prompt.
   */
  public static String content(JsonNode response) {
    model(response);
    require(
        response.path("choices").isArray() && response.path("choices").size() == 1,
        "Resposta auditiva exige uma única alternativa.");
    var message = response.at("/choices/0/message");
    require(message.isObject(), "Mensagem auditiva ausente.");
    require(
        message.path("content").isTextual() && !message.path("content").asText().isBlank(),
        "Resposta auditiva sem conteúdo textual; nenhuma escuta foi comprovada.");
    return message.path("content").asText().trim();
  }

  /**
   * Identifica recusa explícita de crédito ou limite financeiro antes de saída ou uso reportado.
   */
  public static boolean rejectedForAccountLimit(JsonNode response) {
    return response != null
        && response.isObject()
        && response.size() == 1
        && response.path("error").isObject()
        && Set.of(
                "credit_balance_exhausted",
                "organization_spend_limit_exceeded",
                "project_spend_limit_exceeded",
                "organization_usage_limit_exceeded")
            .contains(response.at("/error/code").asText())
        && "insufficient_quota".equals(response.at("/error/type").asText());
  }

  /** Confirma a família exata antes de aplicar suas tarifas ou aceitar seu parecer. */
  public static void model(JsonNode response) {
    String model = response.path("model").asText();
    require(
        MODEL.equals(model) || model.matches("gpt-audio-1\\.5-\\d{4}-\\d{2}-\\d{2}"),
        "Resposta de modelo incompatível.");
  }

  /** Calcula identidade estável do payload integral. */
  public static String hash(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (Exception ex) {
      log.error("Falha de hash na revisão auditiva", ex);
      throw new IllegalStateException(ex);
    }
  }

  /** Exige WAV real, request limitado e configuração oficialmente suportada. */
  public static void request(JsonNode request, String audioHash) {
    var keys = new HashSet<String>();
    request.fieldNames().forEachRemaining(keys::add);
    require(
        keys.equals(
            Set.of(
                "model",
                "modalities",
                "service_tier",
                "store",
                "max_completion_tokens",
                "messages")),
        "Campos inesperados na chamada auditiva.");
    require(MODEL.equals(request.path("model").asText()), "Modelo auditivo incompatível.");
    require(
        request.path("modalities").size() == 1
            && "text".equals(request.at("/modalities/0").asText()),
        "Somente saída textual autorizada.");
    require(
        "default".equals(request.path("service_tier").asText())
            && !request.path("store").asBoolean(true),
        "Tier/armazenamento incompatível.");
    require(request.path("max_completion_tokens").asInt() == 2000, "Limite de saída incompatível.");
    require(
        request.path("messages").size() == 1
            && "user".equals(request.at("/messages/0/role").asText()),
        "Entrada auditiva inválida.");
    var content = request.at("/messages/0/content");
    require(
        content.size() == 2
            && "text".equals(content.path(0).path("type").asText())
            && "input_audio".equals(content.path(1).path("type").asText()),
        "Entrada auditiva incompleta.");
    require(
        content.path(0).path("text").asText().length() <= 50000, "Contexto auditivo excessivo.");
    var audio = content.path(1).path("input_audio");
    require("wav".equals(audio.path("format").asText()), "Formato auditivo inválido.");
    String data = audio.path("data").asText();
    require(data.length() > 60 && data.length() <= 8000000, "Áudio ausente ou excessivo.");
    byte[] bytes = Base64.getDecoder().decode(data);
    require(hash(bytes).equals(audioHash), "Hash do áudio divergiu.");
    require(
        new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
            && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WAVE"),
        "Áudio não é WAV.");
  }

  /** Exige diagnósticos nos limites da mídia, por dimensão, com ações e limites explícitos. */
  public static void assessment(JsonNode result, double duration) {
    require(result != null && result.isObject(), "Resposta auditiva não é objeto JSON.");
    fields(
        result,
        Set.of(
            "contractVersion",
            "listened",
            "verdict",
            "voice",
            "music",
            "timing",
            "moments",
            "limitations"));
    require(
        VERSION.equals(result.path("contractVersion").asText())
            && result.path("listened").isBoolean()
            && result.path("listened").asBoolean(),
        "Escuta não comprovada.");
    require(
        Set.of("PASS", "ADJUST", "INCONCLUSIVE").contains(result.path("verdict").asText()),
        "Decisão auditiva inválida.");
    require(
        result.path("limitations").isArray() && !result.path("limitations").isEmpty(),
        "Declare limitações da escuta.");
    for (var limit : result.path("limitations"))
      require(text(limit, 10), "Limitação auditiva inválida.");
    for (String key : List.of("voice", "music", "timing")) {
      JsonNode item = result.path(key);
      fields(item, Set.of("status", "summary"));
      require(
          Set.of("PASS", "ADJUST", "INCONCLUSIVE", "NOT_PRESENT")
              .contains(item.path("status").asText()),
          "Dimensão auditiva inválida: " + key);
      require(text(item.path("summary"), 15), "Dimensão auditiva sem justificativa: " + key);
      if ("PASS".equals(result.path("verdict").asText()))
        require(
            Set.of("PASS", "NOT_PRESENT").contains(item.path("status").asText()),
            "Aprovação auditiva contraditória.");
      if (!key.equals("music"))
        require(
            !"NOT_PRESENT".equals(item.path("status").asText()),
            "Voz/tempo ausente não comprova aprovação.");
    }
    require(
        result.path("moments").isArray()
            && result.path("moments").size() >= 3
            && result.path("moments").size() <= 12,
        "Escuta sem cobertura temporal.");
    boolean beginning = false, middle = false, end = false;
    for (var moment : result.path("moments")) {
      fields(moment, Set.of("startSeconds", "endSeconds", "heard", "action"));
      double from = moment.path("startSeconds").asDouble(-1),
          to = moment.path("endSeconds").asDouble(-1);
      require(
          moment.path("startSeconds").isNumber()
              && moment.path("endSeconds").isNumber()
              && Double.isFinite(from)
              && Double.isFinite(to)
              && from >= 0
              && to > from
              && to <= duration + .1,
          "Instante auditivo fora da mídia.");
      require(
          text(moment.path("heard"), 10) && text(moment.path("action"), 5),
          "Achado sem evidência ou ação.");
      beginning |= from <= duration * .25;
      middle |= from < duration * .65 && to > duration * .35;
      end |= to >= duration * .75;
    }
    require(
        beginning && middle && end,
        "A escuta precisa cobrir abertura, desenvolvimento e encerramento.");
  }

  /** Confronta a escuta com o mix e os intervalos medidos, preservando respostas contraditórias. */
  public static void groundedAssessment(JsonNode result, JsonNode media) {
    assessment(result, media.path("durationSeconds").asDouble());
    var production = media.path("audioProductionEvidence");
    if ("VOICE_ONLY".equals(production.path("mix").asText())
        && "none".equals(production.path("music").asText()))
      require(
          "NOT_PRESENT".equals(result.at("/music/status").asText()),
          "O parecer descreve música, mas o mix auditado contém somente voz.");
    if (!"SEGMENT_AUDIO_DURATION".equals(production.path("timingSource").asText())) return;
    var segments = production.path("segments");
    require(
        result.path("moments").size() == segments.size(),
        "A escuta deve avaliar cada segmento medido, sem inventar intervalos.");
    for (int i = 0; i < segments.size(); i++) {
      var moment = result.path("moments").get(i);
      var segment = segments.get(i);
      require(
          Math.abs(moment.path("startSeconds").asDouble() - segment.path("startSeconds").asDouble())
                  <= .1
              && Math.abs(
                      moment.path("endSeconds").asDouble() - segment.path("endSeconds").asDouble())
                  <= .1,
          "O instante auditivo diverge do segmento medido da locução.");
    }
  }

  /** Recusa campos omitidos ou extras em respostas sem Structured Outputs do provedor. */
  private static void fields(JsonNode value, Set<String> expected) {
    var actual = new HashSet<String>();
    value.fieldNames().forEachRemaining(actual::add);
    require(
        value.isObject() && actual.equals(expected), "Campos do parecer auditivo incompatíveis.");
  }

  /** Exige texto real e conta caracteres Unicode sem coerção de números ou valores vazios. */
  private static boolean text(JsonNode value, int minimum) {
    if (!value.isTextual()) return false;
    String content = value.asText().strip();
    return content.codePointCount(0, content.length()) >= minimum;
  }

  /** Usa contadores separados de áudio/texto; ausência mantém custo desconhecido. */
  public static BigDecimal cost(JsonNode response) {
    var usage = response.path("usage");
    var audio = usage.at("/prompt_tokens_details/audio_tokens");
    var input = usage.path("prompt_tokens");
    var output = usage.path("completion_tokens");
    if (!audio.isIntegralNumber() || !input.isIntegralNumber() || !output.isIntegralNumber())
      return null;
    long a = audio.asLong(-1), i = input.asLong(-1), o = output.asLong(-1);
    if (a <= 0 || i < a || i > 128000 || o < 0 || o > 2000) return null;
    return BigDecimal.valueOf(a)
        .multiply(new BigDecimal("32"))
        .add(BigDecimal.valueOf(i - a).multiply(new BigDecimal("2.5")))
        .add(BigDecimal.valueOf(o).multiply(new BigDecimal("10")))
        .divide(new BigDecimal("1000000"));
  }

  /** Mantém mensagens de bloqueio uniformes antes do consumo ou aceite. */
  public static void require(boolean condition, String message) {
    if (!condition)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.CONFLICT, message);
  }
}
