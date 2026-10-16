package com.marketinghub.salesvideo.service.reconcileNarrationCosts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Confere recibos de voz e consumo por coorte, sem tratar rateio como fatura por chamada. */
@Slf4j
final class NarrationUsageEvidence {
  /** Impede instâncias do validador sem estado. */
  private NarrationUsageEvidence() {}

  /** Lê a auditoria mais completa, preservada também nas falhas posteriores à síntese. */
  static JsonNode audit(SalesVideoJob job, ObjectMapper json) {
    JsonNode selected = read(job.getMetadataJson(), job.getId(), json);
    for (var event : job.getEvents()) {
      JsonNode candidate = read(event.getDetailsJson(), job.getId(), json);
      if (candidate.path("tts_interactions").size() > selected.path("tts_interactions").size())
        selected = candidate;
    }
    return selected;
  }

  /** Reconhece reutilização íntegra ou falha no gate anterior à voz, sem inventar consumo novo. */
  static boolean withoutNewSpeech(SalesVideoJob job, JsonNode audit, JsonNode metadata) {
    JsonNode interactions = audit.path("tts_interactions");
    if (interactions.isArray() && !interactions.isEmpty()) {
      if (!metadata.has("preservedNarration")
          || metadata.at("/preservedNarration/newTtsAuthorized").asBoolean(true)) return false;
      for (var item : interactions)
        if (!"TEXT_TO_SPEECH_REUSE".equals(item.path("interaction_type").asText())
            || !"OPENAI".equals(item.path("provider").asText())
            || item.path("job_id").asLong() != job.getId()
            || item.path("source_job_id").asLong()
                != metadata.at("/preservedNarration/sourceJobId").asLong()
            || item.path("source_asset_id").asLong() <= 0
            || !item.path("source_sha256").asText().matches("[a-f0-9]{64}")
            || !"REUSED".equals(item.path("status").asText())
            || !item.has("incremental_cost_usd")
            || item.path("incremental_cost_usd").decimalValue().signum() != 0) return false;
      return true;
    }
    // Esses gates antecedem TTS; falha posterior preserva obrigatoriamente os recibos no callback.
    return job.getStatus() == SalesVideoStatus.VIDEO_FAILED
        && Set.of("PDE_PRODUCT_PROOF_INVALID", "DELIVERY_EXAMPLES_INVALID")
            .contains(Objects.toString(job.getFailureCode(), ""))
        && metadata.has("post_production")
        && !metadata.has("preservedNarration");
  }

  /** Exige recibos únicos e tarifa auditada para cada síntese realmente recebida. */
  static PaidSpeech paid(SalesVideoJob job, JsonNode audit, Set<String> allRequests) {
    var interactions = audit.path("tts_interactions");
    require(
        interactions.isArray() && !interactions.isEmpty(),
        "A tentativa não tem recibos suficientes para conciliar voz.");
    String model = null;
    BigDecimal inputRate = null, outputRate = null;
    Instant first = null, last = null;
    int count = 0;
    for (var item : interactions) {
      String requestId = item.at("/raw_response/request_id").asText();
      require(
          "TEXT_TO_SPEECH".equals(item.path("interaction_type").asText())
              && "https://api.openai.com/v1/audio/speech".equals(item.path("endpoint").asText())
              && item.at("/raw_response/bytes").asLong() > 0
              && !item.at("/raw_request/input").asText().isBlank()
              && "COMPLETED".equals(item.path("status").asText())
              && "OPENAI".equals(item.path("provider").asText())
              && item.path("job_id").asLong() == job.getId()
              && item.at("/raw_response/sha256").asText().matches("[a-f0-9]{64}")
              && !requestId.isBlank()
              && allRequests.add(requestId),
          "Recibo ausente, incompleto, duplicado ou pertencente a outra tentativa.");
      String currentModel = item.path("model").asText();
      var pricing = item.path("pricing");
      BigDecimal in = pricing.path("input_usd_per_million_text_tokens").decimalValue();
      BigDecimal out = pricing.path("output_usd_per_million_audio_tokens").decimalValue();
      require(
          !currentModel.isBlank()
              && currentModel.equals(item.at("/raw_request/model").asText())
              && in.signum() > 0
              && out.signum() > 0,
          "A tarifa da voz não está preservada na auditoria.");
      require(
          model == null
              || (model.equals(currentModel)
                  && inputRate.compareTo(in) == 0
                  && outputRate.compareTo(out) == 0),
          "A coorte mistura modelos ou tarifas.");
      model = currentModel;
      inputRate = in;
      outputRate = out;
      Instant completed;
      try {
        completed = Instant.parse(item.path("completed_at").asText());
      } catch (RuntimeException ex) {
        log.warn("Recibo de voz sem horário válido; jobId={}", job.getId(), ex);
        throw invalid("O horário da síntese não foi preservado.");
      }
      first = first == null || completed.isBefore(first) ? completed : first;
      last = last == null || completed.isAfter(last) ? completed : last;
      count++;
    }
    return new PaidSpeech(model, inputRate, outputRate, count, first, last);
  }

  /** Confronta a contagem de recibos e o período exclusivo com o uso oficial importado. */
  static BigDecimal cost(JsonNode usage, List<PaidSpeech> paid, boolean exclusive) {
    if (paid.isEmpty()) {
      require(
          usage == null || usage.isNull(), "Reutilizações não devem receber outra coorte cobrada.");
      return BigDecimal.ZERO;
    }
    require(
        exclusive
            && usage != null
            && usage.path("data").isArray()
            && !usage.path("has_more").asBoolean(true)
            && usage.path("data").size() <= 500
            && usage.toString().length() <= 500000,
        "Importe o consumo completo de um período exclusivo e confirme a correspondência.");
    var baseline = paid.getFirst();
    for (var item : paid)
      require(
          item.model().equals(baseline.model())
              && item.inputRate().compareTo(baseline.inputRate()) == 0
              && item.outputRate().compareTo(baseline.outputRate()) == 0,
          "A coorte mistura modelos ou tarifas.");
    long requests = 0, input = 0, output = 0;
    var seen = new HashSet<String>();
    long from = Long.MAX_VALUE, to = 0;
    for (var bucket : usage.path("data")) {
      long start = number(bucket, "start_time"), end = number(bucket, "end_time");
      require(end > start, "Período de consumo inválido.");
      for (var row : bucket.path("results")) {
        if (!baseline.model().equals(row.path("model").asText())) continue;
        require(
            "organization.usage.completions.result".equals(row.path("object").asText()),
            "O modelo de voz exige o extrato usage/completions.");
        require(seen.add(start + ":" + row.toString()), "O extrato contém consumo duplicado.");
        require(
            row.path("input_audio_tokens").asLong() == 0
                && row.path("output_text_tokens").asLong() == 0,
            "Modalidades não cobertas pela tarifa auditada.");
        requests = Math.addExact(requests, number(row, "num_model_requests"));
        input = Math.addExact(input, number(row, "input_text_tokens"));
        output = Math.addExact(output, number(row, "output_audio_tokens"));
        from = Math.min(from, start);
        to = Math.max(to, end);
      }
    }
    require(
        requests == paid.stream().mapToInt(PaidSpeech::count).sum() && output > 0,
        "A contagem oficial não corresponde aos recibos; não atribua consumo de outros produtos.");
    for (var item : paid)
      require(
          item.first().getEpochSecond() >= from - 300 && item.last().getEpochSecond() <= to + 300,
          "O período importado não cobre os recibos de voz.");
    return BigDecimal.valueOf(input)
        .multiply(baseline.inputRate())
        .add(BigDecimal.valueOf(output).multiply(baseline.outputRate()))
        .divide(new BigDecimal("1000000"), 12, RoundingMode.CEILING);
  }

  /** Recusa contagens ausentes, negativas ou fora do formato integral oficial. */
  private static long number(JsonNode node, String field) {
    JsonNode value = node.path(field);
    require(
        value.isIntegralNumber()
            && value.canConvertToLong()
            && value.asLong() >= 0
            && value.asLong() <= 1000000000000L,
        "Contagem ou período inválido: " + field);
    return value.asLong();
  }

  /** Lê somente auditoria JSON válida, deixando falha observável e sem custo inventado. */
  static JsonNode read(String value, Long jobId, ObjectMapper json) {
    try {
      return json.readTree(value == null || value.isBlank() ? "{}" : value);
    } catch (Exception ex) {
      log.warn("Auditoria de locução inválida; jobId={}", jobId, ex);
      throw invalid("A auditoria da tentativa está inválida.");
    }
  }

  /** Interrompe a conciliação quando a evidência não sustenta o lançamento. */
  static void require(boolean condition, String message) {
    if (!condition) throw invalid(message);
  }

  /** Expõe conflito de evidência sem mudar tarefas, consumo ou orçamento. */
  static ResponseStatusException invalid(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }

  /** Mantém contagem, tarifa e período auditados de uma tentativa com chamadas novas. */
  record PaidSpeech(
      String model,
      BigDecimal inputRate,
      BigDecimal outputRate,
      int count,
      Instant first,
      Instant last) {}
}
