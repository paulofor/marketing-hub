package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
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

  /** Mantém viva uma execução que continua produzindo eventos até concluir normalmente. */
  @Test
  void keepsProcessAliveWhileJsonlAdvances() throws Exception {
    Path log = Files.createTempFile("psique-active-process-", ".log");
    Process process =
        new ProcessBuilder(
                "sh", "-c", "for value in 1 2 3 4 5; do echo event-$value; sleep 0.05; done")
            .redirectErrorStream(true)
            .redirectOutput(log.toFile())
            .start();
    CodexProcessSupervisor supervisor =
        new CodexProcessSupervisor(
            Duration.ofMillis(130), Duration.ofSeconds(2), Duration.ofMillis(20));

    CodexProcessSupervisor.WaitOutcome outcome = supervisor.awaitCompletion(process, log);

    assertThat(outcome).isEqualTo(CodexProcessSupervisor.WaitOutcome.COMPLETED);
    assertThat(process.isAlive()).isFalse();
    Files.deleteIfExists(log);
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

  /** Aplica o teto absoluto mesmo quando uma execução continua emitindo eventos. */
  @Test
  void terminatesActiveProcessAtAbsoluteTimeout() throws Exception {
    Path log = Files.createTempFile("psique-hard-cap-process-", ".log");
    Process process =
        new ProcessBuilder("sh", "-c", "while true; do echo event; sleep 0.03; done")
            .redirectErrorStream(true)
            .redirectOutput(log.toFile())
            .start();
    CodexProcessSupervisor supervisor =
        new CodexProcessSupervisor(
            Duration.ofMillis(120), Duration.ofMillis(320), Duration.ofMillis(20));

    CodexProcessSupervisor.WaitOutcome outcome = supervisor.awaitCompletion(process, log);

    assertThat(outcome).isEqualTo(CodexProcessSupervisor.WaitOutcome.ABSOLUTE_TIMEOUT);
    assertThat(process.isAlive()).isFalse();
    Files.deleteIfExists(log);
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
