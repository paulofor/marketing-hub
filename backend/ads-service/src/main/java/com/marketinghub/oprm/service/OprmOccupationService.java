package com.marketinghub.oprm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.oprm.OprmOccupation;
import com.marketinghub.oprm.dto.OprmOccupationResponseDto;
import com.marketinghub.oprm.dto.OprmOccupationUpsertRequestDto;
import com.marketinghub.oprm.repository.OprmOccupationRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OprmOccupationService {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final OprmOccupationRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<OprmOccupationResponseDto> listOccupations() {
        return repository.findAllByOrderByDisplayNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OprmOccupationResponseDto createOccupation(OprmOccupationUpsertRequestDto request) {
        String normalizedSeedRef = normalizeSeedRef(request.occupationSeedRef());
        repository.findByOccupationSeedRefIgnoreCase(normalizedSeedRef).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "occupationSeedRef already exists");
        });

        OprmOccupation saved = repository.save(OprmOccupation.builder()
                .occupationSeedRef(normalizedSeedRef)
                .displayName(request.displayName().trim())
                .aliasesJson(writeAliasesJson(request.aliases()))
                .active(request.active())
                .build());

        return toResponse(saved);
    }

    @Transactional
    public OprmOccupationResponseDto updateOccupation(UUID id, OprmOccupationUpsertRequestDto request) {
        OprmOccupation occupation = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "OPRM occupation not found"));

        String normalizedSeedRef = normalizeSeedRef(request.occupationSeedRef());
        repository.findByOccupationSeedRefIgnoreCase(normalizedSeedRef)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "occupationSeedRef already exists");
                });

        occupation.setOccupationSeedRef(normalizedSeedRef);
        occupation.setDisplayName(request.displayName().trim());
        occupation.setAliasesJson(writeAliasesJson(request.aliases()));
        occupation.setActive(request.active());

        return toResponse(repository.save(occupation));
    }

    @Transactional
    public void deleteOccupation(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "OPRM occupation not found");
        }
        repository.deleteById(id);
    }

    private OprmOccupationResponseDto toResponse(OprmOccupation occupation) {
        return new OprmOccupationResponseDto(
                occupation.getId().toString(),
                occupation.getOccupationSeedRef(),
                occupation.getDisplayName(),
                readAliases(occupation.getAliasesJson()),
                occupation.isActive(),
                toIso(occupation.getCreatedAt()),
                toIso(occupation.getUpdatedAt())
        );
    }

    private String toIso(Instant value) {
        return value == null ? null : value.toString();
    }

    private String normalizeSeedRef(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "occupationSeedRef is required");
        }
        return normalized;
    }

    private String writeAliasesJson(List<String> aliases) {
        List<String> normalizedAliases = aliases == null
                ? List.of()
                : aliases.stream()
                        .map(alias -> alias == null ? "" : alias.trim())
                        .filter(alias -> !alias.isBlank())
                        .collect(java.util.stream.Collectors.collectingAndThen(
                                java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                                List::copyOf
                        ));
        try {
            return objectMapper.writeValueAsString(normalizedAliases);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "aliases payload is invalid");
        }
    }

    private List<String> readAliases(String aliasesJson) {
        if (aliasesJson == null || aliasesJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(aliasesJson, STRING_LIST);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
