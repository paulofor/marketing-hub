package com.marketinghub.journey.web;

import com.marketinghub.journey.dto.*;
import com.marketinghub.journey.mapper.JourneyMapper;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.journey.service.JourneyStepService;
import com.marketinghub.journey.service.JourneyTemplateService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing CRUD operations for journey templates and their steps.
 */
@RestController
@RequestMapping("/api/journey-templates")
public class JourneyTemplateController {
    private final JourneyTemplateService templateService;
    private final JourneyStepService stepService;
    private final JourneyMapper mapper;

    public JourneyTemplateController(JourneyTemplateService templateService,
                                     JourneyStepService stepService,
                                     JourneyMapper mapper) {
        this.templateService = templateService;
        this.stepService = stepService;
        this.mapper = mapper;
    }

    @GetMapping
    public Page<JourneyTemplateSummaryResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return templateService.list(pageable).map(mapper::toSummary);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JourneyTemplateResponse create(@Valid @RequestBody JourneyTemplateRequest request) {
        JourneyTemplate template = templateService.create(request);
        return mapper.toResponse(templateService.get(template.getId()));
    }

    @GetMapping("/{id}")
    public JourneyTemplateResponse get(@PathVariable Long id) {
        return mapper.toResponse(templateService.get(id));
    }

    @PatchMapping("/{id}")
    public JourneyTemplateResponse update(@PathVariable Long id,
                                          @Valid @RequestBody JourneyTemplateUpdateRequest request) {
        JourneyTemplate updated = templateService.update(id, request);
        return mapper.toResponse(templateService.get(updated.getId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        templateService.delete(id);
    }

    @GetMapping("/{id}/steps")
    public List<JourneyStepResponse> listSteps(@PathVariable Long id) {
        return stepService.listByTemplate(id).stream().map(mapper::toStepResponse).toList();
    }

    @PostMapping("/{id}/steps")
    @ResponseStatus(HttpStatus.CREATED)
    public JourneyStepResponse createStep(@PathVariable Long id, @Valid @RequestBody JourneyStepRequest request) {
        return mapper.toStepResponse(stepService.create(id, request));
    }
}
