package com.marketinghub.catalogovivo.v1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessChainPositionResponse;
import com.marketinghub.repository.jdbc.catalogovivo.OpalaAdoptionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: localizar subprocessos no grafo da cadeia e preservar a posição Opala adotada
 * em ciclos históricos.
 */
@Component
@Slf4j
public class OpalaAdoptionChainPositionResolver {
  private static final String OPALA_PROCESS_CODE = "opala-commercial-preparation-v1";
  private static final String VALUE_PROCESS_CODE = "pde-commercial-homologation-activation";
  private static final int SUBPROCESS_SEQUENCE = 2;

  private final OpalaAdoptionRepository adoptions;
  private final BusinessProcessChainDefinitionRepository chains;
  private final BusinessProcessDefinitionRepository processes;
  private final ObjectMapper json;

  /** Mantém a construção enxuta usada por testes da posição histórica. */
  public OpalaAdoptionChainPositionResolver(
      OpalaAdoptionRepository adoptions, BusinessProcessChainDefinitionRepository chains) {
    this(adoptions, chains, null, new ObjectMapper());
  }

  /** Configura adesões, cadeia, processos e o leitor do contrato de composição tipada. */
  @Autowired
  public OpalaAdoptionChainPositionResolver(
      OpalaAdoptionRepository adoptions,
      BusinessProcessChainDefinitionRepository chains,
      BusinessProcessDefinitionRepository processes,
      ObjectMapper json) {
    this.adoptions = adoptions;
    this.chains = chains;
    this.processes = processes;
    this.json = json;
  }

  /**
   * Retorna a posição 5.2 somente para uma adesão Opala que pertença exatamente ao produto, ciclo e
   * cadeia consultados.
   */
  public Optional<ProductProcessChainPositionResponse> resolve(
      long productId, long processDefinitionId, String processCode, Long cycleId, Long chainId) {
    if (cycleId == null || chainId == null) return Optional.empty();
    var adoption = adoptions.find(cycleId).orElse(null);
    if (OPALA_PROCESS_CODE.equals(processCode) && adoption != null) {
      if (adoption.productId() != productId
          || adoption.processDefinitionId() != processDefinitionId
          || !adoptions.permits(
              cycleId,
              productId,
              chainId,
              processDefinitionId,
              "experiment:" + adoption.experimentId())) return Optional.empty();
      return historicalPosition(chainId);
    }
    return graphPosition(processDefinitionId, processCode, chainId);
  }

  /** Preserva a numeração 5.2 já adotada pelo ciclo histórico sem reinterpretar seu grafo. */
  private Optional<ProductProcessChainPositionResponse> historicalPosition(Long chainId) {
    return chains
        .findById(chainId)
        .flatMap(
            chain ->
                chain.getItems().stream()
                    .filter(
                        item ->
                            VALUE_PROCESS_CODE.equals(item.getProcessDefinition().getProcessCode()))
                    .findFirst()
                    .map(
                        item ->
                            new ProductProcessChainPositionResponse(
                                item.getSequenceNumber() + "." + SUBPROCESS_SEQUENCE,
                                VALUE_PROCESS_CODE,
                                item.getProcessDefinition().getName())));
  }

  /**
   * Calcula a posição pela chamada real da cadeia e pela versão exata do filho quando declarada.
   */
  private Optional<ProductProcessChainPositionResponse> graphPosition(
      long processDefinitionId, String processCode, long chainId) {
    if (processes == null) return Optional.empty();
    var target = processes.findById(processDefinitionId).orElse(null);
    if (target == null || !processCode.equals(target.getProcessCode())) return Optional.empty();
    return chains
        .findById(chainId)
        .flatMap(
            chain ->
                chain.getItems().stream()
                    .filter(item -> calledTaskSequence(item.getProcessDefinition(), target) != null)
                    .findFirst()
                    .map(
                        item ->
                            new ProductProcessChainPositionResponse(
                                item.getSequenceNumber()
                                    + "."
                                    + calledTaskSequence(item.getProcessDefinition(), target),
                                item.getProcessDefinition().getProcessCode(),
                                item.getProcessDefinition().getName())));
  }

  /** Localiza a ordem da atividade delegadora, contando apenas tarefas do processo pai. */
  private Integer calledTaskSequence(
      com.marketinghub.businessprocess.BusinessProcessDefinition parent,
      com.marketinghub.businessprocess.BusinessProcessDefinition target) {
    try {
      int sequence = 0;
      for (var node : json.readTree(parent.getDiagramJson()).path("nodes")) {
        if (!"TASK".equals(node.path("type").asText())) continue;
        sequence++;
        if (target.getProcessCode().equals(node.path("subprocessCode").asText())) return sequence;
        for (var route : node.path("subprocessRoutes"))
          if (target.getProcessCode().equals(route.path("subprocessCode").asText())
              && target.getVersionNumber() == route.path("subprocessVersion").asInt(-1))
            return sequence;
      }
      return null;
    } catch (Exception ex) {
      log.error(
          "Falha ao resolver posição da chamada de subprocesso. parentProcessDefinitionId={} childProcessDefinitionId={}",
          parent.getId(),
          target.getId(),
          ex);
      throw new IllegalStateException("A composição tipada da cadeia está inválida.", ex);
    }
  }
}
