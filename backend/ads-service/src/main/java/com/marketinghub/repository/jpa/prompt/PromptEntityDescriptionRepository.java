package com.marketinghub.repository.jpa.prompt;

import com.marketinghub.prompt.PromptEntityDescription;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pela persistência de PromptEntityDescription. */
public interface PromptEntityDescriptionRepository
    extends JpaRepository<PromptEntityDescription, Long> {
  Optional<PromptEntityDescription> findByEntity_IdAndActiveTrue(Long entityId);
}
