package com.marketinghub.ai.generation.web;

import com.marketinghub.ai.generation.dto.AiWorkerGenerationDto;
import com.marketinghub.ai.generation.dto.AiWorkerGenerationRequest;
import com.marketinghub.ai.generation.mapper.AiWorkerGenerationMapper;
import com.marketinghub.ai.generation.service.AiWorkerGenerationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/ai/generations")
public class AiWorkerGenerationController {
    private final AiWorkerGenerationService service;
    private final AiWorkerGenerationMapper mapper;

    public AiWorkerGenerationController(AiWorkerGenerationService service,
                                        AiWorkerGenerationMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }


    @PostMapping("/internal")
    @ResponseStatus(HttpStatus.CREATED)
    public AiWorkerGenerationDto create(@RequestBody AiWorkerGenerationRequest request) {
        return mapper.toDto(service.recordGeneration(request));
    }

    @GetMapping
    public Page<AiWorkerGenerationDto> list(
            @RequestParam(value = "domain", required = false) String domain,
            @RequestParam(value = "referenceId", required = false) String referenceId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(domain, referenceId, pageable).map(mapper::toDto);
    }
}
