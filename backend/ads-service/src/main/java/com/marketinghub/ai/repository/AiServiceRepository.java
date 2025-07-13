package com.marketinghub.ai.repository;

import com.marketinghub.ai.AiService;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for {@link AiService} entities.
 */
public interface AiServiceRepository extends JpaRepository<AiService, Long> {
}
