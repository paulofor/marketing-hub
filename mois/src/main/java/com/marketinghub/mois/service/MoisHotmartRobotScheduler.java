package com.marketinghub.mois.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MoisHotmartRobotScheduler {

    private static final Logger log = LoggerFactory.getLogger(MoisHotmartRobotScheduler.class);

    private final MoisHotmartRobotProperties properties;
    private final MoisHotmartRobotService service;

    public MoisHotmartRobotScheduler(MoisHotmartRobotProperties properties, MoisHotmartRobotService service) {
        this.properties = properties;
        this.service = service;
    }

    @Scheduled(cron = "${mois.robot.hotmart.cron:0 10 3 * * *}")
    public void run() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            service.triggerScheduledRun();
        } catch (RuntimeException ex) {
            log.error("Falha no robô diário do Hotmart no MOIS.", ex);
        }
    }
}
