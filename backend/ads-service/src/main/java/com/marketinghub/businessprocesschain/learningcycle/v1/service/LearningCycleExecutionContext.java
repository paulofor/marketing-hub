package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: vincular a execução dos BPMs ao produto e experimento explícitos do ciclo. */
@Component
@RequiredArgsConstructor
public class LearningCycleExecutionContext {
  private final LearningSalesCycleRepository cycles;
  private final BusinessProcessChainDefinitionRepository chains;
  private final BusinessProcessDefinitionRepository processes;
  private final LearningCycleJson json;

  /**
   * Resolve a fonte canônica sem trocar a seleção do plano nem escolher o experimento mais novo.
   */
  @Transactional(readOnly = true)
  public String source(
      Long cycleId, Product product, BusinessProcessDefinition process, boolean command) {
    var cycle =
        cycles
            .findById(cycleId)
            .filter(value -> value.getProductId().equals(product.getId()))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Ciclo do produto não encontrado."));
    if (command && !"OPEN".equals(cycle.getStatus()))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O ciclo foi encerrado; consulte seu histórico ou abra o sucessor.");
    var chain = chains.findById(cycle.getChainDefinitionId()).orElseThrow();
    Set<Long> members = new HashSet<>();
    chain.getItems().forEach(item -> members.add(item.getProcessDefinition().getId()));
    var current = process;
    Set<String> visited = new HashSet<>();
    while (!members.contains(current.getId())) {
      String parent = current.getParentProcessCode();
      if (parent == null || !visited.add(parent))
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "O processo não pertence à versão da cadeia deste ciclo.");
      current =
          processes
              .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(parent, "PUBLISHED")
              .orElseThrow(
                  () ->
                      new ResponseStatusException(
                          HttpStatus.CONFLICT, "Processo pai indisponível nesta cadeia."));
    }
    if ("pde-construction-approval".equals(process.getProcessCode())) {
      String contract = product.getValidationDefinitionVersion();
      if ("PDE_AGENT_VALIDATION_V1".equals(contract) || "PDE_AGENT_VALIDATED_V1".equals(contract))
        return "product:" + product.getId() + "@agent-validation-v1";
      if ("PDE_PRIVATE_VALIDATION_V1".equals(contract))
        return "product:" + product.getId() + "@private-validation-v1";
    }
    return "experiment:" + cycle.getExperimentId();
  }

  /** Permite revalidar produto comercial somente na versão e etapa de um único ciclo aberto. */
  @Transactional(readOnly = true)
  public boolean permitsRevalidation(Product product) {
    if (product == null || product.getValidationDefinitionJson() == null) return false;
    var active = cycles.findByProductIdAndOpenSlot(product.getId(), 1);
    if (active.size() != 1) return false;
    var cycle = active.getFirst();
    String version =
        json.read(product.getValidationDefinitionJson())
            .path("privatePrototypeAcceptance")
            .path("prototypeVersion")
            .asText();
    return "OPEN".equals(cycle.getStatus())
        && Set.of("ADJUSTMENT", "VALIDATION").contains(cycle.getStage())
        && cycle.getProductVersion().equals(version);
  }
}
