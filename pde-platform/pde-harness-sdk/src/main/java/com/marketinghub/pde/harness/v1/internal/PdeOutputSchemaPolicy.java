package com.marketinghub.pde.harness.v1.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.pde.harness.v1.PdeHarnessException;
import com.marketinghub.pde.harness.v1.PdeHarnessFailureCategory;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Impede schemas frouxos que fariam o modelo, parser e consumidor discordarem. */
public final class PdeOutputSchemaPolicy {
  /** Impede instanciação de uma política composta apenas por validações estáticas. */
  private PdeOutputSchemaPolicy() {}

  /** Valida raiz objeto e objetos estritos em toda a árvore do schema. */
  public static void validate(JsonNode schema) {
    if (!schema.isObject() || !"object".equals(schema.path("type").asText())) {
      throw invalid("o schema de saída deve possuir objeto na raiz");
    }
    validateNode(schema, "$");
  }

  /** Percorre recursivamente o contrato e valida cada objeto declarado. */
  private static void validateNode(JsonNode node, String path) {
    if (node == null || node.isValueNode()) {
      return;
    }
    if (node.isArray()) {
      for (int index = 0; index < node.size(); index++) {
        validateNode(node.get(index), path + "[" + index + "]");
      }
      return;
    }
    validateLocalReference(node, path, "$ref");
    validateLocalReference(node, path, "$dynamicRef");
    validateLocalReference(node, path, "$recursiveRef");
    if ("object".equals(node.path("type").asText())) {
      validateStrictObject(node, path);
    }
    for (Map.Entry<String, JsonNode> field : node.properties()) {
      validateNode(field.getValue(), path + "." + field.getKey());
    }
  }

  /** Permite somente referências internas para impedir resolução de contrato pela rede. */
  private static void validateLocalReference(JsonNode node, String path, String keyword) {
    if (node.hasNonNull(keyword) && !node.path(keyword).asText().startsWith("#")) {
      throw invalid("referência externa não é permitida em " + path + "." + keyword);
    }
  }

  /** Exige propriedades fechadas e requeridas para evitar saída estruturalmente ambígua. */
  private static void validateStrictObject(JsonNode objectSchema, String path) {
    JsonNode properties = objectSchema.path("properties");
    if (!properties.isObject() || properties.size() == 0) {
      throw invalid("objeto " + path + " deve declarar properties não vazio");
    }
    if (!objectSchema.has("additionalProperties")
        || objectSchema.path("additionalProperties").asBoolean(true)) {
      throw invalid("objeto " + path + " deve declarar additionalProperties=false");
    }
    JsonNode required = objectSchema.path("required");
    if (!required.isArray()) {
      throw invalid("objeto " + path + " deve declarar required");
    }
    Set<String> requiredFields = new HashSet<>();
    required.forEach(
        value -> {
          if (!value.isTextual() || value.asText().isBlank()) {
            throw invalid("required de " + path + " contém nome inválido");
          }
          requiredFields.add(value.asText());
        });
    Iterator<String> propertyNames = properties.fieldNames();
    while (propertyNames.hasNext()) {
      String property = propertyNames.next();
      if (!requiredFields.contains(property)) {
        throw invalid("propriedade " + path + "." + property + " deve ser obrigatória");
      }
    }
    for (String requiredField : requiredFields) {
      if (!properties.has(requiredField)) {
        throw invalid("required de " + path + " referencia propriedade inexistente");
      }
    }
  }

  /** Cria uma falha de configuração estável para o worker bloquear antes do modelo. */
  private static PdeHarnessException invalid(String detail) {
    return new PdeHarnessException(
        PdeHarnessFailureCategory.CONFIGURATION, "Schema de saída inválido: " + detail);
  }
}
