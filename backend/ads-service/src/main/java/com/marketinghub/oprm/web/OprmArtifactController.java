package com.marketinghub.oprm.web;

import com.marketinghub.oprm.OprmArtifactStatus;
import com.marketinghub.oprm.dto.OprmArtifactPublishRequestDto;
import com.marketinghub.oprm.dto.OprmArtifactPublishResponseDto;
import com.marketinghub.oprm.dto.OprmArtifactSummaryDto;
import com.marketinghub.oprm.service.OprmArtifactService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oprm/artifacts")
@RequiredArgsConstructor
public class OprmArtifactController {
    private final OprmArtifactService service;

    @PostMapping
    public ResponseEntity<OprmArtifactPublishResponseDto> publish(@Valid @RequestBody OprmArtifactPublishRequestDto request) {
        return ResponseEntity.accepted().body(service.publishArtifact(request));
    }

    @GetMapping
    public List<OprmArtifactSummaryDto> list(@RequestParam(required = false) String correlationId,
                                             @RequestParam(required = false) String occupationSeedRef,
                                             @RequestParam(required = false) OprmArtifactStatus status) {
        return service.listArtifacts(correlationId, occupationSeedRef, status);
    }
}
