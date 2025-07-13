package com.marketinghub.ai.service;

import com.marketinghub.ai.AiService;
import com.marketinghub.ai.dto.CreateAiServiceRequest;
import com.marketinghub.ai.repository.AiServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for AI services.
 */
@Service
public class AiServiceService {
    private final AiServiceRepository repository;

    public AiServiceService(AiServiceRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates and stores an AI service.
     */
    @Transactional
    public AiService create(CreateAiServiceRequest request) {
        AiService service = AiService.builder()
                .name(request.getName())
                .objective(request.getObjective())
                .price(request.getPrice())
                .cost(request.getCost())
                .build();
        return repository.save(service);
    }

    public AiService get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public Iterable<AiService> list() {
        return repository.findAll();
    }
}
