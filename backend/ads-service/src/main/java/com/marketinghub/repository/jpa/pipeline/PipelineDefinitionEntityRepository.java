package com.marketinghub.repository.jpa.pipeline;

import com.marketinghub.pipeline.PipelineDefinitionEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA centralizado para definições persistentes de pipelines oficiais.
 */
public interface PipelineDefinitionEntityRepository extends JpaRepository<PipelineDefinitionEntity, Long> {
    /**
     * Busca uma definição persistida pelo código canônico e versão canônica.
     */
    Optional<PipelineDefinitionEntity> findByCodeAndCanonicalVersion(String code, String canonicalVersion);
}
