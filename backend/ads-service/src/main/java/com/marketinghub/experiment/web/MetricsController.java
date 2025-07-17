package com.marketinghub.experiment.web;

import com.marketinghub.experiment.dto.MetricSnapshotDto;
import com.marketinghub.experiment.mapper.MetricSnapshotMapper;
import com.marketinghub.experiment.service.MetricService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * REST controller for metrics snapshots.
 */
@RestController
@RequestMapping("/api/metrics")
public class MetricsController {
    private final MetricService service;
    private final MetricSnapshotMapper mapper;

    public MetricsController(MetricService service, MetricSnapshotMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<MetricSnapshotDto> list() {
        return StreamSupport.stream(service.list().spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }
}
