package com.marketinghub.creative.label.web;

import com.marketinghub.creative.label.dto.CreateVisualProofRequest;
import com.marketinghub.creative.label.dto.VisualProofDto;
import com.marketinghub.creative.label.mapper.VisualProofMapper;
import com.marketinghub.creative.label.service.VisualProofService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/visual-proofs")
public class VisualProofController {
    private final VisualProofService service;
    private final VisualProofMapper mapper;

    public VisualProofController(VisualProofService service, VisualProofMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public VisualProofDto create(@RequestBody CreateVisualProofRequest request) {
        return mapper.toDto(service.create(request));
    }

    @GetMapping("/{id}")
    public VisualProofDto get(@PathVariable Long id) {
        return mapper.toDto(service.get(id));
    }

    @GetMapping
    public List<VisualProofDto> list() {
        return StreamSupport.stream(service.list().spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }
}
