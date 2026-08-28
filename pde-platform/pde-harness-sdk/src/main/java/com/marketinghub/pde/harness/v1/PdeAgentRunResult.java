package com.marketinghub.pde.harness.v1;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Entrega ao worker o resultado funcional e a auditoria técnica de um turno PDE. */
public record PdeAgentRunResult(
    PdeRunContext context,
    String threadId,
    PdeThreadBinding threadBinding,
    String turnId,
    PdeRunStatus status,
    String output,
    JsonNode structuredOutput,
    String errorMessage,
    Instant startedAt,
    Instant finishedAt,
    List<PdeHarnessEvent> events,
    JsonNode tokenUsage,
    PdeMemoryAudit memoryAudit,
    String codexVersion,
    String sdkVersion,
    String model,
    String promptVersion,
    String outputSchemaVersion,
    String promptSha256,
    String effectiveInputSha256,
    String outputSchemaSha256) {

  /** Congela listas e payloads para preservar a evidência exata devolvida ao worker. */
  public PdeAgentRunResult {
    context = Objects.requireNonNull(context, "context");
    threadId = Objects.requireNonNull(threadId, "threadId");
    threadBinding = Objects.requireNonNull(threadBinding, "threadBinding");
    if (!threadId.equals(threadBinding.threadId())) {
      throw new IllegalArgumentException("threadId diverge do vínculo retornado");
    }
    turnId = Objects.requireNonNull(turnId, "turnId");
    status = Objects.requireNonNull(status, "status");
    structuredOutput = structuredOutput == null ? null : structuredOutput.deepCopy();
    startedAt = Objects.requireNonNull(startedAt, "startedAt");
    finishedAt = Objects.requireNonNull(finishedAt, "finishedAt");
    events = List.copyOf(Objects.requireNonNull(events, "events"));
    tokenUsage = tokenUsage == null ? null : tokenUsage.deepCopy();
    memoryAudit = Objects.requireNonNull(memoryAudit, "memoryAudit");
    codexVersion = Objects.requireNonNull(codexVersion, "codexVersion");
    sdkVersion = Objects.requireNonNull(sdkVersion, "sdkVersion");
    model = Objects.requireNonNull(model, "model");
    promptVersion = Objects.requireNonNull(promptVersion, "promptVersion");
    outputSchemaVersion = Objects.requireNonNull(outputSchemaVersion, "outputSchemaVersion");
    promptSha256 = Objects.requireNonNull(promptSha256, "promptSha256");
    effectiveInputSha256 = Objects.requireNonNull(effectiveInputSha256, "effectiveInputSha256");
    outputSchemaSha256 = Objects.requireNonNull(outputSchemaSha256, "outputSchemaSha256");
  }

  /** Devolve uma cópia da saída funcional para preservar a evidência consolidada. */
  @Override
  public JsonNode structuredOutput() {
    return structuredOutput == null ? null : structuredOutput.deepCopy();
  }

  /** Devolve uma cópia do uso observado para impedir mutação depois do callback. */
  @Override
  public JsonNode tokenUsage() {
    return tokenUsage == null ? null : tokenUsage.deepCopy();
  }
}
