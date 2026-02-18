package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.dto.ExperimentDiagnosticsDto;
import com.marketinghub.experiment.dto.ExperimentDiagnosticsSeverity;
import com.marketinghub.experiment.dto.ExperimentPublishingArtifactDto;
import com.marketinghub.facebookads.FacebookAdsAd;
import com.marketinghub.facebookads.FacebookAdsAdRepository;
import com.marketinghub.facebookads.FacebookAdsAdSet;
import com.marketinghub.facebookads.FacebookAdsAdSetRepository;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookAdsCampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExperimentDiagnosticsService {
    private final ExperimentService experimentService;
    private final FacebookAdsCampaignRepository campaignRepository;
    private final FacebookAdsAdSetRepository adSetRepository;
    private final FacebookAdsAdRepository adRepository;

    public ExperimentDiagnosticsService(ExperimentService experimentService,
                                        FacebookAdsCampaignRepository campaignRepository,
                                        FacebookAdsAdSetRepository adSetRepository,
                                        FacebookAdsAdRepository adRepository) {
        this.experimentService = experimentService;
        this.campaignRepository = campaignRepository;
        this.adSetRepository = adSetRepository;
        this.adRepository = adRepository;
    }

    @Transactional(readOnly = true)
    public ExperimentDiagnosticsDto diagnose(Long experimentId) {
        Experiment experiment = experimentService.get(experimentId);
        List<FacebookAdsCampaign> campaigns = campaignRepository.findByExperimentId(experimentId);
        List<String> campaignIds = campaigns.stream().map(FacebookAdsCampaign::getId).toList();
        List<FacebookAdsAdSet> adSets = campaignIds.isEmpty()
                ? List.of()
                : adSetRepository.findByCampaignIdIn(campaignIds);
        List<String> adSetIds = adSets.stream().map(FacebookAdsAdSet::getId).toList();
        List<FacebookAdsAd> ads = adSetIds.isEmpty()
                ? List.of()
                : adRepository.findByAdSetIdIn(adSetIds);

        long stuckCampaigns = campaigns.stream().filter(c -> !hasExternalId(c.getExternalId())).count();
        long stuckAdSets = adSets.stream().filter(s -> !hasExternalId(s.getExternalId())).count();
        long stuckAds = ads.stream().filter(a -> !hasExternalId(a.getExternalId())).count();
        List<ExperimentPublishingArtifactDto> artifacts = collectArtifacts(campaigns, adSets, ads);

        boolean hasPendingArtifacts = !artifacts.isEmpty();
        boolean statusFailed = experiment.getStatus() == ExperimentStatus.FAILED;

        ExperimentDiagnosticsSeverity severity;
        String headline;
        String description;
        String resolution;

        if (statusFailed && hasPendingArtifacts) {
            severity = ExperimentDiagnosticsSeverity.ERROR;
            headline = "Experimento falhou com pendências de publicação";
            description = buildFailedWithPendingDescription(stuckCampaigns, stuckAdSets, stuckAds);
            resolution = buildFailedResolution(campaigns, true);
        } else if (statusFailed) {
            severity = ExperimentDiagnosticsSeverity.WARNING;
            headline = "Experimento está marcado como FAILED";
            description = "Enquanto estiver como FAILED o worker não tentará criar campanhas.";
            resolution = buildFailedResolution(campaigns, false);
        } else if (hasPendingArtifacts) {
            severity = ExperimentDiagnosticsSeverity.WARNING;
            headline = "Há ativos criados localmente aguardando publicação";
            description = buildPendingDescription(stuckCampaigns, stuckAdSets, stuckAds);
            resolution = "Mantenha o status em Planejado e verifique os workers de Facebook para concluir a publicação.";
        } else {
            severity = ExperimentDiagnosticsSeverity.INFO;
            headline = "Nenhuma inconsistência detectada";
            description = buildHealthyDescription(experiment, campaigns);
            resolution = experiment.getStatus() == ExperimentStatus.PLANNED
                    ? "O worker deve selecionar este experimento assim que todos os pré-requisitos estiverem verdes."
                    : "Altere o status para Planejado quando quiser liberar uma nova tentativa.";
        }

        return new ExperimentDiagnosticsDto(severity, headline, description, resolution, artifacts);
    }

    private static boolean hasExternalId(String externalId) {
        return StringUtils.hasText(externalId);
    }

    private static String buildPendingDescription(long campaigns, long adSets, long ads) {
        return String.format(
                "%d campanha(s), %d conjunto(s) e %d anúncio(s) foram persistidos apenas no banco local e não receberam um ID do Meta.",
                campaigns,
                adSets,
                ads
        );
    }

    private static String buildFailedWithPendingDescription(long campaigns, long adSets, long ads) {
        return "%s O fluxo foi interrompido antes de concluir a publicação completa no Meta Ads; confira os detalhes em Chamadas Meta e no Playbook de Ad Sets para localizar o ponto de falha."
                .formatted(buildPendingDescription(campaigns, adSets, ads));
    }

    private static String buildHealthyDescription(Experiment experiment, List<FacebookAdsCampaign> campaigns) {
        String campaignSummary = campaigns.isEmpty()
                ? "Nenhuma campanha foi gerada ainda para este experimento."
                : campaigns.stream()
                        .map(c -> "%s (%s)".formatted(c.getName(),
                                hasExternalId(c.getExternalId()) ? "Meta ID " + c.getExternalId() : "sem ID externo"))
                        .collect(Collectors.joining(", "));
        return "Status atual: %s. %s".formatted(experiment.getStatus(), campaignSummary);
    }

    private static String buildFailedResolution(Collection<FacebookAdsCampaign> campaigns, boolean suggestAccountReview) {
        String accountInfo = campaigns.stream()
                .findFirst()
                .map(c -> {
                    String accountName = c.getFacebookAccount() != null ? c.getFacebookAccount().getName() : null;
                    if (!StringUtils.hasText(accountName)) {
                        return c.getAdAccountId();
                    }
                    return "%s (ad account %s)".formatted(accountName, c.getAdAccountId());
                })
                .orElse("a conta de anúncios configurada");
        if (suggestAccountReview) {
            return "Revise o acesso e as permissões da conta %s, corrija os bloqueios identificados nos logs e depois volte o status do experimento para Planejado para liberar uma nova tentativa.".formatted(accountInfo);
        }
        return "Volte o status para Planejado quando as pendências estiverem resolvidas para que o worker possa reenfileirar a publicação.";
    }

    private static List<ExperimentPublishingArtifactDto> collectArtifacts(List<FacebookAdsCampaign> campaigns,
                                                                          List<FacebookAdsAdSet> adSets,
                                                                          List<FacebookAdsAd> ads) {
        List<ExperimentPublishingArtifactDto> artifacts = new ArrayList<>();
        campaigns.stream()
                .filter(c -> !hasExternalId(c.getExternalId()))
                .forEach(c -> artifacts.add(new ExperimentPublishingArtifactDto(
                        "CAMPAIGN",
                        c.getId(),
                        c.getName(),
                        c.getStatus().name(),
                        c.getExternalId())));
        adSets.stream()
                .filter(a -> !hasExternalId(a.getExternalId()))
                .forEach(a -> artifacts.add(new ExperimentPublishingArtifactDto(
                        "AD_SET",
                        a.getId(),
                        a.getName(),
                        a.getStatus().name(),
                        a.getExternalId())));
        ads.stream()
                .filter(a -> !hasExternalId(a.getExternalId()))
                .forEach(a -> artifacts.add(new ExperimentPublishingArtifactDto(
                        "AD",
                        a.getId(),
                        a.getName(),
                        a.getStatus().name(),
                        a.getExternalId())));
        return artifacts;
    }
}
