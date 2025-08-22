package com.marketinghub.prompt.repository;

import com.marketinghub.prompt.PromptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromptEntityRepository extends JpaRepository<PromptEntity, Long> {
    Optional<PromptEntity> findByName(String name);
}
