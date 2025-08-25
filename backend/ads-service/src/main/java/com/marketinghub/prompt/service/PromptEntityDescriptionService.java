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
        return descriptionRepository.findTopByEntity_NameAndActiveTrueOrderByVersionDesc(entityName)
                .map(desc -> {
                    PromptEntityDescriptionDto dto = new PromptEntityDescriptionDto();
                    dto.setDescription(desc.getDescription());
                    dto.setVersion(desc.getVersion());
                    return dto;
                })
                .orElse(null);
    }

    public PromptEntityDescriptionDto update(String entityName, UpdatePromptEntityDescriptionRequest req) {
        PromptEntity entity = entityRepository.findByName(entityName)
                .orElseGet(() -> entityRepository.save(PromptEntity.builder().name(entityName).build()));
        descriptionRepository.findTopByEntity_NameAndActiveTrueOrderByVersionDesc(entityName)
                .ifPresent(prev -> {
                    prev.setActive(false);
                    descriptionRepository.save(prev);
                });
        int nextVersion = descriptionRepository.findTopByEntity_NameOrderByVersionDesc(entityName)
                .map(PromptEntityDescription::getVersion)
                .orElse(0) + 1;
        PromptEntityDescription desc = PromptEntityDescription.builder()
                .entity(entity)
                .description(req.getDescription())
                .version(nextVersion)
                .active(true)
                .build();
        descriptionRepository.save(desc);
        PromptEntityDescriptionDto dto = new PromptEntityDescriptionDto();
        dto.setDescription(desc.getDescription());
        dto.setVersion(desc.getVersion());
        return dto;
    }
}
