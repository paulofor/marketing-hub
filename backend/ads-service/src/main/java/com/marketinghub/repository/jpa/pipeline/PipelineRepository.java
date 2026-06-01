package com.marketinghub.repository.jpa.pipeline;

import com.marketinghub.pipeline.Pipeline;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA centralizado para persistir pipelines operacionais.
 */
public interface PipelineRepository extends JpaRepository<Pipeline, Long> {
}
