package com.marketinghub.experiment.service;

import com.marketinghub.experiment.MetricPreset;
import com.marketinghub.repository.jpa.experiment.MetricPresetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service layer for metric presets.
 */
@Service
public class MetricPresetService {
    private final MetricPresetRepository repository;

    public MetricPresetService(MetricPresetRepository repository) {
        this.repository = repository;
    }

    public Iterable<MetricPreset> list() {
        return repository.findAll();
    }

    public MetricPreset get(String id) {
        if (id == null || id.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "metricPresetId required");
        }
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "metricPresetId not found: " + id));
    }
}
