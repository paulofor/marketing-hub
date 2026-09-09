package com.marketinghub.experimentstrategistworker.learningcyclev1.decision;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experimentstrategistworker.WorkerProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Responsabilidade: executar somente a redação estruturada da proposta comercial de Atena. */
@Component
public class LearningCycleDecisionRunner {
  private static final Logger log = LoggerFactory.getLogger(LearningCycleDecisionRunner.class);
  private final WorkerProperties properties;
  private final ObjectMapper json;

  /** Recebe o harness existente sem criar provedor, credencial ou modelo independente. */
  public LearningCycleDecisionRunner(WorkerProperties properties, ObjectMapper json) {
    this.properties = properties;
    this.json = json;
  }

  /** Compõe o request auditável a partir dos arquivos versionados e do contexto congelado. */
  public Prepared prepare(JsonNode context) throws IOException {
    String prompt =
        read("prompts/experiment-strategist/v1/agent-core.md")
            + "\n\n"
            + read("prompts/learning-cycle/v1/decision.md")
                .replace("{{CYCLE_CONTEXT}}", json.writeValueAsString(context));
    return new Prepared(
        prompt,
        json.readTree(read("prompts/learning-cycle/v1/decision-schema.json")),
        properties.getModel() == null || properties.getModel().isBlank()
            ? "codex-default"
            : properties.getModel());
  }

  /** Executa em diretório temporário sem fontes extras; devolve resposta bruta mesmo em falha. */
  public Result run(Prepared prepared) throws IOException, InterruptedException {
    Path work = Files.createTempDirectory("atena-cycle-decision-");
    Path schema = work.resolve("schema.json"),
        answer = work.resolve("answer.json"),
        events = work.resolve("events.jsonl");
    Process process = null;
    try {
      Files.writeString(schema, json.writeValueAsString(prepared.schema()));
      process =
          new ProcessBuilder(command(work, schema, answer))
              .redirectErrorStream(true)
              .redirectOutput(events.toFile())
              .start();
      process.getOutputStream().write(prepared.prompt().getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      boolean completed =
          process.waitFor(
              Math.min(properties.getCodexTimeout().toMillis(), TimeUnit.MINUTES.toMillis(45)),
              TimeUnit.MILLISECONDS);
      String error =
          !completed
              ? "Atena excedeu o tempo de execução da proposta."
              : process.exitValue() != 0
                  ? "O harness de Atena encerrou com código " + process.exitValue() + "."
                  : null;
      if (!completed) stop(process);
      Long input = null, output = null;
      if (Files.exists(events)) {
        try (var lines = Files.lines(events)) {
          for (String line : lines.toList()) {
            try {
              JsonNode event = json.readTree(line);
              if ("turn.completed".equals(event.path("type").asText())) {
                JsonNode usage = event.path("usage");
                if (usage.path("input_tokens").isIntegralNumber())
                  input = usage.path("input_tokens").asLong();
                if (usage.path("output_tokens").isIntegralNumber())
                  output = usage.path("output_tokens").asLong();
              }
            } catch (IOException ex) {
              log.warn(
                  "Atena: evento de telemetria ilegível na proposta; consumo permanece desconhecido. arquivo={}",
                  events.getFileName(),
                  ex);
            }
          }
        }
      }
      return new Result(
          Files.exists(answer) ? Files.readString(answer) : null, error, input, output);
    } finally {
      if (process != null && process.isAlive()) stop(process);
      Files.deleteIfExists(schema);
      Files.deleteIfExists(answer);
      Files.deleteIfExists(events);
      Files.deleteIfExists(work);
    }
  }

  /**
   * Fixa sandbox de leitura, schema e tier do harness OAuth, sem ferramentas para executar ações.
   */
  List<String> command(Path work, Path schema, Path answer) {
    List<String> args =
        new ArrayList<>(
            List.of(
                properties.getCodexCommand(),
                "exec",
                "-",
                "--ignore-user-config",
                "--ephemeral",
                "--skip-git-repo-check",
                "--sandbox",
                "read-only",
                "--cd",
                work.toString(),
                "--output-schema",
                schema.toString(),
                "--output-last-message",
                answer.toString(),
                "--json",
                "--color",
                "never",
                "--config",
                "approval_policy=\"never\"",
                "--config",
                "service_tier=\"default\"",
                "--config",
                "web_search=\"disabled\"",
                "--config",
                "features.shell_tool=false",
                "--config",
                "features.apps=false",
                "--config",
                "features.multi_agent=false",
                "--config",
                "features.hooks=false",
                "--config",
                "features.shell_snapshot=false",
                "--config",
                "tools.view_image=false",
                "--config",
                "mcp_servers={}",
                "--config",
                "model_reasoning_effort=\"" + properties.requiredReasoningEffort() + "\""));
    if (properties.getModel() != null && !properties.getModel().isBlank())
      args.addAll(List.of("--model", properties.getModel()));
    return args;
  }

  /** Encerra o processo e seus descendentes ao abandonar uma tentativa. */
  private void stop(Process process) {
    process.descendants().forEach(ProcessHandle::destroyForcibly);
    process.destroyForcibly();
  }

  /** Lê o recurso operacional sem embutir prompt ou schema no código. */
  private String read(String resource) throws IOException {
    try (var stream = new ClassPathResource(resource).getInputStream()) {
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Representa o request completo que precisa ser persistido antes da inferência. */
  public record Prepared(String prompt, JsonNode schema, String model) {}

  /** Preserva resposta, erro e consumo observado sem presumir sucesso funcional. */
  public record Result(String rawResponse, String error, Long inputTokens, Long outputTokens) {}
}
