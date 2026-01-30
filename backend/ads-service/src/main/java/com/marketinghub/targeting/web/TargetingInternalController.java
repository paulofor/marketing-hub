package com.marketinghub.targeting.web;

import com.marketinghub.targeting.TargetingRequest;
import com.marketinghub.targeting.dto.TargetingCandidateIngestionRequest;
import com.marketinghub.targeting.dto.TargetingRequestDto;
import com.marketinghub.targeting.mapper.TargetingRequestMapper;
import com.marketinghub.targeting.service.TargetingRequestService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/internal/targeting")
public class TargetingInternalController {
    private final TargetingRequestService service;
    private final TargetingRequestMapper mapper;

    public TargetingInternalController(TargetingRequestService service,
                                       TargetingRequestMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping("/requests/pending")
    public List<TargetingRequestDto> listPending(@RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        List<TargetingRequest> pending = service.listPendingForAi(limit != null ? limit : 10);
        return pending.stream().map(r -> mapper.toDto(r, service.etaSeconds())).toList();
    }

    @PostMapping("/{requestId}/candidates")
    public void saveCandidates(@PathVariable UUID requestId,
                               @RequestBody TargetingCandidateIngestionRequest payload) {
        service.saveCandidates(requestId, payload);
    }
}
