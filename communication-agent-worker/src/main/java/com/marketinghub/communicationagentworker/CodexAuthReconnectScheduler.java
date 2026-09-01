package com.marketinghub.communicationagentworker;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: criar a sessão Codex individual de Íris pelo Marketing Hub. */
@Component
class CodexAuthReconnectScheduler {
  private static final Logger log = LoggerFactory.getLogger(CodexAuthReconnectScheduler.class);
  private final RestClient backend;
  private final String backendUrl;
  private final String repositoryPath;
  private final String agentKey;

  /** Configura a porta do backend e a identidade exclusiva da agente. */
  CodexAuthReconnectScheduler(
      RestClient.Builder backendBuilder,
      @Value("${BACKEND_URL:http://backend:8000}") String backendUrl,
      @Value("${MARKETING_HUB_REPOSITORY:/workspace/marketing-hub}") String repositoryPath,
      @Value("${AGENT_HEALTH_KEY:communication-director}") String agentKey,
      @Value("${CODEX_AUTH_RECONNECT_CONNECT_TIMEOUT:PT2S}") Duration connectTimeout,
      @Value("${CODEX_AUTH_RECONNECT_READ_TIMEOUT:PT3S}") Duration readTimeout) {
    SimpleClientHttpRequestFactory requests = new SimpleClientHttpRequestFactory();
    requests.setConnectTimeout(connectTimeout);
    requests.setReadTimeout(readTimeout);
    backend = backendBuilder.baseUrl(backendUrl).requestFactory(requests).build();
    this.backendUrl = backendUrl;
    this.repositoryPath = repositoryPath;
    this.agentKey = agentKey;
  }

  /** Reserva e executa somente a reconexão destinada a Íris. */
  @Scheduled(cron = "8/15 * * * * *")
  public void processPending() {
    try {
      ResponseEntity<ReconnectJob> response =
          backend
              .get()
              .uri(
                  "/api/internal/agents/executor-health/{agentKey}/codex-auth/reconnections/pending",
                  agentKey)
              .retrieve()
              .toEntity(ReconnectJob.class);
      if (response.getBody() != null) execute(response.getBody());
    } catch (RuntimeException ex) {
      log.error("Falha ao consultar reconexão Codex de Íris. agentKey={}", agentKey, ex);
    }
  }

  /** Executa o device code dentro do CODEX_HOME individual da agente. */
  void execute(ReconnectJob job) {
    ProcessBuilder builder =
        new ProcessBuilder(
            "node",
            Path.of(repositoryPath)
                .resolve("scripts/codex-app-server-device-login.mjs")
                .toString());
    builder.inheritIO();
    builder.environment().put("CODEX_AUTH_RECONNECT_ID", job.id().toString());
    builder.environment().put("CODEX_AUTH_CALLBACK_BASE_URL", backendUrl);
    try {
      Process process = builder.start();
      if (!process.waitFor(16, TimeUnit.MINUTES)) {
        process.destroyForcibly();
        complete(job.id(), "Tempo esgotado aguardando confirmação.");
      } else if (process.exitValue() != 0) {
        complete(job.id(), "Codex App Server encerrou com falha.");
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.error("Reconexão Codex de Íris interrompida. reconnectId={}", job.id(), ex);
      complete(job.id(), "Reconexão interrompida.");
    } catch (IOException ex) {
      log.error("Falha ao iniciar App Server de Íris. reconnectId={}", job.id(), ex);
      complete(job.id(), "Não foi possível iniciar o App Server.");
    }
  }

  /** Registra falha local sem transportar credenciais. */
  private void complete(Long id, String detail) {
    try {
      backend
          .post()
          .uri("/api/internal/agents/executor-health/codex-auth/reconnections/{id}/completion", id)
          .body(Map.of("authenticated", false, "detail", detail))
          .retrieve()
          .toBodilessEntity();
    } catch (RuntimeException ex) {
      log.error("Falha ao registrar conclusão da reconexão de Íris. reconnectId={}", id, ex);
    }
  }

  /** Representa a solicitação segura de reconexão. */
  record ReconnectJob(Long id) {}
}
