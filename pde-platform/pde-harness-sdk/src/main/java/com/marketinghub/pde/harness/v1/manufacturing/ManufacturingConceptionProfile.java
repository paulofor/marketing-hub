package com.marketinghub.pde.harness.v1.manufacturing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.harness.v1.PdeConversationScope;
import com.marketinghub.pde.harness.v1.internal.PdeStructuredOutputValidator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;

/**
 * Prepara e confere concepções de coleções usando o harness existente, sem executar modelo ou
 * fabricante.
 */
public final class ManufacturingConceptionProfile {
  public static final String PRODUCT_TYPE_CODE = "AI_DESIGNED_MANUFACTURED_PRODUCT";
  public static final String VERSION = "manufacturing-conception-v1";
  private static final System.Logger LOGGER =
      System.getLogger(ManufacturingConceptionProfile.class.getName());
  private final ObjectMapper mapper = new ObjectMapper();
  private final PdeStructuredOutputValidator validator = new PdeStructuredOutputValidator(mapper);

  /** Insere contexto persistido como dados, preservando escopo e a versão do template auditável. */
  public String compose(PdeConversationScope scope, JsonNode backendBrief) {
    Objects.requireNonNull(scope, "scope");
    if (backendBrief == null || !backendBrief.isObject() || backendBrief.isEmpty()) {
      throw new IllegalArgumentException("Brief estruturado do backend é obrigatório");
    }
    JsonNode catalog = backendBrief.path("catalogSkus");
    if (!catalog.isArray() || catalog.isEmpty() || catalog.size() > 100) {
      throw new IllegalArgumentException("Catálogo confiável com um a cem SKUs é obrigatório");
    }
    for (JsonNode sku : catalog) {
      if (!sku.isTextual() || sku.asText().isBlank() || sku.asText().length() > 191) {
        throw new IllegalArgumentException("SKU inválido no catálogo do brief");
      }
    }
    var context = mapper.createObjectNode();
    context.put("scopeFingerprint", scope.fingerprint());
    context.set("brief", backendBrief.deepCopy());
    return resource("conception.md").replace("{{backendContext}}", context.toString());
  }

  /** Carrega o schema que limita a saída a concepção, sem preço, artefato fictício ou aprovação. */
  public JsonNode outputSchema() {
    try {
      return mapper.readTree(resource("conception-schema.json"));
    } catch (IOException ex) {
      LOGGER.log(System.Logger.Level.ERROR, "Falha ao interpretar schema de concepção Jaspe", ex);
      throw new IllegalStateException("Schema de concepção indisponível", ex);
    }
  }

  /** Confere a saída bruta contra schema, escopo e SKUs obtidos pelo backend antes de persistir. */
  public JsonNode validate(String rawOutput, PdeConversationScope scope, Set<String> allowedSkus) {
    Objects.requireNonNull(scope, "scope");
    Objects.requireNonNull(allowedSkus, "allowedSkus");
    JsonNode output = validator.validate(rawOutput, outputSchema());
    if (!scope.fingerprint().equals(output.path("scopeFingerprint").asText())) {
      throw new IllegalArgumentException("Concepção de outro cliente, produto ou versão");
    }
    for (JsonNode item : output.path("items")) {
      if (!allowedSkus.contains(item.path("sku").asText())) {
        throw new IllegalArgumentException("SKU não pertence ao catálogo autorizado do brief");
      }
    }
    return output;
  }

  /**
   * Lê apenas recursos fixos versionados e registra a causa completa quando não estão disponíveis.
   */
  private String resource(String name) {
    String path = "/prompts/manufacturing/v1/" + name;
    try (var input = ManufacturingConceptionProfile.class.getResourceAsStream(path)) {
      if (input == null) throw new IOException("Recurso ausente: " + path);
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      LOGGER.log(
          System.Logger.Level.ERROR, "Falha ao carregar contrato Jaspe; resource=" + path, ex);
      throw new IllegalStateException("Contrato Jaspe indisponível", ex);
    }
  }
}
