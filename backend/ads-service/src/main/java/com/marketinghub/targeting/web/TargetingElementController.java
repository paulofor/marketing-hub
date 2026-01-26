package com.marketinghub.targeting.web;

import com.marketinghub.targeting.TargetingElementStatus;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.dto.CreateTargetingElementRequest;
import com.marketinghub.targeting.dto.TargetingElementDto;
import com.marketinghub.targeting.dto.UpdateTargetingElementRequest;
import com.marketinghub.targeting.mapper.TargetingElementMapper;
import com.marketinghub.targeting.service.TargetingElementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST para gerenciar interesses, cargos e comportamentos.
 */
@RestController
@RequestMapping("/api")
public class TargetingElementController {
    private final TargetingElementService service;
    private final TargetingElementMapper mapper;

    public TargetingElementController(TargetingElementService service,
                                      TargetingElementMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/targeting-elements")
    public TargetingElementDto create(@RequestBody CreateTargetingElementRequest request) {
        return mapper.toDto(service.create(request));
    }

    @PatchMapping("/targeting-elements/{id}")
    public TargetingElementDto update(@PathVariable Long id,
                                      @RequestBody UpdateTargetingElementRequest request) {
        return mapper.toDto(service.update(id, request));
    }

    @GetMapping("/targeting-elements/{id}")
    public TargetingElementDto get(@PathVariable Long id) {
        return mapper.toDto(service.get(id));
    }

    @GetMapping("/targeting-elements")
    public List<TargetingElementDto> list(@RequestParam(value = "type", required = false) TargetingElementType type,
                                          @RequestParam(value = "status", required = false) TargetingElementStatus status) {
        return service.list(type, status).stream().map(mapper::toDto).toList();
    }

    @GetMapping("/niches/{nicheId}/targeting-elements")
    public List<TargetingElementDto> listByNiche(@PathVariable Long nicheId,
                                                 @RequestParam(value = "type", required = false) TargetingElementType type,
                                                 @RequestParam(value = "status", required = false) TargetingElementStatus status) {
        return service.listByNiche(nicheId, type, status).stream().map(mapper::toDto).toList();
    }
}
