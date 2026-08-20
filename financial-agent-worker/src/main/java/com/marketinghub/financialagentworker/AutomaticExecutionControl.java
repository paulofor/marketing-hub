package com.marketinghub.financialagentworker;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: bloquear rotinas automáticas de Plutus quando o backend determinar STOP. */
@Component
public class AutomaticExecutionControl {
  private static final Logger log = LoggerFactory.getLogger(AutomaticExecutionControl.class);
  private static final String AGENT_KEY = "financial-agent";
  private final RestClient backend;

  /** Configura a leitura do estado usando a mesma URL operacional de Plutus. */
  public AutomaticExecutionControl(FinancialAgentProperties properties) {
    SimpleClientHttpRequestFactory requests = new SimpleClientHttpRequestFactory();
    requests.setConnectTimeout(Duration.ofSeconds(2));
    requests.setReadTimeout(Duration.ofSeconds(3));
    backend =
        RestClient.builder().baseUrl(properties.getBackendUrl()).requestFactory(requests).build();
  }

  /** Permite novo trabalho somente quando o backend comprovar PLAY. */
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
      log.error("Falha ao comprovar PLAY; Plutus permanecerá parado. agentKey={}", AGENT_KEY, ex);
      return false;
    }
  }

  /** Representa somente o campo necessário para controlar a rotina local. */
  private record ControlState(boolean automaticExecutionEnabled) {}
}
