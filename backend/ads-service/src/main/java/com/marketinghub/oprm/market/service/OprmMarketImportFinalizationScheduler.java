package com.marketinghub.oprm.market.service;

import com.marketinghub.oprm.market.OprmCnpjImportRun;
import com.marketinghub.oprm.market.dto.OprmCompleteImportRunRequestDto;
import com.marketinghub.oprm.market.repository.OprmCnpjImportRunRepository;
import java.time.Instant;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OprmMarketImportFinalizationScheduler {
    private static final Logger log = LoggerFactory.getLogger(OprmMarketImportFinalizationScheduler.class);

    @Value("${oprm.market-import.totalization.schedule.timezone:America/Sao_Paulo}")
    private String scheduleTimezone;

    private final OprmCnpjImportRunRepository runRepository;
    private final OprmMarketImportService importService;

    @Scheduled(cron = "${oprm.market-import.totalization.schedule.cron:0 40 19 * * *}", zone = "${oprm.market-import.totalization.schedule.timezone:America/Sao_Paulo}")
    public void finalizeLatestStartedRunAt1940() {
        Instant executionAt = Instant.now();
        ZoneId zoneId = ZoneId.of(scheduleTimezone);
        log.info("[OPRM-TOTALIZACAO] Scheduler iniciado. executionAtUtc={}, executionAtZone={}, timezone={}",
                executionAt,
                executionAt.atZone(zoneId),
                scheduleTimezone);

        OprmCnpjImportRun run = runRepository.findFirstByStatusOrderByStartedAtDesc("STARTED").orElse(null);
        if (run == null) {
            log.info("[OPRM-TOTALIZACAO] Nenhum run STARTED encontrado para totalização em executionAtUtc={}", executionAt);
            return;
        }

        log.info("[OPRM-TOTALIZACAO] Run candidato encontrado. runId={}, snapshotDate={}, startedAt={}, status={}",
                run.getId(),
                run.getSnapshotDate(),
                run.getStartedAt(),
                run.getStatus());

        log.info("[OPRM-TOTALIZACAO] Iniciando completeRun para runId={} (agendamento 19:40).", run.getId());
        importService.completeRun(run.getId(), new OprmCompleteImportRunRequestDto(
                null,
                executionAt,
                null,
                null,
                null,
                null,
                "Finalização automática da etapa de totalização (19:40 America/Sao_Paulo)"));
        log.info("[OPRM-TOTALIZACAO] completeRun concluído com sucesso para runId={}", run.getId());
    }
}
