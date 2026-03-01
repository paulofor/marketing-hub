package com.marketinghub.openai.repository;

import com.marketinghub.openai.OpenAiModel;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpenAiModelRepository extends JpaRepository<OpenAiModel, Long> {
    Optional<OpenAiModel> findByCode(String code);
}
