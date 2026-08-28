package com.marketinghub.communicationagentworker;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: bloquear novas execuções de Íris quando o backend determinar STOP. */
@Component
public class AutomaticExecutionControl {
  private static final Logger log = LoggerFactory.getLogger(AutomaticExecutionControl.class);
  private static final String AGENT_KEY = "communication-director";
  private final RestClient backend;

  /** Configura a leitura fail-closed do controle operacional. */
  public AutomaticExecutionControl(CommunicationAgentProperties properties) {
    SimpleClientHttpRequestFactory requests = new SimpleClientHttpRequestFactory();
    requests.setConnectTimeout(Duration.ofSeconds(2));
    requests.setReadTimeout(Duration.ofSeconds(3));
    backend =
        RestClient.builder().baseUrl(properties.getBackendUrl()).requestFactory(requests).build();
  }

  /** Permite reservar trabalho somente quando o backend comprovar PLAY. */
  public boolean allowsAutomaticExecution() {
    try {
      ControlState state =
          backend
              .get()
              .uri("/api/internal/agents/executor-health/{agentKey}/automatic-execution", AGENT_KEY)
              .retrieve()
              .body(ControlState.class);
      return state != null && state.automaticExecutionEnabled();
    } catch (RuntimeException ex) {
      log.error("Falha ao comprovar PLAY; Íris permanecerá parada. agentKey={}", AGENT_KEY, ex);
      return false;
    }
  }

  /** Representa somente o estado necessário para decidir o polling local. */
  private record ControlState(boolean automaticExecutionEnabled) {}
}
