package com.marketinghub.experiment.funnel;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.facebookads.FacebookAdStatus;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookCampaignStopReason;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável por colocar experimentos em standby e solicitar pausa de campanhas
 * vinculadas.
 */
@Service
public class ExperimentFunnelStandbyService {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ExperimentFunnelStandbyService.class);

  private final FacebookAdsCampaignRepository campaignRepository;

  /** Cria o serviço com acesso às campanhas Meta vinculadas ao experimento. */
  public ExperimentFunnelStandbyService(FacebookAdsCampaignRepository campaignRepository) {
    this.campaignRepository = campaignRepository;
  }

  /**
   * Coloca o experimento em standby no primeiro envio válido e solicita pausa das campanhas Meta
   * vinculadas.
   *
   * @return {@code true} quando o standby foi aplicado, {@code false} caso o experimento não esteja
   *     elegível.
   */
  public boolean standbyOnFirstValidFormSubmission(Experiment experiment) {
    if (experiment == null || experiment.getStatus() != ExperimentStatus.RUNNING) {
      return false;
    }
    LOGGER.info(
        "Standby triggered for experiment {} after first valid form submission.",
        experiment.getId());
    experiment.setStatus(ExperimentStatus.STANDBY);
    requestFacebookCampaignStops(
        experiment.getId(),
        FacebookCampaignStopReason.FIRST_FORM_SUBMISSION_STANDBY,
        "primeiro envio válido de formulário no regime inicial de validação");
    return true;
  }

  /**
   * Registra a causa comercial em todas as campanhas e reabre a solicitação somente quando ainda
   * houver parada efetiva a executar na Meta.
   */
  public void requestFacebookCampaignStops(
      Long experimentId, FacebookCampaignStopReason stopReason, String businessReason) {
    List<FacebookAdsCampaign> campaigns = campaignRepository.findByExperimentId(experimentId);
    if (campaigns == null || campaigns.isEmpty()) {
      LOGGER.info(
          "Experiment {} has no Facebook campaigns registered; status updated but no stop request was necessary.",
          experimentId);
      return;
    }
    Instant now = Instant.now();
    campaigns.forEach(
        campaign -> {
          boolean requiresStop = requiresStopRequest(campaign);
          campaign.setStopReason(stopReason);
          campaign.setStopLastError(null);
          if (requiresStop) {
            campaign.setStopRequestedAt(now);
            campaign.setStopCompletedAt(null);
          }
          LOGGER.info(
              "Stop cause reconciled for campaign {} from experiment {}: reason={}, requiresStop={}, businessReason={}",
              campaign.getId(),
              experimentId,
              stopReason,
              requiresStop,
              businessReason);
        });
    campaignRepository.saveAll(campaigns);
  }

  /** Informa se a campanha ainda precisa de parada efetiva na Meta. */
  private boolean requiresStopRequest(FacebookAdsCampaign campaign) {
    return campaign.getStopCompletedAt() == null || campaign.getStatus() == FacebookAdStatus.ACTIVE;
  }
}
