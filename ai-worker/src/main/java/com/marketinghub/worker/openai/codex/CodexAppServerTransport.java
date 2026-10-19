package com.marketinghub.worker.openai.codex;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Executa o bridge stdio do App Server e entrega seu recibo integral ao fluxo existente. */
public final class CodexAppServerTransport {
  private static final Logger log = LoggerFactory.getLogger(CodexAppServerTransport.class);
  private final String node;
  private final String script;
  private final Duration timeout;
  private final ObjectMapper mapper;

  /** Recebe os caminhos versionados e o limite da execução local do App Server. */
  public CodexAppServerTransport(
      String node, String script, Duration timeout, ObjectMapper mapper) {
    this.node = node;
    this.script = script;
    this.timeout = timeout;
    this.mapper = mapper;
  }

  /** Executa uma única requisição; falhas nunca abrem fallback para a API paga. */
  public synchronized String execute(byte[] request) {
    Path output = null;
    Path error = null;
    Process process = null;
    try {
      output = Files.createTempFile("ai-worker-codex-output-", ".json");
      error = Files.createTempFile("ai-worker-codex-error-", ".log");
      process =
          new ProcessBuilder(node, script)
              .redirectOutput(output.toFile())
              .redirectError(error.toFile())
              .start();
      process.getOutputStream().write(request);
      process.getOutputStream().close();
      if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
        throw new IllegalStateException(
            "CODEX_TIMEOUT: execução incerta preservada; não repetir inferência automaticamente");
      }
      String raw = Files.readString(output);
      if (process.exitValue() != 0) {
        String detail =
            Files.readString(error)
                .replaceAll("(?:sk-[\\w-]+|gh[pousr]_[\\w]+|github_pat_[\\w]+)", "[REDACTED]");
        log.error(
            "Falha do Codex App Server; operation=responses; exitCode={}; detail={}; rawResponse={}",
            process.exitValue(),
            detail,
            raw);
        String operationalDetail =
            detail.length() <= 1000
                ? detail
                : "Falha do App Server; diagnóstico integral preservado no log do job.";
        throw new IllegalStateException(
            "Codex App Server: "
                + operationalDetail
                + (raw.isBlank() ? "" : "; recibo persistido para o mesmo job."));
      }
      if (!"completed".equals(mapper.readTree(raw).path("status").asText())) {
        throw new IllegalStateException("Codex não entregou um recibo concluído");
      }
      return raw;
    } catch (IOException ex) {
      log.error("Falha de I/O do Codex App Server; operation=responses; script={}", script, ex);
      throw new IllegalStateException("Falha local no transporte Codex", ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.error("Execução Codex interrompida; operation=responses; script={}", script, ex);
      throw new IllegalStateException("Execução Codex interrompida; recibo incerto preservado", ex);
    } finally {
      if (process != null && process.isAlive()) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
      }
      delete(output);
      delete(error);
    }
  }

  /** Remove somente arquivos temporários desta chamada; os recibos persistentes são preservados. */
  private void delete(Path path) {
    if (path == null) return;
    try {
      Files.deleteIfExists(path);
    } catch (IOException ex) {
      log.warn("Falha ao remover arquivo temporário Codex; path={}", path, ex);
    }
  }
}
