package com.marketinghub.oprm.web;

import com.marketinghub.oprm.dto.OprmCreateJobRequestDto;
import com.marketinghub.oprm.dto.OprmJobClaimRequestDto;
import com.marketinghub.oprm.dto.OprmJobClaimResponseDto;
import com.marketinghub.oprm.dto.OprmJobDetailResponseDto;
import com.marketinghub.oprm.dto.OprmJobStatusUpdateRequestDto;
import com.marketinghub.oprm.service.OprmJobOrchestrationService;
import jakarta.validation.Valid;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oprm/jobs")
@RequiredArgsConstructor
public class OprmJobController {
    private final OprmJobOrchestrationService service;

    @PostMapping
    public OprmJobDetailResponseDto createJob(@Valid @RequestBody OprmCreateJobRequestDto request) {
        return service.createJob(request);
    }

    @PostMapping("/claim")
    public ResponseEntity<OprmJobClaimResponseDto> claim(@Valid @RequestBody OprmJobClaimRequestDto request) {
        Optional<OprmJobClaimResponseDto> claimed = service.claimNextJob(request);
        return claimed.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{jobId}")
    public OprmJobDetailResponseDto detail(@PathVariable UUID jobId) {
        return service.getJobDetail(jobId);
    }

    @PostMapping("/{jobId}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable UUID jobId,
                                             @Valid @RequestBody OprmJobStatusUpdateRequestDto request) {
        service.updateJobStatus(jobId, request);
        return ResponseEntity.accepted().build();
    }
}
