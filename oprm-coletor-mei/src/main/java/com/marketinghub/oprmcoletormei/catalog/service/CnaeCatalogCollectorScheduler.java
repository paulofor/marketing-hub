package com.marketinghub.oprmcoletormei.catalog.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.oprmcoletormei.catalog.config.CnaeCatalogScheduleProperties;
import com.marketinghub.oprmcoletormei.catalog.dto.CnaeCatalogCollectRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CnaeCatalogCollectorScheduler {

    private static final Logger log = LoggerFactory.getLogger(CnaeCatalogCollectorScheduler.class);

    private final CnaeCatalogScheduleProperties scheduleProperties;
    private final CnaeCatalogCollectorService collectorService;
    private final ObjectMapper objectMapper;

    public CnaeCatalogCollectorScheduler(
            CnaeCatalogScheduleProperties scheduleProperties,
            CnaeCatalogCollectorService collectorService,
            ObjectMapper objectMapper
    ) {
        this.scheduleProperties = scheduleProperties;
        this.collectorService = collectorService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(cron = "${oprm.collector.schedule.cron}", zone = "${oprm.collector.schedule.timezone}")
    public void runScheduledIngestion() {
        if (!scheduleProperties.enabled()) {
            return;
        }
        if (scheduleProperties.payloadFile() == null || scheduleProperties.payloadFile().isBlank()) {
            log.warn("Agendamento habilitado, mas oprm.collector.schedule.payload-file não foi definido.");
            return;
        }

        try {
            List<CnaeCatalogCollectRequest.RawRecord> records = objectMapper.readValue(
                    Files.readString(Path.of(scheduleProperties.payloadFile())),
                    new TypeReference<>() {
                    }
            );
            CnaeCatalogCollectRequest request = new CnaeCatalogCollectRequest(scheduleProperties.source(), records);
            var result = collectorService.collectAndIngest(request);
            log.info("Ingestão agendada executada com sucesso. Recebidos: {}, normalizados: {}, persistidos: {}",
                    result.received(), result.normalized(), result.persisted());
        } catch (IOException e) {
            log.error("Falha ao carregar payload do arquivo configurado para ingestão agendada.", e);
        } catch (RuntimeException e) {
            log.error("Falha ao executar ingestão agendada.", e);
        }
    }
}
