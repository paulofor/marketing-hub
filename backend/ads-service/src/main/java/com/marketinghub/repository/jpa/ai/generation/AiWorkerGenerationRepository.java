package com.marketinghub.repository.jpa.ai.generation;

import com.marketinghub.ai.generation.AiWorkerGeneration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de AiWorkerGeneration.
 */
public interface AiWorkerGenerationRepository extends JpaRepository<AiWorkerGeneration, Long> {
    Page<AiWorkerGeneration> findByDomainIgnoreCase(String domain, Pageable pageable);

    Page<AiWorkerGeneration> findByReferenceId(String referenceId, Pageable pageable);

    Page<AiWorkerGeneration> findByDomainIgnoreCaseAndReferenceId(String domain, String referenceId, Pageable pageable);

    void deleteByDomainIgnoreCaseAndReferenceId(String domain, String referenceId);
}
