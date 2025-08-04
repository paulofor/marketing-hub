package com.marketinghub.experiment.web;

import com.marketinghub.experiment.dto.MetricPresetDto;
import com.marketinghub.experiment.mapper.MetricPresetMapper;
import com.marketinghub.experiment.service.MetricPresetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * REST controller for metric presets.
 */
@RestController
@RequestMapping("/api/metric-presets")
public class MetricPresetController {
    private final MetricPresetService service;
    private final MetricPresetMapper mapper;

    public MetricPresetController(MetricPresetService service, MetricPresetMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<MetricPresetDto> list() {
        return StreamSupport.stream(service.list().spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }
}
