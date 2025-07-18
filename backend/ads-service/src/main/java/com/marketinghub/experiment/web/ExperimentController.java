package com.marketinghub.experiment.web;

import com.marketinghub.experiment.dto.CreateExperimentRequest;
import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.experiment.mapper.ExperimentMapper;
import com.marketinghub.experiment.service.ExperimentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * REST controller for experiments.
 */
@RestController
@RequestMapping("/api/experiments")
public class ExperimentController {
    private final ExperimentService service;
    private final ExperimentMapper mapper;

    public ExperimentController(ExperimentService service, ExperimentMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/{id}/duplicate")
    public ExperimentDto duplicate(@PathVariable java.util.UUID id) {
        return mapper.toDto(service.duplicate(id));
    }

    @GetMapping("/{id}")
    public ExperimentDto get(@PathVariable java.util.UUID id) {
        return mapper.toDto(service.get(id));
    }

    @GetMapping
    public List<ExperimentDto> list() {
        return StreamSupport.stream(service.list().spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Atualiza apenas o status do experimento.
     */
    @PatchMapping("/{id}/status")
    public ExperimentDto updateStatus(
            @PathVariable java.util.UUID id,
            @RequestParam com.marketinghub.experiment.ExperimentStatus status) {
        return mapper.toDto(service.updateStatus(id, status));
    }

}
