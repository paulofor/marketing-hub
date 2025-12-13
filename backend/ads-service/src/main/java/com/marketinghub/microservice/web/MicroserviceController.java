package com.marketinghub.microservice.web;

import com.marketinghub.microservice.Microservice;
import com.marketinghub.microservice.dto.CreateMicroserviceRequest;
import com.marketinghub.microservice.dto.MicroserviceDto;
import com.marketinghub.microservice.exception.dto.MicroserviceExceptionSummary;
import com.marketinghub.microservice.exception.service.MicroserviceExceptionService;
import com.marketinghub.microservice.mapper.MicroserviceMapper;
import com.marketinghub.microservice.service.MicroserviceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for microservice registry.
 */
@RestController
@RequestMapping("/api/microservices")
public class MicroserviceController {
    private final MicroserviceService service;
    private final MicroserviceMapper mapper;
    private final MicroserviceExceptionService exceptionService;

    public MicroserviceController(MicroserviceService service, MicroserviceMapper mapper,
                                  MicroserviceExceptionService exceptionService) {
        this.service = service;
        this.mapper = mapper;
        this.exceptionService = exceptionService;
    }

    @PostMapping
    public MicroserviceDto create(@RequestBody CreateMicroserviceRequest request) {
        Microservice created = service.create(request);
        MicroserviceExceptionSummary summary = exceptionService.summarizeByMicroservices(List.of(created)).get(created.getId());
        return mapper.toDto(created, summary);
    }

    @GetMapping
    public List<MicroserviceDto> list() {
        List<Microservice> microservices = service.list();
        Map<Long, MicroserviceExceptionSummary> summaries = exceptionService.summarizeByMicroservices(microservices);
        return microservices.stream()
                .map(ms -> mapper.toDto(ms, summaries.get(ms.getId())))
                .toList();
    }

    @GetMapping("/{id}")
    public MicroserviceDto get(@PathVariable Long id) {
        Microservice microservice = service.get(id);
        MicroserviceExceptionSummary summary = exceptionService.summarizeByMicroservices(List.of(microservice)).get(microservice.getId());
        return mapper.toDto(microservice, summary);
    }

    @PutMapping("/{id}")
    public MicroserviceDto update(@PathVariable Long id, @RequestBody CreateMicroserviceRequest request) {
        Microservice microservice = service.update(id, request);
        MicroserviceExceptionSummary summary = exceptionService.summarizeByMicroservices(List.of(microservice)).get(microservice.getId());
        return mapper.toDto(microservice, summary);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
