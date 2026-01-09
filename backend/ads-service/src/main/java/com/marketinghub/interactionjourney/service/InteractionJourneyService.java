package com.marketinghub.interactionjourney.service;

import com.marketinghub.interactionjourney.dto.InteractionJourneyDto;
import com.marketinghub.interactionjourney.dto.InteractionJourneyElementDto;
import com.marketinghub.interactionjourney.dto.InteractionJourneyStepDto;
import com.marketinghub.interactionjourney.model.InteractionJourney;
import com.marketinghub.interactionjourney.model.InteractionJourneyElement;
import com.marketinghub.interactionjourney.model.InteractionJourneyStep;
import com.marketinghub.interactionjourney.repository.InteractionJourneyRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InteractionJourneyService {
    private final InteractionJourneyRepository journeyRepository;

    @Transactional(readOnly = true)
    public List<InteractionJourneyDto> list() {
        return journeyRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public InteractionJourneyDto get(Long id) {
        InteractionJourney journey = journeyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Jornada de interação não encontrada: " + id));
        return toDto(journey);
    }

    @Transactional
    public InteractionJourneyDto create(InteractionJourneyDto dto) {
        validate(dto);
        InteractionJourney journey = new InteractionJourney();
        apply(dto, journey);
        InteractionJourney saved = journeyRepository.save(journey);
        return toDto(saved);
    }

    @Transactional
    public InteractionJourneyDto update(Long id, InteractionJourneyDto dto) {
        validate(dto);
        InteractionJourney journey = journeyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Jornada de interação não encontrada: " + id));

        journey.getSteps().clear();
        apply(dto, journey);

        InteractionJourney saved = journeyRepository.save(journey);
        return toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!journeyRepository.existsById(id)) {
            throw new EntityNotFoundException("Jornada de interação não encontrada: " + id);
        }
        journeyRepository.deleteById(id);
    }

    private void apply(InteractionJourneyDto dto, InteractionJourney journey) {
        journey.setName(dto.getName());
        journey.setDescription(dto.getDescription());

        List<InteractionJourneyStep> steps = new ArrayList<>();
        List<InteractionJourneyStepDto> stepDtos = Optional.ofNullable(dto.getSteps()).orElse(Collections.emptyList());
        for (int i = 0; i < stepDtos.size(); i++) {
            InteractionJourneyStepDto stepDto = stepDtos.get(i);
            InteractionJourneyStep step = new InteractionJourneyStep();
            step.setJourney(journey);
            step.setTitle(stepDto.getTitle());
            step.setDescription(stepDto.getDescription());
            step.setOrderIndex(stepDto.getOrderIndex() != null ? stepDto.getOrderIndex() : i);

            List<InteractionJourneyElement> elements = buildElements(step, stepDto.getElements(), null);
            step.setElements(elements);
            steps.add(step);
        }

        journey.getSteps().addAll(steps);
    }

    private List<InteractionJourneyElement> buildElements(InteractionJourneyStep step,
                                                         List<InteractionJourneyElementDto> elementDtos,
                                                         InteractionJourneyElement parent) {
        List<InteractionJourneyElementDto> safeDtos = Optional.ofNullable(elementDtos).orElse(Collections.emptyList());
        List<InteractionJourneyElement> elements = new ArrayList<>();

        for (int i = 0; i < safeDtos.size(); i++) {
            InteractionJourneyElementDto dto = safeDtos.get(i);
            InteractionJourneyElement element = new InteractionJourneyElement();
            element.setStep(step);
            element.setParent(parent);
            element.setLabel(dto.getLabel());
            element.setType(dto.getType());
            element.setNotes(dto.getNotes());
            element.setOrderIndex(dto.getOrderIndex() != null ? dto.getOrderIndex() : i);

            List<InteractionJourneyElement> children = buildElements(step, dto.getChildren(), element);
            element.setChildren(children);
            elements.add(element);
        }

        return elements;
    }

    private InteractionJourneyDto toDto(InteractionJourney journey) {
        return InteractionJourneyDto.builder()
                .id(journey.getId())
                .name(journey.getName())
                .description(journey.getDescription())
                .createdAt(journey.getCreatedAt())
                .updatedAt(journey.getUpdatedAt())
                .steps(journey.getSteps() == null ? List.of() : journey.getSteps().stream()
                        .map(this::toDto)
                        .toList())
                .build();
    }

    private InteractionJourneyStepDto toDto(InteractionJourneyStep step) {
        List<InteractionJourneyElement> rootElements = step.getElements() == null
                ? List.of()
                : step.getElements().stream()
                .filter(element -> element.getParent() == null)
                .toList();
        return InteractionJourneyStepDto.builder()
                .id(step.getId())
                .title(step.getTitle())
                .description(step.getDescription())
                .orderIndex(step.getOrderIndex())
                .elements(rootElements.stream()
                        .map(this::toDto)
                        .toList())
                .build();
    }

    private InteractionJourneyElementDto toDto(InteractionJourneyElement element) {
        return InteractionJourneyElementDto.builder()
                .id(element.getId())
                .label(element.getLabel())
                .type(element.getType())
                .notes(element.getNotes())
                .orderIndex(element.getOrderIndex())
                .children(element.getChildren() == null ? List.of() : element.getChildren().stream()
                        .map(this::toDto)
                        .toList())
                .build();
    }

    private void validate(InteractionJourneyDto dto) {
        if (!StringUtils.hasText(dto.getName())) {
            throw new IllegalArgumentException("O nome da jornada de interação é obrigatório.");
        }
    }
}
