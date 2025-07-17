package com.marketinghub.experiment.web;

import com.marketinghub.experiment.dto.CreateExperimentRequest;
import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.experiment.mapper.ExperimentMapper;
import com.marketinghub.experiment.service.ExperimentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Nested routes for experiments under a niche.
 */
@RestController
@RequestMapping("/api/niches/{nicheId}/experiments")
public class NicheExperimentController {
    private final ExperimentService service;
    private final ExperimentMapper mapper;

    public NicheExperimentController(ExperimentService service, ExperimentMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ExperimentDto create(@PathVariable Long nicheId, @RequestBody CreateExperimentRequest request) {
        return mapper.toDto(service.create(nicheId, request));
    }

    @GetMapping
    public List<ExperimentDto> list(@PathVariable Long nicheId) {
        return StreamSupport.stream(service.listByNiche(nicheId).spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }
}
