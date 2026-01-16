package com.marketinghub.prompt.mapper;

import com.marketinghub.prompt.PromptDomain;
import com.marketinghub.prompt.PromptDomainObject;
import com.marketinghub.prompt.dto.PromptDomainDto;
import com.marketinghub.prompt.dto.PromptDomainObjectDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PromptDomainMapper {
    public PromptDomainDto toDto(PromptDomain domain) {
        if (domain == null) {
            return null;
        }
        PromptDomainDto dto = new PromptDomainDto();
        dto.setId(domain.getId());
        dto.setCode(domain.getCode());
        dto.setName(domain.getName());
        dto.setDescription(domain.getDescription());
        dto.setCreatedAt(domain.getCreatedAt());
        dto.setUpdatedAt(domain.getUpdatedAt());
        dto.setObjects(mapObjects(domain.getObjects()));
        return dto;
    }

    private List<PromptDomainObjectDto> mapObjects(List<PromptDomainObject> objects) {
        if (objects == null || objects.isEmpty()) {
            return List.of();
        }
        List<PromptDomainObjectDto> list = new ArrayList<>();
        for (PromptDomainObject object : objects) {
            PromptDomainObjectDto dto = new PromptDomainObjectDto();
            dto.setType(object.getObjectType().name());
            dto.setSlug(object.getObjectType().getSlug());
            dto.setLabel(object.getObjectType().getLabel());
            dto.setContextKey(object.getObjectType().getContextKey());
            list.add(dto);
        }
        return list;
    }
}
