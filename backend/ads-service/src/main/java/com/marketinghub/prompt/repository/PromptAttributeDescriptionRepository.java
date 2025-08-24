package com.marketinghub.prompt.repository;

import com.marketinghub.prompt.PromptAttributeDescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromptAttributeDescriptionRepository extends JpaRepository<PromptAttributeDescription, Long> {
    Optional<PromptAttributeDescription> findTopByAttribute_IdOrderByVersionDesc(Long attributeId);
}
