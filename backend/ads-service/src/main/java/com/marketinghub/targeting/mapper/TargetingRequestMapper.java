package com.marketinghub.targeting.mapper;

import com.marketinghub.targeting.TargetingRequest;
import com.marketinghub.targeting.dto.TargetingRequestDto;
import org.springframework.stereotype.Component;

@Component
public class TargetingRequestMapper {
    public TargetingRequestDto toDto(TargetingRequest request, Integer etaSeconds) {
        if (request == null) return null;
        return TargetingRequestDto.builder()
                .id(request.getId())
                .descricao(request.getDescricao())
                .idioma(request.getLocale())
                .pais(request.getCountry())
                .publicoTipo(request.getAudienceType())
                .status(request.getStatus())
                .origin(request.getOrigin())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .etaSeconds(etaSeconds)
                .build();
    }
}
