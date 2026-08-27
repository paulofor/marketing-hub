package com.marketinghub.repository.jpa.businessprocessresource;

import com.marketinghub.businessprocessresource.BusinessProcessExecutionResource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir e consultar o catálogo de recursos executáveis dos processos. */
public interface BusinessProcessExecutionResourceRepository
    extends JpaRepository<BusinessProcessExecutionResource, Long> {

  /** Lista somente recursos disponíveis para novas versões de processo. */
  List<BusinessProcessExecutionResource> findAllByActiveTrueOrderByNameAsc();

  /** Lista os recursos ativos pertencentes ao agente informado. */
  List<BusinessProcessExecutionResource> findAllByResponsibleAgentKeyAndActiveTrueOrderByNameAsc(
      String responsibleAgentKey);

  /** Resolve um recurso ativo pelo código estável gravado na atividade. */
  Optional<BusinessProcessExecutionResource> findByResourceCodeAndActiveTrue(String resourceCode);
}
