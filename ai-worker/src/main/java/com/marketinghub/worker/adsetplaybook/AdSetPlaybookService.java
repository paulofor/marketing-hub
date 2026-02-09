package com.marketinghub.worker.adsetplaybook;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.worker.adsetplaybook.AdSetPlaybookClient.PlaybookWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Coordinates job processing inside the AI Worker.
 */
@Service
public class AdSetPlaybookService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdSetPlaybookService.class);

    private final AdSetPlaybookClient client;
    private final AdSetSeedPlanner seedPlanner;
    private final AdSetSpecBuilder specBuilder;
    private final String workerId;

    public AdSetPlaybookService(AdSetPlaybookClient client,
                                AdSetSeedPlanner seedPlanner,
                                AdSetSpecBuilder specBuilder,
                                @Value("${adset.playbook.worker-id:}") String configuredWorkerId) {
        this.client = client;
        this.seedPlanner = seedPlanner;
        this.specBuilder = specBuilder;
        this.workerId = configuredWorkerId != null && !configuredWorkerId.isBlank()
                ? configuredWorkerId
                : generateWorkerId();
    }

    public void processQueue() {
        List<PlaybookJob> jobs = client.claimJobs(PlaybookWorker.AI, 5, workerId);
        if (jobs.isEmpty()) {
            return;
        }
        LOGGER.info("Processando {} jobs do roteiro de ad sets", jobs.size());
        for (PlaybookJob job : jobs) {
            try {
                JsonNode result = switch (job.type()) {
                    case AI_PREPARE_SEED -> seedPlanner.plan(job.payload());
                    case AI_BUILD_SPECS -> specBuilder.build(job.payload());
                    default -> {
                        LOGGER.warn("Worker AI recebeu job não suportado {}", job.type());
                        yield null;
                    }
                };
                client.completeJob(job.id(), result);
            } catch (Exception ex) {
                LOGGER.error("Falha ao executar job {} do tipo {}", job.id(), job.type(), ex);
                client.failJob(job.id(), ex.getMessage());
            }
        }
    }

    private String generateWorkerId() {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            return "ai-playbook-" + host;
        } catch (UnknownHostException e) {
            return "ai-playbook";
        }
    }
}
