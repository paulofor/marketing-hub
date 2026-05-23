package com.marketinghub.oprmcoletormei.marketimport.service;

import com.marketinghub.oprmcoletormei.marketimport.config.OprmMarketImportCollectorProperties;
import com.marketinghub.oprmcoletormei.marketimport.config.OprmMarketImportScheduleProperties;
import com.marketinghub.oprmcoletormei.marketimport.dto.OprmCompleteImportRunRequestDto;
import com.marketinghub.oprmcoletormei.marketimport.dto.OprmCreateImportRunRequestDto;
import com.marketinghub.oprmcoletormei.marketimport.dto.OprmCnaeUpsertDto;
import com.marketinghub.oprmcoletormei.marketimport.dto.OprmImportFileEventRequestDto;
import com.marketinghub.oprmcoletormei.marketimport.dto.OprmImportFileSeedDto;
import com.marketinghub.oprmcoletormei.marketimport.dto.OprmImportRunCreatedResponseDto;
import com.marketinghub.oprmcoletormei.marketimport.dto.OprmMarketSizeUpsertDto;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Responsável por orquestrar o agendamento e a execução da ingestão de mercado CNPJ/CNAE no OPRM.
 * Também consolida os totais por CNAE enviados ao backend para atualização de market size.
 */
@Component
public class OprmMarketImportScheduler {
    private static final Logger log = LoggerFactory.getLogger(OprmMarketImportScheduler.class);
    private static final Pattern CNAE_7_DIGITS_PATTERN = Pattern.compile("(\\d{7})");
    private static final int DIAGNOSTIC_SAMPLE_LIMIT = 10;

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

    /** Executa a ingestão completa de arquivos CNPJ/CNAE no horário agendado para a execução operacional. */
    @Scheduled(cron = "0 6 14 24 5 *", zone = "America/Sao_Paulo")
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
        Map<String, MarketSizeAccumulator> accumulatedMarketSizesByCnae = new LinkedHashMap<>();
        Map<String, String> cnaeByCnpjBase = new LinkedHashMap<>();
        java.util.Set<String> simplesCountedCnpjBases = new java.util.HashSet<>();
        java.util.Set<String> meiCountedCnpjBases = new java.util.HashSet<>();
        boolean readAllFilesInRun = false;
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
                    if ("EMPRESAS".equalsIgnoreCase(file.datasetType())) {
                        parseAndAccumulateCompanyBaseByCnaeFromEmpresasZip(
                                zipPath, runResponse.runId(), fileId, file.fileName(), cnaeByCnpjBase, accumulatedMarketSizesByCnae);
                    }
                    if ("SIMPLES".equalsIgnoreCase(file.datasetType())) {
                        parseAndAccumulateSimplesAndMeiByCnaeFromSimplesZip(
                                zipPath, runResponse.runId(), fileId, file.fileName(), cnaeByCnpjBase, accumulatedMarketSizesByCnae,
                                simplesCountedCnpjBases, meiCountedCnpjBases);
                    }
                    List<OprmMarketSizeUpsertDto> marketSizes = null;
                    if ("ESTABELECIMENTOS".equalsIgnoreCase(file.datasetType())) {
                        marketSizes = parseAndAccumulateMarketSizesFromEstablishmentsZip(
                                zipPath, runResponse.runId(), fileId, file.fileName(), accumulatedMarketSizesByCnae);
                    }
                    if ("SIMPLES".equalsIgnoreCase(file.datasetType())) {
                        marketSizes = toMarketSizesPayload(accumulatedMarketSizesByCnae);
                        log.info("[run={} fileId={}] Snapshot marketSizes recalculado após SIMPLES para persistir total_empresas/mei/simples. totalRegistros={}",
                                runResponse.runId(),
                                fileId,
                                marketSizes.size());
                    }
                    log.info("[run={} fileId={}] Diagnóstico totalização datasetType={} cnaesCount={} marketSizesCount={}",
                            runResponse.runId(),
                            fileId,
                            file.datasetType(),
                            cnaes != null ? cnaes.size() : 0,
                            marketSizes != null ? marketSizes.size() : 0);
                    totalRowsRead += rowsRead;
                    totalRowsValid += rowsValid;
                    totalRowsRejected += rowsRejected;
                    filesProcessed++;
                    publishFileEvent(runResponse.runId(), fileId, new OprmImportFileEventRequestDto(
                            "COMPLETED", rowsRead, rowsValid, rowsRejected, null, Instant.now(), cnaes, marketSizes));
                    log.info("[run={} fileId={}] Persistência status COMPLETED enviada. rowsRead={}", runResponse.runId(), fileId, rowsRead);
                } catch (Exception e) {
                    hasFailure = true;
                    log.error("[run={} fileId={}] Falha no processamento de arquivo {}. datasetType={} fileUrl={} snapshotDate={} countersAntesFalha={rowsRead:{},rowsValid:{},rowsRejected:{},filesProcessed:{}}",
                            runResponse.runId(),
                            fileId,
                            file.fileName(),
                            file.datasetType(),
                            file.fileUrl(),
                            snapshotDate,
                            totalRowsRead,
                            totalRowsValid,
                            totalRowsRejected,
                            filesProcessed,
                            e);
                    try {
                        publishFileEvent(runResponse.runId(), fileId, new OprmImportFileEventRequestDto(
                                "FAILED", 0L, 0L, 0L, e.getMessage(), Instant.now(), null, null));
                    } catch (Exception publishFailureException) {
                        log.error("[run={} fileId={}] Falha ao publicar evento FAILED após erro de processamento.",
                                runResponse.runId(),
                                fileId,
                                publishFailureException);
                    }
                }
            }
            readAllFilesInRun = true;
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível preparar diretório temporário do run.", e);
        } finally {
            cleanupDirectory(runTempDir, runResponse.runId());
            if (readAllFilesInRun) {
                publishRunComplete(runResponse.runId(), filesProcessed, totalRowsRead, totalRowsValid, totalRowsRejected, hasFailure);
            } else {
                log.warn("[run={}] completeRun não será chamado: a leitura de todos os arquivos da run não foi concluída.",
                        runResponse.runId());
            }
        }

        log.info("Import run OPRM CNPJ agendado com sucesso para snapshotDate={} às 14:06 ({}) com {} arquivos.",
                snapshotDate,
                scheduleProperties.timezone(),
                files.size());
    }



    /** Dispara a finalização automática da última run STARTED após a janela de ingestão. */
    @Scheduled(cron = "0 0 11 * * *", zone = "America/Sao_Paulo")
    public void runScheduledFinalization() {
        log.info("[OPRM-TOTALIZACAO] Disparo de finalização automática iniciado via OPRM-MEI.");
        restClient.post()
                .uri(collectorProperties.backendBaseUrl() + "/api/oprm/market/import-runs/finalize-latest-started")
                .retrieve()
                .toBodilessEntity();
        log.info("[OPRM-TOTALIZACAO] Disparo de finalização automática concluído via OPRM-MEI.");
    }

    /** Lê o arquivo de CNAEs e transforma cada linha válida em payload de upsert para o backend. */
    private List<OprmCnaeUpsertDto> parseCnaesFromZip(Path zipPath, Long runId, Long fileId, String fileName) throws IOException {
        log.info("[run={} fileId={}] Início parse CNAE de {}", runId, fileId, fileName);
        List<OprmCnaeUpsertDto> cnaes = new ArrayList<>();
        try (InputStream in = Files.newInputStream(zipPath);
             java.util.zip.ZipInputStream zipInputStream = new java.util.zip.ZipInputStream(in, StandardCharsets.ISO_8859_1)) {
            java.util.zip.ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream, StandardCharsets.ISO_8859_1));
                String rawLine;
                while ((rawLine = reader.readLine()) != null) {
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

    /** Normaliza campos textuais removendo espaços e aspas de borda quando presentes. */
    private String normalizeField(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }



    /** Extrai somente o primeiro CNAE principal válido (7 dígitos), evitando enviar campo composto para persistência. */
    private String extractPrimaryCnaeCode(String rawCnaeValue) {
        Matcher matcher = CNAE_7_DIGITS_PATTERN.matcher(rawCnaeValue == null ? "" : rawCnaeValue);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1);
    }

    /** Processa ESTABELECIMENTOS e acumula totais por CNAE para consolidar market size incremental. */
    private List<OprmMarketSizeUpsertDto> parseAndAccumulateMarketSizesFromEstablishmentsZip(
            Path zipPath,
            Long runId,
            Long fileId,
            String fileName,
            Map<String, MarketSizeAccumulator> accumulatedMarketSizesByCnae) throws IOException {
        log.info("[run={} fileId={}] Início parse marketSizes (ESTABELECIMENTOS) de {}", runId, fileId, fileName);
        long linhasLidas = 0L;
        long linhasValidas = 0L;
        long linhasIgnoradas = 0L;
        long parseStartMillis = System.currentTimeMillis();
        try (InputStream in = Files.newInputStream(zipPath);
             java.util.zip.ZipInputStream zipInputStream = new java.util.zip.ZipInputStream(in, StandardCharsets.ISO_8859_1)) {
            java.util.zip.ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                long entryReadStartMillis = System.currentTimeMillis();
                log.info("[run={} fileId={}] Lendo entry de ESTABELECIMENTOS: name={} size={}",
                        runId, fileId, entry.getName(), entry.getSize());
                long entryReadDurationMillis = System.currentTimeMillis() - entryReadStartMillis;
                log.info("[run={} fileId={}] Entry aberta para leitura streaming: name={} duracaoMs={} memoriaLivreMb={} memoriaTotalMb={}",
                        runId,
                        fileId,
                        entry.getName(),
                        entryReadDurationMillis,
                        Runtime.getRuntime().freeMemory() / (1024 * 1024),
                        Runtime.getRuntime().totalMemory() / (1024 * 1024));
                long entryProcessStartMillis = System.currentTimeMillis();
                long entryLines = 0L;
                BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream, StandardCharsets.ISO_8859_1));
                String rawLine;
                while ((rawLine = reader.readLine()) != null) {
                    entryLines++;
                    linhasLidas++;
                    if (rawLine.isBlank()) continue;
                    String[] cols = rawLine.split(";", -1);
                    if (cols.length < 12) {
                        linhasIgnoradas++;
                        continue;
                    }
                    String situacaoCadastral = normalizeField(cols[5]);
                    String cnaePrincipal = extractPrimaryCnaeCode(normalizeField(cols[11]));
                    if (cnaePrincipal.isBlank()) {
                        linhasIgnoradas++;
                        log.warn("[run={} fileId={}] Linha ignorada por CNAE principal inválido. coluna11='{}'",
                                runId, fileId, normalizeField(cols[11]));
                        continue;
                    }
                    MarketSizeAccumulator acc = accumulatedMarketSizesByCnae.computeIfAbsent(cnaePrincipal, key -> new MarketSizeAccumulator());
                    acc.totalEstabelecimentos++;
                    if ("02".equals(situacaoCadastral)) {
                        acc.totalEstabelecimentosAtivos++;
                    }
                    linhasValidas++;
                    if (linhasLidas % 500000 == 0) {
                        log.info("[run={} fileId={}] Progresso leitura ESTABELECIMENTOS: linhasLidas={} linhasValidas={} linhasIgnoradas={} cnaesConsolidados={}",
                                runId, fileId, linhasLidas, linhasValidas, linhasIgnoradas, accumulatedMarketSizesByCnae.size());
                    }
                }
                log.info("[run={} fileId={}] Entry processada: name={} duracaoMs={} linhasLidasAcumuladas={} linhasValidasAcumuladas={} linhasIgnoradasAcumuladas={} cnaesConsolidados={}",
                        runId,
                        fileId,
                        entry.getName(),
                        System.currentTimeMillis() - entryProcessStartMillis,
                        linhasLidas,
                        linhasValidas,
                        linhasIgnoradas,
                        accumulatedMarketSizesByCnae.size());
                log.info("[run={} fileId={}] Resumo entry ESTABELECIMENTOS: name={} linhasLidasEntry={}",
                        runId, fileId, entry.getName(), entryLines);
                zipInputStream.closeEntry();
            }
        }
        List<OprmMarketSizeUpsertDto> marketSizes = toMarketSizesPayload(accumulatedMarketSizesByCnae);
        log.info("[run={} fileId={}] Parse marketSizes (ESTABELECIMENTOS) concluído. linhasLidas={} linhasValidas={} linhasIgnoradas={} totalCnaesConsolidados={} duracaoTotalMs={} memoriaLivreMb={} memoriaTotalMb={}",
                runId,
                fileId,
                linhasLidas,
                linhasValidas,
                linhasIgnoradas,
                marketSizes.size(),
                System.currentTimeMillis() - parseStartMillis,
                Runtime.getRuntime().freeMemory() / (1024 * 1024),
                Runtime.getRuntime().totalMemory() / (1024 * 1024));
        return marketSizes;
    }

    private List<OprmMarketSizeUpsertDto> toMarketSizesPayload(Map<String, MarketSizeAccumulator> accumulatedMarketSizesByCnae) {
        List<OprmMarketSizeUpsertDto> payload = new ArrayList<>();
        for (Map.Entry<String, MarketSizeAccumulator> entry : accumulatedMarketSizesByCnae.entrySet()) {
            MarketSizeAccumulator acc = entry.getValue();
            payload.add(new OprmMarketSizeUpsertDto(
                    entry.getKey(),
                    acc.totalEstabelecimentos,
                    acc.totalEstabelecimentosAtivos,
                    acc.totalEmpresas,
                    acc.totalEmpresasMei,
                    acc.totalEmpresasSimples,
                    0.0
            ));
        }
        return payload;
    }

    /** Processa EMPRESAS para consolidar o CNAE principal por CNPJ base e total de empresas por CNAE. */
    private void parseAndAccumulateCompanyBaseByCnaeFromEmpresasZip(Path zipPath,
                                                                     Long runId,
                                                                     Long fileId,
                                                                     String fileName,
                                                                     Map<String, String> cnaeByCnpjBase,
                                                                     Map<String, MarketSizeAccumulator> accumulatedMarketSizesByCnae) throws IOException {
        log.info("[run={} fileId={}] Início parse EMPRESAS para total_empresas por CNAE: {}", runId, fileId, fileName);
        String lastRawLine = null;
        List<String> empresasMapSamples = new ArrayList<>();
        try (InputStream in = Files.newInputStream(zipPath);
             java.util.zip.ZipInputStream zipInputStream = new java.util.zip.ZipInputStream(in, StandardCharsets.ISO_8859_1)) {
            java.util.zip.ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream, StandardCharsets.ISO_8859_1));
                String rawLine;
                while ((rawLine = reader.readLine()) != null) {
                    lastRawLine = rawLine;
                    if (rawLine.isBlank()) continue;
                    String[] cols = rawLine.split(";", -1);
                    if (cols.length < 7) continue;
                    String cnpjBase = normalizeField(cols[0]);
                    String cnaePrincipal = extractPrimaryCnaeCode(normalizeField(cols[6]));
                    if (cnpjBase.isBlank() || cnaePrincipal.isBlank()) continue;
                    if (cnaeByCnpjBase.putIfAbsent(cnpjBase, cnaePrincipal) == null) {
                        MarketSizeAccumulator acc = accumulatedMarketSizesByCnae.computeIfAbsent(cnaePrincipal, key -> new MarketSizeAccumulator());
                        acc.totalEmpresas++;
                        if (empresasMapSamples.size() < DIAGNOSTIC_SAMPLE_LIMIT) {
                            empresasMapSamples.add("cnpjBase='" + cnpjBase + "' cnpjBaseDigits='" + digitsOnly(cnpjBase) + "' cnaePrincipal='" + cnaePrincipal + "'");
                        }
                    }
                }
                zipInputStream.closeEntry();
            }
        } catch (Exception ex) {
            log.error("[run={} fileId={}] Falha no parse EMPRESAS. fileName={} lastRawPayload={} mapCnpjBaseSize={} cnaesConsolidados={}",
                    runId, fileId, fileName, lastRawLine, cnaeByCnpjBase.size(), accumulatedMarketSizesByCnae.size(), ex);
            throw ex;
        }
        log.info("[run={} fileId={}] Parse EMPRESAS concluído. cnpjBaseMapSize={} sampleMapeamentos={}",
                runId,
                fileId,
                cnaeByCnpjBase.size(),
                empresasMapSamples);
    }

    /** Processa SIMPLES para consolidar total de empresas no Simples e no MEI por CNAE. */
    private void parseAndAccumulateSimplesAndMeiByCnaeFromSimplesZip(Path zipPath,
                                                                      Long runId,
                                                                      Long fileId,
                                                                      String fileName,
                                                                      Map<String, String> cnaeByCnpjBase,
                                                                      Map<String, MarketSizeAccumulator> accumulatedMarketSizesByCnae,
                                                                      java.util.Set<String> simplesCountedCnpjBases,
                                                                      java.util.Set<String> meiCountedCnpjBases) throws IOException {
        log.info("[run={} fileId={}] Início parse SIMPLES para total_empresas_simples/mei por CNAE: {}", runId, fileId, fileName);
        String lastRawLine = null;
        long linhasLidas = 0L;
        long linhasComCnpjBase = 0L;
        long linhasSemMatchCnae = 0L;
        long linhasComMatchCnae = 0L;
        String sampleMissingCnpjBase = null;
        String sampleMissingCnpjBaseOnlyDigits = null;
        List<String> simplesInputSamples = new ArrayList<>();
        List<String> simplesMatchSamples = new ArrayList<>();
        List<String> simplesMissingSamples = new ArrayList<>();
        try (InputStream in = Files.newInputStream(zipPath);
             java.util.zip.ZipInputStream zipInputStream = new java.util.zip.ZipInputStream(in, StandardCharsets.ISO_8859_1)) {
            java.util.zip.ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream, StandardCharsets.ISO_8859_1));
                String rawLine;
                while ((rawLine = reader.readLine()) != null) {
                    linhasLidas++;
                    lastRawLine = rawLine;
                    if (rawLine.isBlank()) continue;
                    String[] cols = rawLine.split(";", -1);
                    if (cols.length < 5) continue;
                    String cnpjBase = normalizeField(cols[0]);
                    if (!cnpjBase.isBlank()) {
                        linhasComCnpjBase++;
                    }
                    String simplesOptante = normalizeField(cols[1]);
                    String meiOptante = normalizeField(cols[4]);
                    if (simplesInputSamples.size() < DIAGNOSTIC_SAMPLE_LIMIT) {
                        simplesInputSamples.add("cnpjBase='" + cnpjBase + "' cnpjBaseDigits='" + digitsOnly(cnpjBase) + "' simplesOptante='" + simplesOptante + "' meiOptante='" + meiOptante + "'");
                    }
                    String cnaeCode = cnaeByCnpjBase.get(cnpjBase);
                    if (cnaeCode == null || cnaeCode.isBlank()) {
                        linhasSemMatchCnae++;
                        if (simplesMissingSamples.size() < DIAGNOSTIC_SAMPLE_LIMIT) {
                            simplesMissingSamples.add("cnpjBase='" + cnpjBase + "' cnpjBaseDigits='" + digitsOnly(cnpjBase) + "' simplesOptante='" + simplesOptante + "' meiOptante='" + meiOptante + "'");
                        }
                        if (sampleMissingCnpjBase == null && !cnpjBase.isBlank()) {
                            sampleMissingCnpjBase = cnpjBase;
                            sampleMissingCnpjBaseOnlyDigits = digitsOnly(cnpjBase);
                        }
                        continue;
                    }
                    linhasComMatchCnae++;
                    if (simplesMatchSamples.size() < DIAGNOSTIC_SAMPLE_LIMIT) {
                        simplesMatchSamples.add("cnpjBase='" + cnpjBase + "' cnpjBaseDigits='" + digitsOnly(cnpjBase) + "' cnaeCode='" + cnaeCode + "' simplesOptante='" + simplesOptante + "' meiOptante='" + meiOptante + "'");
                    }
                    MarketSizeAccumulator acc = accumulatedMarketSizesByCnae.computeIfAbsent(cnaeCode, key -> new MarketSizeAccumulator());
                    if ("S".equalsIgnoreCase(simplesOptante) && simplesCountedCnpjBases.add(cnpjBase)) acc.totalEmpresasSimples++;
                    if ("S".equalsIgnoreCase(meiOptante) && meiCountedCnpjBases.add(cnpjBase)) acc.totalEmpresasMei++;
                }
                zipInputStream.closeEntry();
            }
        } catch (Exception ex) {
            log.error("[run={} fileId={}] Falha no parse SIMPLES. fileName={} lastRawPayload={} cnpjMapSize={} simplesCounted={} meiCounted={} cnaesConsolidados={}",
                    runId, fileId, fileName, lastRawLine, cnaeByCnpjBase.size(), simplesCountedCnpjBases.size(), meiCountedCnpjBases.size(), accumulatedMarketSizesByCnae.size(), ex);
            throw ex;
        }
        log.info("[run={} fileId={}] Diagnóstico de mapeamento SIMPLES->CNAE: linhasLidas={} linhasComCnpjBase={} linhasComMatchCnae={} linhasSemMatchCnae={} sampleMissingCnpjBase={} sampleMissingCnpjBaseDigits={} cnpjMapSize={}",
                runId,
                fileId,
                linhasLidas,
                linhasComCnpjBase,
                linhasComMatchCnae,
                linhasSemMatchCnae,
                sampleMissingCnpjBase,
                sampleMissingCnpjBaseOnlyDigits,
                cnaeByCnpjBase.size());
        log.info("[run={} fileId={}] Diagnóstico conteúdo SIMPLES sampleInput={} sampleMatch={} sampleMissing={}",
                runId,
                fileId,
                simplesInputSamples,
                simplesMatchSamples,
                simplesMissingSamples);
        log.info("[run={} fileId={}] Parse SIMPLES concluído. simplesEmpresas={} meiEmpresas={}",
                runId, fileId, simplesCountedCnpjBases.size(), meiCountedCnpjBases.size());
    }

    /** Remove caracteres não numéricos para apoiar diagnóstico de chave CNPJ base entre fontes diferentes. */
    private String digitsOnly(String value) {
        if (value == null || value.isBlank()) return "";
        return value.replaceAll("\\D", "");
    }

    private static final class MarketSizeAccumulator {
        private long totalEstabelecimentos;
        private long totalEstabelecimentosAtivos;
        private long totalEmpresas;
        private long totalEmpresasMei;
        private long totalEmpresasSimples;
    }

    /** Monta a lista padrão de arquivos da base CNPJ a serem processados na execução. */
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

    /** Faz download HTTP do arquivo de origem e persiste localmente no diretório temporário da run. */
    private void downloadToFile(String fileUrl, Path target) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(fileUrl)).GET().build();
        HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(target));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Falha no download HTTP status=" + response.statusCode());
        }
    }

    /** Conta linhas brutas do ZIP para telemetria de ingestão antes da transformação de payload. */
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

    /** Publica o evento de processamento do arquivo (COMPLETED/FAILED) para persistência no backend. */
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

    /** Publica a finalização da run com os contadores consolidados após o processamento dos arquivos. */
    private void publishRunComplete(Long runId, int filesProcessed, long rowsRead, long rowsValid, long rowsRejected, boolean hasFailure) {
        String finalStatus = hasFailure ? "PARTIAL" : "COMPLETED";
        log.info("[run={}] Publicando consolidação final do run status={} filesProcessed={} rowsRead={} rowsValid={} rowsRejected={}",
                runId, finalStatus, filesProcessed, rowsRead, rowsValid, rowsRejected);
        restClient.post().uri(collectorProperties.backendBaseUrl() + "/api/oprm/market/import-runs/" + runId + "/complete")
                .body(new OprmCompleteImportRunRequestDto(finalStatus, Instant.now(), filesProcessed, rowsRead, rowsValid, rowsRejected, null))
                .retrieve().toBodilessEntity();
        log.info("[run={}] Consolidação final do run persistida com sucesso.", runId);
    }

    /** Remove o diretório temporário da run para evitar acúmulo de arquivos locais. */
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
