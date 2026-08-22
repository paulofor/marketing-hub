package com.marketinghub.businessprocesschain.service;

import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.businessprocesschain.service.getChain.BusinessProcessChainDetailResponse;
import com.marketinghub.businessprocesschain.service.getChain.BusinessProcessChainProcessResponse;
import com.marketinghub.businessprocesschain.service.listChains.BusinessProcessChainSummaryResponse;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: apresentar cadeias e processos sem executar ou avançar trabalho operacional.
 */
@Service
@RequiredArgsConstructor
public class BusinessProcessChainService {
  private static final String PUBLISHED_STATUS = "PUBLISHED";

  private final BusinessProcessChainDefinitionRepository repository;

  /** Lista somente as cadeias publicadas em uso, com contagens calculadas pelo backend. */
  @Transactional(readOnly = true)
  public List<BusinessProcessChainSummaryResponse> listChains() {
    return repository.findAllByStatusOrderByNameAscVersionNumberDesc(PUBLISHED_STATUS).stream()
        .map(this::summary)
        .toList();
  }

  /** Lista as cadeias às quais pertence uma versão exata de processo. */
  @Transactional(readOnly = true)
  public List<BusinessProcessChainSummaryResponse> listChainsByProcess(Long processDefinitionId) {
    return repository.findByProcessDefinitionId(processDefinitionId).stream()
        .map(this::summary)
        .toList();
  }

  /** Exibe a cadeia selecionada com as versões exatas de seus processos em ordem. */
  @Transactional(readOnly = true)
  public BusinessProcessChainDetailResponse getChain(Long id) {
    BusinessProcessChainDefinition chain =
        repository
            .findById(id)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Cadeia de processos não encontrada."));
    List<BusinessProcessChainProcessResponse> processes =
        chain.getItems().stream()
            .sorted(Comparator.comparing(BusinessProcessChainItem::getSequenceNumber))
            .map(this::process)
            .toList();
    return new BusinessProcessChainDetailResponse(
        chain.getId(),
        chain.getChainCode(),
        chain.getName(),
        chain.getPurpose(),
        chain.getOutcomeDescription(),
        chain.getPrimaryMetric(),
        chain.getVersionNumber(),
        chain.getStatus(),
        processes.size(),
        chain.getCreatedAt(),
        chain.getPublishedAt(),
        processes);
  }

  /** Converte a cadeia persistida no contrato enxuto da listagem. */
  private BusinessProcessChainSummaryResponse summary(BusinessProcessChainDefinition chain) {
    return new BusinessProcessChainSummaryResponse(
        chain.getId(),
        chain.getChainCode(),
        chain.getName(),
        chain.getPurpose(),
        chain.getOutcomeDescription(),
        chain.getPrimaryMetric(),
        chain.getVersionNumber(),
        chain.getStatus(),
        chain.getItems().size(),
        chain.getPublishedAt());
  }

  /** Converte um vínculo da cadeia no contrato comercial de processo e contribuição de valor. */
  private BusinessProcessChainProcessResponse process(BusinessProcessChainItem item) {
    var definition = item.getProcessDefinition();
    return new BusinessProcessChainProcessResponse(
        item.getSequenceNumber(),
        item.getValueContribution(),
        definition.getId(),
        definition.getProcessCode(),
        definition.getName(),
        definition.getPurpose(),
        definition.getOwnerName(),
        definition.getTriggerDescription(),
        definition.getOutcomeDescription(),
        definition.getVersionNumber(),
        definition.getStatus());
  }
}
