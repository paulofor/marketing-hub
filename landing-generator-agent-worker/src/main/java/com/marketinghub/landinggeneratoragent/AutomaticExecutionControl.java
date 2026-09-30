package com.marketinghub.landinggeneratoragent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: bloquear rotinas automáticas de Dédalo quando o backend determinar STOP. */
@Component
public class AutomaticExecutionControl {
  private static final Logger log = LoggerFactory.getLogger(AutomaticExecutionControl.class);
  private static final String AGENT_KEY = "landing-generator";
  private final RestClient backend;

  /** Configura uma consulta curta ao backend, sem reservar trabalho durante a verificação. */
  public AutomaticExecutionControl(@Value("${BACKEND_URL:http://backend:8000}") String backendUrl) {
    backend = LandingGeneratorBackendRestClientFactory.create(backendUrl);
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
      log.error("Falha ao comprovar PLAY; Dédalo permanecerá parado. agentKey={}", AGENT_KEY, ex);
      return false;
    }
  }

  /** Representa somente o campo necessário para controlar a rotina local. */
  private record ControlState(boolean automaticExecutionEnabled) {}
}
