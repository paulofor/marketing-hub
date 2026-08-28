package com.marketinghub.communicationagentworker;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: consumir e concluir atividades de Íris exclusivamente pelo backend. */
@Component
public class CommunicationAgentBackendClient {
  private static final Logger log = LoggerFactory.getLogger(CommunicationAgentBackendClient.class);
  private static final String AGENT_KEY = "communication-director";
  private final RestClient client;

  /** Configura o cliente com a URL operacional do Marketing Hub. */
  public CommunicationAgentBackendClient(CommunicationAgentProperties properties) {
    client = RestClient.builder().baseUrl(properties.getBackendUrl()).build();
  }

  /** Reserva no máximo uma atividade do contrato explicitamente informado. */
  public Map<String, Object> claim(String processCode, String activityId) {
    String endpoint =
        "/api/internal/agent-tasks/{agent}/stage-executions/pending?processCode={processCode}&activityId={activityId}&executionResourceCode={executionResourceCode}";
    try {
      log.info(
          "Consultando fila de Íris. url={} processCode={} activityId={}",
          endpoint,
          processCode,
          activityId);
      List<Map<String, Object>> pending =
          client
              .get()
              .uri(endpoint, AGENT_KEY, processCode, activityId, "iris-communication-worker")
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});
      log.info(
          "Resposta da fila de Íris recebida. url={} processCode={} activityId={} quantidade={}",
          endpoint,
          processCode,
          activityId,
          pending == null ? 0 : pending.size());
      return pending == null || pending.isEmpty() ? null : pending.getFirst();
    } catch (RuntimeException ex) {
      log.error(
          "Falha no módulo communication-agent-worker ao consultar fila. url={} processCode={} activityId={}",
          endpoint,
          processCode,
          activityId,
          ex);
      throw ex;
    }
  }

  /** Envia o resultado funcional e sua auditoria sem avançar o pipeline localmente. */
  public void complete(long taskId, Map<String, Object> payload) {
    String endpoint = "/api/internal/agent-tasks/{agent}/stage-executions/{taskId}/result";
    try {
      log.info("Enviando resultado de Íris. url={} taskId={}", endpoint, taskId);
      client.post().uri(endpoint, AGENT_KEY, taskId).body(payload).retrieve().toBodilessEntity();
      log.info("Resposta de conclusão de Íris recebida. url={} taskId={}", endpoint, taskId);
    } catch (RuntimeException ex) {
      log.error("Falha ao concluir atividade de Íris. url={} taskId={}", endpoint, taskId, ex);
      throw ex;
    }
  }

  /** Registra bloqueio técnico ou funcional preservando toda a auditoria disponível. */
  public void fail(long taskId, Map<String, Object> payload) {
    String endpoint = "/api/internal/agent-tasks/{agent}/stage-executions/{taskId}/failure";
    try {
      log.info("Enviando bloqueio de Íris. url={} taskId={}", endpoint, taskId);
      client.post().uri(endpoint, AGENT_KEY, taskId).body(payload).retrieve().toBodilessEntity();
      log.info("Resposta de bloqueio de Íris recebida. url={} taskId={}", endpoint, taskId);
    } catch (RuntimeException ex) {
      log.error("Falha ao registrar bloqueio de Íris. url={} taskId={}", endpoint, taskId, ex);
      throw ex;
    }
  }
}
