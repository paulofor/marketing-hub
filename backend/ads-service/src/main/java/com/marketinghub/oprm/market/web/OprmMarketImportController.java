package com.marketinghub.oprm.market.web;

import com.marketinghub.oprm.market.OprmCnpjCnaeDim;
import com.marketinghub.oprm.market.OprmCnpjImportFile;
import com.marketinghub.oprm.market.OprmCnpjImportRun;
import com.marketinghub.oprm.market.dto.OprmCreateImportRunRequestDto;
import com.marketinghub.oprm.market.dto.OprmCompleteImportRunRequestDto;
import com.marketinghub.oprm.market.dto.OprmImportFileEventRequestDto;
import com.marketinghub.oprm.market.dto.OprmImportRunCreatedResponseDto;
import com.marketinghub.oprm.market.dto.OprmTopCnaeMarketVolumeDto;
import com.marketinghub.oprm.market.service.OprmMarketImportService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/oprm/market/import-runs")
@RequiredArgsConstructor
public class OprmMarketImportController {
    private final OprmMarketImportService service;

    @PostMapping
    public OprmImportRunCreatedResponseDto createRun(@Valid @RequestBody OprmCreateImportRunRequestDto request) {
        return service.startRun(request);
    }

    @PostMapping("/{runId}/files/{fileId}/events")
    public ResponseEntity<Void> fileEvent(@PathVariable Long runId,
                                          @PathVariable Long fileId,
                                          @RequestBody OprmImportFileEventRequestDto request) {
        service.registerFileEvent(runId, fileId, request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{runId}/complete")
    public ResponseEntity<Void> completeRun(@PathVariable Long runId,
                                            @RequestBody OprmCompleteImportRunRequestDto request) {
        service.completeRun(runId, request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping
    public List<OprmCnpjImportRun> listRuns() {
        return service.listRuns();
    }

    @GetMapping("/{runId}/files")
    public List<OprmCnpjImportFile> listRunFiles(@PathVariable Long runId) {
        return service.listRunFiles(runId);
    }

    @GetMapping("/cnaes")
    public List<OprmCnpjCnaeDim> listCnaes() {
        return service.listCnaes();
    }

    @GetMapping("/cnaes/top-volume")
    public List<OprmTopCnaeMarketVolumeDto> listTopCnaesByVolume(@RequestParam(defaultValue = "20") int limit) {
        return service.listTopCnaesByMarketVolume(limit);
    }
}
