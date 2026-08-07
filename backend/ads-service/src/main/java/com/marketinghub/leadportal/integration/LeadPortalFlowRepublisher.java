package com.marketinghub.leadportal.integration;

import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.service.LeadPortalFlowService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Responsabilidade: republicar no portal público os fluxos aprovados após o backend iniciar. */
@Component
public class LeadPortalFlowRepublisher {

  private static final Logger log = LoggerFactory.getLogger(LeadPortalFlowRepublisher.class);

  private final LeadPortalFlowService flowService;
  private final LeadPortalFlowPublisher publisher;
  private final LeadPortalIntegrationProperties properties;

  /**
   * Configura o republicador com a leitura canônica, o publicador e as propriedades da integração.
   */
  public LeadPortalFlowRepublisher(
      LeadPortalFlowService flowService,
      LeadPortalFlowPublisher publisher,
      LeadPortalIntegrationProperties properties) {
    this.flowService = flowService;
    this.publisher = publisher;
    this.properties = properties;
  }

  /** Republica os fluxos aprovados quando a aplicação fica pronta. */
  @EventListener(ApplicationReadyEvent.class)
  public void republishApprovedFlowsOnStartup() {
    republishApprovedFlows();
  }

  /** Carrega os contratos completos e tenta republicar cada fluxo aprovado de forma isolada. */
  void republishApprovedFlows() {
    if (!properties.isEnabled()) {
      log.debug("Lead portal integration disabled; skipping republish step");
      return;
    }

    List<LeadPortalFlow> approvedFlows = flowService.listApproved();
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
