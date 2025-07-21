package com.marketinghub.creative.label.web;

import com.marketinghub.creative.label.dto.CreateEmotionalTriggerRequest;
import com.marketinghub.creative.label.dto.EmotionalTriggerDto;
import com.marketinghub.creative.label.mapper.EmotionalTriggerMapper;
import com.marketinghub.creative.label.service.EmotionalTriggerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/emotional-triggers")
public class EmotionalTriggerController {
    private final EmotionalTriggerService service;
    private final EmotionalTriggerMapper mapper;

    public EmotionalTriggerController(EmotionalTriggerService service, EmotionalTriggerMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public EmotionalTriggerDto create(@RequestBody CreateEmotionalTriggerRequest request) {
        return mapper.toDto(service.create(request));
    }

    @GetMapping("/{id}")
    public EmotionalTriggerDto get(@PathVariable Long id) {
        return mapper.toDto(service.get(id));
    }

    @GetMapping
    public List<EmotionalTriggerDto> list() {
        return StreamSupport.stream(service.list().spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }
}
