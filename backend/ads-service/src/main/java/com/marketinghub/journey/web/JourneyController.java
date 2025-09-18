package com.marketinghub.journey.web;

import com.marketinghub.journey.dto.JourneyRequest;
import com.marketinghub.journey.dto.JourneyResponse;
import com.marketinghub.journey.dto.JourneyUpdateRequest;
import com.marketinghub.journey.mapper.JourneyMapper;
import com.marketinghub.journey.model.Journey;
import com.marketinghub.journey.model.JourneyStatus;
import com.marketinghub.journey.service.JourneyService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for journey instances.
 */
@RestController
@RequestMapping("/api/journeys")
public class JourneyController {
    private final JourneyService journeyService;
    private final JourneyMapper mapper;

    public JourneyController(JourneyService journeyService, JourneyMapper mapper) {
        this.journeyService = journeyService;
        this.mapper = mapper;
    }

    @GetMapping
    public Page<JourneyResponse> list(@RequestParam(value = "templateId", required = false) Long templateId,
                                      @RequestParam(value = "status", required = false) JourneyStatus status,
                                      @PageableDefault(size = 20) Pageable pageable) {
        return journeyService.list(templateId, status, pageable).map(mapper::toJourneyResponse);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JourneyResponse create(@Valid @RequestBody JourneyRequest request) {
        Journey journey = journeyService.create(request);
        return mapper.toJourneyResponse(journeyService.get(journey.getId()));
    }

    @GetMapping("/{id}")
    public JourneyResponse get(@PathVariable Long id) {
        return mapper.toJourneyResponse(journeyService.get(id));
    }

    @PatchMapping("/{id}")
    public JourneyResponse update(@PathVariable Long id, @Valid @RequestBody JourneyUpdateRequest request) {
        Journey updated = journeyService.update(id, request);
        return mapper.toJourneyResponse(journeyService.get(updated.getId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        journeyService.delete(id);
    }
}
