package com.marketinghub.landinggeneratoragent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Responsabilidade: executar localmente reconexões Codex solicitadas pelo backend. */
@Component
public class CodexAuthReconnectScheduler {
  private static final Logger log = LoggerFactory.getLogger(CodexAuthReconnectScheduler.class);
  private final LandingGeneratorBackendClient backend;
  private final LandingGeneratorAgentProperties properties;

  /** Configura a porta de backend e os caminhos versionados do executor. */
  public CodexAuthReconnectScheduler(
      LandingGeneratorBackendClient backend, LandingGeneratorAgentProperties properties) {
    this.backend = backend;
    this.properties = properties;
  }

  /** Consulta o pending canônico e mantém uma única autenticação ativa por sessão. */
  @Scheduled(cron = "5/15 * * * * *")
  public void processPending() {
    try {
      CodexAuthReconnectJob job = backend.claimCodexAuthReconnect();
      if (job != null) execute(job);
    } catch (RuntimeException ex) {
      log.error("Falha ao processar reconexão Codex do Dédalo", ex);
    }
  }

  /** Executa o App Server sob o mesmo lock usado pelas chamadas Codex. */
  void execute(CodexAuthReconnectJob job) {
    Path script =
        Path.of(properties.getRepositoryPath())
            .resolve("scripts/codex-app-server-device-login.mjs");
    ProcessBuilder builder =
        new ProcessBuilder(
            "flock",
            System.getenv("CODEX_HOME") + "/.oauth-session.lock",
            "node",
            script.toString());
    builder.inheritIO();
    builder.environment().put("CODEX_AUTH_RECONNECT_ID", job.id().toString());
    builder.environment().put("CODEX_AUTH_CALLBACK_BASE_URL", properties.getBackendUrl());
    try {
      Process process = builder.start();
      if (!process.waitFor(16, TimeUnit.MINUTES)) {
        process.destroyForcibly();
        backend.completeCodexAuth(job.id(), false, "Tempo esgotado aguardando confirmação.");
      } else if (process.exitValue() != 0) {
        backend.completeCodexAuth(job.id(), false, "Codex App Server encerrou com falha.");
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.error("Reconexão Codex interrompida. reconnectId={}", job.id(), ex);
      backend.completeCodexAuth(job.id(), false, "Reconexão interrompida.");
    } catch (IOException ex) {
      log.error("Falha ao iniciar App Server. reconnectId={}", job.id(), ex);
      backend.completeCodexAuth(job.id(), false, "Não foi possível iniciar o App Server.");
    }
  }
}
