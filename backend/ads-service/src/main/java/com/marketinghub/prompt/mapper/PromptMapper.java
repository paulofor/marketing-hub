package com.marketinghub.prompt.mapper;

import com.marketinghub.prompt.Prompt;
import com.marketinghub.prompt.dto.PromptDto;
import org.springframework.stereotype.Component;

@Component
public class PromptMapper {
    public PromptDto toDto(Prompt prompt) {
        if (prompt == null) return null;
        PromptDto dto = new PromptDto();
        dto.setId(prompt.getId());
        dto.setName(prompt.getName());
        dto.setDomain(prompt.getDomain());
        dto.setTemplate(prompt.getTemplate());
        dto.setActive(prompt.isActive());
        dto.setCreatedAt(prompt.getCreatedAt());
        dto.setUpdatedAt(prompt.getUpdatedAt());
        return dto;
    }
}
