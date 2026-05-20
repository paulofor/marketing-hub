package com.marketinghub.modelos.openai.catalogo.v1.repository;

import com.marketinghub.modelos.openai.catalogo.v1.entity.OpenAiCatalogModelV1;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpenAiCatalogModelV1Repository extends JpaRepository<OpenAiCatalogModelV1, Long> {
    Optional<OpenAiCatalogModelV1> findByCode(String code);
}
