package com.marketinghub.prompt.repository;

import com.marketinghub.prompt.PromptEntityDescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromptEntityDescriptionRepository extends JpaRepository<PromptEntityDescription, Long> {
    Optional<PromptEntityDescription> findTopByEntity_NameOrderByVersionDesc(String entityName);
}
