package com.marketinghub.ai.generation.repository;

import com.marketinghub.ai.generation.AiWorkerGeneration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiWorkerGenerationRepository extends JpaRepository<AiWorkerGeneration, Long> {
    Page<AiWorkerGeneration> findByDomainIgnoreCase(String domain, Pageable pageable);
}
