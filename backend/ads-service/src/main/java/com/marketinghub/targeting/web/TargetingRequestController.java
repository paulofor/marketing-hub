package com.marketinghub.targeting.web;

import com.marketinghub.targeting.TargetingRequest;
import com.marketinghub.targeting.dto.CreateTargetingRequestPayload;
import com.marketinghub.targeting.dto.TargetingRequestDto;
import com.marketinghub.targeting.mapper.TargetingRequestMapper;
import com.marketinghub.targeting.service.TargetingRequestService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/targeting")
public class TargetingRequestController {
    private final TargetingRequestService service;
    private final TargetingRequestMapper mapper;

    public TargetingRequestController(TargetingRequestService service,
                                      TargetingRequestMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/requests")
    public TargetingRequestDto create(@Valid @RequestBody CreateTargetingRequestPayload payload) {
        TargetingRequest saved = service.create(payload);
        return mapper.toDto(saved, service.etaSeconds());
    }
}
