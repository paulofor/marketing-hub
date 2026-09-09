package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

/** Responsabilidade: impedir timeout cego e processos Codex órfãos no worker de Psique. */
class CodexProcessSupervisorTest {
  /** Confirma que o Spring seleciona o construtor produtivo mesmo com o construtor de teste. */
  @Test
  void createsSupervisorThroughSpringContext() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context
          .getBeanFactory()
          .setConversionService(ApplicationConversionService.getSharedInstance());
      context
          .getEnvironment()
          .getPropertySources()
          .addFirst(
              new MapPropertySource(
                  "supervisor-test", java.util.Map.of("CUSTOMER_AGENT_MODEL_TIMEOUT", "PT1M")));
      context.register(CodexProcessSupervisor.class);
      context.refresh();

      assertThat(context.getBean(CodexProcessSupervisor.class).inactivityTimeout())
          .isEqualTo(Duration.ofMinutes(1));
    }
  }

  /** Renova a janela por eventos com tempo controlado, mesmo após superar a inatividade inicial. */
  @Test
  void keepsProcessAliveWhileJsonlAdvances() throws Exception {
    Path log = Files.createTempFile("psique-active-process-", ".log");
    AtomicLong clock = new AtomicLong();
    Process process = activeProcess(log, clock, Duration.ofMillis(300));
    CodexProcessSupervisor supervisor =
        new CodexProcessSupervisor(
            Duration.ofMillis(130), Duration.ofSeconds(2), Duration.ofMillis(20), clock::get);

    try {
      CodexProcessSupervisor.WaitOutcome outcome = supervisor.awaitCompletion(process, log);
      assertThat(outcome).isEqualTo(CodexProcessSupervisor.WaitOutcome.COMPLETED);
      assertThat(clock.get()).isGreaterThan(supervisor.inactivityTimeout().toNanos());
    } finally {
      Files.deleteIfExists(log);
    }
  }

  /** Encerra lançador e descendente quando não existe progresso observável. */
  @Test
  void terminatesWholeProcessTreeAfterInactivity() throws Exception {
    Path log = Files.createTempFile("psique-stalled-process-", ".log");
    Process process =
        new ProcessBuilder("sh", "-c", "sleep 30 & wait")
            .redirectErrorStream(true)
            .redirectOutput(log.toFile())
            .start();
    List<ProcessHandle> descendants = awaitDescendants(process);
    assertThat(descendants).isNotEmpty();
    CodexProcessSupervisor supervisor =
        new CodexProcessSupervisor(
            Duration.ofMillis(120), Duration.ofSeconds(1), Duration.ofMillis(20));

    CodexProcessSupervisor.WaitOutcome outcome = supervisor.awaitCompletion(process, log);

    assertThat(outcome).isEqualTo(CodexProcessSupervisor.WaitOutcome.INACTIVITY_TIMEOUT);
    assertThat(process.isAlive()).isFalse();
    assertThat(descendants).allSatisfy(child -> assertThat(child.isAlive()).isFalse());
    Files.deleteIfExists(log);
  }

  /** Aplica o teto absoluto com tempo controlado apesar do progresso em todas as observações. */
  @Test
  void terminatesActiveProcessAtAbsoluteTimeout() throws Exception {
    Path log = Files.createTempFile("psique-hard-cap-process-", ".log");
    AtomicLong clock = new AtomicLong();
    Process process = activeProcess(log, clock, Duration.ofSeconds(10));
    CodexProcessSupervisor supervisor =
        new CodexProcessSupervisor(
            Duration.ofMillis(120), Duration.ofMillis(320), Duration.ofMillis(20), clock::get);

    try {
      CodexProcessSupervisor.WaitOutcome outcome = supervisor.awaitCompletion(process, log);
      assertThat(outcome).isEqualTo(CodexProcessSupervisor.WaitOutcome.ABSOLUTE_TIMEOUT);
      assertThat(clock.get()).isEqualTo(supervisor.absoluteTimeout().toNanos());
    } finally {
      Files.deleteIfExists(log);
    }
  }

  /** Simula progresso em arquivo real e avanço monotônico sem sleeps ou comandos de modelo. */
  private Process activeProcess(Path log, AtomicLong clock, Duration completion) throws Exception {
    Process process = mock(Process.class);
    when(process.descendants()).thenAnswer(invocation -> Stream.empty());
    when(process.toHandle()).thenReturn(mock(ProcessHandle.class));
    when(process.waitFor(anyLong(), eq(TimeUnit.NANOSECONDS)))
        .thenAnswer(
            invocation -> {
              long now = clock.addAndGet(invocation.getArgument(0));
              Files.writeString(log, "evento\n", java.nio.file.StandardOpenOption.APPEND);
              return now >= completion.toNanos();
            });
    return process;
  }

  /** Aguarda o shell materializar o filho usado para provar a limpeza recursiva. */
  private List<ProcessHandle> awaitDescendants(Process process) throws InterruptedException {
    for (int attempt = 0; attempt < 50; attempt++) {
      List<ProcessHandle> descendants = process.descendants().toList();
      if (!descendants.isEmpty()) return descendants;
      Thread.sleep(10L);
    }
    return List.of();
  }
}
