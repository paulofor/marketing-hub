package com.marketinghub.experimentstrategistworker;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.slf4j.*;
import org.springframework.stereotype.Component;

/** Conserva recibos da pesquisa de Big Idea e recupera callbacks sem repetir o modelo. */
@Component
public class BigIdeaResearchDelivery {
  private static final Logger log = LoggerFactory.getLogger(BigIdeaResearchDelivery.class);
  private final StrategistBackendClient backend;
  private final CodexStrategistRunner runner;
  private final ObjectMapper json;
  private final Path directory;
  private final PdeMarketStrategyOutbox outbox;

  /** Reutiliza o armazenamento durável de Atena em um diretório exclusivo desta finalidade. */
  public BigIdeaResearchDelivery(
      StrategistBackendClient backend,
      CodexStrategistRunner runner,
      WorkerProperties properties,
      ObjectMapper json) {
    this.backend = backend;
    this.runner = runner;
    this.json = json;
    directory = Path.of(properties.getBpmStateDirectory()).resolve("big-idea-research");
    outbox = new PdeMarketStrategyOutbox(directory, json);
  }

  /** Reenvia resultado anterior antes de reservar outra pesquisa. */
  public boolean replay() throws IOException {
    try (var lock = outbox.lock()) {
      if (lock == null) return true;
      var pending = outbox.read();
      if (pending == null) return false;
      deliver(pending, true);
      return true;
    }
  }

  /** Reserva armazenamento antes da inferência e conserva bytes e consumo até confirmação. */
  public void start(StrategistJob job) throws IOException {
    try (var lock = outbox.lock()) {
      if (lock == null || outbox.read() != null)
        throw new IllegalStateException("Recibo anterior ainda pendente.");
      Map<String, Object> task = json.convertValue(job, Map.class);
      var pending =
          new PdeMarketStrategyOutbox.Pending(
              task, Map.of("callbackAttempts", 0), false, "MODEL", null);
      outbox.save(pending);
      deliver(pending, false);
    }
  }

  /** Executa uma vez ou recupera os mesmos arquivos; falha sem saída não autoriza nova chamada. */
  private void deliver(PdeMarketStrategyOutbox.Pending pending, boolean recovering)
      throws IOException {
    StrategistJob job = json.convertValue(pending.task(), StrategistJob.class);
    if (pending.callback() == null) {
      try {
        Map<String, Object> callback;
        if (pending.modelStarted()) {
          if (!Files.isRegularFile(outbox.output()))
            throw new IllegalStateException(
                "Execução interrompida sem resultado; não repetir inferência automaticamente.");
          callback = runner.payload(job, outbox.output(), outbox.events());
        } else {
          if (recovering)
            throw new IllegalStateException(
                "Pesquisa interrompida antes da inferência; conferir antes de retomar.");
          outbox.save(
              new PdeMarketStrategyOutbox.Pending(
                  pending.task(), pending.audit(), true, "MODEL", null));
          callback = runner.run(job, outbox.output(), outbox.events());
        }
        pending =
            new PdeMarketStrategyOutbox.Pending(
                pending.task(), pending.audit(), true, "COMPLETE", callback);
      } catch (Exception ex) {
        log.error("Falha na pesquisa Big Idea; recibos preservados. executionId={}", job.id(), ex);
        pending =
            new PdeMarketStrategyOutbox.Pending(
                pending.task(),
                pending.audit(),
                true,
                "FAIL",
                Map.of("errorMessage", ex.toString()));
      }
      outbox.save(pending);
    }
    try {
      if ("COMPLETE".equals(pending.operation())) backend.complete(job.id(), pending.callback());
      else backend.fail(job.id(), String.valueOf(pending.callback().get("errorMessage")));
      archive(job.id());
    } catch (Exception ex) {
      log.error("Falha no callback Big Idea; recuperação sem modelo. executionId={}", job.id(), ex);
      int attempts = ((Number) pending.audit().getOrDefault("callbackAttempts", 0)).intValue() + 1;
      outbox.save(
          new PdeMarketStrategyOutbox.Pending(
              pending.task(),
              Map.of("callbackAttempts", attempts),
              true,
              pending.operation(),
              pending.callback()));
      if (attempts >= 2) {
        backend.fail(
            job.id(),
            "Callback da Big Idea recusado duas vezes; resposta e consumo preservados no executor. "
                + ex.getClass().getSimpleName());
        archive(job.id());
      }
    }
  }

  /** Guarda recibo encerrado para auditoria e libera somente a posição ativa da fila. */
  private void archive(Long executionId) throws IOException {
    Path archive = directory.resolve("receipts").resolve(executionId.toString());
    Files.createDirectories(archive);
    for (String name :
        List.of("pending.json", "model-result.json", "model-events.jsonl", "model-prompt.md")) {
      Path source = directory.resolve(name);
      if (Files.exists(source))
        Files.copy(source, archive.resolve(name), StandardCopyOption.REPLACE_EXISTING);
    }
    outbox.acknowledge();
    Files.deleteIfExists(directory.resolve("model-prompt.md"));
  }
}
