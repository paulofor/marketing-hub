package com.marketinghub.nichocnae.meiaudiencesegmenter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Dispara periodicamente a segmentação comportamental MEI/autônomo após a síntese de rotina. */
@Component
public class MeiAudienceSegmenterScheduler {
    private static final Logger log = LoggerFactory.getLogger(MeiAudienceSegmenterScheduler.class);
    private final MeiAudienceSegmenterService service;

    /** Inicializa o scheduler com o serviço operacional da segmentação MEI/autônomo. */
    public MeiAudienceSegmenterScheduler(MeiAudienceSegmenterService service) {
        this.service = service;
    }

    /** Executa a segmentação em intervalo fixo para manter o pipeline avançando automaticamente. */
    @Scheduled(cron = "0 */10 * * * *")
    public void runScheduled() {
        try {
            int processed = service.processPending("scheduler").size();
            if (processed > 0) {
                log.info("Segmentação MEI/autônomo OPRM nichocnae executada pelo scheduler (processed={})", processed);
            }
        } catch (RuntimeException ex) {
            log.error("Erro no scheduler da segmentação MEI/autônomo OPRM nichocnae", ex);
            throw ex;
        }
    }
}
