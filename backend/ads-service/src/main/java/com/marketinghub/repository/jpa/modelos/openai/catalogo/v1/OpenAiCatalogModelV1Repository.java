package com.marketinghub.repository.jpa.modelos.openai.catalogo.v1;

import com.marketinghub.modelos.openai.catalogo.v1.entity.OpenAiCatalogModelV1;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de OpenAiCatalogModelV1.
 */
public interface OpenAiCatalogModelV1Repository extends JpaRepository<OpenAiCatalogModelV1, Long> {
    Optional<OpenAiCatalogModelV1> findByCode(String code);
}
