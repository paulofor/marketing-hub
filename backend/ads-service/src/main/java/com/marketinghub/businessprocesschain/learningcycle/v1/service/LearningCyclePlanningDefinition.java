package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleRules.require;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.product.ProductBigIdeaBindingRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: transportar a definição registrada do produto sem herdar uma oferta vencida.
 */
@Component
@RequiredArgsConstructor
public class LearningCyclePlanningDefinition {
  private final ProductBigIdeaBindingRepository bindings;
  private final LearningCycleJson json;
  private final com.marketinghub.hypothesis.service.HypothesisService hypotheses;

  /** Cria uma versão vinculada ao predecessor sem manter a oferta vencida nem inventar preço. */
  public com.marketinghub.hypothesis.Hypothesis hypothesisFor(Experiment source) {
    var product = source.getProduct();
    var definition = snapshot(product);
    require(source.getHypothesisRef() != null, "O experimento precisa de uma hipótese de origem.");
    String persona = product.getTargetAudience();
    require(persona != null && !persona.isBlank(), "A definição precisa informar o público.");
    if (persona.codePointCount(0, persona.length()) > 255)
      persona = persona.substring(0, persona.offsetByCodePoints(0, 252)) + "...";
    return hypotheses.createPlanningVersion(
        source.getHypothesisRef().getId(),
        product.getId(),
        new com.marketinghub.hypothesis.dto.CreateHypothesisVersionRequest(
            product.getExplicitPain(),
            persona,
            product.getPromise(),
            product.getUniqueMechanism(),
            product.getUniqueMechanism(),
            definition.path("offer").asText(),
            product.getPrimaryHypothesis(),
            null,
            product.getCurrentPriceBrl()));
  }

  /** Detecta mudança explícita de direção em relação à referência fixada no predecessor. */
  public boolean changedSince(Experiment source) {
    var product = source.getProduct();
    if (product == null || product.getBigIdea() == null) return false;
    return bindings
        .findByProductIdAndSourceReference(product.getId(), "experiment:" + source.getId())
        .map(
            binding ->
                binding.getBigIdea() != null
                    && !Objects.equals(binding.getBigIdea().getId(), product.getBigIdea().getId()))
        .orElse(false);
  }

  /** Congela formato, receita, preço em aberto e limites sem converter ausência em gratuidade. */
  public ObjectNode snapshot(Product product) {
    require(
        product.getBigIdea() != null,
        "Defina a direção do produto antes de alinhar o planejamento.");
    require(
        product.getProductFormat() != null
            && !product.getProductFormat().isBlank()
            && product.getRevenueModel() != null
            && !product.getRevenueModel().isBlank(),
        "A definição do produto precisa informar formato e modelo de receita.");
    var definition = json.read(product.getValidationDefinitionJson());
    require(
        definition != null && definition.path("economics").isObject(),
        "A definição econômica do produto ainda não está estruturada.");
    var economics = definition.path("economics");
    var price = economics.path("priceBrl");
    require(
        economics.has("priceBrl") && (price.isNull() || price.isNumber()),
        "O preço precisa estar definido ou explicitamente em aberto.");
    require(
        price.isNull()
            ? product.getCurrentPriceBrl() == null
            : product.getCurrentPriceBrl() != null
                && product.getCurrentPriceBrl().compareTo(price.decimalValue()) == 0,
        "O preço do cadastro diverge da definição econômica; concilie a origem.");
    var snapshot = (ObjectNode) json.read("{}");
    snapshot.put("contractVersion", "LEARNING_CYCLE_PLANNING_DEFINITION_V1");
    snapshot.put("productId", product.getId());
    snapshot.put("bigIdeaId", product.getBigIdea().getId());
    snapshot.put("bigIdeaRevision", product.getBigIdea().getRevision());
    snapshot.put("validationDefinitionVersion", product.getValidationDefinitionVersion());
    snapshot.put("format", product.getProductFormat());
    snapshot.put("revenueModel", product.getRevenueModel());
    snapshot.set("economics", economics.deepCopy());
    snapshot.put(
        "offer",
        product.getName()
            + ": "
            + product.getProductFormat()
            + ". "
            + product.getRevenueModel()
            + ". "
            + (price.isNull()
                ? "Preço a definir com Plutus. "
                : "Preço registrado: R$ " + price.asText() + ". ")
            + economics.path("pricingStatus").asText()
            + ". "
            + economics.path("usageLimits").asText());
    snapshot.put("billingAuthorized", false);
    snapshot.put("mediaAuthorized", false);
    snapshot.put("audiovisualAuthorized", false);
    return (ObjectNode) json.read(json.write(snapshot));
  }
}
