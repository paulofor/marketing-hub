package com.marketinghub.customeragentworker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Responsabilidade: supervisionar atividade e encerramento integral dos processos Codex. */
@Component
final class CodexProcessSupervisor {
  private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(15);
  private static final Duration TERMINATION_GRACE = Duration.ofSeconds(2);
  private final Duration inactivityTimeout;
  private final Duration absoluteTimeout;
  private final Duration pollInterval;

  /** Configura inatividade e teto absoluto de três janelas conforme o cânone dos agentes. */
  @Autowired
  CodexProcessSupervisor(
      @Value("${CUSTOMER_AGENT_MODEL_TIMEOUT:PT40M}") Duration inactivityTimeout) {
    this(
        inactivityTimeout,
        inactivityTimeout == null ? null : inactivityTimeout.multipliedBy(3),
        DEFAULT_POLL_INTERVAL);
  }

  /** Permite testes rápidos com janelas controladas sem alterar a política produtiva. */
  CodexProcessSupervisor(
      Duration inactivityTimeout, Duration absoluteTimeout, Duration pollInterval) {
    this.inactivityTimeout = requiredPositive(inactivityTimeout, "inatividade");
    this.absoluteTimeout = requiredPositive(absoluteTimeout, "teto absoluto");
    this.pollInterval = requiredPositive(pollInterval, "polling");
    if (this.absoluteTimeout.compareTo(this.inactivityTimeout) < 0) {
      throw new IllegalArgumentException("Teto absoluto não pode ser menor que a inatividade.");
    }
  }

  /** Aguarda progresso pelo JSONL e encerra toda a árvore quando uma janela expira. */
  WaitOutcome awaitCompletion(Process process, Path processLog)
      throws IOException, InterruptedException {
    long startedAt = System.nanoTime();
    long lastActivityAt = startedAt;
    long observedSize = Files.size(processLog);
    long inactivityNanos = inactivityTimeout.toNanos();
    long absoluteNanos = absoluteTimeout.toNanos();
    while (true) {
      long elapsed = System.nanoTime() - startedAt;
      long remaining = absoluteNanos - elapsed;
      if (remaining <= 0) {
        terminateTree(process);
        return WaitOutcome.ABSOLUTE_TIMEOUT;
      }
      long waitNanos = Math.max(1L, Math.min(pollInterval.toNanos(), remaining));
      if (process.waitFor(waitNanos, TimeUnit.NANOSECONDS)) return WaitOutcome.COMPLETED;
      long currentSize = Files.size(processLog);
      long now = System.nanoTime();
      if (currentSize != observedSize) {
        observedSize = currentSize;
        lastActivityAt = now;
      }
      if (now - lastActivityAt >= inactivityNanos) {
        terminateTree(process);
        return WaitOutcome.INACTIVITY_TIMEOUT;
      }
    }
  }

  /** Encerra descendentes antes do lançador para impedir processos reparentados ao PID 1. */
  void terminateTree(Process process) {
    List<ProcessHandle> descendants = new ArrayList<>(process.descendants().toList());
    Collections.reverse(descendants);
    descendants.forEach(CodexProcessSupervisor::destroy);
    destroy(process.toHandle());
    awaitExit(process, descendants, TERMINATION_GRACE);
    descendants.forEach(CodexProcessSupervisor::destroyForcibly);
    destroyForcibly(process.toHandle());
    try {
      process.waitFor(TERMINATION_GRACE.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  /** Informa a janela produtiva de inatividade usada na mensagem auditável. */
  Duration inactivityTimeout() {
    return inactivityTimeout;
  }

  /** Informa o teto absoluto usado para impedir execução infinita com saída contínua. */
  Duration absoluteTimeout() {
    return absoluteTimeout;
  }

  /** Aguarda brevemente a coleta normal da árvore antes de forçar sobreviventes. */
  private static void awaitExit(
      Process process, List<ProcessHandle> descendants, Duration gracePeriod) {
    long deadline = System.nanoTime() + gracePeriod.toNanos();
    while (process.isAlive() || descendants.stream().anyMatch(ProcessHandle::isAlive)) {
      if (System.nanoTime() >= deadline) return;
      try {
        Thread.sleep(25L);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }

  /** Solicita encerramento normal somente para um processo ainda vivo. */
  private static void destroy(ProcessHandle process) {
    if (process.isAlive()) process.destroy();
  }

  /** Força o encerramento somente quando a solicitação normal não foi suficiente. */
  private static void destroyForcibly(ProcessHandle process) {
    if (process.isAlive()) process.destroyForcibly();
  }

  /** Rejeita duração ausente, zero ou negativa antes de iniciar qualquer tarefa. */
  private static Duration requiredPositive(Duration value, String label) {
    if (value == null || value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException("Duração de " + label + " deve ser positiva.");
    }
    return value;
  }

  /** Representa conclusão, inatividade ou alcance do teto operacional. */
  enum WaitOutcome {
    COMPLETED,
    INACTIVITY_TIMEOUT,
    ABSOLUTE_TIMEOUT
  }
}
