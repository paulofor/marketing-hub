package com.marketinghub.ai.generation.service;

import com.marketinghub.ai.generation.AiWorkerGeneration;
import com.marketinghub.ai.generation.dto.AiWorkerGenerationRequest;
import com.marketinghub.ai.generation.repository.AiWorkerGenerationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class AiWorkerGenerationService {
    private final AiWorkerGenerationRepository repository;

    public AiWorkerGenerationService(AiWorkerGenerationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AiWorkerGeneration recordGeneration(AiWorkerGenerationRequest request) {
        BigDecimal cost = request.getCostUsd();
        if (cost == null) {
            cost = BigDecimal.ZERO;
        } else {
            cost = cost.setScale(4, RoundingMode.HALF_UP);
        }
        AiWorkerGeneration generation = AiWorkerGeneration.builder()
                .domain(request.getDomain())
                .referenceId(request.getReferenceId())
                .prompt(request.getPrompt())
                .rawResponse(request.getRawResponse())
                .model(request.getModel())
                .inputTokens(request.getInputTokens())
                .outputTokens(request.getOutputTokens())
                .costUsd(cost)
                .build();
        return repository.save(generation);
    }

    @Transactional(readOnly = true)
    public Page<AiWorkerGeneration> list(String domain, Pageable pageable) {
        if (StringUtils.hasText(domain)) {
            return repository.findByDomainIgnoreCase(domain, pageable);
        }
        return repository.findAll(pageable);
    }
}
