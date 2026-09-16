package com.marketinghub.catalogovivo.v1.service;

import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessChainPositionResponse;
import com.marketinghub.repository.jdbc.catalogovivo.OpalaAdoptionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: localizar o subprocesso Opala adotado na posição comercial correta da cadeia
 * histórica, sem reescrever a definição que foi fixada no ciclo.
 */
@Component
public class OpalaAdoptionChainPositionResolver {
  private static final String OPALA_PROCESS_CODE = "opala-commercial-preparation-v1";
  private static final String VALUE_PROCESS_CODE = "pde-commercial-homologation-activation";
  private static final int SUBPROCESS_SEQUENCE = 2;

  private final OpalaAdoptionRepository adoptions;
  private final BusinessProcessChainDefinitionRepository chains;

  /** Configura as fontes que comprovam a adesão e a posição na cadeia do ciclo. */
  public OpalaAdoptionChainPositionResolver(
      OpalaAdoptionRepository adoptions, BusinessProcessChainDefinitionRepository chains) {
    this.adoptions = adoptions;
    this.chains = chains;
  }

  /**
   * Retorna a posição 5.2 somente para uma adesão Opala que pertença exatamente ao produto, ciclo e
   * cadeia consultados.
   */
  public Optional<ProductProcessChainPositionResponse> resolve(
      long productId, long processDefinitionId, String processCode, Long cycleId, Long chainId) {
    if (!OPALA_PROCESS_CODE.equals(processCode) || cycleId == null || chainId == null)
      return Optional.empty();
    var adoption = adoptions.find(cycleId).orElse(null);
    if (adoption == null
        || adoption.productId() != productId
        || adoption.processDefinitionId() != processDefinitionId
        || !adoptions.permits(
            cycleId,
            productId,
            chainId,
            processDefinitionId,
            "experiment:" + adoption.experimentId())) return Optional.empty();
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
}
