package com.marketinghub.businessprocesscomposition.service;

import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesscomposition.service.getcomposition.BusinessProcessCompositionResponse;
import com.marketinghub.businessprocesscomposition.service.getcomposition.BusinessProcessReferenceResponse;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: expor a composição oficial e vigente dos processos de negócio. */
@Service
public class BusinessProcessCompositionService {
  private final BusinessProcessDefinitionRepository repository;

  /** Configura o acesso ao catálogo persistido de processos. */
  public BusinessProcessCompositionService(BusinessProcessDefinitionRepository repository) {
    this.repository = repository;
  }

  /** Retorna pai e filhos vigentes do processo informado sem inferência no frontend. */
  @Transactional(readOnly = true)
  public BusinessProcessCompositionResponse getComposition(Long processDefinitionId) {
    BusinessProcessDefinition process =
        repository
            .findById(processDefinitionId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Processo não encontrado."));
    BusinessProcessDefinition parent = findPublishedParent(process);
    List<BusinessProcessReferenceResponse> subprocesses = findPublishedSubprocesses(process);
    return new BusinessProcessCompositionResponse(
        reference(process),
        parent == null ? null : reference(parent),
        subprocesses.size(),
        subprocesses);
  }

  /** Localiza o processo de valor pai vigente quando a definição consultada é um subprocesso. */
  private BusinessProcessDefinition findPublishedParent(BusinessProcessDefinition process) {
    if (process.getParentProcessCode() == null) {
      return null;
    }
    return repository
        .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            process.getParentProcessCode(), "PUBLISHED")
        .orElse(null);
  }

  /** Lista somente subprocessos publicados quando a definição consultada é um processo de valor. */
  private List<BusinessProcessReferenceResponse> findPublishedSubprocesses(
      BusinessProcessDefinition process) {
    if (!"VALUE_PROCESS".equals(processType(process)) || "RETIRED".equals(process.getStatus())) {
      return List.of();
    }
    return repository
        .findAllByParentProcessCodeAndStatusOrderByNameAscVersionNumberDesc(
            process.getProcessCode(), "PUBLISHED")
        .stream()
        .map(this::reference)
        .toList();
  }

  /** Converte a entidade em referência enxuta para navegação e explicação da hierarquia. */
  private BusinessProcessReferenceResponse reference(BusinessProcessDefinition process) {
    return new BusinessProcessReferenceResponse(
        process.getId(),
        process.getProcessCode(),
        process.getName(),
        process.getPurpose(),
        process.getOwnerName(),
        process.getVersionNumber(),
        process.getStatus(),
        processType(process));
  }

  /** Interpreta registros anteriores à classificação como processos de valor. */
  private String processType(BusinessProcessDefinition process) {
    return process.getProcessType() == null ? "VALUE_PROCESS" : process.getProcessType();
  }
}
