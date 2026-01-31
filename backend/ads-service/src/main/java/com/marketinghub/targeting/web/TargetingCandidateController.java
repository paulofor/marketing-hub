package com.marketinghub.targeting.web;

import com.marketinghub.targeting.TargetingCandidate;
import com.marketinghub.targeting.dto.TargetingCandidateDto;
import com.marketinghub.targeting.dto.TargetingCandidateReprocessRequest;
import com.marketinghub.targeting.mapper.TargetingCandidateMapper;
import com.marketinghub.targeting.service.TargetingRequestService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/targeting/candidates")
public class TargetingCandidateController {
    private final TargetingRequestService service;
    private final TargetingCandidateMapper mapper;

    public TargetingCandidateController(TargetingRequestService service,
                                        TargetingCandidateMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/{candidateId}/reprocess")
    public TargetingCandidateDto reprocess(@PathVariable Long candidateId,
                                           @RequestBody(required = false) TargetingCandidateReprocessRequest payload) {
        TargetingCandidate candidate = service.reprocessCandidate(candidateId, payload);
        return mapper.toDto(candidate);
    }
}
