package com.marketinghub.repository.jpa.businessprocesschain;

import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: consultar cadeias versionadas com seus processos ordenados. */
public interface BusinessProcessChainDefinitionRepository
    extends JpaRepository<BusinessProcessChainDefinition, Long> {
  /** Lista as cadeias e carrega os processos necessários ao resumo oficial. */
  @EntityGraph(attributePaths = {"items", "items.processDefinition"})
  List<BusinessProcessChainDefinition> findAllByOrderByNameAscVersionNumberDesc();

  /** Busca uma cadeia e carrega os processos necessários ao detalhe oficial. */
  @Override
  @EntityGraph(attributePaths = {"items", "items.processDefinition"})
  Optional<BusinessProcessChainDefinition> findById(Long id);
}
