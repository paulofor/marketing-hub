package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.dto.ExperimentDiagnosticsDto;
import com.marketinghub.experiment.dto.ExperimentDiagnosticsSeverity;
import com.marketinghub.experiment.dto.ExperimentFailureDetailsDto;
import com.marketinghub.experiment.dto.ExperimentPublishingArtifactDto;
import com.marketinghub.facebookads.playbook.dto.ExperimentFacebookApiLogDto;
import com.marketinghub.facebookads.playbook.service.ExperimentFacebookApiLogService;
import com.marketinghub.facebookads.FacebookAdsAd;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsAdRepository;
import com.marketinghub.facebookads.FacebookAdsAdSet;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsAdSetRepository;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
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
    private final ExperimentFacebookApiLogService facebookApiLogService;

    public ExperimentDiagnosticsService(ExperimentService experimentService,
                                        FacebookAdsCampaignRepository campaignRepository,
                                        FacebookAdsAdSetRepository adSetRepository,
                                        FacebookAdsAdRepository adRepository,
                                        ExperimentFacebookApiLogService facebookApiLogService) {
        this.experimentService = experimentService;
        this.campaignRepository = campaignRepository;
        this.adSetRepository = adSetRepository;
        this.adRepository = adRepository;
        this.facebookApiLogService = facebookApiLogService;
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

        long stuckCampaigns = campaigns.stream().filter(c -> !hasMetaId(c.getExternalId(), c.getId())).count();
        long stuckAdSets = adSets.stream().filter(s -> !hasMetaId(s.getExternalId(), s.getId())).count();
        long stuckAds = ads.stream().filter(a -> !hasMetaId(a.getExternalId(), a.getId())).count();
        List<ExperimentPublishingArtifactDto> artifacts = collectArtifacts(campaigns, adSets, ads);

        boolean hasPendingArtifacts = !artifacts.isEmpty();
        boolean statusFailed = experiment.getStatus() == ExperimentStatus.FAILED;
        ExperimentFailureDetailsDto failureDetails = resolveFailureDetails(experimentId, statusFailed);

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

        return new ExperimentDiagnosticsDto(severity, headline, description, resolution, artifacts, failureDetails);
    }

    private ExperimentFailureDetailsDto resolveFailureDetails(Long experimentId, boolean statusFailed) {
        if (!statusFailed) {
            return null;
        }
        return facebookApiLogService.findLogs(experimentId, 200).stream()
                .filter(this::isFailureLog)
                .findFirst()
                .map(log -> new ExperimentFailureDetailsDto(
                        log.errorMessage(),
                        log.endpoint(),
                        log.statusCode(),
                        resolveOccurrence(log),
                        resolveFailureSource(log)
                ))
                .orElse(null);
    }

    private boolean isFailureLog(ExperimentFacebookApiLogDto log) {
        if (log == null) {
            return false;
        }
        if (StringUtils.hasText(log.errorMessage())) {
            return true;
        }
        return log.statusCode() != null && log.statusCode() >= 400;
    }

    private Instant resolveOccurrence(ExperimentFacebookApiLogDto log) {
        if (log.respondedAt() != null) {
            return log.respondedAt();
        }
        if (log.requestedAt() != null) {
            return log.requestedAt();
        }
        return log.createdAt();
    }

    private String resolveFailureSource(ExperimentFacebookApiLogDto log) {
        if (log.jobWorker() != null) {
            return log.jobWorker().name();
        }
        if (StringUtils.hasText(log.provider())) {
            return log.provider();
        }
        return "FACEBOOK_API_LOG";
    }

    private static boolean hasMetaId(String externalId, String internalId) {
        return StringUtils.hasText(resolveMetaId(externalId, internalId));
    }

    private static String resolveMetaId(String externalId, String internalId) {
        if (StringUtils.hasText(externalId)) {
            return externalId;
        }
        if (StringUtils.hasText(internalId) && !isUuid(internalId)) {
            return internalId;
        }
        return null;
    }

    private static boolean isUuid(String value) {
        try {
            java.util.UUID.fromString(value.trim());
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
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
                                hasMetaId(c.getExternalId(), c.getId())
                                        ? "Meta ID " + resolveMetaId(c.getExternalId(), c.getId())
                                        : "sem ID externo"))
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
                .filter(c -> !hasMetaId(c.getExternalId(), c.getId()))
                .forEach(c -> artifacts.add(new ExperimentPublishingArtifactDto(
                        "CAMPAIGN",
                        c.getId(),
                        c.getName(),
                        c.getStatus().name(),
                        resolveMetaId(c.getExternalId(), c.getId()))));
        adSets.stream()
                .filter(a -> !hasMetaId(a.getExternalId(), a.getId()))
                .forEach(a -> artifacts.add(new ExperimentPublishingArtifactDto(
                        "AD_SET",
                        a.getId(),
                        a.getName(),
                        a.getStatus().name(),
                        resolveMetaId(a.getExternalId(), a.getId()))));
        ads.stream()
                .filter(a -> !hasMetaId(a.getExternalId(), a.getId()))
                .forEach(a -> artifacts.add(new ExperimentPublishingArtifactDto(
                        "AD",
                        a.getId(),
                        a.getName(),
                        a.getStatus().name(),
                        resolveMetaId(a.getExternalId(), a.getId()))));
        return artifacts;
    }
}
