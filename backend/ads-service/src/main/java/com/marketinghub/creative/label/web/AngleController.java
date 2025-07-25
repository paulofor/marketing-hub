package com.marketinghub.creative.label.web;

import com.marketinghub.creative.label.dto.AngleDto;
import com.marketinghub.creative.label.dto.CreateAngleRequest;
import com.marketinghub.creative.label.mapper.AngleMapper;
import com.marketinghub.creative.label.service.AngleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/angles")
public class AngleController {
    private final AngleService service;
    private final AngleMapper mapper;

    public AngleController(AngleService service, AngleMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public AngleDto create(@Valid @RequestBody CreateAngleRequest request) {
        return mapper.toDto(service.create(request));
    }

    @PutMapping("/{id}")
    public AngleDto update(@PathVariable Long id, @Valid @RequestBody AngleDto dto) {
        return mapper.toDto(service.update(id, dto));
    }

    @GetMapping("/{id}")
    public AngleDto get(@PathVariable Long id) {
        return mapper.toDto(service.get(id));
    }

    @GetMapping
    public List<AngleDto> list() {
        return StreamSupport.stream(service.list().spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }
}
