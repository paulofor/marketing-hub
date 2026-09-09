package com.marketinghub.experimentstrategistworker;

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

/** Responsabilidade: conservar a execução e o callback de Atena até confirmação do backend. */
final class PdeMarketStrategyOutbox {
  private final Path directory;
  private final ObjectMapper json;

  /** Define armazenamento exclusivo da fila de estratégia do executor. */
  PdeMarketStrategyOutbox(Path directory, ObjectMapper json) {
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
    FileLock lock = channel.tryLock();
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
      Map<String, Object> callback) {}

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
