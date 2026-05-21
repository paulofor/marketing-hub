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
import com.marketinghub.oprm.market.exception.SQLExcpetion;
import org.springframework.data.domain.PageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Slf4j
@RequiredArgsConstructor
/**
 * Serviço responsável por orquestrar o ciclo de ingestão OPRM CNPJ/CNAE,
 * incluindo abertura de run, registro de eventos de arquivos, consolidação e finalização.
 */
public class OprmMarketImportService {

    private static final String UPSERT_MARKET_SIZE_SQL = "insert into oprm_market_size_by_cnae (avg_socios_por_empresa,total_empresas,total_empresas_mei,total_empresas_simples,total_estabelecimentos,total_estabelecimentos_ativos,updated_at,cnae_code,snapshot_date) values (?,?,?,?,?,?,?,?,?)";
    private final OprmCnpjImportRunRepository runRepository;
    private final OprmCnpjImportFileRepository fileRepository;
    private final OprmCnpjCnaeDimRepository cnaeDimRepository;
    private final OprmMarketSizeByCnaeRepository marketSizeRepository;

    /**
     * Inicia uma nova run de importação e persiste os arquivos sementes vinculados.
     */
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

    /**
     * Registra evento de processamento de arquivo e aplica upsert dos dados derivados recebidos.
     */
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
                String cnaeCodeNormalized = row.cnaeCode().trim();
                log.info("[runId={} fileId={}] Preparando upsert marketSize: snapshotDate={} cnaeCodeRaw='{}' cnaeCodeNormalized='{}' cnaeCodeLength={} totalEstabelecimentos={} totalEstabelecimentosAtivos={} totalEmpresas={} totalEmpresasMei={} totalEmpresasSimples={} avgSociosPorEmpresa={}",
                        runId,
                        fileId,
                        run.getSnapshotDate(),
                        row.cnaeCode(),
                        cnaeCodeNormalized,
                        cnaeCodeNormalized.length(),
                        row.totalEstabelecimentos(),
                        row.totalEstabelecimentosAtivos(),
                        row.totalEmpresas(),
                        row.totalEmpresasMei(),
                        row.totalEmpresasSimples(),
                        row.avgSociosPorEmpresa());
                OprmMarketSizeByCnaeId id = new OprmMarketSizeByCnaeId();
                id.setSnapshotDate(run.getSnapshotDate());
                id.setCnaeCode(cnaeCodeNormalized);
                OprmMarketSizeByCnae item = marketSizeRepository.findById(id).orElseGet(OprmMarketSizeByCnae::new);
                item.setId(id);
                item.setTotalEstabelecimentos(row.totalEstabelecimentos() != null ? row.totalEstabelecimentos() : 0L);
                item.setTotalEstabelecimentosAtivos(row.totalEstabelecimentosAtivos() != null ? row.totalEstabelecimentosAtivos() : 0L);
                item.setTotalEmpresas(row.totalEmpresas() != null ? row.totalEmpresas() : 0L);
                item.setTotalEmpresasMei(row.totalEmpresasMei() != null ? row.totalEmpresasMei() : 0L);
                item.setTotalEmpresasSimples(row.totalEmpresasSimples() != null ? row.totalEmpresasSimples() : 0L);
                item.setAvgSociosPorEmpresa(row.avgSociosPorEmpresa());
                item.setUpdatedAt(Instant.now());
                try {
                    marketSizeRepository.save(item);
                } catch (RuntimeException ex) {
                    log.error("[runId={} fileId={}] Falha ao persistir marketSize: snapshotDate={} cnaeCodeRaw='{}' cnaeCodeNormalized='{}' cnaeCodeLength={} payload={} sqlTentada='{}'.",
                            runId,
                            fileId,
                            run.getSnapshotDate(),
                            row.cnaeCode(),
                            cnaeCodeNormalized,
                            cnaeCodeNormalized.length(),
                            row,
                            UPSERT_MARKET_SIZE_SQL,
                            ex);
                    throw new SQLExcpetion(UPSERT_MARKET_SIZE_SQL, ex);
                }
            }
            log.info("[runId={} fileId={}] Consolidacao marketSizes persistida com sucesso. snapshotDate={} totalRegistros={}",
                    runId,
                    fileId,
                    run.getSnapshotDate(),
                    request.marketSizes().size());
        }
    }

    /**
     * Finaliza a run informada, consolidando status/contadores e bloqueando fechamento indevido.
     */
    @Transactional
    public void completeRun(Long runId, OprmCompleteImportRunRequestDto request) {
        log.info("[OPRM-TOTALIZACAO] completeRun recebido. runId={}, requestedStatus={}, requestedFinishedAt={}, requestedFilesProcessed={}, requestedRowsRead={}, requestedRowsValid={}, requestedRowsRejected={}, requestedErrorMessage={}",
                runId,
                request.status(),
                request.finishedAt(),
                request.filesProcessed(),
                request.rowsRead(),
                request.rowsValid(),
                request.rowsRejected(),
                request.errorMessage());

        OprmCnpjImportRun run = runRepository.findById(runId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Import run not found: " + runId));
        List<OprmCnpjImportFile> files = fileRepository.findByRunId(runId);
        if (files.isEmpty()) {
            log.warn("[OPRM-TOTALIZACAO] completeRun abortado: runId={} não possui arquivos vinculados.", runId);
            throw new ResponseStatusException(BAD_REQUEST, "Import run has no files to finalize: " + runId);
        }

        long startedCount = files.stream().filter(file -> "STARTED".equalsIgnoreCase(file.getStatus())).count();
        long startedEstablishmentsCount = files.stream()
                .filter(file -> "STARTED".equalsIgnoreCase(file.getStatus()))
                .filter(file -> "ESTABELECIMENTOS".equalsIgnoreCase(file.getDatasetType()))
                .count();
        long failedCount = files.stream().filter(file -> "FAILED".equalsIgnoreCase(file.getStatus())).count();
        long completedCount = files.stream().filter(file -> "COMPLETED".equalsIgnoreCase(file.getStatus())).count();
        long partialCount = files.stream().filter(file -> "PARTIAL".equalsIgnoreCase(file.getStatus())).count();

        log.info("[OPRM-TOTALIZACAO] Estado pré-finalização runId={}: filesTotal={}, started={}, completed={}, partial={}, failed={}",
                runId,
                files.size(),
                startedCount,
                completedCount,
                partialCount,
                failedCount);

        if (startedEstablishmentsCount > 0) {
            String message = "Finalização bloqueada: existem " + startedEstablishmentsCount
                    + " arquivos ESTABELECIMENTOS em STARTED. O run não pode ser fechado antes da consolidação de market size.";
            log.error("[OPRM-TOTALIZACAO] {} runId={}", message, runId);
            throw new ResponseStatusException(CONFLICT, message);
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
        log.info("[OPRM-TOTALIZACAO] completeRun persistido. runId={}, finalStatus={}, finishedAt={}, filesProcessed={}, rowsRead={}, rowsValid={}, rowsRejected={}, errorMessage={}",
                runId,
                run.getStatus(),
                run.getFinishedAt(),
                run.getFilesProcessed(),
                run.getRowsRead(),
                run.getRowsValid(),
                run.getRowsRejected(),
                run.getErrorMessage());
    }

    /**
     * Finaliza automaticamente a run mais recente em status STARTED, quando existir.
     */
    @Transactional
    public void finalizeLatestStartedRun(String triggerLabel) {
        Instant executionAt = Instant.now();
        OprmCnpjImportRun run = runRepository.findFirstByStatusOrderByStartedAtDesc("STARTED").orElse(null);
        if (run == null) {
            log.info("[OPRM-TOTALIZACAO] Nenhum run STARTED encontrado para finalização automática. triggerLabel={} executionAtUtc={}",
                    triggerLabel,
                    executionAt);
            return;
        }

        log.info("[OPRM-TOTALIZACAO] Finalização automática iniciada. triggerLabel={} runId={} snapshotDate={} startedAt={} status={}",
                triggerLabel,
                run.getId(),
                run.getSnapshotDate(),
                run.getStartedAt(),
                run.getStatus());

        completeRun(run.getId(), new OprmCompleteImportRunRequestDto(
                null,
                executionAt,
                null,
                null,
                null,
                null,
                "Finalização automática da etapa de totalização (" + triggerLabel + ")"));

        log.info("[OPRM-TOTALIZACAO] Finalização automática concluída com sucesso. triggerLabel={} runId={}",
                triggerLabel,
                run.getId());
    }

    /**
     * Valida se o arquivo informado pertence à run recebida.
     */
    private void validateFileBelongsToRun(Long runId, OprmCnpjImportFile file) {
        if (file.getRun() == null || file.getRun().getId() == null || !runId.equals(file.getRun().getId())) {
            throw new ResponseStatusException(BAD_REQUEST, "File does not belong to informed run.");
        }
    }

    /**
     * Resolve o status final da run priorizando o status solicitado e fallback por presença de falhas.
     */
    private String resolveRunStatus(String requestedStatus, boolean hasFailure) {
        if (requestedStatus != null && !requestedStatus.isBlank()) {
            return requestedStatus;
        }
        return hasFailure ? "PARTIAL" : "COMPLETED";
    }

    /**
     * Lista todas as runs de importação persistidas.
     */
    @Transactional(readOnly = true)
    public List<OprmCnpjImportRun> listRuns() {
        return runRepository.findAll();
    }

    /**
     * Lista os arquivos vinculados a uma run específica.
     */
    @Transactional(readOnly = true)
    public List<OprmCnpjImportFile> listRunFiles(Long runId) {
        return fileRepository.findByRunId(runId);
    }

    /**
     * Lista a dimensão de CNAEs carregada no backend.
     */
    @Transactional(readOnly = true)
    public List<OprmCnpjCnaeDim> listCnaes() {
        return cnaeDimRepository.findAll();
    }

    /**
     * Retorna os principais CNAEs por volume de mercado no snapshot mais recente.
     */
    @Transactional(readOnly = true)
    public List<OprmTopCnaeMarketVolumeDto> listTopCnaesByMarketVolume(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return marketSizeRepository.findTopByLatestSnapshot(PageRequest.of(0, safeLimit));
    }
}
