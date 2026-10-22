package com.marketinghub.landinggeneratoragent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Responsabilidade: produzir a prova comercial do PDE sem gerar nova copy, mídia sintética ou
 * inferência.
 */
final class OpalaCommercialUiProofCapture {
  private static final Logger log = LoggerFactory.getLogger(OpalaCommercialUiProofCapture.class);
  static final String CONTRACT = "OPALA_COMMERCIAL_UI_PROOF_V1";
  private final RestClient backend;
  private final ObjectMapper json;
  private final CaptureCommand captureCommand;

  /** Recebe somente o transporte oficial e o serializador do executor Dédalo. */
  OpalaCommercialUiProofCapture(RestClient backend, ObjectMapper json) {
    this(backend, json, OpalaCommercialUiProofCapture::runNode);
  }

  /** Permite testar transporte e recuperação com captura local determinística injetada. */
  OpalaCommercialUiProofCapture(
      RestClient backend, ObjectMapper json, CaptureCommand captureCommand) {
    this.backend = backend;
    this.json = json;
    this.captureCommand = captureCommand;
  }

  /** Identifica a captura expressamente preparada pelo backend para a atividade criativa. */
  JsonNode input(Map<String, Object> task) throws Exception {
    if (!"opala-commercial-preparation-v1".equals(task.get("processCode"))
        || !"creative".equals(task.get("activityId"))) return null;
    JsonNode proof =
        json.readTree(String.valueOf(task.get("processContextJson")))
            .path("opalaCommercial")
            .path("staticProofPreparation");
    return CONTRACT.equals(proof.path("contractVersion").asText()) ? proof : null;
  }

  /**
   * Captura, persiste o PNG e conclui apenas a preparação; o backend decide a próxima atividade.
   */
  void complete(Map<String, Object> task, JsonNode input) throws Exception {
    long taskId = ((Number) task.get("taskId")).longValue();
    var fullInput = input.deepCopy();
    ((com.fasterxml.jackson.databind.node.ObjectNode) fullInput).put("taskId", taskId);
    var audit =
        Map.of(
            "executionMode",
            "DETERMINISTIC",
            "modelCode",
            CONTRACT,
            "reasoningEffort",
            "NOT_APPLICABLE",
            "promptSent",
            json.writeValueAsString(fullInput),
            "activityPromptPart",
            json.writeValueAsString(fullInput),
            "accessedUrls",
            java.util.List.of());
    String prefix = "/api/internal/agent-tasks/landing-generator/stage-executions/" + taskId;
    backend.put().uri(prefix + "/execution-audit").body(audit).retrieve().toBodilessEntity();
    Path directory = Files.createTempDirectory("opala-ui-proof-" + taskId + "-");
    Map<String, Object> terminalPayload = null;
    try {
      Path script = directory.resolve("capture.mjs"),
          source = directory.resolve("input.json"),
          png = directory.resolve("proof.png"),
          receiptFile = directory.resolve("receipt.json"),
          processLog = directory.resolve("capture.log");
      Files.write(
          script,
          new ClassPathResource("proofs/opala/v1/capture-ui-proof.mjs").getContentAsByteArray());
      Files.writeString(source, json.writeValueAsString(fullInput));
      captureCommand.run(script, source, png, receiptFile, processLog);
      var receipt =
          (com.fasterxml.jackson.databind.node.ObjectNode)
              json.readTree(Files.readString(receiptFile));
      var multipart = new LinkedMultiValueMap<String, Object>();
      for (String key :
          java.util.List.of(
              "captureSessionId",
              "evidenceKey",
              "evidenceType",
              "deviceProfile",
              "pageNumber",
              "foldNumber",
              "viewportWidth",
              "viewportHeight",
              "pageHeightPx",
              "scrollY",
              "sourceUrl",
              "finalUrl",
              "capturedAt")) multipart.add(key, receipt.path(key).asText());
      multipart.add("file", new FileSystemResource(png));
      var uploaded =
          backend
              .post()
              .uri(prefix + "/visual-evidence")
              .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
              .body(multipart)
              .retrieve()
              .body(JsonNode.class);
      if (uploaded == null
          || !receipt.path("sha256").asText().equals(uploaded.path("sha256").asText()))
        throw new IllegalStateException("O backend não confirmou os mesmos pixels da captura.");
      receipt.put("artifactId", uploaded.path("id").asLong());
      var evidence = json.createObjectNode();
      evidence.set(
          "opalaScope",
          json.readTree(String.valueOf(task.get("processContextJson"))).path("opalaCommercial"));
      evidence.set("commercialUiProof", receipt);
      var result =
          json.createObjectNode()
              .put("decision", "READY")
              .put(
                  "rationale",
                  "Prova atual do primeiro valor capturada com dados de teste e copy de Íris preservada.");
      result
          .putArray("alternatives")
          .add("Reutilizar pixels privados")
          .add("Gerar nova mídia")
          .add("Capturar o PDE real");
      result.put(
          "selectedApproach",
          "Captura determinística do primeiro valor da candidata comercial, sem nova inferência.");
      result
          .putArray("acceptanceCriteria")
          .add(
              "Identidade, primeiro valor local e pixels conferidos; revisão comercial própria pendente.");
      var instruction = result.putObject("instruction");
      instruction.putNull("videoAssetId");
      for (String key : java.util.List.of("headline", "primaryText", "description"))
        instruction.put(key, input.path("copy").path(key).asText());
      instruction.putArray("targetingElementIds");
      terminalPayload =
          Map.of(
              "resultJson",
              json.writeValueAsString(result),
              "evidenceJson",
              json.writeValueAsString(evidence),
              "executionAudit",
              audit,
              "modelUsages",
              java.util.List.of());
      backend.post().uri(prefix + "/result").body(terminalPayload).retrieve().toBodilessEntity();
    } catch (Exception ex) {
      log.error("Falha na captura comercial determinística. taskId={}", taskId, ex);
      Map<String, Object> failure = new java.util.LinkedHashMap<>();
      failure.put("error", "Captura comercial não concluída: " + ex.getMessage());
      failure.put(
          "evidenceJson",
          terminalPayload == null
              ? json.writeValueAsString(
                  Map.of(
                      "commercialUiProofFailure",
                      Map.of(
                          "errorType",
                          ex.getClass().getSimpleName(),
                          "message",
                          String.valueOf(ex.getMessage()))))
              : terminalPayload.get("evidenceJson"));
      if (terminalPayload != null) failure.put("resultJson", terminalPayload.get("resultJson"));
      failure.put("executionAudit", audit);
      failure.put("modelUsages", java.util.List.of());
      failure.put(
          "blockerGuidance",
          Map.of(
              "category",
              "TECHNICAL_FAILURE",
              "recommendedAction",
              "Corrigir a captura ou reaplicar o callback preservado antes de retomar; manter copy, pixels e pareceres anteriores. Esta rotina não precisa de inferência.",
              "helpLinks",
              java.util.List.of(
                  Map.of("label", "Abrir auditoria da tarefa", "url", "/agent-tasks"))));
      backend.post().uri(prefix + "/failure").body(failure).retrieve().toBodilessEntity();
    } finally {
      try (var files = Files.walk(directory)) {
        for (Path file : files.sorted(java.util.Comparator.reverseOrder()).toList())
          Files.deleteIfExists(file);
      } catch (Exception ex) {
        log.error("Falha ao limpar captura comercial. taskId={}", taskId, ex);
      }
    }
  }

  /** Executa o navegador empacotado com prazo limitado e diagnóstico estruturado de falha. */
  private static void runNode(Path script, Path source, Path png, Path receiptFile, Path processLog)
      throws Exception {
    Process process =
        new ProcessBuilder(
                "node",
                script.toString(),
                source.toString(),
                png.toString(),
                receiptFile.toString())
            .redirectErrorStream(true)
            .redirectOutput(processLog.toFile())
            .start();
    if (!process.waitFor(Duration.ofMinutes(4).toMillis(), TimeUnit.MILLISECONDS)) {
      process.descendants().forEach(ProcessHandle::destroyForcibly);
      process.destroyForcibly();
      throw new IllegalStateException("A captura da prova comercial excedeu o prazo.");
    }
    if (process.exitValue() != 0) {
      log.error(
          "Captura Opala não concluída. operation=CAPTURE_FIRST_VALUE diagnostic={}",
          Files.readString(processLog));
      String diagnostic =
          Files.exists(receiptFile)
              ? new ObjectMapper()
                  .readTree(Files.readString(receiptFile))
                  .path("message")
                  .asText("Captura incompleta.")
              : "Capturador não produziu recibo de diagnóstico.";
      throw new IllegalStateException("Prova comercial não concluída: " + diagnostic);
    }
  }

  /**
   * Responsabilidade: executar apenas a captura, permitindo doubles locais sem outro motor de
   * pipeline.
   */
  @FunctionalInterface
  interface CaptureCommand {
    /** Produz os arquivos de pixels e recibo ou informa falha sem executar inferência. */
    void run(Path script, Path source, Path png, Path receiptFile, Path processLog)
        throws Exception;
  }
}
