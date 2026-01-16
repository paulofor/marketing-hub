package com.marketinghub.prompt.repository;

import com.marketinghub.prompt.PromptDomain;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromptDomainRepository extends JpaRepository<PromptDomain, Long> {
    @EntityGraph(attributePaths = "objects")
    Optional<PromptDomain> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}
