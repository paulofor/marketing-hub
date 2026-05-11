package com.marketinghub.oprm.niche.web;

import com.marketinghub.oprm.niche.dto.OprmNicheCatalogIngestRequestDto;
import com.marketinghub.oprm.niche.dto.OprmNicheSnapshotIngestResponseDto;
import com.marketinghub.oprm.niche.service.OprmNicheCatalogIngestionService;
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
public class OprmNicheCatalogIngestionController {

    private final OprmNicheCatalogIngestionService service;

    @PostMapping("/catalog:ingest")
    public ResponseEntity<OprmNicheSnapshotIngestResponseDto> ingest(@Valid @RequestBody OprmNicheCatalogIngestRequestDto request) {
        return ResponseEntity.accepted().body(service.ingest(request));
    }
}
