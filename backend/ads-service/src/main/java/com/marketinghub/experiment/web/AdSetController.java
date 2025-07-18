package com.marketinghub.experiment.web;

import com.marketinghub.experiment.dto.AdSetDto;
import com.marketinghub.experiment.dto.CreateAdSetRequest;
import com.marketinghub.experiment.mapper.AdSetMapper;
import com.marketinghub.experiment.service.AdSetService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * REST controller for ad sets.
 */
@RestController
@RequestMapping("/api/adsets")
public class AdSetController {
    private final AdSetService service;
    private final AdSetMapper mapper;

    public AdSetController(AdSetService service, AdSetMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public AdSetDto create(@RequestBody CreateAdSetRequest request) {
        return mapper.toDto(service.create(request));
    }

    @GetMapping
    public List<AdSetDto> list(@RequestParam Long experimentId) {
        return StreamSupport.stream(service.listByExperiment(experimentId).spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }
}
