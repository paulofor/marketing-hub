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

    @Scheduled(cron = "${mois.robot.hotmart.cron:0 0 14 * * *}")
    public void run() {
        log.info("MOIS Hotmart scheduler acionado (enabled={}, cron={})",
                properties.isEnabled(),
                properties.getCron());
        if (!properties.isEnabled()) {
            log.warn("MOIS Hotmart scheduler ignorado: robot desabilitado (cron={})", properties.getCron());
            return;
        }
        long startedAt = System.currentTimeMillis();
        log.info("MOIS Hotmart scheduler iniciado (cron={}, workspaceId={}, niche={}, marketTheme={})",
                properties.getCron(),
                properties.getWorkspaceId(),
                properties.getNiche(),
                properties.getMarketTheme());
        try {
            service.triggerScheduledRun();
            log.info("MOIS Hotmart scheduler finalizado com sucesso (elapsedMs={})",
                    System.currentTimeMillis() - startedAt);
        } catch (RuntimeException ex) {
            log.error("Falha no robô diário do Hotmart no MOIS.", ex);
        }
    }
}
