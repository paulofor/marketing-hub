package com.marketinghub.repository.jpa.prompt;

import com.marketinghub.prompt.PromptAttributeDescription;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pela persistência de PromptAttributeDescription. */
public interface PromptAttributeDescriptionRepository
    extends JpaRepository<PromptAttributeDescription, Long> {
  Optional<PromptAttributeDescription> findByAttribute_IdAndActiveTrue(Long attributeId);
}
