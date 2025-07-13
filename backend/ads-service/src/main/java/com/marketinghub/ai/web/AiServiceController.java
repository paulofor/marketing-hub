package com.marketinghub.ai.web;

import com.marketinghub.ai.dto.CreateAiServiceRequest;
import com.marketinghub.ai.dto.AiServiceDto;
import com.marketinghub.ai.mapper.AiServiceMapper;
import com.marketinghub.ai.service.AiServiceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * REST controller for AI services.
 */
@RestController
@RequestMapping("/api/ai-services")
public class AiServiceController {
    private final AiServiceService service;
    private final AiServiceMapper mapper;

    public AiServiceController(AiServiceService service, AiServiceMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public AiServiceDto create(@RequestBody CreateAiServiceRequest request) {
        return mapper.toDto(service.create(request));
    }

    @GetMapping("/{id}")
    public AiServiceDto get(@PathVariable Long id) {
        return mapper.toDto(service.get(id));
    }

    @GetMapping
    public List<AiServiceDto> list() {
        return StreamSupport.stream(service.list().spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }
}
