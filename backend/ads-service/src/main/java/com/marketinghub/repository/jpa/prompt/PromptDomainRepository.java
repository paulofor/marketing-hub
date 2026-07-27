package com.marketinghub.repository.jpa.prompt;

import com.marketinghub.prompt.PromptDomain;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pela persistência de PromptDomain. */
public interface PromptDomainRepository extends JpaRepository<PromptDomain, Long> {
  @EntityGraph(attributePaths = "objects")
  Optional<PromptDomain> findByCodeIgnoreCase(String code);

  boolean existsByCodeIgnoreCase(String code);
}
