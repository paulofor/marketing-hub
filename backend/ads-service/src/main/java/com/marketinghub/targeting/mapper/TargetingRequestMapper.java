package com.marketinghub.targeting.mapper;

import com.marketinghub.targeting.TargetingRequest;
import com.marketinghub.targeting.dto.TargetingRequestDto;
import org.springframework.stereotype.Component;

@Component
public class TargetingRequestMapper {
    private final TargetingCandidateMapper candidateMapper;

    public TargetingRequestMapper(TargetingCandidateMapper candidateMapper) {
        this.candidateMapper = candidateMapper;
    }

    public TargetingRequestDto toDto(TargetingRequest request, Integer etaSeconds) {
        return toDto(request, etaSeconds, false);
    }

    public TargetingRequestDto toDetailedDto(TargetingRequest request, Integer etaSeconds) {
        return toDto(request, etaSeconds, true);
    }

    private TargetingRequestDto toDto(TargetingRequest request, Integer etaSeconds, boolean includeCandidates) {
        if (request == null) return null;
        TargetingRequestDto.TargetingRequestDtoBuilder builder = TargetingRequestDto.builder()
                .id(request.getId())
                .descricao(request.getDescricao())
                .idioma(request.getLocale())
                .pais(request.getCountry())
                .publicoTipo(request.getAudienceType())
                .nicheId(request.getNiche() != null ? request.getNiche().getId() : null)
                .hypothesisId(request.getHypothesis() != null ? request.getHypothesis().getId() : null)
                .status(request.getStatus())
                .origin(request.getOrigin())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .etaSeconds(etaSeconds);
        if (includeCandidates && request.getCandidates() != null) {
            builder.candidates(request.getCandidates().stream().map(candidateMapper::toDto).toList());
        }
        return builder.build();
    }
}
