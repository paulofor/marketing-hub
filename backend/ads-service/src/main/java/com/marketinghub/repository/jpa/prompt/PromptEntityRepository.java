package com.marketinghub.repository.jpa.prompt;

import com.marketinghub.prompt.PromptEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pela persistência de PromptEntity. */
public interface PromptEntityRepository extends JpaRepository<PromptEntity, Long> {
  Optional<PromptEntity> findByName(String name);
}
