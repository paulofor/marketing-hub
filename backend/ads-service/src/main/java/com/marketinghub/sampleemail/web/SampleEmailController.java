package com.marketinghub.sampleemail.web;

import com.marketinghub.sampleemail.dto.CreateSampleEmailRequest;
import com.marketinghub.sampleemail.dto.SampleEmailDto;
import com.marketinghub.sampleemail.mapper.SampleEmailMapper;
import com.marketinghub.sampleemail.service.SampleEmailService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints para consulta e manutenção dos e-mails de amostra de um experimento.
 */
@RestController
@RequestMapping("/api/experiments/{experimentId}/sample-emails")
public class SampleEmailController {
    private final SampleEmailService service;
    private final SampleEmailMapper mapper;

    public SampleEmailController(SampleEmailService service, SampleEmailMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<SampleEmailDto> list(@PathVariable Long experimentId) {
        return service.listByExperiment(experimentId).stream().map(mapper::toDto).toList();
    }

    @PostMapping
    public SampleEmailDto create(@PathVariable Long experimentId, @RequestBody CreateSampleEmailRequest request) {
        return mapper.toDto(service.create(experimentId, request));
    }
}
