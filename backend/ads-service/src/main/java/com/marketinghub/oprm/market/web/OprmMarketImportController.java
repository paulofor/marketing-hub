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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller responsável por expor APIs OPRM de runs de importação e ranking de CNAEs.
 */
@RestController
@RequestMapping("/api/oprm/market/import-runs")
@RequiredArgsConstructor
public class OprmMarketImportController {
    private final OprmMarketImportService service;

    /**
     * Cria uma run de importação CNPJ/CNAE com seus arquivos iniciais.
     */
    @PostMapping
    public OprmImportRunCreatedResponseDto createRun(@Valid @RequestBody OprmCreateImportRunRequestDto request) {
        return service.startRun(request);
    }

    /**
     * Registra evento operacional de processamento para um arquivo da run.
     */
    @PostMapping("/{runId}/files/{fileId}/events")
    public ResponseEntity<Void> fileEvent(
            @PathVariable Long runId,
            @PathVariable Long fileId,
            @RequestBody OprmImportFileEventRequestDto request) {
        service.registerFileEvent(runId, fileId, request);
        return ResponseEntity.accepted().build();
    }

    /**
     * Finaliza uma run de importação após validação dos arquivos vinculados.
     */
    @PostMapping("/{runId}/complete")
    public ResponseEntity<Void> completeRun(
            @PathVariable Long runId,
            @RequestBody OprmCompleteImportRunRequestDto request) {
        service.completeRun(runId, request);
        return ResponseEntity.accepted().build();
    }

    /**
     * Finaliza automaticamente a run STARTED mais recente para apoio operacional.
     */
    @PostMapping("/finalize-latest-started")
    public ResponseEntity<Void> finalizeLatestStartedRun() {
        service.finalizeLatestStartedRun("endpoint /api/oprm/market/import-runs/finalize-latest-started");
        return ResponseEntity.accepted().build();
    }

    /**
     * Lista as runs de importação CNPJ/CNAE registradas.
     */
    @GetMapping
    public List<OprmCnpjImportRun> listRuns() {
        return service.listRuns();
    }

    /**
     * Lista os arquivos vinculados a uma run de importação específica.
     */
    @GetMapping("/{runId}/files")
    public List<OprmCnpjImportFile> listRunFiles(@PathVariable Long runId) {
        return service.listRunFiles(runId);
    }

    /**
     * Lista o catálogo de CNAEs importado para o backend.
     */
    @GetMapping("/cnaes")
    public List<OprmCnpjCnaeDim> listCnaes() {
        return service.listCnaes();
    }

    /**
     * Lista CNAEs paginados para a tela operacional, ordenados por score OPRM decrescente.
     */
    @GetMapping("/cnaes/top-volume")
    public List<OprmTopCnaeMarketVolumeDto> listTopCnaesByVolume(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return service.listTopCnaesByMarketVolume(page, size);
    }
}
