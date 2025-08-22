package com.marketinghub.prompt.service;

import com.marketinghub.prompt.PromptAttribute;
import com.marketinghub.prompt.PromptEntity;
import com.marketinghub.prompt.dto.CreatePromptAttributeRequest;
import com.marketinghub.prompt.dto.PromptAttributeDto;
import com.marketinghub.prompt.mapper.PromptAttributeMapper;
import com.marketinghub.prompt.repository.PromptAttributeRepository;
import com.marketinghub.prompt.repository.PromptEntityRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PromptAttributeService {
    private final PromptAttributeRepository attributeRepository;
    private final PromptEntityRepository entityRepository;
    private final PromptAttributeMapper mapper;

    public PromptAttributeService(PromptAttributeRepository attributeRepository,
                                  PromptEntityRepository entityRepository,
                                  PromptAttributeMapper mapper) {
        this.attributeRepository = attributeRepository;
        this.entityRepository = entityRepository;
        this.mapper = mapper;
    }

    public List<PromptAttributeDto> listLatest(String entityName) {
        List<PromptAttribute> attrs = attributeRepository.findByEntity_Name(entityName);
        Map<String, PromptAttribute> latest = attrs.stream()
                .collect(Collectors.toMap(PromptAttribute::getName, Function.identity(),
                        (a, b) -> a.getVersion() > b.getVersion() ? a : b));
        return latest.values().stream().map(mapper::toDto).toList();
    }

    public PromptAttributeDto create(String entityName, CreatePromptAttributeRequest req) {
        PromptEntity entity = entityRepository.findByName(entityName)
                .orElseGet(() -> entityRepository.save(PromptEntity.builder().name(entityName).build()));
        int nextVersion = attributeRepository.findTopByEntity_NameAndNameOrderByVersionDesc(entityName, req.getName())
                .map(PromptAttribute::getVersion)
                .orElse(0) + 1;
        PromptAttribute attr = PromptAttribute.builder()
                .entity(entity)
                .name(req.getName())
                .description(req.getDescription())
                .version(nextVersion)
                .build();
        attributeRepository.save(attr);
        return mapper.toDto(attr);
    }
}
