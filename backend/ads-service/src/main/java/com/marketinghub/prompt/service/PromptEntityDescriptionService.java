package com.marketinghub.prompt.service;

import com.marketinghub.prompt.PromptEntity;
import com.marketinghub.prompt.PromptEntityDescription;
import com.marketinghub.prompt.dto.PromptEntityDescriptionDto;
import com.marketinghub.prompt.dto.UpdatePromptEntityDescriptionRequest;
import com.marketinghub.repository.jpa.prompt.PromptEntityDescriptionRepository;
import com.marketinghub.repository.jpa.prompt.PromptEntityRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PromptEntityDescriptionService {
    private final PromptEntityDescriptionRepository descriptionRepository;
    private final PromptEntityRepository entityRepository;

    public PromptEntityDescriptionService(PromptEntityDescriptionRepository descriptionRepository,
                                          PromptEntityRepository entityRepository) {
        this.descriptionRepository = descriptionRepository;
        this.entityRepository = entityRepository;
    }

    public PromptEntityDescriptionDto getLatest(Long entityId) {
        return descriptionRepository.findByEntity_IdAndActiveTrue(entityId)
                .map(desc -> {
                    PromptEntityDescriptionDto dto = new PromptEntityDescriptionDto();
                    dto.setDescription(desc.getDescription());
                    return dto;
                })
                .orElse(null);
    }

    public PromptEntityDescriptionDto update(Long entityId, UpdatePromptEntityDescriptionRequest req) {
        PromptEntity entity = entityRepository.findById(entityId)
                .orElseThrow(() -> new EntityNotFoundException("PromptEntity not found"));
        descriptionRepository.findByEntity_IdAndActiveTrue(entityId)
                .ifPresent(prev -> {
                    prev.setActive(false);
                    descriptionRepository.save(prev);
                });
        PromptEntityDescription desc = PromptEntityDescription.builder()
                .entity(entity)
                .description(req.getDescription())
                .active(true)
                .build();
        descriptionRepository.save(desc);
        PromptEntityDescriptionDto dto = new PromptEntityDescriptionDto();
        dto.setDescription(desc.getDescription());
        return dto;
    }
}
