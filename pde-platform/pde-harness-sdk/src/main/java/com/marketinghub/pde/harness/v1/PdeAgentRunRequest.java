package com.marketinghub.pde.harness.v1;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;

/** Reúne a entrada funcional e os contratos versionados de um único turno do agente PDE. */
public record PdeAgentRunRequest(
    PdeRunContext context,
    String model,
    String prompt,
    String promptVersion,
    JsonNode outputSchema,
    String outputSchemaVersion,
    String existingThreadId,
    boolean ephemeralThread) {

  /** Valida a entrada e congela uma cópia do schema para evitar mutação durante a execução. */
  public PdeAgentRunRequest {
    context = Objects.requireNonNull(context, "context");
    model = requireText(model, "model");
    prompt = requireText(prompt, "prompt");
    promptVersion = requireText(promptVersion, "promptVersion");
    outputSchema = Objects.requireNonNull(outputSchema, "outputSchema").deepCopy();
    outputSchemaVersion = requireText(outputSchemaVersion, "outputSchemaVersion");
    existingThreadId = normalizeOptional(existingThreadId);
    if (existingThreadId != null && ephemeralThread) {
      throw new IllegalArgumentException("thread efêmera não pode ser retomada após descarte");
    }
  }

  /** Devolve uma cópia do schema para impedir alteração externa do contrato em execução. */
  @Override
  public JsonNode outputSchema() {
    return outputSchema.deepCopy();
  }

  /** Cria uma solicitação que inicia uma thread nova. */
  public static PdeAgentRunRequest newThread(
      PdeRunContext context,
      String model,
      String prompt,
      String promptVersion,
      JsonNode outputSchema,
      String outputSchemaVersion,
      boolean ephemeralThread) {
    return new PdeAgentRunRequest(
        context,
        model,
        prompt,
        promptVersion,
        outputSchema,
        outputSchemaVersion,
        null,
        ephemeralThread);
  }

  /** Cria uma solicitação que retoma uma thread persistida e inicia um novo turno. */
  public static PdeAgentRunRequest resumeThread(
      PdeRunContext context,
      String model,
      String prompt,
      String promptVersion,
      JsonNode outputSchema,
      String outputSchemaVersion,
      String threadId) {
    return new PdeAgentRunRequest(
        context, model, prompt, promptVersion, outputSchema, outputSchemaVersion, threadId, false);
  }

  /** Valida texto obrigatório e remove espaços externos. */
  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " é obrigatório");
    }
    return value.trim();
  }

  /** Normaliza texto opcional sem transformar ausência em string vazia. */
  private static String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
