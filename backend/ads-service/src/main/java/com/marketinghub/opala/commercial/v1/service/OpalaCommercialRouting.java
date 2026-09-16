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

/** Responsabilidade: navegar pela chamada Opala da cadeia exata sem migrar ciclos históricos. */
@Component
@RequiredArgsConstructor
public class OpalaCommercialRouting {
  private final BusinessProcessDefinitionRepository processes;
  private final BusinessProcessChainDefinitionRepository chains;
  private final BusinessProcessActivityDefinitionRepository activities;
  private final BusinessProcessActivityInstanceRepository instances;
  private final ProductRepository products;
  private final OpalaCommercialContext context;

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

  /** Localiza somente chamada explícita dentro da cadeia original do ciclo. */
  public BusinessProcessDefinition target(LearningSalesCycle cycle) {
    if (!isOpala(cycle.getProductId())) return null;
    var chain = chains.findById(cycle.getChainDefinitionId()).orElseThrow();
    boolean called =
        chain.getItems().stream()
            .map(i -> i.getProcessDefinition())
            .filter(p -> "pde-sales-delivery-learning".equals(p.getProcessCode()))
            .anyMatch(p -> hasCall(p));
    return called
        ? processes
            .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
                OpalaCommercialContext.CODE, "PUBLISHED")
            .orElse(null)
        : null;
  }

  /** Confere a chamada no grafo publicado, sem aceitar apenas parentesco cadastral. */
  private boolean hasCall(BusinessProcessDefinition process) {
    for (var node : context.read(process.getDiagramJson()).path("nodes"))
      if ("TASK".equals(node.path("type").asText())
          && OpalaCommercialContext.CODE.equals(node.path("subprocessCode").asText())) return true;
    return false;
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
                    .findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
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
              return saved.equals(context.snapshot("experiment:" + cycle.getExperimentId()));
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
