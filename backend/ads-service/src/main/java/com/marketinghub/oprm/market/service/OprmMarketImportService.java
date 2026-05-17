package com.marketinghub.oprm.market.service;

import com.marketinghub.oprm.market.OprmCnpjCnaeDim;
import com.marketinghub.oprm.market.OprmCnpjImportFile;
import com.marketinghub.oprm.market.OprmCnpjImportRun;
import com.marketinghub.oprm.market.OprmMarketSizeByCnae;
import com.marketinghub.oprm.market.OprmMarketSizeByCnaeId;
import com.marketinghub.oprm.market.dto.*;
import com.marketinghub.oprm.market.repository.OprmCnpjCnaeDimRepository;
import com.marketinghub.oprm.market.repository.OprmCnpjImportFileRepository;
import com.marketinghub.oprm.market.repository.OprmCnpjImportRunRepository;
import com.marketinghub.oprm.market.repository.OprmMarketSizeByCnaeRepository;
import java.time.Instant;
import java.util.List;
import com.marketinghub.oprm.market.dto.OprmTopCnaeMarketVolumeDto;
import org.springframework.data.domain.PageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Slf4j
@RequiredArgsConstructor
public class OprmMarketImportService {
    private final OprmCnpjImportRunRepository runRepository;
    private final OprmCnpjImportFileRepository fileRepository;
    private final OprmCnpjCnaeDimRepository cnaeDimRepository;
    private final OprmMarketSizeByCnaeRepository marketSizeRepository;

    @Transactional
    public OprmImportRunCreatedResponseDto startRun(OprmCreateImportRunRequestDto request) {
        OprmCnpjImportRun run = new OprmCnpjImportRun();
        run.setSnapshotDate(request.snapshotDate());
        run.setSourceUrl(request.sourceUrl());
        run.setStatus(request.status());
        run.setStartedAt(request.startedAt());
        run.setFinishedAt(request.finishedAt());
        run.setFilesTotal(request.filesTotal() != null ? request.filesTotal() : request.files().size());
        run.setFilesProcessed(request.filesProcessed() != null ? request.filesProcessed() : 0);
        run.setRowsRead(request.rowsRead() != null ? request.rowsRead() : 0L);
        run.setRowsValid(request.rowsValid() != null ? request.rowsValid() : 0L);
        run.setRowsRejected(request.rowsRejected() != null ? request.rowsRejected() : 0L);
        run.setErrorMessage(request.errorMessage());
        run = runRepository.save(run);
        final OprmCnpjImportRun persistedRun = run;

        List<Long> fileIds = request.files().stream().map(seed -> {
            OprmCnpjImportFile file = new OprmCnpjImportFile();
            file.setRun(persistedRun);
            file.setFileName(seed.fileName());
            file.setFileUrl(seed.fileUrl());
            file.setDatasetType(seed.datasetType());
            file.setStatus(seed.status() != null ? seed.status() : "STARTED");
            file.setRowsRead(seed.rowsRead() != null ? seed.rowsRead() : 0L);
            file.setRowsValid(seed.rowsValid() != null ? seed.rowsValid() : 0L);
            file.setRowsRejected(seed.rowsRejected() != null ? seed.rowsRejected() : 0L);
            file.setErrorMessage(seed.errorMessage());
            file.setStartedAt(seed.startedAt() != null ? seed.startedAt() : request.startedAt());
            file.setFinishedAt(seed.finishedAt());
            return fileRepository.save(file).getId();
        }).toList();

        return new OprmImportRunCreatedResponseDto(run.getId(), fileIds);
    }

    @Transactional
    public void registerFileEvent(Long runId, Long fileId, OprmImportFileEventRequestDto request) {
        OprmCnpjImportRun run = runRepository.findById(runId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Import run not found: " + runId));
        OprmCnpjImportFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Import file not found: " + fileId));
        validateFileBelongsToRun(runId, file);

        if (request.status() != null) file.setStatus(request.status());
        if (request.rowsRead() != null) file.setRowsRead(request.rowsRead());
        if (request.rowsValid() != null) file.setRowsValid(request.rowsValid());
        if (request.rowsRejected() != null) file.setRowsRejected(request.rowsRejected());
        if (request.errorMessage() != null) file.setErrorMessage(request.errorMessage());
        if (request.finishedAt() != null) file.setFinishedAt(request.finishedAt());
        fileRepository.save(file);

        int cnaesReceived = request.cnaes() != null ? request.cnaes().size() : 0;
        int marketSizesReceived = request.marketSizes() != null ? request.marketSizes().size() : 0;
        log.info("[runId={} fileId={}] registerFileEvent status={} rowsRead={} rowsValid={} rowsRejected={} cnaesReceived={} marketSizesReceived={} finishedAt={}",
                runId,
                fileId,
                file.getStatus(),
                file.getRowsRead(),
                file.getRowsValid(),
                file.getRowsRejected(),
                cnaesReceived,
                marketSizesReceived,
                file.getFinishedAt());

        if (request.cnaes() != null && !request.cnaes().isEmpty()) {
            for (OprmCnaeUpsertDto row : request.cnaes()) {
                OprmCnpjCnaeDim item = cnaeDimRepository.findById(row.cnaeCode()).orElseGet(OprmCnpjCnaeDim::new);
                item.setCnaeCode(row.cnaeCode());
                item.setDescription(row.description());
                item.setActive(row.active());
                item.setUpdatedAt(Instant.now());
                cnaeDimRepository.save(item);
            }
        }

        if (request.marketSizes() != null && !request.marketSizes().isEmpty()) {
            log.info("[runId={} fileId={}] Consolidando marketSizes para snapshotDate={} totalRegistros={}",
                    runId,
                    fileId,
                    run.getSnapshotDate(),
                    request.marketSizes().size());
            for (OprmMarketSizeUpsertDto row : request.marketSizes()) {
                if (row.cnaeCode() == null || row.cnaeCode().isBlank()) {
                    throw new ResponseStatusException(BAD_REQUEST, "cnaeCode is required in marketSizes.");
                }
                OprmMarketSizeByCnaeId id = new OprmMarketSizeByCnaeId();
                id.setSnapshotDate(run.getSnapshotDate());
                id.setCnaeCode(row.cnaeCode());
                OprmMarketSizeByCnae item = marketSizeRepository.findById(id).orElseGet(OprmMarketSizeByCnae::new);
                item.setId(id);
                item.setTotalEstabelecimentos(row.totalEstabelecimentos() != null ? row.totalEstabelecimentos() : 0L);
                item.setTotalEstabelecimentosAtivos(row.totalEstabelecimentosAtivos() != null ? row.totalEstabelecimentosAtivos() : 0L);
                item.setTotalEmpresas(row.totalEmpresas() != null ? row.totalEmpresas() : 0L);
                item.setTotalEmpresasMei(row.totalEmpresasMei() != null ? row.totalEmpresasMei() : 0L);
                item.setTotalEmpresasSimples(row.totalEmpresasSimples() != null ? row.totalEmpresasSimples() : 0L);
                item.setAvgSociosPorEmpresa(row.avgSociosPorEmpresa());
                item.setUpdatedAt(Instant.now());
                marketSizeRepository.save(item);
            }
            log.info("[runId={} fileId={}] Consolidacao marketSizes persistida com sucesso. snapshotDate={} totalRegistros={}",
                    runId,
                    fileId,
                    run.getSnapshotDate(),
                    request.marketSizes().size());
        }
    }

    @Transactional
    public void completeRun(Long runId, OprmCompleteImportRunRequestDto request) {
        OprmCnpjImportRun run = runRepository.findById(runId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Import run not found: " + runId));
        List<OprmCnpjImportFile> files = fileRepository.findByRunId(runId);
        if (files.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Import run has no files to finalize: " + runId);
        }
        boolean hasFailure = false;
        for (OprmCnpjImportFile file : files) {
            if (file.getFinishedAt() == null) {
                file.setFinishedAt(request.finishedAt() != null ? request.finishedAt() : Instant.now());
            }
            if ("STARTED".equalsIgnoreCase(file.getStatus())) {
                file.setStatus("FAILED");
                if (file.getErrorMessage() == null || file.getErrorMessage().isBlank()) {
                    file.setErrorMessage("File closed during run finalization without explicit completion event.");
                }
            }
            if ("FAILED".equalsIgnoreCase(file.getStatus())) {
                hasFailure = true;
            }
        }
        fileRepository.saveAll(files);

        run.setFinishedAt(request.finishedAt() != null ? request.finishedAt() : Instant.now());
        run.setStatus(resolveRunStatus(request.status(), hasFailure));
        run.setFilesProcessed(request.filesProcessed() != null ? request.filesProcessed() : files.size());
        if (request.rowsRead() != null) run.setRowsRead(request.rowsRead());
        if (request.rowsValid() != null) run.setRowsValid(request.rowsValid());
        if (request.rowsRejected() != null) run.setRowsRejected(request.rowsRejected());
        if (request.errorMessage() != null) run.setErrorMessage(request.errorMessage());
        runRepository.save(run);
    }

    private void validateFileBelongsToRun(Long runId, OprmCnpjImportFile file) {
        if (file.getRun() == null || file.getRun().getId() == null || !runId.equals(file.getRun().getId())) {
            throw new ResponseStatusException(BAD_REQUEST, "File does not belong to informed run.");
        }
    }

    private String resolveRunStatus(String requestedStatus, boolean hasFailure) {
        if (requestedStatus != null && !requestedStatus.isBlank()) {
            return requestedStatus;
        }
        return hasFailure ? "PARTIAL" : "COMPLETED";
    }

    @Transactional(readOnly = true)
    public List<OprmCnpjImportRun> listRuns() {
        return runRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<OprmCnpjImportFile> listRunFiles(Long runId) {
        return fileRepository.findByRunId(runId);
    }

    @Transactional(readOnly = true)
    public List<OprmCnpjCnaeDim> listCnaes() {
        return cnaeDimRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<OprmTopCnaeMarketVolumeDto> listTopCnaesByMarketVolume(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return marketSizeRepository.findTopByLatestSnapshot(PageRequest.of(0, safeLimit));
    }
}
