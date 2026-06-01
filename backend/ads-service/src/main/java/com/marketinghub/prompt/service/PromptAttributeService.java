package com.marketinghub.prompt.service;

import com.marketinghub.prompt.PromptAttribute;
import com.marketinghub.prompt.PromptAttributeDescription;
import com.marketinghub.prompt.PromptEntity;
import com.marketinghub.prompt.dto.CreatePromptAttributeRequest;
import com.marketinghub.prompt.dto.PromptAttributeDto;
import com.marketinghub.prompt.dto.UpdatePromptAttributeRequest;
import com.marketinghub.prompt.mapper.PromptAttributeMapper;
import com.marketinghub.repository.jpa.prompt.PromptAttributeDescriptionRepository;
import com.marketinghub.repository.jpa.prompt.PromptAttributeRepository;
import com.marketinghub.repository.jpa.prompt.PromptEntityRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PromptAttributeService {
    private final PromptAttributeRepository attributeRepository;
    private final PromptAttributeDescriptionRepository descriptionRepository;
    private final PromptEntityRepository entityRepository;
    private final PromptAttributeMapper mapper;

    public PromptAttributeService(PromptAttributeRepository attributeRepository,
                                  PromptAttributeDescriptionRepository descriptionRepository,
                                  PromptEntityRepository entityRepository,
                                  PromptAttributeMapper mapper) {
        this.attributeRepository = attributeRepository;
        this.descriptionRepository = descriptionRepository;
        this.entityRepository = entityRepository;
        this.mapper = mapper;
    }

    public List<PromptAttributeDto> listLatest(String entityName) {
        List<PromptAttribute> attrs = attributeRepository.findByEntity_Name(entityName);
        return attrs.stream().map(attr -> {
            PromptAttributeDescription desc = descriptionRepository
                    .findByAttribute_IdAndActiveTrue(attr.getId())
                    .orElse(null);
            return mapper.toDto(attr, desc);
        }).toList();
    }

    public PromptAttributeDto create(String entityName, CreatePromptAttributeRequest req) {
        PromptAttribute attribute = attributeRepository
                .findByEntity_NameAndName(entityName, req.getName())
                .orElseGet(() -> {
                    PromptEntity entity = entityRepository.findByName(entityName)
                            .orElseThrow(() -> new EntityNotFoundException("PromptEntity not found"));
                    PromptAttribute newAttr = PromptAttribute.builder()
                            .entity(entity)
                            .name(req.getName())
                            .build();
                    return attributeRepository.save(newAttr);
                });
        descriptionRepository.findByAttribute_IdAndActiveTrue(attribute.getId())
                .ifPresent(prev -> {
                    prev.setActive(false);
                    descriptionRepository.save(prev);
                });
        PromptAttributeDescription desc = PromptAttributeDescription.builder()
                .attribute(attribute)
                .description(req.getDescription())
                .active(true)
                .build();
        descriptionRepository.save(desc);
        return mapper.toDto(attribute, desc);
    }

    public PromptAttributeDto getLatest(String entityName, String attrName) {
        PromptAttribute attribute = attributeRepository
                .findByEntity_NameAndName(entityName, attrName)
                .orElseThrow(() -> new EntityNotFoundException("PromptAttribute not found"));
        PromptAttributeDescription desc = descriptionRepository
                .findByAttribute_IdAndActiveTrue(attribute.getId())
                .orElseThrow(() -> new EntityNotFoundException("PromptAttributeDescription not found"));
        return mapper.toDto(attribute, desc);
    }

    public PromptAttributeDto update(String entityName, String attrName, UpdatePromptAttributeRequest req) {
        PromptAttribute attribute = attributeRepository
                .findByEntity_NameAndName(entityName, attrName)
                .orElseThrow(() -> new EntityNotFoundException("PromptAttribute not found"));
        descriptionRepository.findByAttribute_IdAndActiveTrue(attribute.getId())
                .ifPresent(prev -> {
                    prev.setActive(false);
                    descriptionRepository.save(prev);
                });
        PromptAttributeDescription desc = PromptAttributeDescription.builder()
                .attribute(attribute)
                .description(req.getDescription())
                .active(true)
                .build();
        descriptionRepository.save(desc);
        return mapper.toDto(attribute, desc);
    }

    public void delete(String entityName, String attrName) {
        attributeRepository.findByEntity_NameAndName(entityName, attrName)
                .ifPresent(attribute -> {
                    attributeRepository.delete(attribute);
                });
    }
}
