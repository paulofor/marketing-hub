package com.marketinghub.creative.label.service;

import com.marketinghub.creative.label.EmotionalTrigger;
import com.marketinghub.creative.label.dto.CreateEmotionalTriggerRequest;
import com.marketinghub.creative.label.dto.EmotionalTriggerDto;
import com.marketinghub.creative.label.repository.EmotionalTriggerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmotionalTriggerService {
    private final EmotionalTriggerRepository repository;

    public EmotionalTriggerService(EmotionalTriggerRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public EmotionalTrigger create(CreateEmotionalTriggerRequest request) {
        EmotionalTrigger trigger = EmotionalTrigger.builder()
                .name(request.getName())
                .valence(request.getValence())
                .description(request.getDescription())
                .build();
        return repository.save(trigger);
    }

    public EmotionalTrigger get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public Iterable<EmotionalTrigger> list() {
        return repository.findAll();
    }

    @Transactional
    public EmotionalTrigger update(Long id, EmotionalTriggerDto dto) {
        EmotionalTrigger trigger = repository.findById(id).orElseThrow();
        trigger.setName(dto.getName());
        trigger.setValence(dto.getValence());
        trigger.setDescription(dto.getDescription());
        return repository.save(trigger);
    }
}
