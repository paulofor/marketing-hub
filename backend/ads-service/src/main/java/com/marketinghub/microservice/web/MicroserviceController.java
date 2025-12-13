package com.marketinghub.microservice.web;

import com.marketinghub.microservice.dto.CreateMicroserviceRequest;
import com.marketinghub.microservice.dto.MicroserviceDto;
import com.marketinghub.microservice.mapper.MicroserviceMapper;
import com.marketinghub.microservice.service.MicroserviceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * REST controller for microservice registry.
 */
@RestController
@RequestMapping("/api/microservices")
public class MicroserviceController {
    private final MicroserviceService service;
    private final MicroserviceMapper mapper;

    public MicroserviceController(MicroserviceService service, MicroserviceMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public MicroserviceDto create(@RequestBody CreateMicroserviceRequest request) {
        return mapper.toDto(service.create(request));
    }

    @GetMapping
    public List<MicroserviceDto> list() {
        return StreamSupport.stream(service.list().spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public MicroserviceDto get(@PathVariable Long id) {
        return mapper.toDto(service.get(id));
    }

    @PutMapping("/{id}")
    public MicroserviceDto update(@PathVariable Long id, @RequestBody CreateMicroserviceRequest request) {
        return mapper.toDto(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
