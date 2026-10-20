package com.marketinghub.gerasalespage.v1.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.product.Product;
import com.marketinghub.producttype.ProductTypeDefinition;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Responsabilidade: preservar os itens explícitos dos kits e recusar fontes ausentes ou
 * divergentes.
 */
class GeraSalesPageProductDeliverablesTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Constrói duas identidades com o mesmo contrato funcional, sem fontes comerciais fictícias. */
  static Experiment kit(long id) {
    var type = new ProductTypeDefinition();
    type.setCode("LOW_TICKET_DIGITAL_PRODUCT");
    var product = new Product();
    product.setId(id);
    product.setName("Kit sintético " + id);
    product.setProductTypeDefinition(type);
    product.setCurrentPriceBrl(new BigDecimal("67.00"));
    product.setValidationDefinitionVersion("v1");
    String contract =
        "{\"delivery\":{\"formats\":{\"posts\":\"10 PNG 1080x1080\",\"stories\":\"10 PNG 1080x1920\",\"captions\":\"10 legendas em TXT\",\"whatsappMessages\":\"5 mensagens em TXT\",\"calendar\":\"7 dias em TXT\"}}}";
    product.setValidationDefinitionJson(contract);
    product.setPdeExperienceJson(contract);
    var experiment = new Experiment();
    experiment.setId(id);
    experiment.setProduct(product);
    experiment.setUnitPrice(new BigDecimal("67.00"));
    experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
    return experiment;
  }

  /** Copia somente os cinco grupos declarados, sem criar identificador ou pacote FEO. */
  @ParameterizedTest
  @ValueSource(longs = {105, 9317})
  void exposesDeclaredItemsWithRealSource(long id) {
    var result = GeraSalesPageProductDeliverables.from(kit(id), json);
    assertThat(result)
        .containsEntry("source", "PRODUCT_DELIVERY_CONTRACT")
        .containsEntry("productId", id);
    assertThat(result).doesNotContainKey("id");
    var items = json.valueToTree(result).path("deliverables");
    assertThat(items.size()).isEqualTo(5);
    assertThat(items.get(0).path("description").asText()).isEqualTo("10 PNG 1080x1080");
    assertThat(items.get(4).path("sourceField").asText()).isEqualTo("delivery.formats.calendar");
  }

  /** Ausência, tipo legado e divergência mantêm a lacuna visível em vez de inventar entregáveis. */
  @ParameterizedTest
  @ValueSource(strings = {"empty", "different", "price", "malformed", "unknown", "type"})
  void refusesIncompleteOrConflictingSources(String cause) {
    var experiment = kit(105);
    switch (cause) {
      case "empty" -> experiment.getProduct().setValidationDefinitionJson("{}");
      case "different" -> experiment.getProduct().setPdeExperienceJson("{}");
      case "price" -> experiment.setUnitPrice(new BigDecimal("79"));
      case "malformed" -> experiment.getProduct().setPdeExperienceJson("not-json");
      case "unknown" -> {
        experiment
            .getProduct()
            .setValidationDefinitionJson(
                "{\"delivery\":{\"formats\":{\"unconfirmed\":\"a list\"}}}");
        experiment
            .getProduct()
            .setPdeExperienceJson(experiment.getProduct().getValidationDefinitionJson());
      }
      case "type" -> experiment.getProduct().setProductTypeDefinition(null);
    }
    assertThat(GeraSalesPageProductDeliverables.from(experiment, json)).isEmpty();
  }
}
