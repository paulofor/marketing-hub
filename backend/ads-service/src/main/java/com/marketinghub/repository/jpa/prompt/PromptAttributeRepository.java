package com.marketinghub.repository.jpa.prompt;

import com.marketinghub.prompt.PromptAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório JPA responsável pela persistência de PromptAttribute.
 */
public interface PromptAttributeRepository extends JpaRepository<PromptAttribute, Long> {
    List<PromptAttribute> findByEntity_Name(String entityName);
    Optional<PromptAttribute> findByEntity_NameAndName(String entityName, String name);
    void deleteByEntity_NameAndName(String entityName, String name);
}
