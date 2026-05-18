package com.marketinghub.oprm.market.service;

import com.marketinghub.oprm.market.OprmCnpjImportRun;
import com.marketinghub.oprm.market.dto.OprmCompleteImportRunRequestDto;
import com.marketinghub.oprm.market.repository.OprmCnpjImportRunRepository;
import java.time.Instant;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OprmMarketImportFinalizationScheduler {
    private static final Logger log = LoggerFactory.getLogger(OprmMarketImportFinalizationScheduler.class);
    private static final String SCHEDULE_TIMEZONE = "America/Sao_Paulo";

    private final OprmCnpjImportRunRepository runRepository;
    private final OprmMarketImportService importService;

    @Scheduled(cron = "0 40 19 * * *", zone = SCHEDULE_TIMEZONE)
    public void finalizeLatestStartedRunAt1940() {
        finalizeLatestStartedRun("19:40");
    }

    @Scheduled(cron = "0 30 23 * * *", zone = SCHEDULE_TIMEZONE)
    public void finalizeLatestStartedRunAt2330() {
        finalizeLatestStartedRun("23:30");
    }

    private void finalizeLatestStartedRun(String scheduleLabel) {
        Instant executionAt = Instant.now();
        ZoneId zoneId = ZoneId.of(SCHEDULE_TIMEZONE);
        log.info("[OPRM-TOTALIZACAO] Scheduler iniciado. executionAtUtc={}, executionAtZone={}, timezone={}",
                executionAt,
                executionAt.atZone(zoneId),
                SCHEDULE_TIMEZONE);

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

        log.info("[OPRM-TOTALIZACAO] Iniciando completeRun para runId={} (agendamento {}).", run.getId(), scheduleLabel);
        importService.completeRun(run.getId(), new OprmCompleteImportRunRequestDto(
                null,
                executionAt,
                null,
                null,
                null,
                null,
                "Finalização automática da etapa de totalização (" + scheduleLabel + " America/Sao_Paulo)"));
        log.info("[OPRM-TOTALIZACAO] completeRun concluído com sucesso para runId={}", run.getId());
    }
}
