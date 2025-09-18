package com.marketinghub.journey.web;

import com.marketinghub.journey.dto.JourneyStepResponse;
import com.marketinghub.journey.dto.JourneyStepUpdateRequest;
import com.marketinghub.journey.mapper.JourneyMapper;
import com.marketinghub.journey.service.JourneyStepService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Controller exposing operations on individual journey steps.
 */
@RestController
@RequestMapping("/api/journey-steps")
public class JourneyStepController {
    private final JourneyStepService stepService;
    private final JourneyMapper mapper;

    public JourneyStepController(JourneyStepService stepService, JourneyMapper mapper) {
        this.stepService = stepService;
        this.mapper = mapper;
    }

    @GetMapping("/{id}")
    public JourneyStepResponse get(@PathVariable Long id) {
        return mapper.toStepResponse(stepService.get(id));
    }

    @PatchMapping("/{id}")
    public JourneyStepResponse update(@PathVariable Long id, @Valid @RequestBody JourneyStepUpdateRequest request) {
        return mapper.toStepResponse(stepService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        stepService.delete(id);
    }
}
