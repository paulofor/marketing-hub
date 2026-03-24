package com.marketinghub.videomanagement.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Camada responsável por integrar com providers externos.
 * Nesta sprint o processamento é apenas um placeholder observável.
 */
@Component
@Slf4j
public class VideoJobProcessor {

    public void process(VideoJobSummary job) {
        if (job == null) {
            return;
        }
        log.info("[video-management] job pendente recebido id={} tipo={} provider={}",
                job.id(), job.jobType(), job.providerName());
        // Futuramente aqui chamaremos o provider real e reportaremos progresso ao backend.
    }
}
