package com.marketinghub.worker.openai.codex;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Executa os comandos administrativos de conexão sem disputar a sessão com outras verificações. */
@Component
@ConditionalOnProperty(name = "openai.text-transport", havingValue = "codex-app-server")
public class CodexConnectionScheduler {
  private static final Logger log = LoggerFactory.getLogger(CodexConnectionScheduler.class);
  private final String node;
  private final String script;
  private final ReentrantLock lock = new ReentrantLock();

  /** Recebe o cliente versionado; nenhuma credencial é carregada pela camada Java. */
  public CodexConnectionScheduler(
      @Value("${openai.codex.node:node}") String node,
      @Value("${openai.codex.connection-script:/app/codex/connection.mjs}") String script) {
    this.node = node;
    this.script = script;
  }

  /** Consome a fila própria e recupera callbacks já concluídos antes de abrir outra conexão. */
  @Scheduled(cron = "9/15 * * * * *")
  public void processPending() {
    execute("--poll", Duration.ofMinutes(16));
  }

  /** Publica prova recente sem iniciar inferência ou interromper o login em andamento. */
  @Scheduled(cron = "4 * * * * *")
  public void checkSession() {
    execute("--health", Duration.ofSeconds(45));
  }

  /** Limita a execução e encerra todos os descendentes ao perder o processo autenticador. */
  void execute(String action, Duration timeout) {
    if (!lock.tryLock()) return;
    Process process = null;
    try {
      process = new ProcessBuilder(node, script, action)
          .redirectOutput(ProcessBuilder.Redirect.DISCARD)
          .redirectError(ProcessBuilder.Redirect.INHERIT).start();
      if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
        log.warn("Tempo esgotado na conexão do Worker IA; operação={}; prazo={}", action, timeout);
      } else if (process.exitValue() != 0) {
        log.warn("Conexão do Worker IA sem confirmação; operação={}; exitCode={}", action, process.exitValue());
      }
    } catch (IOException ex) {
      log.error("Falha local na conexão do Worker IA; operação={}; script={}", action, script, ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.error("Conexão do Worker IA interrompida; operação={}", action, ex);
    } finally {
      if (process != null && process.isAlive()) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
      }
      lock.unlock();
    }
  }
}
