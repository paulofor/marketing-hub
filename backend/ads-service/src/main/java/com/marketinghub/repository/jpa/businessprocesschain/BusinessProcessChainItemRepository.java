package com.marketinghub.repository.jpa.businessprocesschain;

import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir e substituir os processos vinculados a uma cadeia versionada. */
public interface BusinessProcessChainItemRepository
    extends JpaRepository<BusinessProcessChainItem, Long> {
  /** Remove imediatamente os itens de um rascunho antes de reconstruir a sequência. */
  @Modifying(flushAutomatically = true)
  @Query(
      "delete from BusinessProcessChainItem item "
          + "where item.chainDefinition.id = :chainDefinitionId")
  int deleteByChainDefinitionId(@Param("chainDefinitionId") Long chainDefinitionId);
}
