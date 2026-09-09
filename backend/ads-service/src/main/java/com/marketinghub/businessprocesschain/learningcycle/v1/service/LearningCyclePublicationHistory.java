package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.LearningCycleHistoricalPublication;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.run.ExperimentRunMode;
import com.marketinghub.repository.jpa.experiment.ExperimentRunRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Responsabilidade: consultar provas persistidas de publicação anterior sem alterar a operação. */
@Component
@RequiredArgsConstructor
public class LearningCyclePublicationHistory {
  private final ExperimentRunRepository runs;
  private final FacebookAdsCampaignRepository campaigns;

  /** Reconhece runs ou contratos de recibo legado, sem acessar a entidade interna de campanhas. */
  public Optional<LearningCycleHistoricalPublication> find(Experiment experiment) {
    var published =
        runs.findTopByExperimentIdAndModeAndPublishedAtIsNotNullOrderByRunNumberDesc(
                experiment.getId(), ExperimentRunMode.PRODUCTION)
            .map(
                run ->
                    new LearningCycleHistoricalPublication(
                        "PRODUCTION_RUN",
                        experiment.getId(),
                        "experiment_run:" + run.getId(),
                        run.getPublishedAt(),
                        run.getPreflightCompletedAt() != null,
                        run.getPreflightCompletedAt() != null
                            ? "Referência histórica com publicação e preflight registrados. Iniciar pela conciliação; o sucessor exige homologação e autorização próprias."
                            : "Publicação histórica comprovada, sem preflight registrado. Preservar a lacuna e iniciar pela conciliação; o sucessor exige homologação e autorização próprias."));
    if (published.isPresent() || experiment.getPlatform() != ExperimentPlatform.FACEBOOK) {
      return published;
    }
    return campaigns.findHistoricalPublicationReceipts(experiment.getId()).stream()
        .findFirst()
        .map(
            campaign ->
                new LearningCycleHistoricalPublication(
                    "LEGACY_META_CAMPAIGN",
                    experiment.getId(),
                    "facebook_ads_campaign:" + campaign.campaignId(),
                    campaign.recordedAt(),
                    false,
                    "Campanha Meta histórica comprovada por recibo externo, sem publicação registrada em run/preflight. Preservar a lacuna e iniciar pela conciliação; o sucessor exige homologação e autorização próprias."));
  }
}
