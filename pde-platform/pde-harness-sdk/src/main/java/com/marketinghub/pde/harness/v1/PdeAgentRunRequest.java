package com.marketinghub.pde.harness.v1;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Reúne a entrada funcional e os contratos versionados de um único turno do agente PDE. */
public record PdeAgentRunRequest(
    PdeRunContext context,
    PdeCustomerMemory memory,
    String model,
    String prompt,
    String promptVersion,
    JsonNode outputSchema,
    String outputSchemaVersion,
    List<PdeLocalImageInput> imageInputs,
    PdeThreadBinding existingThreadBinding,
    boolean ephemeralThread) {

  /** Valida a entrada e congela uma cópia do schema para evitar mutação durante a execução. */
  public PdeAgentRunRequest {
    context = Objects.requireNonNull(context, "context");
    memory = Objects.requireNonNull(memory, "memory");
    model = requireText(model, "model");
    prompt = requireText(prompt, "prompt");
    promptVersion = requireText(promptVersion, "promptVersion");
    outputSchema = Objects.requireNonNull(outputSchema, "outputSchema").deepCopy();
    outputSchemaVersion = requireText(outputSchemaVersion, "outputSchemaVersion");
    imageInputs = List.copyOf(Objects.requireNonNull(imageInputs, "imageInputs"));
    if (imageInputs.size() > 8) {
      throw new IllegalArgumentException("imageInputs excede 8 imagens por turno");
    }
    Set<String> references = new HashSet<>();
    for (PdeLocalImageInput imageInput : imageInputs) {
      Objects.requireNonNull(imageInput, "imageInput");
      if (!references.add(imageInput.reference())) {
        throw new IllegalArgumentException(
            "reference de imagem duplicada: " + imageInput.reference());
      }
    }
    if (existingThreadBinding != null && ephemeralThread) {
      throw new IllegalArgumentException("thread efêmera não pode ser retomada após descarte");
    }
  }

  /** Mantém o construtor original para integrações textuais que ainda não enviam imagens. */
  public PdeAgentRunRequest(
      PdeRunContext context,
      PdeCustomerMemory memory,
      String model,
      String prompt,
      String promptVersion,
      JsonNode outputSchema,
      String outputSchemaVersion,
      PdeThreadBinding existingThreadBinding,
      boolean ephemeralThread) {
    this(
        context,
        memory,
        model,
        prompt,
        promptVersion,
        outputSchema,
        outputSchemaVersion,
        List.of(),
        existingThreadBinding,
        ephemeralThread);
  }

  /** Devolve uma cópia do schema para impedir alteração externa do contrato em execução. */
  @Override
  public JsonNode outputSchema() {
    return outputSchema.deepCopy();
  }

  /** Cria uma solicitação que inicia uma thread nova. */
  public static PdeAgentRunRequest newThread(
      PdeRunContext context,
      PdeCustomerMemory memory,
      String model,
      String prompt,
      String promptVersion,
      JsonNode outputSchema,
      String outputSchemaVersion,
      boolean ephemeralThread) {
    return new PdeAgentRunRequest(
        context,
        memory,
        model,
        prompt,
        promptVersion,
        outputSchema,
        outputSchemaVersion,
        List.of(),
        null,
        ephemeralThread);
  }

  /** Cria uma solicitação que retoma uma thread persistida e inicia um novo turno. */
  public static PdeAgentRunRequest resumeThread(
      PdeRunContext context,
      PdeCustomerMemory memory,
      String model,
      String prompt,
      String promptVersion,
      JsonNode outputSchema,
      String outputSchemaVersion,
      PdeThreadBinding threadBinding) {
    return new PdeAgentRunRequest(
        context,
        memory,
        model,
        prompt,
        promptVersion,
        outputSchema,
        outputSchemaVersion,
        List.of(),
        Objects.requireNonNull(threadBinding, "threadBinding"),
        false);
  }

  /** Cria uma solicitação multimodal que inicia uma thread nova. */
  public static PdeAgentRunRequest newThreadWithImages(
      PdeRunContext context,
      PdeCustomerMemory memory,
      String model,
      String prompt,
      String promptVersion,
      JsonNode outputSchema,
      String outputSchemaVersion,
      List<PdeLocalImageInput> imageInputs,
      boolean ephemeralThread) {
    return new PdeAgentRunRequest(
        context,
        memory,
        model,
        prompt,
        promptVersion,
        outputSchema,
        outputSchemaVersion,
        imageInputs,
        null,
        ephemeralThread);
  }

  /** Cria uma solicitação multimodal que retoma uma thread autorizada. */
  public static PdeAgentRunRequest resumeThreadWithImages(
      PdeRunContext context,
      PdeCustomerMemory memory,
      String model,
      String prompt,
      String promptVersion,
      JsonNode outputSchema,
      String outputSchemaVersion,
      List<PdeLocalImageInput> imageInputs,
      PdeThreadBinding threadBinding) {
    return new PdeAgentRunRequest(
        context,
        memory,
        model,
        prompt,
        promptVersion,
        outputSchema,
        outputSchemaVersion,
        imageInputs,
        Objects.requireNonNull(threadBinding, "threadBinding"),
        false);
  }

  /** Valida texto obrigatório e remove espaços externos. */
  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " é obrigatório");
    }
    return value.trim();
  }
}
