package com.marketinghub.repository.jpa.prompt;

import com.marketinghub.prompt.PromptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositório JPA responsável pela persistência de PromptEntity.
 */
public interface PromptEntityRepository extends JpaRepository<PromptEntity, Long> {
    Optional<PromptEntity> findByName(String name);
}
