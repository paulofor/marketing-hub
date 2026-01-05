package com.marketinghub.openai.repository;

import com.marketinghub.openai.OpenAiModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpenAiModelRepository extends JpaRepository<OpenAiModel, Long> {}
