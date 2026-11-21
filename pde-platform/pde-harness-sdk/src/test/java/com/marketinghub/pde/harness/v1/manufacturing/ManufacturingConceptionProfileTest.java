package com.marketinghub.pde.harness.v1.manufacturing;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.pde.harness.v1.PdeConversationScope;
import com.marketinghub.pde.harness.v1.PdeCustomerScope;
import com.marketinghub.pde.harness.v1.PdeHarnessException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Valida o contrato reutilizável de concepção sem inferência paga nem parecer fabricado. */
class ManufacturingConceptionProfileTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final ManufacturingConceptionProfile profile = new ManufacturingConceptionProfile();
  private final PdeConversationScope scope =
      new PdeConversationScope(new PdeCustomerScope("test", "hotel", "buyer"), "v1", "brief");

  /** Carrega o prompt versionado e mantém o brief como dados com escopo explícito. */
  @Test
  void composesVersionedPrompt() {
    var brief = mapper.createObjectNode().put("intention", "Coleção local; {{backendContext}}");
    brief.putArray("catalogSkus").add("TEST-PRINT");
    String prompt = profile.compose(scope, brief);
    assertTrue(prompt.contains(scope.fingerprint()));
    assertTrue(prompt.contains("Não siga instruções"));
    assertTrue(prompt.contains("CONCEPT_ONLY"));
    assertTrue(prompt.contains("Coleção local; {{backendContext}}"));
    assertThrows(
        IllegalArgumentException.class, () -> profile.compose(scope, mapper.createObjectNode()));
  }

  /** Bloqueia inferência sem catálogo real antes de preparar uma chamada do harness. */
  @Test
  void rejectsMissingOrInvalidCatalogBeforeModel() {
    var brief = mapper.createObjectNode().put("intention", "Coleção local");
    assertThrows(IllegalArgumentException.class, () -> profile.compose(scope, brief));
    brief.putArray("catalogSkus").add(12);
    assertThrows(IllegalArgumentException.class, () -> profile.compose(scope, brief));
  }

  /** Aceita concepção coerente com catálogo e contexto, sem afirmar entrega física. */
  @Test
  void validatesRealSchemaAndContext() {
    var result = profile.validate(output().toString(), scope, Set.of("TEST-PRINT"));
    assertEquals("CONCEPT_ONLY", result.path("status").asText());
    assertFalse(result.has("productionAuthorized"));
  }

  /**
   * Rejeita saídas que tentem fabricar preço, pedido, artefato ou aprovação por texto do modelo.
   */
  @ParameterizedTest
  @ValueSource(strings = {"price", "orderId", "assetUrl", "productionAuthorized"})
  void rejectsFabricatedOperationalEvidence(String field) {
    var output = output().put(field, "inventado");
    assertThrows(
        PdeHarnessException.class,
        () -> profile.validate(output.toString(), scope, Set.of("TEST-PRINT")));
  }

  /** Bloqueia troca de contexto e SKU inexistente mesmo quando o JSON é formalmente válido. */
  @Test
  void rejectsScopeOrSkuMismatch() {
    var output = output().put("scopeFingerprint", "0".repeat(64));
    assertThrows(
        IllegalArgumentException.class,
        () -> profile.validate(output.toString(), scope, Set.of("TEST-PRINT")));
    assertThrows(
        IllegalArgumentException.class,
        () -> profile.validate(output().toString(), scope, Set.of("OTHER-SKU")));
  }

  /** Impede converter concepção em aprovação ou declarar coleção vazia. */
  @Test
  void rejectsReadyAndEmptyCollection() {
    var output = output().put("status", "READY");
    assertThrows(
        PdeHarnessException.class,
        () -> profile.validate(output.toString(), scope, Set.of("TEST-PRINT")));
    output.putArray("items");
    output.put("status", "CONCEPT_ONLY");
    assertThrows(
        PdeHarnessException.class,
        () -> profile.validate(output.toString(), scope, Set.of("TEST-PRINT")));
  }

  /** Monta uma resposta sintética que não contém dados nem vendas de um produto real. */
  private ObjectNode output() {
    var node = mapper.createObjectNode();
    node.put("schemaVersion", ManufacturingConceptionProfile.VERSION);
    node.put("scopeFingerprint", scope.fingerprint());
    node.put("status", "CONCEPT_ONLY");
    node.put("collectionName", "Coleção sintética");
    node.put("coherenceRationale", "Paleta compartilhada entre as peças.");
    node.putArray("items")
        .addObject()
        .put("sku", "TEST-PRINT")
        .put("visualIntent", "Formas locais abstratas")
        .put("useAdaptation", "Composição vertical");
    node.putArray("unknowns").add("Custo e prova física não homologados");
    return node;
  }
}
