package com.marketinghub.landinggeneratoragent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Responsabilidade: projetar contexto sem perda e impedir inferência acima do orçamento. */
final class PdeContextBudget {
  static final int LIMIT = 900_000;
  private final ObjectMapper json;
  private final Map<String, String> references = new LinkedHashMap<>();

  /** Define o serializador compartilhado com o executor. */
  PdeContextBudget(ObjectMapper json) {
    this.json = json;
  }

  /** Preserva contratos atuais e referencia folhas extensas do histórico com hash verificável. */
  Map<String, Object> project(Map<String, Object> task) throws IOException {
    ObjectNode root = json.valueToTree(CatalogPromptInput.context(task));
    JsonNode context = root.get("processContextJson");
    if (context != null && context.isTextual()) {
      JsonNode parsed = json.readTree(context.asText());
      for (String field :
          new String[] {
            "historicalContinuityArtifacts", "versionContinuity", "correctionAttempts"
          }) {
        if (parsed.has(field)) reduce(parsed.get(field));
      }
      root.put("processContextJson", json.writeValueAsString(parsed));
    }
    var sizes = new LinkedHashMap<String, Integer>();
    for (String field :
        java.util.List.of(
            "processContextJson",
            "taskTarget",
            "operatorGuidance",
            "description",
            "researchIntelligence"))
      if (root.has(field)) sizes.put(field, json.writeValueAsBytes(root.get(field)).length);
    org.slf4j.LoggerFactory.getLogger(PdeContextBudget.class)
        .info(
            "PDE_CONTEXT_BUDGET_V1 serializedSectionBytes={} uniqueReferences={}",
            sizes,
            references.size());
    return json.convertValue(root, Map.class);
  }

  /**
   * Externaliza somente valores extensos, mantendo chaves, critérios e estrutura de proveniência.
   */
  private void reduce(JsonNode node) throws IOException {
    if (node.isObject()) {
      var fields = new java.util.ArrayList<String>();
      node.fieldNames().forEachRemaining(fields::add);
      for (String key : fields) {
        JsonNode value = node.get(key);
        String exact = json.writeValueAsString(value);
        boolean payload =
            java.util.Set.of("result", "evidence", "resultJson", "evidenceJson").contains(key);
        if (exact.length() > 4096 && (value.isTextual() || payload)) {
          String hash = CatalogPromptInput.sha256(exact);
          references.putIfAbsent(hash, exact);
          ((ObjectNode) node)
              .set(
                  key,
                  json.valueToTree(
                      Map.of(
                          "contextReference",
                          hash,
                          "sha256",
                          hash,
                          "serializedChars",
                          exact.length(),
                          "tool",
                          "read_task_context")));
        } else reduce(value);
      }
    } else if (node.isArray()) for (JsonNode item : node) reduce(item);
  }

  /** Informa se a projeção exige a ferramenta de recuperação. */
  boolean hasReferences() {
    return !references.isEmpty();
  }

  /** Grava apenas referências desta tarefa; nunca permite consultar outro snapshot. */
  void save(Path path, Object taskId) throws IOException {
    byte[] bytes = json.writeValueAsBytes(Map.of("taskId", taskId, "references", references));
    Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
    try (var channel =
        java.nio.channels.FileChannel.open(
            temporary,
            java.nio.file.StandardOpenOption.CREATE,
            java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
            java.nio.file.StandardOpenOption.WRITE)) {
      var buffer = java.nio.ByteBuffer.wrap(bytes);
      while (buffer.hasRemaining()) channel.write(buffer);
      channel.force(true);
    }
    Files.move(
        temporary,
        path,
        java.nio.file.StandardCopyOption.ATOMIC_MOVE,
        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
  }

  /** Mede envelope escapado, schema e reserva de transporte antes de iniciar qualquer processo. */
  void preflight(String prompt, String schema) throws IOException {
    String envelope =
        json.writeValueAsString(Map.of("input", prompt, "outputSchema", json.readTree(schema)));
    int bytes = envelope.getBytes(StandardCharsets.UTF_8).length;
    org.slf4j.LoggerFactory.getLogger(PdeContextBudget.class)
        .info(
            "PDE_CONTEXT_BUDGET_V1 promptChars={} serializedChars={} envelopeBytes={} schemaChars={} uniqueReferences={} limit={} fingerprint={}",
            prompt.codePointCount(0, prompt.length()),
            envelope.length(),
            bytes,
            schema.length(),
            references.size(),
            LIMIT,
            CatalogPromptInput.sha256(envelope));
    if (bytes > LIMIT)
      throw new IOException(
          "PDE_CONTEXT_BUDGET_V1|Interrupção antes do modelo: contexto mínimo excede o orçamento. bytes="
              + bytes
              + "; limit="
              + LIMIT
              + "; sha256="
              + CatalogPromptInput.sha256(envelope));
  }
}
