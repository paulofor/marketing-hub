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
import java.io.BufferedWriter;
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
    private static final int SIMPLES_DEBUG_LINE_LIMIT = 20;
    private static final int SIMPLES_CNPJ_PARTITION_COUNT = 128;

    private final OprmMarketImportScheduleProperties scheduleProperties;
    private final OprmMarketImportCollectorProperties collectorProperties;
    private final RestClient restClient;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /** Inicializa o scheduler com propriedades operacionais, configuração do coletor e cliente HTTP do backend. */
    public OprmMarketImportScheduler(OprmMarketImportScheduleProperties scheduleProperties,
                                     OprmMarketImportCollectorProperties collectorProperties,
                                     RestClient restClient) {
        this.scheduleProperties = scheduleProperties;
        this.collectorProperties = collectorProperties;
        this.restClient = restClient;
    }

    /** Executa a ingestão completa de arquivos CNPJ/CNAE no horário agendado para a execução operacional. */
    @Scheduled(cron = "0 10 23 * * *", zone = "America/Sao_Paulo")
    public void runScheduledImport() {
        log.info("Iniciando runScheduledImport do OPRM CNPJ/CNAE.");
        if (!scheduleProperties.enabled()) {
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
        Path cnpjCnaePartitionDir = runTempDir.resolve("cnpj-cnae-partitions");
        Path simplesPartitionDir = runTempDir.resolve("simples-partitions");
        boolean readAllFilesInRun = false;
        try {
            Files.createDirectories(runTempDir);
            Files.createDirectories(cnpjCnaePartitionDir);
            Files.createDirectories(simplesPartitionDir);
            for (int i = 0; i < files.size(); i++) {
                OprmImportFileSeedDto file = files.get(i);
                Long fileId = runResponse.fileIds().get(i);
                Path zipPath = runTempDir.resolve(file.fileName());
                try {
                    downloadToFile(file.fileUrl(), zipPath);
                    long rowsRead = countRowsFromZip(zipPath, runResponse.runId(), fileId, file.fileName());
                    long rowsValid = rowsRead;
                    long rowsRejected = 0L;
                    List<OprmCnaeUpsertDto> cnaes = "CNAE".equalsIgnoreCase(file.datasetType())
                            ? parseCnaesFromZip(zipPath, runResponse.runId(), fileId, file.fileName())
                            : null;
                    if ("SIMPLES".equalsIgnoreCase(file.datasetType())) {
                        parseAndAccumulateSimplesAndMeiByCnaeInPartitionsFromSimplesZip(
                                zipPath, runResponse.runId(), fileId, file.fileName(), cnpjCnaePartitionDir, simplesPartitionDir, accumulatedMarketSizesByCnae);
                    }
                    List<OprmMarketSizeUpsertDto> marketSizes = null;
                    if ("ESTABELECIMENTOS".equalsIgnoreCase(file.datasetType())) {
                        marketSizes = parseAndAccumulateMarketSizesFromEstablishmentsZip(
                                zipPath, runResponse.runId(), fileId, file.fileName(), accumulatedMarketSizesByCnae, cnpjCnaePartitionDir);
                    }
                    if ("SIMPLES".equalsIgnoreCase(file.datasetType())) {
                        marketSizes = toMarketSizesPayload(accumulatedMarketSizesByCnae);
                    }
                    totalRowsRead += rowsRead;
                    totalRowsValid += rowsValid;
                    totalRowsRejected += rowsRejected;
                    filesProcessed++;
                    publishFileEvent(runResponse.runId(), fileId, new OprmImportFileEventRequestDto(
                            "COMPLETED", rowsRead, rowsValid, rowsRejected, null, Instant.now(), cnaes, marketSizes));
                } catch (OutOfMemoryError e) {
                    hasFailure = true;
                    log.error("[run={} fileId={}] Falha de capacidade ao processar arquivo OPRM. fileName={} datasetType={}",
                            runResponse.runId(), fileId, file.fileName(), file.datasetType(), e);
                    publishFailedFileEvent(runResponse.runId(), fileId, "OutOfMemoryError: " + e.getMessage());
                } catch (Exception e) {
                    hasFailure = true;
                    log.error("[run={} fileId={}] Falha ao processar arquivo OPRM. fileName={} datasetType={}",
                            runResponse.runId(), fileId, file.fileName(), file.datasetType(), e);
                    publishFailedFileEvent(runResponse.runId(), fileId, e.getMessage());
                }
            }
            readAllFilesInRun = true;
        } catch (IOException e) {
            log.error("[run={}] Falha ao preparar diretório temporário do run OPRM. tempDir={}", runResponse.runId(), runTempDir, e);
            throw new IllegalStateException("Não foi possível preparar diretório temporário do run.", e);
        } finally {
            cleanupDirectory(runTempDir, runResponse.runId());
            if (readAllFilesInRun) {
                publishRunComplete(runResponse.runId(), filesProcessed, totalRowsRead, totalRowsValid, totalRowsRejected, hasFailure);
            }
        }
    }



    /** Dispara a finalização automática da última run STARTED após a janela de ingestão. */
    @Scheduled(cron = "0 0 11 * * *", zone = "America/Sao_Paulo")
    public void runScheduledFinalization() {
        log.info("[OPRM-TOTALIZACAO] Disparo de finalização automática iniciado via OPRM-MEI.");
        if (!scheduleProperties.enabled()) {
            log.info("[OPRM-TOTALIZACAO] Finalização automática ignorada porque a rotina OPRM CNPJ/CNAE está desativada.");
            return;
        }
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
            Map<String, MarketSizeAccumulator> accumulatedMarketSizesByCnae,
            Path cnpjCnaePartitionDir) throws IOException {
        log.info("[run={} fileId={}] Início parse marketSizes (ESTABELECIMENTOS) de {}", runId, fileId, fileName);
        long linhasLidas = 0L;
        long linhasDetalhadasSemMatch = 0L;
        long linhasValidas = 0L;
        long linhasIgnoradas = 0L;
        long parseStartMillis = System.currentTimeMillis();
        try (InputStream in = Files.newInputStream(zipPath);
             java.util.zip.ZipInputStream zipInputStream = new java.util.zip.ZipInputStream(in, StandardCharsets.ISO_8859_1);
             PartitionedCnpjCnaeWriters cnpjCnaeWriters = new PartitionedCnpjCnaeWriters(cnpjCnaePartitionDir)) {
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
                    String cnpjBase = normalizeField(cols[0]);
                    String situacaoCadastral = normalizeField(cols[5]);
                    String cnaePrincipal = extractPrimaryCnaeCode(normalizeField(cols[11]));
                    if (cnaePrincipal.isBlank()) {
                        linhasIgnoradas++;
                        log.warn("[run={} fileId={}] Linha ignorada por CNAE principal inválido. coluna11='{}'",
                                runId, fileId, normalizeField(cols[11]));
                        continue;
                    }
                    if (!cnpjBase.isBlank()) {
                        cnpjCnaeWriters.write(cnpjBase, cnaePrincipal);
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

    /** Converte os acumuladores compactos por CNAE para o payload oficial de upsert no backend. */
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

    /** Processa SIMPLES em partições para limitar o mapa cnpjBase->CNAE mantido em heap. */
    private void parseAndAccumulateSimplesAndMeiByCnaeInPartitionsFromSimplesZip(
            Path simplesZipPath,
            Long runId,
            Long fileId,
            String fileName,
            Path cnpjCnaePartitionDir,
            Path simplesPartitionDir,
            Map<String, MarketSizeAccumulator> accumulatedMarketSizesByCnae) throws IOException {
        log.info("[run={} fileId={}] Início parse SIMPLES em {} partições para total_empresas_simples/mei por CNAE: {}",
                runId, fileId, SIMPLES_CNPJ_PARTITION_COUNT, fileName);
        partitionSimplesZipByCnpjBase(simplesZipPath, runId, fileId, simplesPartitionDir);
        long totalLinhasLidas = 0L;
        long totalLinhasComCnpjBase = 0L;
        long totalLinhasComMatchCnae = 0L;
        long totalLinhasSemMatchCnae = 0L;
        long totalSimplesEmpresas = 0L;
        long totalMeiEmpresas = 0L;
        for (int partition = 0; partition < SIMPLES_CNPJ_PARTITION_COUNT; partition++) {
            Map<String, String> cnaeByCnpjBase = loadCompanyBaseCnaeMapPartitionFromFile(
                    cnpjCnaePartitionFile(cnpjCnaePartitionDir, partition), runId, fileId, partition);
            SimplesPartitionCounters counters = parseSimplesPartitionAndAccumulateByCnae(
                    simplesPartitionFile(simplesPartitionDir, partition),
                    runId,
                    fileId,
                    fileName,
                    partition,
                    cnaeByCnpjBase,
                    accumulatedMarketSizesByCnae);
            totalLinhasLidas += counters.linhasLidas;
            totalLinhasComCnpjBase += counters.linhasComCnpjBase;
            totalLinhasComMatchCnae += counters.linhasComMatchCnae;
            totalLinhasSemMatchCnae += counters.linhasSemMatchCnae;
            totalSimplesEmpresas += counters.simplesEmpresas;
            totalMeiEmpresas += counters.meiEmpresas;
            log.info("[run={} fileId={}] Partição SIMPLES concluída. partition={}/{} cnpjMapSize={} linhasLidas={} linhasComMatchCnae={} linhasSemMatchCnae={} simplesEmpresas={} meiEmpresas={} memoriaLivreMb={} memoriaTotalMb={}",
                    runId,
                    fileId,
                    partition + 1,
                    SIMPLES_CNPJ_PARTITION_COUNT,
                    cnaeByCnpjBase.size(),
                    counters.linhasLidas,
                    counters.linhasComMatchCnae,
                    counters.linhasSemMatchCnae,
                    counters.simplesEmpresas,
                    counters.meiEmpresas,
                    Runtime.getRuntime().freeMemory() / (1024 * 1024),
                    Runtime.getRuntime().totalMemory() / (1024 * 1024));
        }
        log.info("[run={} fileId={}] Parse SIMPLES particionado concluído. totalParticoes={} linhasLidas={} linhasComCnpjBase={} linhasComMatchCnae={} linhasSemMatchCnae={} simplesEmpresas={} meiEmpresas={} cnaesConsolidados={}",
                runId,
                fileId,
                SIMPLES_CNPJ_PARTITION_COUNT,
                totalLinhasLidas,
                totalLinhasComCnpjBase,
                totalLinhasComMatchCnae,
                totalLinhasSemMatchCnae,
                totalSimplesEmpresas,
                totalMeiEmpresas,
                accumulatedMarketSizesByCnae.size());
    }

    /** Particiona o ZIP de SIMPLES em arquivos texto menores com os campos necessários para o cruzamento. */
    private void partitionSimplesZipByCnpjBase(Path zipPath, Long runId, Long fileId, Path simplesPartitionDir) throws IOException {
        long linhasLidas = 0L;
        long linhasGravadas = 0L;
        try (InputStream in = Files.newInputStream(zipPath);
             java.util.zip.ZipInputStream zipInputStream = new java.util.zip.ZipInputStream(in, StandardCharsets.ISO_8859_1);
             PartitionedSimplesWriters simplesWriters = new PartitionedSimplesWriters(simplesPartitionDir)) {
            java.util.zip.ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream, StandardCharsets.ISO_8859_1));
                String rawLine;
                while ((rawLine = reader.readLine()) != null) {
                    linhasLidas++;
                    if (rawLine.isBlank()) continue;
                    String[] cols = rawLine.split(";", -1);
                    if (cols.length < 5) continue;
                    String cnpjBase = normalizeField(cols[0]);
                    if (cnpjBase.isBlank()) continue;
                    simplesWriters.write(cnpjBase, normalizeField(cols[1]), normalizeField(cols[4]));
                    linhasGravadas++;
                }
                zipInputStream.closeEntry();
            }
        } catch (Exception ex) {
            log.error("[run={} fileId={}] Falha ao particionar SIMPLES em arquivos menores. zipPath={} linhasLidas={} linhasGravadas={}",
                    runId, fileId, zipPath.getFileName(), linhasLidas, linhasGravadas, ex);
            throw ex;
        }
        log.info("[run={} fileId={}] Particionamento SIMPLES concluído. zipPath={} totalParticoes={} linhasLidas={} linhasGravadas={}",
                runId, fileId, zipPath.getFileName(), SIMPLES_CNPJ_PARTITION_COUNT, linhasLidas, linhasGravadas);
    }

    /** Carrega para memória somente o arquivo particionado de vínculo CNPJ base -> CNAE solicitado. */
    private Map<String, String> loadCompanyBaseCnaeMapPartitionFromFile(Path partitionFile, Long runId, Long fileId, int partition) throws IOException {
        Map<String, String> cnaeByCnpjBase = new LinkedHashMap<>();
        if (!Files.exists(partitionFile)) {
            return cnaeByCnpjBase;
        }
        long linhasLidas = 0L;
        try (BufferedReader reader = Files.newBufferedReader(partitionFile, StandardCharsets.UTF_8)) {
            String rawLine;
            while ((rawLine = reader.readLine()) != null) {
                linhasLidas++;
                String[] cols = rawLine.split(";", -1);
                if (cols.length < 2) continue;
                String cnpjBase = normalizeField(cols[0]);
                String cnaePrincipal = extractPrimaryCnaeCode(normalizeField(cols[1]));
                if (cnpjBase.isBlank() || cnaePrincipal.isBlank()) continue;
                cnaeByCnpjBase.putIfAbsent(cnpjBase, cnaePrincipal);
            }
        } catch (Exception ex) {
            log.error("[run={} fileId={}] Falha ao carregar partição CNPJ->CNAE. partition={}/{} partitionFile={} linhasLidas={} mapSize={}",
                    runId, fileId, partition + 1, SIMPLES_CNPJ_PARTITION_COUNT, partitionFile, linhasLidas, cnaeByCnpjBase.size(), ex);
            throw ex;
        }
        log.info("[run={} fileId={}] Partição CNPJ->CNAE carregada. partition={}/{} partitionFile={} linhasLidas={} mapSize={}",
                runId, fileId, partition + 1, SIMPLES_CNPJ_PARTITION_COUNT, partitionFile.getFileName(), linhasLidas, cnaeByCnpjBase.size());
        return cnaeByCnpjBase;
    }

    /** Processa uma partição textual do SIMPLES e acumula contadores de empresas por CNAE. */
    private SimplesPartitionCounters parseSimplesPartitionAndAccumulateByCnae(
            Path partitionFile,
            Long runId,
            Long fileId,
            String fileName,
            int partition,
            Map<String, String> cnaeByCnpjBase,
            Map<String, MarketSizeAccumulator> accumulatedMarketSizesByCnae) throws IOException {
        String lastRawLine = null;
        SimplesPartitionCounters counters = new SimplesPartitionCounters();
        java.util.Set<String> simplesCountedCnpjBases = new java.util.HashSet<>();
        java.util.Set<String> meiCountedCnpjBases = new java.util.HashSet<>();
        List<String> simplesInputSamples = new ArrayList<>();
        List<String> simplesMatchSamples = new ArrayList<>();
        List<String> simplesMissingSamples = new ArrayList<>();
        if (!Files.exists(partitionFile)) {
            return counters;
        }
        try (BufferedReader reader = Files.newBufferedReader(partitionFile, StandardCharsets.UTF_8)) {
            String rawLine;
            while ((rawLine = reader.readLine()) != null) {
                lastRawLine = rawLine;
                if (rawLine.isBlank()) continue;
                String[] cols = rawLine.split(";", -1);
                if (cols.length < 3) continue;
                String cnpjBase = normalizeField(cols[0]);
                counters.linhasLidas++;
                if (!cnpjBase.isBlank()) {
                    counters.linhasComCnpjBase++;
                }
                String simplesOptante = normalizeField(cols[1]);
                String meiOptante = normalizeField(cols[2]);
                if (simplesInputSamples.size() < DIAGNOSTIC_SAMPLE_LIMIT) {
                    simplesInputSamples.add("cnpjBase='" + cnpjBase + "' cnpjBaseDigits='" + digitsOnly(cnpjBase) + "' simplesOptante='" + simplesOptante + "' meiOptante='" + meiOptante + "'");
                }
                String cnaeCode = cnaeByCnpjBase.get(cnpjBase);
                if (cnaeCode == null || cnaeCode.isBlank()) {
                    counters.linhasSemMatchCnae++;
                    if (simplesMissingSamples.size() < DIAGNOSTIC_SAMPLE_LIMIT) {
                        simplesMissingSamples.add("cnpjBase='" + cnpjBase + "' cnpjBaseDigits='" + digitsOnly(cnpjBase) + "' simplesOptante='" + simplesOptante + "' meiOptante='" + meiOptante + "'");
                    }
                    continue;
                }
                counters.linhasComMatchCnae++;
                if (simplesMatchSamples.size() < DIAGNOSTIC_SAMPLE_LIMIT) {
                    simplesMatchSamples.add("cnpjBase='" + cnpjBase + "' cnpjBaseDigits='" + digitsOnly(cnpjBase) + "' cnaeCode='" + cnaeCode + "' simplesOptante='" + simplesOptante + "' meiOptante='" + meiOptante + "'");
                }
                MarketSizeAccumulator acc = accumulatedMarketSizesByCnae.computeIfAbsent(cnaeCode, key -> new MarketSizeAccumulator());
                if ("S".equalsIgnoreCase(simplesOptante) && simplesCountedCnpjBases.add(cnpjBase)) {
                    acc.totalEmpresasSimples++;
                    counters.simplesEmpresas++;
                }
                if ("S".equalsIgnoreCase(meiOptante) && meiCountedCnpjBases.add(cnpjBase)) {
                    acc.totalEmpresasMei++;
                    counters.meiEmpresas++;
                }
            }
        } catch (Exception ex) {
            log.error("[run={} fileId={}] Falha no parse particionado SIMPLES. fileName={} partition={}/{} lastRawPayload={} cnpjMapSize={} simplesCounted={} meiCounted={} cnaesConsolidados={}",
                    runId, fileId, fileName, partition + 1, SIMPLES_CNPJ_PARTITION_COUNT, lastRawLine, cnaeByCnpjBase.size(), simplesCountedCnpjBases.size(), meiCountedCnpjBases.size(), accumulatedMarketSizesByCnae.size(), ex);
            throw ex;
        }
        log.info("[run={} fileId={}] Diagnóstico conteúdo SIMPLES particionado. partition={}/{} sampleInput={} sampleMatch={} sampleMissing={}",
                runId,
                fileId,
                partition + 1,
                SIMPLES_CNPJ_PARTITION_COUNT,
                simplesInputSamples,
                simplesMatchSamples,
                simplesMissingSamples);
        return counters;
    }

    /** Resolve o caminho do arquivo de partição CNPJ->CNAE usado como armazenamento intermediário em disco. */
    private Path cnpjCnaePartitionFile(Path partitionDir, int partition) {
        return partitionDir.resolve("cnpj-cnae-part-" + partition + ".csv");
    }

    /** Resolve o caminho do arquivo de partição SIMPLES usado como armazenamento intermediário em disco. */
    private Path simplesPartitionFile(Path partitionDir, int partition) {
        return partitionDir.resolve("simples-part-" + partition + ".csv");
    }

    /** Informa se o CNPJ base pertence à partição solicitada sem manter todas as chaves em memória. */
    private boolean belongsToPartition(String cnpjBase, int partition, int totalPartitions) {
        if (cnpjBase == null || cnpjBase.isBlank()) return false;
        return Math.floorMod(cnpjBase.hashCode(), totalPartitions) == partition;
    }

    /** Remove caracteres não numéricos para apoiar diagnóstico de chave CNPJ base entre fontes diferentes. */
    private String digitsOnly(String value) {
        if (value == null || value.isBlank()) return "";
        return value.replaceAll("\\D", "");
    }


    /** Gerencia escritores particionados para vínculos CNPJ base -> CNAE gravados em disco. */
    private final class PartitionedCnpjCnaeWriters implements AutoCloseable {
        private final Path partitionDir;
        private final BufferedWriter[] writers = new BufferedWriter[SIMPLES_CNPJ_PARTITION_COUNT];

        /** Inicializa o gerenciador de escritores para o diretório de partições CNPJ->CNAE. */
        private PartitionedCnpjCnaeWriters(Path partitionDir) {
            this.partitionDir = partitionDir;
        }

        /** Grava uma linha no arquivo da partição calculada a partir do CNPJ base. */
        private void write(String cnpjBase, String cnaePrincipal) throws IOException {
            int partition = Math.floorMod(cnpjBase.hashCode(), SIMPLES_CNPJ_PARTITION_COUNT);
            BufferedWriter writer = writers[partition];
            if (writer == null) {
                writer = Files.newBufferedWriter(cnpjCnaePartitionFile(partitionDir, partition), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                writers[partition] = writer;
            }
            writer.write(cnpjBase);
            writer.write(';');
            writer.write(cnaePrincipal);
            writer.newLine();
        }

        /** Fecha todos os escritores de partição abertos. */
        @Override
        public void close() throws IOException {
            IOException failure = null;
            for (BufferedWriter writer : writers) {
                if (writer == null) continue;
                try {
                    writer.close();
                } catch (IOException ex) {
                    if (failure == null) failure = ex;
                    else failure.addSuppressed(ex);
                }
            }
            if (failure != null) throw failure;
        }
    }

    /** Gerencia escritores particionados para linhas de SIMPLES gravadas em disco. */
    private final class PartitionedSimplesWriters implements AutoCloseable {
        private final Path partitionDir;
        private final BufferedWriter[] writers = new BufferedWriter[SIMPLES_CNPJ_PARTITION_COUNT];

        /** Inicializa o gerenciador de escritores para o diretório de partições SIMPLES. */
        private PartitionedSimplesWriters(Path partitionDir) {
            this.partitionDir = partitionDir;
        }

        /** Grava uma linha no arquivo da partição calculada a partir do CNPJ base. */
        private void write(String cnpjBase, String simplesOptante, String meiOptante) throws IOException {
            int partition = Math.floorMod(cnpjBase.hashCode(), SIMPLES_CNPJ_PARTITION_COUNT);
            BufferedWriter writer = writers[partition];
            if (writer == null) {
                writer = Files.newBufferedWriter(simplesPartitionFile(partitionDir, partition), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                writers[partition] = writer;
            }
            writer.write(cnpjBase);
            writer.write(';');
            writer.write(simplesOptante);
            writer.write(';');
            writer.write(meiOptante);
            writer.newLine();
        }

        /** Fecha todos os escritores de partição abertos. */
        @Override
        public void close() throws IOException {
            IOException failure = null;
            for (BufferedWriter writer : writers) {
                if (writer == null) continue;
                try {
                    writer.close();
                } catch (IOException ex) {
                    if (failure == null) failure = ex;
                    else failure.addSuppressed(ex);
                }
            }
            if (failure != null) throw failure;
        }
    }

    /** Mantém contadores compactos por CNAE durante a consolidação de market size. */
    private static final class MarketSizeAccumulator {
        private long totalEstabelecimentos;
        private long totalEstabelecimentosAtivos;
        private long totalEmpresas;
        private long totalEmpresasMei;
        private long totalEmpresasSimples;
    }

    /** Mantém os contadores de uma partição do SIMPLES sem compartilhar sets grandes entre partições. */
    private static final class SimplesPartitionCounters {
        private long linhasLidas;
        private long linhasComCnpjBase;
        private long linhasComMatchCnae;
        private long linhasSemMatchCnae;
        private long simplesEmpresas;
        private long meiEmpresas;
    }

    /** Monta a lista de arquivos da base CNPJ considerando somente ESTABELECIMENTOS e SIMPLES. */
    private List<OprmImportFileSeedDto> buildFiles(String sourceUrl) {
        List<OprmImportFileSeedDto> files = new ArrayList<>();
        for (int i = 1; i < 10; i++) files.add(file("Estabelecimentos" + i + ".zip", sourceUrl, "ESTABELECIMENTOS"));
        files.add(file("Simples.zip", sourceUrl, "SIMPLES"));
        return files;
    }

    /** Cria a semente de arquivo esperada pelo backend para rastrear a run de importação. */
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

    /** Publica um evento FAILED de arquivo e preserva log completo caso a publicação também falhe. */
    private void publishFailedFileEvent(Long runId, Long fileId, String errorMessage) {
        try {
            publishFileEvent(runId, fileId, new OprmImportFileEventRequestDto(
                    "FAILED", 0L, 0L, 0L, errorMessage, Instant.now(), null, null));
        } catch (Exception publishFailureException) {
            log.error("[run={} fileId={}] Falha ao publicar evento FAILED após erro de processamento.", runId, fileId, publishFailureException);
            throw new IllegalStateException("Falha ao publicar evento FAILED após erro de processamento.", publishFailureException);
        }
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
