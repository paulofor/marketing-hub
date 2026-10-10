package com.marketinghub.experimentstrategistworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Responsabilidade: comprovar interoperabilidade do lock Java com o publicador Python real. */
class PdeMarketStrategyPublicationLockTest {
  @TempDir Path state;

  /** Garante que o publicador aguarde a entrega completa, sem substituir uma inferência ativa. */
  @Test
  void publisherWaitsForJavaConsumerBeforeRunningDeployment() throws Exception {
    var outbox = new PdeMarketStrategyOutbox(state, new ObjectMapper());
    var lock = outbox.lock();
    assertThat(lock).isNotNull();
    var helper = Path.of("../scripts/with-agent-consumer-lock.py").toAbsolutePath();
    var process =
        new ProcessBuilder(
                "python3",
                helper.toString(),
                "--state-directory",
                state.toString(),
                "--revision",
                "a".repeat(40),
                "--wait-seconds",
                "5",
                "--",
                "python3",
                "-c",
                "import pathlib,sys;p=pathlib.Path(sys.argv[1]);assert (p/'result.json').exists();(p/'published').touch()",
                state.toString())
            .redirectErrorStream(true)
            .start();
    try {
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
      while (!Files.exists(state.resolve("publisher-pause.json")) && System.nanoTime() < deadline)
        Thread.sleep(20);
      assertThat(Files.exists(state.resolve("publisher-pause.json"))).isTrue();
      assertThat(process.isAlive()).isTrue();
      assertThat(Files.exists(state.resolve("published"))).isFalse();
      Files.writeString(state.resolve("result.json"), "{\"decision\":\"APPROVE\"}");
      lock.close();
      lock = null;
      assertThat(process.waitFor(5, TimeUnit.SECONDS)).isTrue();
      assertThat(process.exitValue())
          .as(new String(process.getInputStream().readAllBytes()))
          .isZero();
      assertThat(Files.exists(state.resolve("published"))).isTrue();
      assertThat(Files.exists(state.resolve("publisher-pause.json"))).isFalse();
    } finally {
      if (lock != null) lock.close();
      if (process.isAlive()) process.destroyForcibly();
    }
  }
}
