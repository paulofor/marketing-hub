package com.marketinghub.gerasalespage.v1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.service.LowTicketCheckoutContract;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsabilidade: fornecer os entregáveis explícitos de kits sem inventar um pacote FEO. */
final class GeraSalesPageProductDeliverables {
  private static final Logger log = LoggerFactory.getLogger(GeraSalesPageProductDeliverables.class);
  private static final Map<String, String> LABELS =
      Map.of(
          "posts",
          "Posts para feed",
          "stories",
          "Stories",
          "captions",
          "Legendas",
          "whatsappMessages",
          "Mensagens para WhatsApp",
          "calendar",
          "Calendário de publicação");

  /** Impede instâncias de um adaptador puro do contrato comercial persistido. */
  private GeraSalesPageProductDeliverables() {}

  /** Exige formatos concordantes nas duas fontes e preço vigente antes de expor a lista. */
  static Map<String, Object> from(Experiment experiment, ObjectMapper json) {
    if (!LowTicketCheckoutContract.applies(experiment)) return Map.of();
    var product = experiment.getProduct();
    if (product.getCurrentPriceBrl() == null
        || experiment.getUnitPrice() == null
        || product.getCurrentPriceBrl().compareTo(experiment.getUnitPrice()) != 0) return Map.of();
    try {
      var validation = json.readTree(product.getValidationDefinitionJson());
      var experience = json.readTree(product.getPdeExperienceJson());
      if (validation == null || experience == null) return Map.of();
      var formats = validation.path("delivery").path("formats");
      if (!formats.isObject()
          || formats.isEmpty()
          || !formats.equals(experience.path("delivery").path("formats"))) return Map.of();
      var items = new ArrayList<Map<String, Object>>();
      var fields = formats.fields();
      while (fields.hasNext()) {
        var field = fields.next();
        if (!LABELS.containsKey(field.getKey())
            || !field.getValue().isTextual()
            || field.getValue().asText().isBlank()) return Map.of();
        String title = LABELS.get(field.getKey());
        String description = field.getValue().asText();
        items.add(
            Map.of(
                "sourceField",
                "delivery.formats." + field.getKey(),
                "title",
                title,
                "publicTitle",
                title,
                "description",
                description,
                "publicDescription",
                description));
      }
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("source", "PRODUCT_DELIVERY_CONTRACT");
      result.put("productId", product.getId());
      result.put("productVersion", product.getValidationDefinitionVersion());
      result.put("name", product.getName());
      result.put(
          "description",
          "Itens declarados no contrato de entrega vigente; a homologação comercial permanece obrigatória.");
      result.put("deliverables", List.copyOf(items));
      return result;
    } catch (JsonProcessingException | IllegalArgumentException ex) {
      log.warn(
          "Contrato de entregáveis inválido productId={} experimentId={}",
          product.getId(),
          experiment.getId(),
          ex);
      return Map.of();
    }
  }
}
