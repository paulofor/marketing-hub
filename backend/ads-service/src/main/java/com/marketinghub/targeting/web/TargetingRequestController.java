package com.marketinghub.targeting.web;

import com.marketinghub.targeting.TargetingRequest;
import com.marketinghub.targeting.TargetingRequestStatus;
import com.marketinghub.targeting.dto.CreateTargetingRequestPayload;
import com.marketinghub.targeting.dto.TargetingRecentRequestDto;
import com.marketinghub.targeting.dto.TargetingRequestDto;
import com.marketinghub.targeting.mapper.TargetingRequestMapper;
import com.marketinghub.targeting.service.TargetingRequestService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    @GetMapping("/requests")
    public List<TargetingRequestDto> list(@RequestParam(value = "status", required = false) TargetingRequestStatus status,
                                          @RequestParam(value = "limit", defaultValue = "10") Integer limit,
                                          @RequestParam(value = "includeCandidates", defaultValue = "true") boolean includeCandidates,
                                          @RequestParam(value = "nicheId", required = false) Long nicheId,
                                          @RequestParam(value = "hypothesisId", required = false) UUID hypothesisId) {
        List<TargetingRequest> requests = service.listRequests(status, limit != null ? limit : 10, nicheId, hypothesisId);
        return requests.stream()
                .map(r -> includeCandidates ? mapper.toDetailedDto(r, service.etaSeconds()) : mapper.toDto(r, service.etaSeconds()))
                .toList();
    }

    @GetMapping("/requests/recent")
    public List<TargetingRecentRequestDto> listRecent(@RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return service.listRecentRequests(limit != null ? limit : 10);
    }

    @GetMapping("/requests/{requestId}")
    public TargetingRequestDto get(@PathVariable UUID requestId,
                                   @RequestParam(value = "includeCandidates", defaultValue = "true") boolean includeCandidates) {
        TargetingRequest request = service.getWithCandidates(requestId);
        return includeCandidates ? mapper.toDetailedDto(request, service.etaSeconds()) : mapper.toDto(request, service.etaSeconds());
    }
}
