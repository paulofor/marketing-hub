package com.marketinghub.oprm.niche.web;

import com.marketinghub.oprm.niche.dto.OprmNicheSnapshotIngestRequestDto;
import com.marketinghub.oprm.niche.dto.OprmNicheSnapshotIngestResponseDto;
import com.marketinghub.oprm.niche.service.OprmNicheIngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/niches")
@RequiredArgsConstructor
public class OprmNicheIngestionController {
    private final OprmNicheIngestionService service;

    @PostMapping("/snapshots:ingest")
    public ResponseEntity<OprmNicheSnapshotIngestResponseDto> ingest(@Valid @RequestBody OprmNicheSnapshotIngestRequestDto request) {
        return ResponseEntity.accepted().body(service.ingest(request));
    }
}
