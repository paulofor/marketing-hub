package com.marketinghub.oprm.nichocnae.routineresearchcycle.service;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Agenda a proteção que impede ciclos OPRM NichoCNAE parados de aparecerem como saudáveis. */
@Component
public class BackendRoutineResearchCycleStallGuardScheduler {
  private static final Logger LOGGER = LoggerFactory.getLogger(BackendRoutineResearchCycleStallGuardScheduler.class);

  private final BackendRoutineResearchCycleStallGuardService stallGuardService;

  /** Inicializa o scheduler com o serviço de proteção dos ciclos de pesquisa de rotina. */
  public BackendRoutineResearchCycleStallGuardScheduler(
      BackendRoutineResearchCycleStallGuardService stallGuardService) {
    this.stallGuardService = stallGuardService;
  }

  /** Executa a varredura periódica de ciclos RUNNING sem progresso operacional. */
  @Scheduled(cron = "0 */10 * * * *")
  public void markStalledCycles() {
    int stalledCount = stallGuardService.markRunningCyclesWithoutProgressAsStalled(Instant.now());
    if (stalledCount > 0) {
      LOGGER.warn("Varredura de ciclos parados OPRM NichoCNAE concluída (stalledCount={})", stalledCount);
    }
  }
}
