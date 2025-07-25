package com.marketinghub.experiment.web;

import com.marketinghub.experiment.dto.CreateCreativeRequest;
import com.marketinghub.experiment.dto.CreativeVariantDto;
import com.marketinghub.experiment.mapper.CreativeVariantMapper;
import com.marketinghub.experiment.service.CreativeVariantService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * REST controller for creative variants.
 */
@RestController
@RequestMapping("/api/creatives")
public class CreativeVariantController {
    private final CreativeVariantService service;
    private final CreativeVariantMapper mapper;

    public CreativeVariantController(CreativeVariantService service, CreativeVariantMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public CreativeVariantDto create(@RequestBody CreateCreativeRequest request) {
        return mapper.toDto(service.create(request));
    }

    @GetMapping
    public List<CreativeVariantDto> list(@RequestParam Long experimentId) {
        return StreamSupport.stream(service.listByExperiment(experimentId).spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }
}
