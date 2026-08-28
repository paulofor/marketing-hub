package com.marketinghub.metaadapproverworker;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Responsabilidade: bloquear cada papel automático quando seu agente proprietário estiver em STOP.
 */
@Component
public class AutomaticExecutionControl {
  private static final Logger log = LoggerFactory.getLogger(AutomaticExecutionControl.class);
  private final RestClient backend;
  private final String agentKey;

  /** Configura uma leitura curta independente das filas de produção e revisão. */
  public AutomaticExecutionControl(MetaAdApproverProperties properties) {
    SimpleClientHttpRequestFactory requests = new SimpleClientHttpRequestFactory();
    requests.setConnectTimeout(Duration.ofSeconds(2));
    requests.setReadTimeout(Duration.ofSeconds(3));
    backend =
        RestClient.builder().baseUrl(properties.getBackendUrl()).requestFactory(requests).build();
    agentKey = ownerAgentKey(properties.getExecutionRole());
  }

  /** Permite novo trabalho somente quando o backend comprovar PLAY. */
  public boolean allowsAutomaticExecution() {
    try {
      ControlState state =
          backend
              .get()
              .uri("/api/internal/agents/executor-health/{agentKey}/automatic-execution", agentKey)
              .retrieve()
              .body(ControlState.class);
      return state != null && state.automaticExecutionEnabled();
    } catch (RuntimeException ex) {
      log.error("Falha ao comprovar PLAY; o papel permanecerá parado. agentKey={}", agentKey, ex);
      return false;
    }
  }

  /** Resolve o proprietário funcional sem atribuir o recurso técnico de imagem a Têmis. */
  static String ownerAgentKey(String executionRole) {
    return "image-studio".equals(executionRole) ? "communication-director" : "meta-ad-approver";
  }

  /** Representa somente o campo necessário para controlar a rotina local. */
  private record ControlState(boolean automaticExecutionEnabled) {}
}
