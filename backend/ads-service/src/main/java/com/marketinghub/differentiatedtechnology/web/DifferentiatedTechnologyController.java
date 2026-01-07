package com.marketinghub.differentiatedtechnology.web;

import com.marketinghub.differentiatedtechnology.dto.CreateDifferentiatedTechnologyRequest;
import com.marketinghub.differentiatedtechnology.dto.DifferentiatedTechnologyDto;
import com.marketinghub.differentiatedtechnology.dto.UpdateDifferentiatedTechnologyRequest;
import com.marketinghub.differentiatedtechnology.mapper.DifferentiatedTechnologyMapper;
import com.marketinghub.differentiatedtechnology.service.DifferentiatedTechnologyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/differentiated-technologies")
public class DifferentiatedTechnologyController {
    private final DifferentiatedTechnologyService service;
    private final DifferentiatedTechnologyMapper mapper;

    public DifferentiatedTechnologyController(DifferentiatedTechnologyService service,
                                              DifferentiatedTechnologyMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public DifferentiatedTechnologyDto create(@RequestBody CreateDifferentiatedTechnologyRequest request) {
        return mapper.toDto(service.create(request));
    }

    @GetMapping
    public List<DifferentiatedTechnologyDto> list() {
        return service.list().stream().map(mapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public DifferentiatedTechnologyDto get(@PathVariable Long id) {
        return mapper.toDto(service.get(id));
    }

    @PutMapping("/{id}")
    public DifferentiatedTechnologyDto update(@PathVariable Long id,
                                              @RequestBody UpdateDifferentiatedTechnologyRequest request) {
        return mapper.toDto(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
