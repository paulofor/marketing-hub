package com.marketinghub.pde.harness.v1.internal;

import com.marketinghub.pde.harness.v1.PdeHarnessConfiguration;
import com.marketinghub.pde.harness.v1.PdeHarnessException;
import com.marketinghub.pde.harness.v1.PdeHarnessFailureCategory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Confirma a versão do binário Codex antes de abrir o protocolo experimental. */
public final class CodexVersionVerifier {
  private static final System.Logger LOGGER =
      System.getLogger(CodexVersionVerifier.class.getName());

  /** Prepara o CODEX_HOME, executa `codex --version` e bloqueia versão diferente do bundle. */
  public void verify(PdeHarnessConfiguration configuration) {
    if (!configuration.verifyCodexVersion()) {
      return;
    }
    Process process = null;
    try {
      Files.createDirectories(configuration.codexHome());
      ArrayList<String> command = new ArrayList<>();
      command.add(configuration.codexCommand());
      command.add("--version");
      ProcessBuilder builder = new ProcessBuilder(command);
      sanitizeEnvironment(builder.environment(), configuration);
      process = builder.start();
      Duration ceiling =
          configuration.requestTimeout().compareTo(Duration.ofSeconds(10)) < 0
              ? configuration.requestTimeout()
              : Duration.ofSeconds(10);
      if (!process.waitFor(ceiling.toMillis(), TimeUnit.MILLISECONDS)) {
        process.destroyForcibly();
        throw new PdeHarnessException(
            PdeHarnessFailureCategory.APP_SERVER_UNAVAILABLE,
            "Tempo esgotado ao consultar a versão do Codex");
      }
      String output =
          new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
      String diagnostics =
          new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();
      if (process.exitValue() != 0) {
        PdeHarnessException failure =
            new PdeHarnessException(
                PdeHarnessFailureCategory.APP_SERVER_UNAVAILABLE,
                "Codex --version encerrou com código " + process.exitValue());
        LOGGER.log(
            System.Logger.Level.ERROR,
            "Falha ao consultar versão Codex; diagnóstico=" + SecretSanitizer.sanitize(diagnostics),
            failure);
        throw failure;
      }
      String actualVersion = parseVersion(output);
      if (!configuration.expectedCodexVersion().equals(actualVersion)) {
        throw new PdeHarnessException(
            PdeHarnessFailureCategory.PROTOCOL_INCOMPATIBLE,
            "Codex "
                + actualVersion
                + " diverge do bundle "
                + configuration.expectedCodexVersion());
      }
    } catch (IOException ex) {
      LOGGER.log(System.Logger.Level.ERROR, "Falha ao executar codex --version", ex);
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.APP_SERVER_UNAVAILABLE,
          "Não foi possível executar o binário Codex",
          ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      LOGGER.log(System.Logger.Level.ERROR, "Consulta da versão Codex foi interrompida", ex);
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.APP_SERVER_UNAVAILABLE,
          "Consulta da versão Codex foi interrompida",
          ex);
    } finally {
      if (process != null && process.isAlive()) {
        process.destroyForcibly();
      }
    }
  }

  /** Extrai a versão do formato estável `codex-cli x.y.z`. */
  private String parseVersion(String output) {
    String prefix = "codex-cli ";
    if (!output.startsWith(prefix) || output.length() == prefix.length()) {
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.PROTOCOL_INCOMPATIBLE, "Saída inesperada de codex --version");
    }
    return output.substring(prefix.length()).trim();
  }

  /** Remove chaves de API e fixa o CODEX_HOME sem registrar valores sensíveis. */
  private void sanitizeEnvironment(
      Map<String, String> environment, PdeHarnessConfiguration configuration) {
    environment.keySet().removeIf(PdeHarnessConfiguration::isForbiddenEnvironmentKey);
    environment.putAll(configuration.environmentOverrides());
    environment.put("CODEX_HOME", configuration.codexHome().toString());
  }
}
