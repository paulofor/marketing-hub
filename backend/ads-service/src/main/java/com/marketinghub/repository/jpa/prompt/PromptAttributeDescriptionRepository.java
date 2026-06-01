package com.marketinghub.repository.jpa.prompt;

import com.marketinghub.prompt.PromptAttributeDescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositório JPA responsável pela persistência de PromptAttributeDescription.
 */
public interface PromptAttributeDescriptionRepository extends JpaRepository<PromptAttributeDescription, Long> {
    Optional<PromptAttributeDescription> findByAttribute_IdAndActiveTrue(Long attributeId);
}
