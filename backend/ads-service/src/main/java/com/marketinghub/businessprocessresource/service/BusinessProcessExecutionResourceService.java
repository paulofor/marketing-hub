package com.marketinghub.businessprocessresource.service;

import com.marketinghub.businessprocessresource.BusinessProcessExecutionResource;
import com.marketinghub.businessprocessresource.service.listResources.BusinessProcessExecutionResourceResponse;
import com.marketinghub.repository.jpa.businessprocessresource.BusinessProcessExecutionResourceRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: oferecer o catálogo ativo de recursos especializados dos processos. */
@Service
public class BusinessProcessExecutionResourceService {
  private final BusinessProcessExecutionResourceRepository repository;

  /** Configura a fonte de verdade persistida dos recursos executáveis. */
  public BusinessProcessExecutionResourceService(
      BusinessProcessExecutionResourceRepository repository) {
    this.repository = repository;
  }

  /** Lista os recursos ativos em ordem legível para a edição de atividades. */
  @Transactional(readOnly = true)
  public List<BusinessProcessExecutionResourceResponse> listResources() {
    return repository.findAllByActiveTrueOrderByNameAsc().stream().map(this::response).toList();
  }

  /** Converte a entidade persistida no contrato público imutável. */
  private BusinessProcessExecutionResourceResponse response(
      BusinessProcessExecutionResource resource) {
    return new BusinessProcessExecutionResourceResponse(
        resource.getId(),
        resource.getResourceCode(),
        resource.getName(),
        resource.getDescription(),
        resource.getResourceType(),
        resource.getResponsibleAgentKey(),
        resource.getExecutorReference(),
        resource.getUsageInstructions());
  }
}
