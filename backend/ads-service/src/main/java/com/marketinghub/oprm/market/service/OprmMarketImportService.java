package com.marketinghub.oprm.market.service;

import com.marketinghub.oprm.market.OprmCnpjCnaeDim;
import com.marketinghub.oprm.market.OprmCnpjImportFile;
import com.marketinghub.oprm.market.OprmCnpjImportRun;
import com.marketinghub.oprm.market.dto.*;
import com.marketinghub.oprm.market.repository.OprmCnpjCnaeDimRepository;
import com.marketinghub.oprm.market.repository.OprmCnpjImportFileRepository;
import com.marketinghub.oprm.market.repository.OprmCnpjImportRunRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OprmMarketImportService {
    private final OprmCnpjImportRunRepository runRepository;
    private final OprmCnpjImportFileRepository fileRepository;
    private final OprmCnpjCnaeDimRepository cnaeDimRepository;

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
        OprmCnpjImportFile file = fileRepository.findById(fileId).orElseThrow();

        if (request.status() != null) file.setStatus(request.status());
        if (request.rowsRead() != null) file.setRowsRead(request.rowsRead());
        if (request.rowsValid() != null) file.setRowsValid(request.rowsValid());
        if (request.rowsRejected() != null) file.setRowsRejected(request.rowsRejected());
        if (request.errorMessage() != null) file.setErrorMessage(request.errorMessage());
        if (request.finishedAt() != null) file.setFinishedAt(request.finishedAt());
        fileRepository.save(file);

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
}
