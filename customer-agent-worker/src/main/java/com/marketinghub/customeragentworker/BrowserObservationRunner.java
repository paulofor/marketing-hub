package com.marketinghub.customeragentworker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Responsabilidade: observar fontes autorizadas em navegador mobile sem executar mutações. */
@Component
public class BrowserObservationRunner {
  private final ObjectMapper mapper;
  private final String nodeBinary;
  private final String scriptPath;

  /** Inicializa o executor determinístico do navegador e seus caminhos versionados. */
  public BrowserObservationRunner(
      ObjectMapper mapper,
      @Value("${CUSTOMER_AGENT_NODE_BIN:node}") String nodeBinary,
      @Value("${CUSTOMER_AGENT_BROWSER_SCRIPT:/app/browser/mobile-observation.mjs}")
          String scriptPath) {
    this.mapper = mapper;
    this.nodeBinary = nodeBinary;
    this.scriptPath = scriptPath;
  }

  /** Valida as fontes públicas, executa a emulação mobile e devolve fatos com screenshots. */
  public BrowserObservation observe(String authorizedSourcesJson, Path workDirectory)
      throws Exception {
    List<String> urls = mapper.readValue(authorizedSourcesJson, new TypeReference<>() {});
    if (urls.isEmpty()) throw new IllegalArgumentException("Nenhuma fonte autorizada informada.");
    for (String url : urls) validatePublicUrl(url);

    Files.createDirectories(workDirectory);
    Path input = workDirectory.resolve("browser-input.json");
    Path output = workDirectory.resolve("browser-output.json");
    Path evidence = workDirectory.resolve("evidence");
    Files.writeString(
        input, mapper.writeValueAsString(Map.of("urls", urls)), StandardCharsets.UTF_8);
    Process process =
        new ProcessBuilder(
                nodeBinary, scriptPath, input.toString(), output.toString(), evidence.toString())
            .redirectErrorStream(true)
            .redirectOutput(workDirectory.resolve("browser.log").toFile())
            .start();
    if (!process.waitFor(3, TimeUnit.MINUTES)) {
      process.destroyForcibly();
      throw new IllegalStateException("Timeout da navegação mobile.");
    }
    if (process.exitValue() != 0) {
      throw new IllegalStateException(
          "Navegação mobile falhou: "
              + Files.readString(workDirectory.resolve("browser.log"), StandardCharsets.UTF_8));
    }
    Map<String, Object> facts = mapper.readValue(output.toFile(), new TypeReference<>() {});
    List<Path> screenshots;
    try (var paths = Files.list(evidence)) {
      screenshots = paths.filter(Files::isRegularFile).sorted().toList();
    }
    return new BrowserObservation(facts, screenshots);
  }

  /** Bloqueia esquemas, credenciais e destinos privados antes de abrir o navegador. */
  private void validatePublicUrl(String value) throws Exception {
    URI uri = URI.create(value);
    if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
        || uri.getHost() == null
        || uri.getUserInfo() != null) {
      throw new IllegalArgumentException("Fonte pública inválida: " + value);
    }
    for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
      if (address.isAnyLocalAddress()
          || address.isLoopbackAddress()
          || address.isLinkLocalAddress()
          || address.isSiteLocalAddress()) {
        throw new IllegalArgumentException("Fonte privada não autorizada: " + value);
      }
    }
  }

  /** Representa fatos determinísticos e evidências geradas pela mesma sessão. */
  public record BrowserObservation(Map<String, Object> facts, List<Path> screenshots) {}
}
