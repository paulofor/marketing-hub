package com.marketinghub.targeting.mapper;

import com.marketinghub.targeting.TargetingCandidate;
import com.marketinghub.targeting.TargetingOption;
import com.marketinghub.targeting.dto.TargetingCandidateDto;
import com.marketinghub.targeting.dto.TargetingOptionDto;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class TargetingCandidateMapper {
    public TargetingCandidateDto toDto(TargetingCandidate candidate) {
        if (candidate == null) {
            return null;
        }
        UUID requestId = candidate.getRequest() != null ? candidate.getRequest().getId() : null;
        return TargetingCandidateDto.builder()
                .id(candidate.getId())
                .requestId(requestId)
                .textoSugerido(candidate.getTextoSugerido())
                .tipo(candidate.getType())
                .status(candidate.getStatus())
                .idioma(candidate.getIdioma())
                .pais(candidate.getCountry())
                .origem(candidate.getOrigem())
                .intentTag(candidate.getIntentTag())
                .score(candidate.getScore())
                .rationale(candidate.getRationale())
                .rejectionReason(candidate.getRejectionReason())
                .createdAt(candidate.getCreatedAt())
                .updatedAt(candidate.getUpdatedAt())
                .options(mapOptions(candidate.getOptions()))
                .build();
    }

    private List<TargetingOptionDto> mapOptions(List<TargetingOption> options) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }
        return options.stream().map(this::toOptionDto).toList();
    }

    private TargetingOptionDto toOptionDto(TargetingOption option) {
        if (option == null) {
            return null;
        }
        return TargetingOptionDto.builder()
                .id(option.getId())
                .facebookId(option.getFacebookId())
                .name(option.getName())
                .type(option.getType())
                .audienceSize(option.getAudienceSize())
                .matchScore(option.getMatchScore())
                .path(option.getPath() == null ? Collections.emptyList() : List.copyOf(option.getPath()))
                .searchLocale(option.getSearchLocale())
                .searchCountry(option.getSearchCountry())
                .searchTerm(option.getSearchTerm())
                .createdAt(option.getCreatedAt())
                .updatedAt(option.getUpdatedAt())
                .build();
    }
}
