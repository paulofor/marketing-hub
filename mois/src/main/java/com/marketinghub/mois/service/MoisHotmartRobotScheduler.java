package com.marketinghub.mois.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MoisHotmartRobotScheduler {

    private static final Logger log = LoggerFactory.getLogger(MoisHotmartRobotScheduler.class);

    private final MoisHotmartRobotProperties properties;
    private final MoisHotmartRobotService hotmartRobotService;

    public MoisHotmartRobotScheduler(MoisHotmartRobotProperties properties,
                                     MoisHotmartRobotService hotmartRobotService) {
        this.properties = properties;
        this.hotmartRobotService = hotmartRobotService;
    }

    @Scheduled(cron = "${mois.robot.hotmart.cron:0 */15 * * * *}")
    public void run() {
        log.info("MOIS Hotmart scheduler heartbeat: agendamento ativo (enabled={}, cron={})",
                properties.isEnabled(),
                properties.getCron());

        if (!properties.isEnabled()) {
            log.info("MOIS Hotmart scheduler ignorado: robô desabilitado na configuração");
            return;
        }

        log.info("MOIS Hotmart scheduler iniciando execução automática do robô");
        hotmartRobotService.triggerScheduledRun();
    }
}
