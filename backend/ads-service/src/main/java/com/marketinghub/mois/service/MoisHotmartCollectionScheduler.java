package com.marketinghub.mois.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MoisHotmartCollectionScheduler {

    @Scheduled(cron = "0 */20 * * * *")
    public void scheduleCollection() {
        log.info("MOIS scheduler heartbeat: execução simples a cada 20 minutos.");
    }
}
