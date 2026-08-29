package com.marketinghub.pde.harness.v1.consultant;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.pde.harness.v1.PdeCustomerMemory;
import com.marketinghub.pde.harness.v1.PdeLocalImageInput;
import com.marketinghub.pde.harness.v1.PdeRunContext;
import com.marketinghub.pde.harness.v1.PdeThreadBinding;
import java.util.List;
import java.util.Objects;

/** Reúne um turno de consultoria independente do canal e do produto concreto. */
public record PdeConsultantTurnRequest(
    PdeConsultantChannel channel,
    PdeRunContext context,
    PdeCustomerMemory memory,
    String model,
    PdeConsultantPromptParts promptParts,
    JsonNode outputSchema,
    String outputSchemaVersion,
    List<PdeLocalImageInput> imageInputs,
    PdeThreadBinding existingThreadBinding,
    boolean ephemeralThread) {

  /** Congela o contrato do turno e impede retomada de uma thread marcada como efêmera. */
  public PdeConsultantTurnRequest {
    channel = Objects.requireNonNull(channel, "channel");
    context = Objects.requireNonNull(context, "context");
    memory = Objects.requireNonNull(memory, "memory");
    model = requireText(model, "model");
    promptParts = Objects.requireNonNull(promptParts, "promptParts");
    outputSchema = Objects.requireNonNull(outputSchema, "outputSchema").deepCopy();
    outputSchemaVersion = requireText(outputSchemaVersion, "outputSchemaVersion");
    imageInputs = List.copyOf(Objects.requireNonNull(imageInputs, "imageInputs"));
    if (existingThreadBinding != null && ephemeralThread) {
      throw new IllegalArgumentException("thread efêmera não pode ser retomada");
    }
  }

  /** Devolve uma cópia do schema para impedir alteração durante a execução. */
  @Override
  public JsonNode outputSchema() {
    return outputSchema.deepCopy();
  }

  /** Valida texto obrigatório e remove espaços externos. */
  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " é obrigatório");
    }
    return value.trim();
  }
}
