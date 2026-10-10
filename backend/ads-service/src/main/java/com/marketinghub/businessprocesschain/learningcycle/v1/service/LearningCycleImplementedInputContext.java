package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Responsabilidade: entregar aos agentes a entrada implementada da versão exata do ciclo. */
@Component
@RequiredArgsConstructor
@Slf4j
public class LearningCycleImplementedInputContext {
  private final ProductRepository products;
  private final ObjectMapper json;

  /** Resolve somente versão e produto compatíveis, sem herdar aprovação ou autorizar consumo. */
  public Optional<JsonNode> resolve(LearningSalesCycle cycle) {
    var product = products.findById(cycle.getProductId()).orElse(null);
    if (product == null) return Optional.empty();
    try (var input = getClass().getResourceAsStream("/contracts/pde-implemented-inputs-v1.json")) {
      if (input == null) throw new IOException("Catálogo de entradas implementadas ausente.");
      var matches = new ArrayList<JsonNode>();
      for (var item : json.readTree(input).path("implementations")) {
        boolean supported = false;
        for (var version : item.path("prototypeVersions")) {
          supported |= Objects.equals(cycle.getProductVersion(), version.asText());
        }
        if (Objects.equals(product.getSlug(), item.path("productSlug").asText()) && supported)
          matches.add(item);
      }
      if (matches.isEmpty()) return Optional.empty();
      if (matches.size() != 1) throw new IOException("Entrada implementada ambígua.");
      ObjectNode result = matches.getFirst().deepCopy();
      result.remove("prototypeVersions");
      result.put("contractVersion", "PDE_IMPLEMENTED_INPUT_V1");
      result.put("productId", cycle.getProductId());
      result.put("cycleId", cycle.getId());
      result.put("experimentId", cycle.getExperimentId());
      result.put("prototypeVersion", cycle.getProductVersion());
      result.put("scope", "IMPLEMENTATION_DESCRIPTION");
      result.put("agentApprovalClaimed", false);
      result.put("commercialEvidenceClaimed", false);
      return Optional.of(result);
    } catch (IOException ex) {
      log.error(
          "Falha ao carregar entrada implementada productId={} cycleId={} prototypeVersion={}",
          cycle.getProductId(),
          cycle.getId(),
          cycle.getProductVersion(),
          ex);
      throw new IllegalStateException("Entrada implementada indisponível para o planejamento.", ex);
    }
  }
}
