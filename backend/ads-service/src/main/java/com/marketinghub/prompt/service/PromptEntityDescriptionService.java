package com.marketinghub.prompt.service;

import com.marketinghub.prompt.PromptEntity;
import com.marketinghub.prompt.PromptEntityDescription;
import com.marketinghub.prompt.dto.PromptEntityDescriptionDto;
import com.marketinghub.prompt.dto.UpdatePromptEntityDescriptionRequest;
import com.marketinghub.prompt.repository.PromptEntityDescriptionRepository;
import com.marketinghub.prompt.repository.PromptEntityRepository;
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

    public PromptEntityDescriptionDto getLatest(String entityName) {
        return descriptionRepository.findByEntity_NameAndActiveTrue(entityName)
                .map(desc -> {
                    PromptEntityDescriptionDto dto = new PromptEntityDescriptionDto();
                    dto.setDescription(desc.getDescription());
                    return dto;
                })
                .orElse(null);
    }

    public PromptEntityDescriptionDto update(String entityName, UpdatePromptEntityDescriptionRequest req) {
        PromptEntity entity = entityRepository.findByName(entityName)
                .orElseGet(() -> entityRepository.save(PromptEntity.builder().name(entityName).build()));
        descriptionRepository.findByEntity_NameAndActiveTrue(entityName)
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
