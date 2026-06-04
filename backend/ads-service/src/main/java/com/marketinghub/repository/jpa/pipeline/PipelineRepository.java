package com.marketinghub.repository.jpa.pipeline;

import com.marketinghub.pipeline.Pipeline;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA centralizado para persistir pipelines operacionais.
 */
public interface PipelineRepository extends JpaRepository<Pipeline, Long> {
    /**
     * Localiza um pipeline pelo código operacional único.
     */
    Optional<Pipeline> findByCode(String code);
}
