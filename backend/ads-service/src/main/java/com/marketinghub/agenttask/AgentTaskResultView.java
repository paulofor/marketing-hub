package com.marketinghub.agenttask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsabilidade: extrair seções tipadas do parecer funcional persistido na tarefa. */
public final class AgentTaskResultView {
  private static final Logger log = LoggerFactory.getLogger(AgentTaskResultView.class);
  private static final ObjectMapper JSON = new ObjectMapper();

  /** Impede instanciação do conversor sem estado. */
  private AgentTaskResultView() {}

  /** Devolve uma seção objetiva do resultado sem inferir conteúdo ausente ou inválido. */
  public static JsonNode section(AgentTask task, String fieldName) {
    if (task == null
        || task.getResultJson() == null
        || task.getResultJson().isBlank()
        || fieldName == null
        || fieldName.isBlank()) {
      return null;
    }
    try {
      JsonNode value = JSON.readTree(task.getResultJson()).path(fieldName);
      return value.isObject() || value.isArray() ? value.deepCopy() : null;
    } catch (Exception ex) {
      log.warn(
          "Resultado funcional inválido ao extrair seção. taskId={} fieldName={}",
          task.getId(),
          fieldName,
          ex);
      return null;
    }
  }
}
