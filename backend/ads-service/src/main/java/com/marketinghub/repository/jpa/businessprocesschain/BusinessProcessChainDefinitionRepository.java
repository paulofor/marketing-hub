package com.marketinghub.repository.jpa.businessprocesschain;

import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: consultar cadeias versionadas com seus processos ordenados. */
public interface BusinessProcessChainDefinitionRepository
    extends JpaRepository<BusinessProcessChainDefinition, Long> {
  /** Lista as cadeias de um status e carrega os processos necessários ao resumo oficial. */
  @EntityGraph(attributePaths = {"items", "items.processDefinition"})
  List<BusinessProcessChainDefinition> findAllByStatusOrderByNameAscVersionNumberDesc(
      String status);

  /** Lista as cadeias que contêm uma versão exata de processo. */
  @EntityGraph(attributePaths = {"items", "items.processDefinition"})
  @Query(
      """
      select distinct chain
      from BusinessProcessChainDefinition chain
      join chain.items item
      where item.processDefinition.id = :processDefinitionId
      order by chain.name asc, chain.versionNumber desc
      """)
  List<BusinessProcessChainDefinition> findByProcessDefinitionId(
      @Param("processDefinitionId") Long processDefinitionId);

  /** Busca uma cadeia e carrega os processos necessários ao detalhe oficial. */
  @Override
  @EntityGraph(attributePaths = {"items", "items.processDefinition"})
  Optional<BusinessProcessChainDefinition> findById(Long id);
}
