package com.marketinghub.prompt.repository;

import com.marketinghub.prompt.PromptAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromptAttributeRepository extends JpaRepository<PromptAttribute, Long> {
    List<PromptAttribute> findByEntity_Name(String entityName);
    Optional<PromptAttribute> findTopByEntity_NameAndNameOrderByVersionDesc(String entityName, String name);
}
