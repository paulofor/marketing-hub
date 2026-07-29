package com.marketinghub.socialmediaworker.youtube;

import com.marketinghub.socialmediaworker.dto.YoutubePublicationInput;
import com.marketinghub.socialmediaworker.dto.YoutubePublicationOutput;
import com.marketinghub.socialmediaworker.pipeline.PipelineWorker;
import com.marketinghub.socialmediaworker.pipeline.StageContext;
import com.marketinghub.socialmediaworker.pipeline.StageResult;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Controla a rotina operacional que consome a fila de publicacoes YouTube.
 */
@Component
public class YoutubePublicationScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(YoutubePublicationScheduler.class);
    private static final int BATCH_SIZE = 5;

    private final YoutubeBackendClient backendClient;
    private final PipelineWorker<YoutubePublicationInput, YoutubePublicationOutput> worker;

    /**
     * Monta o scheduler com backend e processor da etapa YouTube.
     */
    public YoutubePublicationScheduler(YoutubeBackendClient backendClient, YoutubePublicationProcessor processor) {
        this.backendClient = backendClient;
        this.worker = new PipelineWorker<>(processor);
    }

    /**
     * Executa a cada cinco minutos a busca e processamento de pendencias YouTube.
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void processPendingPublications() {
        List<YoutubePublicationInput> pending = backendClient.fetchPending(BATCH_SIZE);
        if (pending.isEmpty()) {
            LOGGER.debug("Nenhuma publicacao YouTube pendente.");
            return;
        }
        for (YoutubePublicationInput input : pending) {
            backendClient.markPublishing(input.publicationId());
            StageContext context = StageContext.of("youtube-publication-v1", input.publicationId(), "youtube-" + input.publicationId());
            StageResult<YoutubePublicationOutput> result = worker.run(context, input);
            backendClient.reportResult(input.publicationId(), result);
        }
    }
}
