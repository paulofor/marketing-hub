package com.marketinghub.landinggeneratoragent;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/** Responsabilidade: conservar a execução e o callback de Dédalo até confirmação do backend. */
final class PdeConstructionOutbox {
  private final Path directory;
  private final ObjectMapper json;

  /** Define armazenamento exclusivo da fila de construção do executor. */
  PdeConstructionOutbox(Path directory, ObjectMapper json) {
    this.directory = directory;
    this.json = json;
  }

  /** Serializa consumidores locais e verifica armazenamento antes de reservar trabalho remoto. */
  Lock lock() throws IOException {
    Files.createDirectories(directory);
    FileChannel channel =
        FileChannel.open(
            directory.resolve("consumer.lock"),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE);
    FileLock lock;
    try {
      lock = channel.tryLock();
    } catch (java.nio.channels.OverlappingFileLockException ex) {
      org.slf4j.LoggerFactory.getLogger(PdeConstructionOutbox.class)
          .debug("Outro consumidor local mantém a reserva; sem nova execução.", ex);
      channel.close();
      return null;
    }
    if (lock == null) {
      channel.close();
      return null;
    }
    return new Lock(channel, lock);
  }

  /** Carrega a execução ainda não confirmada, inclusive após reinício do container. */
  Pending read() throws IOException {
    Path file = directory.resolve("pending.json");
    return Files.exists(file) ? json.readValue(Files.readAllBytes(file), Pending.class) : null;
  }

  /** Grava o estado por troca atômica após sincronizar os bytes com o armazenamento. */
  void save(Pending pending) throws IOException {
    Path temporary = directory.resolve("pending.tmp");
    byte[] bytes = json.writeValueAsBytes(pending);
    try (FileChannel channel =
        FileChannel.open(
            temporary,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE)) {
      ByteBuffer buffer = ByteBuffer.wrap(bytes);
      while (buffer.hasRemaining()) channel.write(buffer);
      channel.force(true);
    }
    Files.move(
        temporary,
        directory.resolve("pending.json"),
        StandardCopyOption.ATOMIC_MOVE,
        StandardCopyOption.REPLACE_EXISTING);
  }

  /** Persiste o pedido antes da rede, para recuperar a mesma reserva após timeout ou reinício. */
  void saveClaim(Map<String, Object> claim) throws IOException {
    Path temporary = directory.resolve("claim.tmp");
    Files.write(temporary, json.writeValueAsBytes(claim));
    try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
      channel.force(true);
    }
    Files.move(
        temporary,
        directory.resolve("claim.json"),
        StandardCopyOption.ATOMIC_MOVE,
        StandardCopyOption.REPLACE_EXISTING);
  }

  /** Lê exclusivamente a identidade local do pedido, sem reconstruir uma tentativa. */
  Map<String, Object> readClaim() throws IOException {
    Path file = directory.resolve("claim.json");
    return Files.exists(file)
        ? json.readValue(
            Files.readAllBytes(file),
            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {})
        : null;
  }

  /** Libera apenas pedido cuja fila respondeu vazia ou callback já foi confirmado. */
  void clearClaim() throws IOException {
    Files.deleteIfExists(directory.resolve("claim.json"));
  }

  /** Preserva PID e início real para não confundir processo original com PID reutilizado. */
  void recordProcess(Process process) throws IOException {
    Files.writeString(
        directory.resolve("process.json"),
        json.writeValueAsString(
            Map.of(
                "pid",
                process.pid(),
                "startedAt",
                process
                    .info()
                    .startInstant()
                    .map(java.time.Instant::toString)
                    .orElseGet(
                        () -> {
                          if (process.isAlive())
                            throw new IllegalStateException(
                                "Processo ativo sem início verificável");
                          return "EXITED";
                        }))));
  }

  /**
   * Confirma que o processo iniciado deixou identidade persistida antes de qualquer recuperação.
   */
  boolean hasProcessIdentity() {
    return Files.exists(directory.resolve("process.json"));
  }

  /** Impede recuperação terminal enquanto o processo original ainda estiver vivo. */
  boolean originalProcessAlive() throws IOException {
    Path file = directory.resolve("process.json");
    if (!Files.exists(file)) return false;
    var identity = json.readTree(Files.readString(file));
    var process = ProcessHandle.of(identity.path("pid").asLong());
    if (process.isEmpty() || !process.get().isAlive()) return false;
    return process
        .get()
        .info()
        .startInstant()
        .map(java.time.Instant::toString)
        .map(identity.path("startedAt").asText()::equals)
        .orElse(true);
  }

  /** Expõe o destino persistente da resposta bruta do modelo. */
  Path output() {
    return directory.resolve("model-result.json");
  }

  /** Expõe o destino persistente dos eventos e tokens reportados pelo modelo. */
  Path events() {
    return directory.resolve("model-events.jsonl");
  }

  /** Remove somente os arquivos desta entrega depois da confirmação canônica. */
  void acknowledge() throws IOException {
    clearClaim();
    Files.deleteIfExists(directory.resolve("process.json"));
    Files.deleteIfExists(output());
    Files.deleteIfExists(events());
    Files.deleteIfExists(directory.resolve("pending.json"));
  }

  /** Preserva snapshot, auditoria e envelope de envio sem recalcular uma resposta já produzida. */
  record Pending(
      Map<String, Object> task,
      Map<String, Object> audit,
      boolean modelStarted,
      String operation,
      Map<String, Object> callback,
      int deliveryRefusals) {
    /** Inicia preservação sem recusa de contrato; ausência legada do contador equivale a zero. */
    Pending(
        Map<String, Object> task,
        Map<String, Object> audit,
        boolean modelStarted,
        String operation,
        Map<String, Object> callback) {
      this(task, audit, modelStarted, operation, callback, 0);
    }

    /** Registra recusa permanente sem modificar os bytes ou iniciar outra inferência. */
    Pending refused() {
      return new Pending(task, audit, modelStarted, operation, callback, deliveryRefusals + 1);
    }
  }

  /** Responsabilidade: liberar a exclusão mútua e seu descritor ao encerrar uma passagem. */
  record Lock(FileChannel channel, FileLock lock) implements AutoCloseable {
    /** Libera o lock mesmo quando a passagem terminou com falha de integração. */
    @Override
    public void close() throws IOException {
      try {
        lock.release();
      } finally {
        channel.close();
      }
    }
  }
}
