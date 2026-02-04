package com.marketinghub.targeting.mapper;

import com.marketinghub.targeting.TargetingCandidate;
import com.marketinghub.targeting.TargetingOption;
import com.marketinghub.targeting.dto.TargetingCandidateDto;
import com.marketinghub.targeting.dto.TargetingOptionDto;
import org.springframework.stereotype.Component;

import java.util.Collection;
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
        List<String> variants = copyVariants(candidate.getSeedVariants());
        return TargetingCandidateDto.builder()
                .id(candidate.getId())
                .requestId(requestId)
                .seed(candidate.getSeed())
                .legacySeed(candidate.getSeed())
                .seedVariants(variants)
                .tipo(candidate.getType())
                .status(candidate.getStatus())
                .idiomaHint(candidate.getLocaleHint())
                .idioma(candidate.getLocaleHint())
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

    private List<String> copyVariants(List<String> variants) {
        if (variants == null || variants.isEmpty()) {
            return Collections.emptyList();
        }
        return List.copyOf(variants);
    }

    private List<TargetingOptionDto> mapOptions(Collection<TargetingOption> options) {
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
                .finalScore(option.getFinalScore())
                .path(option.getPath() == null ? Collections.emptyList() : List.copyOf(option.getPath()))
                .searchLocale(option.getSearchLocale())
                .searchCountry(option.getSearchCountry())
                .searchTerm(option.getSearchTerm())
                .source(option.getSource())
                .seedVariant(option.getSeedVariant())
                .createdAt(option.getCreatedAt())
                .updatedAt(option.getUpdatedAt())
                .build();
    }
}
