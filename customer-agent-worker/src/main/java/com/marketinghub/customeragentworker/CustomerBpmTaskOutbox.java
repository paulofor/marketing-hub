package com.marketinghub.customeragentworker;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

/** Responsabilidade: preservar a execução BPM de Psique até o backend confirmar seu callback. */
final class CustomerBpmTaskOutbox {
  private final Path directory;
  private final ObjectMapper json;

  /** Define o armazenamento persistente e exclusivo da fila BPM de Psique. */
  CustomerBpmTaskOutbox(Path directory, ObjectMapper json) {
    this.directory = directory;
    this.json = json;
  }

  /** Serializa o consumidor local antes de consultar ou alterar a reserva remota. */
  Lock lock() throws IOException {
    Files.createDirectories(directory);
    FileChannel channel =
        FileChannel.open(
            directory.resolve("consumer.lock"),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE);
    FileLock lock = channel.tryLock();
    if (lock == null) {
      channel.close();
      return null;
    }
    return new Lock(channel, lock);
  }

  /** Carrega a execução não confirmada, inclusive depois do reinício do container. */
  Pending read() throws IOException {
    Path file = directory.resolve("pending.json");
    return Files.exists(file) ? json.readValue(Files.readAllBytes(file), Pending.class) : null;
  }

  /** Grava o estado por troca atômica depois de sincronizar os bytes no volume. */
  void save(Pending pending) throws IOException {
    Files.createDirectories(directory);
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

  /** Expõe o resultado bruto persistente da inferência atual. */
  Path output() {
    return directory.resolve("model-result.json");
  }

  /** Expõe os eventos persistentes que contêm progresso, fontes e consumo informado. */
  Path events() {
    return directory.resolve("model-events.jsonl");
  }

  /** Remove somente a entrega já confirmada pelo backend. */
  void acknowledge() throws IOException {
    Files.deleteIfExists(output());
    Files.deleteIfExists(events());
    Files.deleteIfExists(directory.resolve("pending.json"));
  }

  /** Preserva entrada, auditoria, provas e callback sem recomputar uma revisão paga. */
  record Pending(
      Map<String, Object> task,
      Map<String, Object> audit,
      List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> visualEvidence,
      boolean modelStarted,
      String operation,
      Map<String, Object> callback,
      int deliveryAttempts) {
    /** Mantém compatibilidade com envelopes criados antes do contador de entrega. */
    Pending(
        Map<String, Object> task,
        Map<String, Object> audit,
        List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> visualEvidence,
        boolean modelStarted,
        String operation,
        Map<String, Object> callback) {
      this(task, audit, visualEvidence, modelStarted, operation, callback, 0);
    }

    /** Normaliza coleções opcionais para manter o contrato legível depois da desserialização. */
    Pending {
      visualEvidence = visualEvidence == null ? List.of() : List.copyOf(visualEvidence);
      if (deliveryAttempts < 0) deliveryAttempts = 0;
    }
  }

  /** Responsabilidade: liberar o lock e seu descritor ao terminar uma passagem. */
  record Lock(FileChannel channel, FileLock lock) implements AutoCloseable {
    /** Libera a exclusão mútua mesmo quando a integração terminou com falha. */
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
