package com.marketinghub.repository.jpa.openai;

import com.marketinghub.openai.OpenAiModel;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de OpenAiModel.
 */
public interface OpenAiModelRepository extends JpaRepository<OpenAiModel, Long> {
    Optional<OpenAiModel> findByCode(String code);
}
