package com.marketinghub.gerasalespage.v1.service.retry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.aiprompt.AiPromptSchemaTemplate;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageExecution;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsabilidade: permitir retomada técnica apenas com entrada auditada e inalterada. */
public final class GeraSalesPageRetryPolicy {
  private static final Logger log = LoggerFactory.getLogger(GeraSalesPageRetryPolicy.class);
  private static final Pattern PLACEHOLDER =
      Pattern.compile(
          "\\{\\{(experiment|previousStageOutputs)}}|\\$\\{(experiment|previousStageOutputs)}");

  /** Impede instanciação da política determinística. */
  private GeraSalesPageRetryPolicy() {}

  /** Distingue interrupção de transporte sem resposta de reprovação ou resultado já consumido. */
  public static boolean transportFailureWithoutResult(GeraSalesPageStageExecution execution) {
    String detail = Objects.toString(execution.getErrorDetail(), "");
    String error =
        Objects.toString(execution.getErrorMessage(), "").toLowerCase(java.util.Locale.ROOT);
    return "FALHA".equals(execution.getStatus())
        && detail.contains("WebClientRequestException")
        && (error.contains("connection reset")
            || error.contains("timed out")
            || error.contains("connection refused"))
        && execution.getOpenAiJobId() == null
        && execution.getRawResponse() == null
        && execution.getModelResponse() == null
        && execution.getCostUsd() == null
        && execution.getInputTokens() == null
        && execution.getOutputTokens() == null;
  }

  /** Compara semanticamente os dados inseridos no prompt, modelo e schema, sem ignorar campos. */
  public static boolean sameInputs(
      GeraSalesPageStageExecution execution,
      AiPromptSchemaTemplate template,
      Map<String, Object> experiment,
      Map<String, Object> previous,
      ObjectMapper mapper) {
    if (!Objects.equals(execution.getPromptTemplateKey(), template.getTemplateKey())
        || !Objects.equals(
            execution.getPromptMarkdownContent(), template.getPromptMarkdownContent())
        || !Objects.equals(execution.getOpenAiModel(), template.getOpenAiModel())
        || execution.getPrompt() == null
        || execution.getSchemaJson() == null) return false;
    try {
      if (!mapper
          .readTree(execution.getSchemaJson())
          .equals(mapper.readTree(template.getSchemaJson()))) return false;
      String source = template.getPromptMarkdownContent();
      var placeholders = PLACEHOLDER.matcher(source);
      var names = new ArrayList<String>();
      var regex = new StringBuilder("\\A");
      int offset = 0;
      while (placeholders.find()) {
        regex.append(Pattern.quote(source.substring(offset, placeholders.start()))).append("(.*?)");
        names.add(placeholders.group(1) != null ? placeholders.group(1) : placeholders.group(2));
        offset = placeholders.end();
      }
      if (!names.contains("experiment") || !names.contains("previousStageOutputs")) return false;
      regex.append(Pattern.quote(source.substring(offset))).append("\\z");
      var values = Pattern.compile(regex.toString(), Pattern.DOTALL).matcher(execution.getPrompt());
      if (!values.matches()) return false;
      for (int i = 0; i < names.size(); i++) {
        var current = mapper.valueToTree(names.get(i).equals("experiment") ? experiment : previous);
        if (!equivalent(mapper.readTree(values.group(i + 1)), current)) return false;
      }
      return true;
    } catch (JsonProcessingException ex) {
      log.warn(
          "GeraSalesPage: entrada auditada inválida para retomada. jobId={}",
          execution.getIdJob(),
          ex);
      return false;
    }
  }

  /**
   * Aceita apenas diferenças de representação numérica do mesmo JSON; preserva valores e listas.
   */
  private static boolean equivalent(JsonNode oldValue, JsonNode current) {
    if (oldValue == null || current == null) return oldValue == current;
    if (oldValue.isNumber() && current.isNumber())
      return oldValue.decimalValue().compareTo(current.decimalValue()) == 0;
    if (oldValue.isObject() && current.isObject()) {
      if (oldValue.size() != current.size()) return false;
      var fields = oldValue.fields();
      while (fields.hasNext()) {
        var field = fields.next();
        if (!equivalent(field.getValue(), current.get(field.getKey()))) return false;
      }
      return true;
    }
    if (oldValue.isArray() && current.isArray()) {
      if (oldValue.size() != current.size()) return false;
      for (int i = 0; i < oldValue.size(); i++)
        if (!equivalent(oldValue.get(i), current.get(i))) return false;
      return true;
    }
    return oldValue.equals(current);
  }
}
