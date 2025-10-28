package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.config.ResultProcessingProperties;
import com.marketinghub.leadportal.model.Lead;
import com.marketinghub.leadportal.model.LeadStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ResultProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultProcessor.class);
    private final LeadService leadService;
    private final ResultProcessingProperties properties;
    private final Random random = new Random();

    public ResultProcessor(LeadService leadService, ResultProcessingProperties properties) {
        this.leadService = leadService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "PT2S")
    public void processPendingLeads() {
        Duration delay = properties.getDelay();
        Instant now = Instant.now();
        for (Lead lead : leadService.getAllLeads()) {
            if (lead.getStatus() == LeadStatus.PROCESSING
                    && lead.getCreatedAt().plus(delay).isBefore(now)) {
                String resultMessage = buildResultMessage(lead.getId());
                leadService.completeLead(lead.getId(), resultMessage);
                LOGGER.info("Lead {} processed with result {}", lead.getId(), resultMessage);
            }
        }
    }

    private String buildResultMessage(UUID id) {
        int score = 60 + random.nextInt(41);
        return "Processamento concluído para " + id + ". Score de qualidade: " + score;
    }
}
