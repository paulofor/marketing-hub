package com.marketinghub.mois.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MoisHotmartRobotScheduler {

    private static final Logger log = LoggerFactory.getLogger(MoisHotmartRobotScheduler.class);

    private final MoisHotmartRobotProperties properties;
    public MoisHotmartRobotScheduler(MoisHotmartRobotProperties properties) {
        this.properties = properties;
    }

    @Scheduled(cron = "${mois.robot.hotmart.cron:0 0 * * * *}")
    public void run() {
        log.info("MOIS Hotmart scheduler heartbeat: agendamento ativo (enabled={}, cron={})",
                properties.isEnabled(),
                properties.getCron());
    }
}
