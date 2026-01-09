package com.marketinghub.interactionjourney.web;

import com.marketinghub.interactionjourney.dto.InteractionJourneyDto;
import com.marketinghub.interactionjourney.service.InteractionJourneyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interaction-journeys")
@RequiredArgsConstructor
public class InteractionJourneyController {
    private final InteractionJourneyService service;

    @GetMapping
    public List<InteractionJourneyDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public InteractionJourneyDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public InteractionJourneyDto create(@RequestBody InteractionJourneyDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public InteractionJourneyDto update(@PathVariable Long id, @RequestBody InteractionJourneyDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
