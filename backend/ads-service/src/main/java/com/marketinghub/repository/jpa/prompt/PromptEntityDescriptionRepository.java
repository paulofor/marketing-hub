package com.marketinghub.repository.jpa.prompt;

import com.marketinghub.prompt.PromptEntityDescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositório JPA responsável pela persistência de PromptEntityDescription.
 */
public interface PromptEntityDescriptionRepository extends JpaRepository<PromptEntityDescription, Long> {
    Optional<PromptEntityDescription> findByEntity_IdAndActiveTrue(Long entityId);
}
