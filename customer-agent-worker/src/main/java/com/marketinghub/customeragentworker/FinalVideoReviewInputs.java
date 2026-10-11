package com.marketinghub.customeragentworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Entrega a Psique frames temporais do MP4 final e exige parecer vinculado à mesma mídia. */
@Component
public class FinalVideoReviewInputs {
  private final ObjectMapper json;
  private final String node;
  private final String script;

  /** Configura o capturador versionado sem fornecer ao modelo autoridade de produção. */
  public FinalVideoReviewInputs(
      ObjectMapper json,
      @Value("${CUSTOMER_AGENT_NODE_BIN:node}") String node,
      @Value("${CUSTOMER_AGENT_FINAL_VIDEO_SCRIPT:/app/browser/final-video-review.mjs}")
          String script) {
    this.json = json;
    this.node = node;
    this.script = script;
  }

  /** Obtém somente o contrato audiovisual explicitamente recebido do backend nesta tarefa. */
  static JsonNode inputs(ObjectMapper json, Map<String, Object> task) {
    return json.valueToTree(task.get("taskTarget")).at("/pdeContext/finalVideoReview");
  }

  /** Captura antes da inferência e preserva logs quando não for possível inspecionar os vídeos. */
  List<BpmVisualEvidenceRunner.VisualArtifact> capture(Map<String, Object> task, Path directory)
      throws Exception {
    JsonNode inputs = inputs(json, task);
    if (inputs.isMissingNode() || inputs.isEmpty()) return List.of();
    if (!inputs.isArray() || inputs.size() > 2)
      throw new IllegalArgumentException("Contrato audiovisual inválido.");
    for (JsonNode media : inputs)
      BpmVisualEvidenceRunner.validatePublicUrl(media.path("url").asText());
    Path input = directory.resolve("video-input.json"),
        output = directory.resolve("video-frames.json");
    Files.writeString(input, inputs.toString());
    Path log = directory.resolve("video-capture.log");
    Process process =
        new ProcessBuilder(node, script, input.toString(), output.toString(), directory.toString())
            .redirectErrorStream(true)
            .redirectOutput(log.toFile())
            .start();
    if (!process.waitFor(180, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      throw new IllegalStateException("Timeout da inspeção dos vídeos finais, sem iniciar modelo.");
    }
    if (process.exitValue() != 0 || !Files.isRegularFile(output))
      throw new IllegalStateException("Falha ao capturar vídeo final: " + Files.readString(log));
    var artifacts =
        Arrays.asList(
            json.readValue(output.toFile(), BpmVisualEvidenceRunner.VisualArtifact[].class));
    if (artifacts.size() < inputs.size() * 6)
      throw new IllegalStateException("Sequência temporal incompleta.");
    for (var artifact : artifacts) {
      Path file = Path.of(artifact.localPath());
      if (!"VIDEO_FRAME".equals(artifact.evidenceType())
          || !file.toRealPath().startsWith(directory.toRealPath()))
        throw new IllegalStateException("Frame fora do diretório ou tipo autorizado.");
    }
    return List.copyOf(artifacts);
  }

  /** Impede que o parecer da imagem estática ou de outra edição aprove o vídeo atual. */
  static void validate(ObjectMapper json, Map<String, Object> task, JsonNode result) {
    JsonNode inputs = inputs(json, task);
    if (inputs.isMissingNode() || inputs.isEmpty()) return;
    for (JsonNode modality : result.at("/behavioralResponse/sensoryExperience/availableModalities")) {
      if ("AUDIO".equals(modality.asText()))
        throw new IllegalArgumentException("Frames e transcrição não comprovam audição de áudio.");
    }
    JsonNode audit = result.path("videoAudit");
    if (!audit.isArray() || audit.size() != inputs.size())
      throw new IllegalArgumentException("Psique deve auditar cada vídeo final recebido.");
    var seen = new HashSet<Long>();
    for (JsonNode media : inputs) {
      JsonNode item = null;
      for (JsonNode candidate : audit)
        if (candidate.path("assetId").asLong() == media.path("assetId").asLong()) item = candidate;
      if (item == null
          || !seen.add(item.path("assetId").asLong())
          || !media.path("sha256").asText().equals(item.path("sha256").asText())
          || item.path("evidence").asText().isBlank()
          || !"NOT_LISTENED".equals(item.path("audioAssessment").asText()))
        throw new IllegalArgumentException(
            "Parecer audiovisual sem identidade, evidência ou limite de áudio correto.");
      if ("APPROVED".equals(result.path("decision").asText())) {
        if (item.path("criticalDeficit").asBoolean(true))
          throw new IllegalArgumentException("Déficit audiovisual crítico impede aprovação.");
        for (String criterion :
            List.of("desire", "clarity", "firstAction", "proofLegibility", "visualProgression"))
          if (item.path(criterion).asInt(-1) < 3 || item.path(criterion).asInt() > 5)
            throw new IllegalArgumentException("Critério audiovisual insuficiente: " + criterion);
      }
    }
  }
}
