package com.marketinghub.leadportal.integration;

import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.repository.LeadPortalFlowRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Ensures previously approved flows are synchronised with the public lead portal application.
 */
@Component
public class LeadPortalFlowRepublisher {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalFlowRepublisher.class);

    private final LeadPortalFlowRepository repository;
    private final LeadPortalFlowPublisher publisher;
    private final LeadPortalIntegrationProperties properties;

    public LeadPortalFlowRepublisher(
            LeadPortalFlowRepository repository,
            LeadPortalFlowPublisher publisher,
            LeadPortalIntegrationProperties properties) {
        this.repository = repository;
        this.publisher = publisher;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void republishApprovedFlowsOnStartup() {
        republishApprovedFlows();
    }

    void republishApprovedFlows() {
        if (!properties.isEnabled()) {
            log.debug("Lead portal integration disabled; skipping republish step");
            return;
        }

        List<LeadPortalFlow> approvedFlows = repository.findAllByApprovedTrue();
        if (approvedFlows.isEmpty()) {
            log.debug("No approved lead portal flows to republish");
            return;
        }

        log.info("Republishing {} approved lead portal flows to public portal", approvedFlows.size());
        for (LeadPortalFlow flow : approvedFlows) {
            try {
                publisher.publish(flow);
            } catch (LeadPortalPublicationException ex) {
                log.warn("Failed to republish lead portal flow {}", flow.getSlug(), ex);
            }
        }
    }
}
