package com.marketinghub.oprm.market.service;

import com.marketinghub.oprm.market.OprmCnpjImportRun;
import com.marketinghub.oprm.market.dto.OprmCompleteImportRunRequestDto;
import com.marketinghub.oprm.market.repository.OprmCnpjImportRunRepository;
import java.time.Instant;
import java.time.LocalDate;
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
    private static final String TARGET_DATE = "2026-05-17";
    private static final String TARGET_TIMEZONE = "America/Sao_Paulo";

    private final OprmCnpjImportRunRepository runRepository;
    private final OprmMarketImportService importService;

    @Scheduled(cron = "0 30 18 17 5 *", zone = TARGET_TIMEZONE)
    public void finalizeLatestStartedRunForToday1830() {
        LocalDate todayInZone = LocalDate.now(ZoneId.of(TARGET_TIMEZONE));
        if (!LocalDate.parse(TARGET_DATE).equals(todayInZone)) {
            log.info("Finalização agendada ignorada. Hoje em {} é {}, alvo={}.",
                    TARGET_TIMEZONE, todayInZone, TARGET_DATE);
            return;
        }

        OprmCnpjImportRun run = runRepository.findFirstByStatusOrderByStartedAtDesc("STARTED").orElse(null);
        if (run == null) {
            log.info("Finalização agendada: nenhum run STARTED encontrado.");
            return;
        }

        log.info("Finalização agendada: iniciando fechamento do runId={} às 18:30 ({})", run.getId(), TARGET_TIMEZONE);
        importService.completeRun(run.getId(), new OprmCompleteImportRunRequestDto(
                null, Instant.now(), null, null, null, null, "Finalização manual agendada para 18:30"));
        log.info("Finalização agendada: fechamento concluído para runId={}", run.getId());
    }
}
