package com.marketinghub.oprmcoletormei.marketimport.service;

import com.marketinghub.oprmcoletormei.marketimport.config.OprmMarketImportCollectorProperties;
import com.marketinghub.oprmcoletormei.marketimport.config.OprmMarketImportScheduleProperties;
import com.marketinghub.oprmcoletormei.marketimport.dto.OprmCreateImportRunRequestDto;
import com.marketinghub.oprmcoletormei.marketimport.dto.OprmImportFileSeedDto;
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

    public OprmMarketImportScheduler(OprmMarketImportScheduleProperties scheduleProperties,
                                     OprmMarketImportCollectorProperties collectorProperties,
                                     RestClient restClient) {
        this.scheduleProperties = scheduleProperties;
        this.collectorProperties = collectorProperties;
        this.restClient = restClient;
    }

    @Scheduled(cron = "${oprm.market-import.schedule.cron}", zone = "${oprm.market-import.schedule.timezone}")
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

        restClient.post()
                .uri(collectorProperties.backendBaseUrl() + "/api/oprm/market/import-runs")
                .body(request)
                .retrieve()
                .toBodilessEntity();

        log.info("Import run OPRM CNPJ agendado com sucesso para snapshotDate={} às 22:00 ({}) com {} arquivos.",
                snapshotDate,
                scheduleProperties.timezone(),
                files.size());
    }

    private List<OprmImportFileSeedDto> buildFiles(String sourceUrl) {
        List<OprmImportFileSeedDto> files = new ArrayList<>();
        files.add(file("Cnaes.zip", sourceUrl, "CNAE"));
        for (int i = 0; i < 10; i++) files.add(file("Empresas" + i + ".zip", sourceUrl, "EMPRESAS"));
        for (int i = 0; i < 10; i++) files.add(file("Estabelecimentos" + i + ".zip", sourceUrl, "ESTABELECIMENTOS"));
        files.add(file("Simples.zip", sourceUrl, "SIMPLES"));
        for (int i = 0; i < 10; i++) files.add(file("Socios" + i + ".zip", sourceUrl, "SOCIOS"));
        return files;
    }

    private OprmImportFileSeedDto file(String fileName, String sourceUrl, String datasetType) {
        return new OprmImportFileSeedDto(fileName, sourceUrl + "/" + fileName, datasetType, "STARTED");
    }
}
