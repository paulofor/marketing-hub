package com.marketinghub.experiment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskTargetContextProvider;
import com.marketinghub.agenttask.AgentTaskTargetResponse;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: publicar o alvo comercial segregado das tarefas ligadas a PDEs. */
@Service
public class ExperimentAgentTaskTargetContextProvider implements AgentTaskTargetContextProvider {
  private static final Logger log =
      LoggerFactory.getLogger(ExperimentAgentTaskTargetContextProvider.class);
  private static final Pattern EXPERIMENT_REFERENCE =
      Pattern.compile("experiment:([1-9][0-9]*)(?:@[^:]+)?(?:[:].*)?");
  private static final Pattern PRODUCT_REFERENCE =
      Pattern.compile("product:([1-9][0-9]*)(?:@[^:]+)?(?:[:].*)?");
  private final ExperimentRepository experiments;
  private final ProductRepository products;
  private final ObjectMapper objectMapper;

  /** Configura as fontes canônicas de experimento, produto e contrato PDE. */
  public ExperimentAgentTaskTargetContextProvider(
      ExperimentRepository experiments, ProductRepository products, ObjectMapper objectMapper) {
    this.experiments = experiments;
    this.products = products;
    this.objectMapper = objectMapper;
  }

  /** Resolve somente referências explícitas e nunca usa nome livre da tarefa como identidade. */
  @Override
  @Transactional(readOnly = true)
  public Optional<AgentTaskTargetResponse> resolve(String sourceReference) {
    if (sourceReference == null || sourceReference.isBlank()) return Optional.empty();
    String normalized = sourceReference.trim();
    Matcher experimentMatcher = EXPERIMENT_REFERENCE.matcher(normalized);
    if (experimentMatcher.matches()) {
      return experiments
          .findById(Long.valueOf(experimentMatcher.group(1)))
          .flatMap(experiment -> target(normalized, experiment, experiment.getProduct()));
    }
    Matcher productMatcher = PRODUCT_REFERENCE.matcher(normalized);
    if (!productMatcher.matches()) return Optional.empty();
    return products
        .findById(Long.valueOf(productMatcher.group(1)))
        .flatMap(product -> target(normalized, null, product));
  }

  /** Monta a identidade mínima exigida para impedir mistura de produtos ou versões. */
  private Optional<AgentTaskTargetResponse> target(
      String sourceReference, Experiment experiment, Product product) {
    if (product == null || product.getId() == null || blank(product.getSlug())) {
      return Optional.empty();
    }
    String experienceVersion = experienceVersion(product);
    if (blank(experienceVersion)) return Optional.empty();
    return Optional.of(
        new AgentTaskTargetResponse(
            sourceReference,
            experiment == null ? null : experiment.getId(),
            product.getId(),
            product.getSlug(),
            product.getName(),
            product.getInternalName(),
            experienceVersion,
            experiment != null && !blank(experiment.getFollowUpActionUrl())
                ? experiment.getFollowUpActionUrl()
                : product.getPublicUrl(),
            experiment == null ? null : experiment.getCommercialCheckoutUrl(),
            experiment == null ? product.getCurrentPriceBrl() : experiment.getUnitPrice()));
  }

  /** Extrai a versão funcional do JSON canônico persistido no produto. */
  private String experienceVersion(Product product) {
    if (blank(product.getPdeExperienceJson())) return null;
    try {
      JsonNode contract = objectMapper.readTree(product.getPdeExperienceJson());
      String value = contract.path("experienceVersion").asText(null);
      return blank(value) ? null : value.trim();
    } catch (Exception ex) {
      log.error(
          "Contrato PDE inválido ao resolver alvo da tarefa. productId={} productSlug={}",
          product.getId(),
          product.getSlug(),
          ex);
      return null;
    }
  }

  /** Verifica ausência sem transformar texto vazio em identidade válida. */
  private boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
