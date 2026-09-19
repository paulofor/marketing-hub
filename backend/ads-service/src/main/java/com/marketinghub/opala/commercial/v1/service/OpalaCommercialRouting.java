package com.marketinghub.opala.commercial.v1.service;

import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: navegar pela chamada Opala ou adesão explícita sem substituir a cadeia
 * histórica.
 */
@Component
@RequiredArgsConstructor
public class OpalaCommercialRouting {
  private final BusinessProcessDefinitionRepository processes;
  private final BusinessProcessChainDefinitionRepository chains;
  private final BusinessProcessActivityDefinitionRepository activities;
  private final BusinessProcessActivityInstanceRepository instances;
  private final ProductRepository products;
  private final OpalaCommercialContext context;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private com.marketinghub.repository.jdbc.catalogovivo.OpalaAdoptionRepository catalogAdoptions;

  /** Reconhece o tipo persistido, sem inferir o mineral pelo nome ou formato do produto. */
  public boolean isOpala(Long productId) {
    return products
        .findById(productId)
        .map(
            p ->
                p.getProductTypeDefinition() != null
                    && "PDE".equals(p.getProductTypeDefinition().getCode()))
        .orElse(false);
  }

  /** Localiza a definição fixada na adesão explícita ou a chamada da cadeia original do ciclo. */
  public BusinessProcessDefinition target(LearningSalesCycle cycle) {
    if (!isOpala(cycle.getProductId())) return null;
    if (catalogAdoptions != null) {
      var adoption = catalogAdoptions.find(cycle.getId());
      if (adoption.isPresent()
          && adoption.get().productId() == cycle.getProductId()
          && adoption.get().experimentId() == cycle.getExperimentId()) {
        return processes.findById(adoption.get().processDefinitionId()).orElseThrow();
      }
    }
    var chain = chains.findById(cycle.getChainDefinitionId()).orElseThrow();
    return chain.getItems().stream()
        .map(item -> calledTarget(item.getProcessDefinition()))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  /** Resolve a chamada no grafo e preserva a versão exata declarada na rota do tipo. */
  private BusinessProcessDefinition calledTarget(BusinessProcessDefinition process) {
    for (var node : context.read(process.getDiagramJson()).path("nodes")) {
      if (!"TASK".equals(node.path("type").asText())) continue;
      if (OpalaCommercialContext.CODE.equals(node.path("subprocessCode").asText()))
        return processes
            .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
                OpalaCommercialContext.CODE, "PUBLISHED")
            .orElse(null);
      for (var route : node.path("subprocessRoutes"))
        if ("PDE".equals(route.path("productTypeCode").asText())
            && OpalaCommercialContext.CODE.equals(route.path("subprocessCode").asText())) {
          int version = route.path("subprocessVersion").asInt(-1);
          if (version < 1) throw new IllegalStateException("A rota Opala não possui versão exata.");
          return processes
              .findByProcessCodeAndVersionNumber(OpalaCommercialContext.CODE, version)
              .filter(candidate -> "PUBLISHED".equals(candidate.getStatus()))
              .orElse(null);
        }
    }
    return null;
  }

  /** Reconhece a prova final da versão atual sem tratar ausência de prova como sucesso. */
  public boolean completed(LearningSalesCycle cycle) {
    var target = target(cycle);
    if (target == null) return false;
    return activities
        .findByProcessDefinitionIdAndActivityId(target.getId(), "ready")
        .flatMap(
            a ->
                instances
                    .findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
                        a.getId(), "experiment:" + cycle.getExperimentId()))
        .filter(i -> i.isObjectiveAchieved() && "COMPLETED".equals(i.getStatus()))
        .map(
            i -> {
              var proof = context.read(i.getObjectiveEvidenceJson());
              if (!Objects.equals(cycle.getProductVersion(), proof.path("productVersion").asText()))
                return false;
              if (!"OPEN".equals(cycle.getStatus())
                  || !Set.of("AUTHORIZATION", "PUBLICATION").contains(cycle.getStage()))
                return true;
              var saved = ((com.fasterxml.jackson.databind.node.ObjectNode) proof).deepCopy();
              saved.remove(List.of("evidenceType", "salesProven"));
              return OpalaCommercialAssetComparator.sameReviewableAssets(
                  saved, context.snapshot("experiment:" + cycle.getExperimentId()));
            })
        .orElse(false);
  }

  /** Abre a preparação antes da homologação final apenas nas cadeias que adotaram a chamada. */
  public String navigation(LearningSalesCycle cycle) {
    if (!"OPEN".equals(cycle.getStatus())
        || !Set.of("AUTHORIZATION", "PUBLICATION").contains(cycle.getStage())) return null;
    var target = target(cycle);
    return target == null || completed(cycle)
        ? null
        : "/products/"
            + cycle.getProductId()
            + "/value-chain-history/processes/"
            + target.getId()
            + "/activities?learningCycleId="
            + cycle.getId()
            + "&chainId="
            + cycle.getChainDefinitionId();
  }
}
