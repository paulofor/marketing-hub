package com.marketinghub.repository.jpa.oprm.niche;

import com.marketinghub.oprm.niche.OprmNicheCatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de OprmNicheCatalogItem.
 */
public interface OprmNicheCatalogItemRepository extends JpaRepository<OprmNicheCatalogItem, Long> {
}
