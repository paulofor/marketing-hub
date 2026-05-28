package com.marketinghub.oprmcoletormei.estabelecimento.service;

import com.marketinghub.oprmcoletormei.estabelecimento.dto.OprmEstabelecimentoCnaeRaizBatchRequestDto;
import com.marketinghub.oprmcoletormei.estabelecimento.dto.OprmEstabelecimentoCnaeRaizUpsertDto;
import com.marketinghub.oprmcoletormei.marketimport.config.OprmMarketImportCollectorProperties;
import com.marketinghub.oprmcoletormei.marketimport.config.OprmMarketImportScheduleProperties;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Responsável pelo processo simples de ingestão da tabela oprm_estabelecimento_cnae_raiz a partir dos arquivos ESTABELECIMENTOS.
 */
@Component
public class OprmEstabelecimentoCnaeRaizIngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(OprmEstabelecimentoCnaeRaizIngestionScheduler.class);
    private static final int BATCH_SIZE = 5000;
    private static final Pattern CNAE_7_DIGITS_PATTERN = Pattern.compile("(\\d{7})");

    private final OprmMarketImportScheduleProperties scheduleProperties;
    private final OprmMarketImportCollectorProperties collectorProperties;
    private final RestClient restClient;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Cria o scheduler com propriedades compartilhadas de ingestão e cliente HTTP do backend.
     */
    public OprmEstabelecimentoCnaeRaizIngestionScheduler(
            OprmMarketImportScheduleProperties scheduleProperties,
            OprmMarketImportCollectorProperties collectorProperties,
            RestClient restClient) {
        this.scheduleProperties = scheduleProperties;
        this.collectorProperties = collectorProperties;
        this.restClient = restClient;
    }

    /**
     * Executa em 29/05 às 10:00 a ingestão direta de todos os arquivos ESTABELECIMENTOS na tabela dedicada.
     */
    @Scheduled(cron = "0 0 10 29 5 *", zone = "America/Sao_Paulo")
    public void runScheduledEstabelecimentoCnaeRaizIngestion() {
        log.info("Iniciando ingestão simples OPRM de ESTABELECIMENTOS para oprm_estabelecimento_cnae_raiz.");
        if (!scheduleProperties.enabled()) {
            log.info("Ingestão simples OPRM de ESTABELECIMENTOS ignorada porque schedule.enabled=false.");
            return;
        }
        LocalDate snapshotDate = resolveSnapshotDate();
        String sourceUrl = resolveSourceUrl(snapshotDate);
        Path runTempDir = Paths.get(collectorProperties.tempDir(), "estabelecimento-cnae-raiz-" + snapshotDate);
        long totalRowsRead = 0L;
        long totalRowsValid = 0L;
        long totalRowsRejected = 0L;
        try {
            Files.createDirectories(runTempDir);
            for (String fileName : buildEstabelecimentosFileNames()) {
                Path zipPath = runTempDir.resolve(fileName);
                String fileUrl = sourceUrl + "/" + fileName;
                downloadToFile(fileUrl, zipPath);
                IngestionFileCounters counters = ingestEstabelecimentosZip(zipPath, fileName);
                totalRowsRead += counters.rowsRead();
                totalRowsValid += counters.rowsValid();
                totalRowsRejected += counters.rowsRejected();
            }
            log.info("Ingestão simples OPRM de ESTABELECIMENTOS concluída. snapshotDate={} rowsRead={} rowsValid={} rowsRejected={}",
                    snapshotDate, totalRowsRead, totalRowsValid, totalRowsRejected);
        } catch (Exception ex) {
            log.error("Falha na ingestão simples OPRM de ESTABELECIMENTOS. snapshotDate={} sourceUrl={} tempDir={} rowsRead={} rowsValid={} rowsRejected={} exceptionClass={} exceptionMessage={} errorLine={}",
                    snapshotDate,
                    sourceUrl,
                    runTempDir,
                    totalRowsRead,
                    totalRowsValid,
                    totalRowsRejected,
                    ex.getClass().getName(),
                    ex.getMessage(),
                    firstStackLine(ex),
                    ex);
            throw new IllegalStateException("Falha na ingestão simples OPRM de ESTABELECIMENTOS.", ex);
        } finally {
            cleanupDirectory(runTempDir);
        }
    }

    /**
     * Resolve o snapshot configurado, preservando o fallback por data atual no fuso operacional.
     */
    private LocalDate resolveSnapshotDate() {
        if (scheduleProperties.snapshotDate() == null || scheduleProperties.snapshotDate().isBlank()) {
            return LocalDate.now(ZoneId.of(scheduleProperties.timezone()));
        }
        return LocalDate.parse(scheduleProperties.snapshotDate());
    }

    /**
     * Monta a URL base do snapshot sem barra final duplicada.
     */
    private String resolveSourceUrl(LocalDate snapshotDate) {
        String sourceBaseUrl = scheduleProperties.sourceBaseUrl().endsWith("/")
                ? scheduleProperties.sourceBaseUrl().substring(0, scheduleProperties.sourceBaseUrl().length() - 1)
                : scheduleProperties.sourceBaseUrl();
        return sourceBaseUrl + "/" + snapshotDate;
    }

    /**
     * Lista todos os arquivos ESTABELECIMENTOS disponíveis no snapshot público da Receita.
     */
    private List<String> buildEstabelecimentosFileNames() {
        List<String> fileNames = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            fileNames.add("Estabelecimentos" + i + ".zip");
        }
        return fileNames;
    }

    /**
     * Faz download HTTP do arquivo de origem para processamento local em streaming.
     */
    private void downloadToFile(String fileUrl, Path target) throws IOException, InterruptedException {
        log.info("Baixando arquivo ESTABELECIMENTOS. fileUrl={} target={}", fileUrl, target);
        HttpRequest request = HttpRequest.newBuilder(URI.create(fileUrl)).GET().build();
        HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(target));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Falha no download HTTP status=" + response.statusCode() + " fileUrl=" + fileUrl);
        }
    }

    /**
     * Lê um ZIP de ESTABELECIMENTOS em streaming e envia lotes normalizados ao backend.
     */
    private IngestionFileCounters ingestEstabelecimentosZip(Path zipPath, String fileName) throws IOException {
        log.info("Iniciando leitura streaming de ESTABELECIMENTOS. fileName={} zipPath={}", fileName, zipPath);
        long rowsRead = 0L;
        long rowsValid = 0L;
        long rowsRejected = 0L;
        List<OprmEstabelecimentoCnaeRaizUpsertDto> batch = new ArrayList<>(BATCH_SIZE);
        try (InputStream in = Files.newInputStream(zipPath);
             ZipInputStream zipInputStream = new ZipInputStream(in, StandardCharsets.ISO_8859_1)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream, StandardCharsets.ISO_8859_1));
                String rawLine;
                while ((rawLine = reader.readLine()) != null) {
                    rowsRead++;
                    log.debug("payload_bruto_estabelecimento fileName={} raw='{}'", fileName, rawLine);
                    OprmEstabelecimentoCnaeRaizUpsertDto row = parseEstabelecimentoLine(rawLine);
                    if (row == null) {
                        rowsRejected++;
                    } else {
                        rowsValid++;
                        batch.add(row);
                    }
                    if (batch.size() >= BATCH_SIZE) {
                        publishBatch(batch, fileName);
                        batch.clear();
                    }
                    if (rowsRead % 500000 == 0) {
                        log.info("Progresso ingestão simples ESTABELECIMENTOS. fileName={} rowsRead={} rowsValid={} rowsRejected={}",
                                fileName, rowsRead, rowsValid, rowsRejected);
                    }
                }
                zipInputStream.closeEntry();
            }
        }
        if (!batch.isEmpty()) {
            publishBatch(batch, fileName);
        }
        log.info("Arquivo ESTABELECIMENTOS processado. fileName={} rowsRead={} rowsValid={} rowsRejected={}",
                fileName, rowsRead, rowsValid, rowsRejected);
        return new IngestionFileCounters(rowsRead, rowsValid, rowsRejected);
    }

    /**
     * Converte uma linha bruta de ESTABELECIMENTOS no DTO mínimo aceito pelo backend.
     */
    private OprmEstabelecimentoCnaeRaizUpsertDto parseEstabelecimentoLine(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return null;
        }
        String[] cols = rawLine.split(";", -1);
        if (cols.length < 28) {
            return null;
        }
        String cnpjRaiz = digitsOnly(normalizeField(cols[0]));
        String cnaeCode = extractPrimaryCnaeCode(normalizeField(cols[11]));
        if (cnpjRaiz.length() != 8 || cnaeCode.isBlank()) {
            return null;
        }
        return new OprmEstabelecimentoCnaeRaizUpsertDto(cnpjRaiz, cnaeCode, normalizeEmail(normalizeField(cols[27])));
    }

    /**
     * Publica um lote já normalizado no endpoint backend da tabela de estabelecimentos por CNAE raiz.
     */
    private void publishBatch(List<OprmEstabelecimentoCnaeRaizUpsertDto> batch, String fileName) {
        log.info("Publicando lote de estabelecimentos OPRM. fileName={} batchSize={}", fileName, batch.size());
        restClient.post()
                .uri(collectorProperties.backendBaseUrl() + "/api/oprm/market/estabelecimentos-cnae-raiz/batch")
                .body(new OprmEstabelecimentoCnaeRaizBatchRequestDto(List.copyOf(batch)))
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Normaliza campos textuais removendo espaços e aspas de borda quando presentes.
     */
    private String normalizeField(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    /**
     * Extrai somente o primeiro CNAE principal válido com 7 dígitos.
     */
    private String extractPrimaryCnaeCode(String rawCnaeValue) {
        Matcher matcher = CNAE_7_DIGITS_PATTERN.matcher(rawCnaeValue == null ? "" : rawCnaeValue);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1);
    }

    /**
     * Remove caracteres não numéricos para normalizar chaves do arquivo ESTABELECIMENTOS.
     */
    private String digitsOnly(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("\\D", "");
    }

    /**
     * Normaliza email vazio para null e limita o tamanho ao contrato do backend.
     */
    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String normalized = email.trim().toLowerCase();
        return normalized.length() > 254 ? normalized.substring(0, 254) : normalized;
    }

    /**
     * Identifica a primeira linha de stack trace disponível para diagnóstico operacional.
     */
    private String firstStackLine(Exception ex) {
        return ex.getStackTrace().length == 0 ? "sem-stacktrace" : ex.getStackTrace()[0].toString();
    }

    /**
     * Remove os arquivos temporários baixados durante a ingestão simples.
     */
    private void cleanupDirectory(Path runTempDir) {
        if (runTempDir == null || !Files.exists(runTempDir)) {
            return;
        }
        try (var walk = Files.walk(runTempDir)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    log.warn("Falha ao deletar arquivo temporário da ingestão simples OPRM. path={}", path, ex);
                }
            });
            log.info("Limpeza do diretório temporário da ingestão simples concluída. tempDir={}", runTempDir);
        } catch (IOException ex) {
            log.warn("Falha ao limpar diretório temporário da ingestão simples OPRM. tempDir={}", runTempDir, ex);
        }
    }

    /**
     * Agrupa contadores de leitura, persistência e rejeição do arquivo processado.
     */
    private record IngestionFileCounters(long rowsRead, long rowsValid, long rowsRejected) {}
}
