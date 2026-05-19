package com.marketinghub.oprmcoletormei.marketimport.service;

import com.marketinghub.oprmcoletormei.marketimport.config.OprmMarketImportCollectorProperties;
import com.marketinghub.oprmcoletormei.marketimport.config.OprmMarketImportScheduleProperties;
import com.marketinghub.oprmcoletormei.marketimport.dto.OprmCompleteImportRunRequestDto;
import com.marketinghub.oprmcoletormei.marketimport.dto.OprmCreateImportRunRequestDto;
import com.marketinghub.oprmcoletormei.marketimport.dto.OprmCnaeUpsertDto;
import com.marketinghub.oprmcoletormei.marketimport.dto.OprmImportFileEventRequestDto;
import com.marketinghub.oprmcoletormei.marketimport.dto.OprmImportFileSeedDto;
import com.marketinghub.oprmcoletormei.marketimport.dto.OprmImportRunCreatedResponseDto;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OprmMarketImportScheduler {
    private static final Logger log = LoggerFactory.getLogger(OprmMarketImportScheduler.class);

    private final OprmMarketImportScheduleProperties scheduleProperties;
    private final OprmMarketImportCollectorProperties collectorProperties;
    private final RestClient restClient;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public OprmMarketImportScheduler(OprmMarketImportScheduleProperties scheduleProperties,
                                     OprmMarketImportCollectorProperties collectorProperties,
                                     RestClient restClient) {
        this.scheduleProperties = scheduleProperties;
        this.collectorProperties = collectorProperties;
        this.restClient = restClient;
    }

    @Scheduled(cron = "0 40 4 * * *", zone = "America/Sao_Paulo")
    public void runScheduledImport() {
        if (!scheduleProperties.enabled()) {
            log.info("Scheduler OPRM market import desabilitado.");
            return;
        }

        ZoneId zoneId = ZoneId.of(scheduleProperties.timezone());
        LocalDate snapshotDate = (scheduleProperties.snapshotDate() == null || scheduleProperties.snapshotDate().isBlank())
                ? LocalDate.now(zoneId)
                : LocalDate.parse(scheduleProperties.snapshotDate());

        String sourceBaseUrl = scheduleProperties.sourceBaseUrl().endsWith("/")
                ? scheduleProperties.sourceBaseUrl().substring(0, scheduleProperties.sourceBaseUrl().length() - 1)
                : scheduleProperties.sourceBaseUrl();
        String sourceUrl = sourceBaseUrl + "/" + snapshotDate;

        List<OprmImportFileSeedDto> files = buildFiles(sourceUrl);
        Instant startedAt = Instant.now();

        OprmCreateImportRunRequestDto request = new OprmCreateImportRunRequestDto(
                snapshotDate,
                sourceUrl,
                "STARTED",
                startedAt,
                null,
                files.size(),
                0,
                0L,
                0L,
                0L,
                null,
                files
        );

        OprmImportRunCreatedResponseDto runResponse = restClient.post()
                .uri(collectorProperties.backendBaseUrl() + "/api/oprm/market/import-runs")
                .body(request)
                .retrieve()
                .body(OprmImportRunCreatedResponseDto.class);

        if (runResponse == null || runResponse.runId() == null || runResponse.fileIds() == null || runResponse.fileIds().size() != files.size()) {
            throw new IllegalStateException("Resposta inválida ao criar import run OPRM.");
        }

        Path runTempDir = Paths.get(collectorProperties.tempDir(), "run-" + runResponse.runId());
        long totalRowsRead = 0L;
        long totalRowsValid = 0L;
        long totalRowsRejected = 0L;
        int filesProcessed = 0;
        boolean hasFailure = false;
        try {
            Files.createDirectories(runTempDir);
            log.info("[run={}] Diretório temporário criado: {}", runResponse.runId(), runTempDir);
            for (int i = 0; i < files.size(); i++) {
                OprmImportFileSeedDto file = files.get(i);
                Long fileId = runResponse.fileIds().get(i);
                Path zipPath = runTempDir.resolve(file.fileName());
                log.info("[run={} fileId={}] Início download {}", runResponse.runId(), fileId, file.fileUrl());
                try {
                    downloadToFile(file.fileUrl(), zipPath);
                    log.info("[run={} fileId={}] Download concluído: {}", runResponse.runId(), fileId, zipPath);
                    long rowsRead = countRowsFromZip(zipPath, runResponse.runId(), fileId, file.fileName());
                    long rowsValid = rowsRead;
                    long rowsRejected = 0L;
                    List<OprmCnaeUpsertDto> cnaes = "CNAE".equalsIgnoreCase(file.datasetType())
                            ? parseCnaesFromZip(zipPath, runResponse.runId(), fileId, file.fileName())
                            : null;
                    log.info("[run={} fileId={}] Diagnóstico totalização datasetType={} cnaesCount={} marketSizesCount={} (marketSizes ainda não calculado no coletor para este arquivo)",
                            runResponse.runId(),
                            fileId,
                            file.datasetType(),
                            cnaes != null ? cnaes.size() : 0,
                            0);
                    totalRowsRead += rowsRead;
                    totalRowsValid += rowsValid;
                    totalRowsRejected += rowsRejected;
                    filesProcessed++;
                    publishFileEvent(runResponse.runId(), fileId, new OprmImportFileEventRequestDto(
                            "COMPLETED", rowsRead, rowsValid, rowsRejected, null, Instant.now(), cnaes, null));
                    log.info("[run={} fileId={}] Persistência status COMPLETED enviada. rowsRead={}", runResponse.runId(), fileId, rowsRead);
                } catch (Exception e) {
                    hasFailure = true;
                    log.error("[run={} fileId={}] Falha no processamento de arquivo {}", runResponse.runId(), fileId, file.fileName(), e);
                    publishFileEvent(runResponse.runId(), fileId, new OprmImportFileEventRequestDto(
                            "FAILED", 0L, 0L, 0L, e.getMessage(), Instant.now(), null, null));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível preparar diretório temporário do run.", e);
        } finally {
            cleanupDirectory(runTempDir, runResponse.runId());
            publishRunComplete(runResponse.runId(), filesProcessed, totalRowsRead, totalRowsValid, totalRowsRejected, hasFailure);
        }

        log.info("Import run OPRM CNPJ agendado com sucesso para snapshotDate={} às 04:40 ({}) com {} arquivos.",
                snapshotDate,
                scheduleProperties.timezone(),
                files.size());
    }



    @Scheduled(cron = "0 0 11 * * *", zone = "America/Sao_Paulo")
    public void runScheduledFinalization() {
        log.info("[OPRM-TOTALIZACAO] Disparo de finalização automática iniciado via OPRM-MEI.");
        restClient.post()
                .uri(collectorProperties.backendBaseUrl() + "/api/oprm/market/import-runs/finalize-latest-started")
                .retrieve()
                .toBodilessEntity();
        log.info("[OPRM-TOTALIZACAO] Disparo de finalização automática concluído via OPRM-MEI.");
    }

    private List<OprmCnaeUpsertDto> parseCnaesFromZip(Path zipPath, Long runId, Long fileId, String fileName) throws IOException {
        log.info("[run={} fileId={}] Início parse CNAE de {}", runId, fileId, fileName);
        List<OprmCnaeUpsertDto> cnaes = new ArrayList<>();
        try (InputStream in = Files.newInputStream(zipPath);
             java.util.zip.ZipInputStream zipInputStream = new java.util.zip.ZipInputStream(in, StandardCharsets.ISO_8859_1)) {
            java.util.zip.ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String content = new String(zipInputStream.readAllBytes(), StandardCharsets.ISO_8859_1);
                String[] lines = content.split("\\R");
                for (String rawLine : lines) {
                    if (rawLine == null || rawLine.isBlank()) continue;
                    log.info("[run={} fileId={}] payload_bruto_cnae='{}'", runId, fileId, rawLine);
                    String[] cols = rawLine.split(";", 2);
                    if (cols.length < 2) continue;
                    String cnaeCode = normalizeField(cols[0]);
                    String description = normalizeField(cols[1]);
                    if (cnaeCode.isBlank() || description.isBlank()) continue;
                    cnaes.add(new OprmCnaeUpsertDto(cnaeCode, description, true));
                }
                zipInputStream.closeEntry();
            }
        }
        log.info("[run={} fileId={}] Parse CNAE concluído. totalRegistros={}", runId, fileId, cnaes.size());
        return cnaes;
    }

    private String normalizeField(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private List<OprmImportFileSeedDto> buildFiles(String sourceUrl) {
        List<OprmImportFileSeedDto> files = new ArrayList<>();
        files.add(file("Cnaes.zip", sourceUrl, "CNAE"));
        for (int i = 1; i < 10; i++) files.add(file("Empresas" + i + ".zip", sourceUrl, "EMPRESAS"));
        for (int i = 1; i < 10; i++) files.add(file("Estabelecimentos" + i + ".zip", sourceUrl, "ESTABELECIMENTOS"));
        files.add(file("Simples.zip", sourceUrl, "SIMPLES"));
        for (int i = 1; i < 10; i++) files.add(file("Socios" + i + ".zip", sourceUrl, "SOCIOS"));
        return files;
    }

    private OprmImportFileSeedDto file(String fileName, String sourceUrl, String datasetType) {
        return new OprmImportFileSeedDto(fileName, sourceUrl + "/" + fileName, datasetType, "STARTED");
    }

    private void downloadToFile(String fileUrl, Path target) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(fileUrl)).GET().build();
        HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(target));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Falha no download HTTP status=" + response.statusCode());
        }
    }

    private long countRowsFromZip(Path zipPath, Long runId, Long fileId, String fileName) throws IOException {
        log.info("[run={} fileId={}] Início unzip/leitura: {}", runId, fileId, fileName);
        long rows = 0L;
        try (InputStream in = Files.newInputStream(zipPath);
             java.util.zip.ZipInputStream zipInputStream = new java.util.zip.ZipInputStream(in)) {
            java.util.zip.ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                byte[] buffer = new byte[8192];
                int read;
                while ((read = zipInputStream.read(buffer)) != -1) {
                    for (int i = 0; i < read; i++) {
                        if (buffer[i] == "\n".charAt(0)) rows++;
                    }
                }
                zipInputStream.closeEntry();
            }
        }
        log.info("[run={} fileId={}] Fim leitura. rowsRead={}", runId, fileId, rows);
        return rows;
    }

    private void publishFileEvent(Long runId, Long fileId, OprmImportFileEventRequestDto event) {
        int cnaesCount = event.cnaes() != null ? event.cnaes().size() : 0;
        int marketSizesCount = event.marketSizes() != null ? event.marketSizes().size() : 0;
        log.info("[run={} fileId={}] Publicando evento de arquivo status={} rowsRead={} rowsValid={} rowsRejected={} cnaesCount={} marketSizesCount={} finishedAt={}",
                runId, fileId, event.status(), event.rowsRead(), event.rowsValid(), event.rowsRejected(), cnaesCount, marketSizesCount, event.finishedAt());
        if (marketSizesCount == 0 && !"FAILED".equalsIgnoreCase(event.status())) {
            log.warn("[run={} fileId={}] Evento publicado sem marketSizes. A totalização em oprm_market_size_by_cnae não será atualizada para este arquivo.",
                    runId, fileId);
        }
        restClient.post().uri(collectorProperties.backendBaseUrl() + "/api/oprm/market/import-runs/" + runId + "/files/" + fileId + "/events")
                .body(event).retrieve().toBodilessEntity();
        log.info("[run={} fileId={}] Evento de arquivo persistido com sucesso.", runId, fileId);
    }

    private void publishRunComplete(Long runId, int filesProcessed, long rowsRead, long rowsValid, long rowsRejected, boolean hasFailure) {
        String finalStatus = hasFailure ? "PARTIAL" : "COMPLETED";
        log.info("[run={}] Publicando consolidação final do run status={} filesProcessed={} rowsRead={} rowsValid={} rowsRejected={}",
                runId, finalStatus, filesProcessed, rowsRead, rowsValid, rowsRejected);
        restClient.post().uri(collectorProperties.backendBaseUrl() + "/api/oprm/market/import-runs/" + runId + "/complete")
                .body(new OprmCompleteImportRunRequestDto(finalStatus, Instant.now(), filesProcessed, rowsRead, rowsValid, rowsRejected, null))
                .retrieve().toBodilessEntity();
        log.info("[run={}] Consolidação final do run persistida com sucesso.", runId);
    }

    private void cleanupDirectory(Path runTempDir, Long runId) {
        if (runTempDir == null || !Files.exists(runTempDir)) return;
        try (var walk = Files.walk(runTempDir)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    log.warn("[run={}] Falha ao deletar temporário {}", runId, path, e);
                }
            });
            log.info("[run={}] Limpeza do diretório temporário concluída: {}", runId, runTempDir);
        } catch (IOException e) {
            log.warn("[run={}] Falha ao limpar diretório temporário {}", runId, runTempDir, e);
        }
    }
}
