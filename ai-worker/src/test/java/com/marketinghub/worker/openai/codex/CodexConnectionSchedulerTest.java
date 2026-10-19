package com.marketinghub.worker.openai.codex;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Confere o consumo local do comando e o encerramento de um autenticador que não responde. */
class CodexConnectionSchedulerTest {
  @TempDir Path directory;

  /** Executa o cliente versionado com a operação solicitada e sem argumentos de credenciais. */
  @Test
  void invokesConnectionClient() throws Exception {
    Path file = directory.resolve("connection.mjs");
    Path result = directory.resolve("operation.txt");
    Files.writeString(file, "import{writeFileSync}from'node:fs';writeFileSync(new URL('./operation.txt',import.meta.url),process.argv[2]);");
    new CodexConnectionScheduler("node", file.toString()).execute("--poll", Duration.ofSeconds(5));
    assertThat(Files.readString(result)).isEqualTo("--poll");
  }

  /** Encerra processo e descendentes dentro do prazo operacional configurado. */
  @Test
  void boundsUnresponsiveProcess() throws Exception {
    Path file = directory.resolve("blocked.mjs");
    Path pid = directory.resolve("pid.txt");
    Files.writeString(file, "import{writeFileSync}from'node:fs';writeFileSync(new URL('./pid.txt',import.meta.url),String(process.pid));setInterval(()=>{},1000);");
    new CodexConnectionScheduler("node", file.toString()).execute("--health", Duration.ofMillis(500));
    ProcessHandle process = ProcessHandle.of(Long.parseLong(Files.readString(pid))).orElse(null);
    if (process != null) process.onExit().get();
    assertThat(process == null || !process.isAlive()).isTrue();
  }
}
