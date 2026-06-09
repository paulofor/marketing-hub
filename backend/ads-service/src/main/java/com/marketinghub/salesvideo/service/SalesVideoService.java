package com.marketinghub.salesvideo.service;

import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetType;
import com.marketinghub.media.MediaProvider;
import com.marketinghub.salesvideo.SalesVideoJobType;
import com.marketinghub.salesvideo.SalesVideoProviderFamily;
import com.marketinghub.salesvideo.SalesVideoStatus;
import com.marketinghub.salesvideo.dto.ApproveSalesVideoScriptRequest;
import com.marketinghub.salesvideo.dto.CreateLandingVideoSlotRequest;
import com.marketinghub.salesvideo.dto.CreateSalesVideoCommercialPlaybookRequest;
import com.marketinghub.salesvideo.dto.CreateSalesVideoConversionEventRequest;
import com.marketinghub.salesvideo.dto.CreateSalesVideoProfileRequest;
import com.marketinghub.salesvideo.dto.GenerateSalesVideoScriptRequest;
import com.marketinghub.salesvideo.dto.JobClaimRequest;
import com.marketinghub.salesvideo.dto.JobCompletionRequest;
import com.marketinghub.salesvideo.dto.JobExpirationRequest;
import com.marketinghub.salesvideo.dto.JobFailureRequest;
import com.marketinghub.salesvideo.dto.JobHeartbeatRequest;
import com.marketinghub.salesvideo.dto.JobProgressRequest;
import com.marketinghub.salesvideo.dto.LandingVideoSlotDto;
import com.marketinghub.salesvideo.dto.LandingVideoSlotHistoryDto;
import com.marketinghub.salesvideo.dto.RequestVideoRenderRequest;
import com.marketinghub.salesvideo.dto.RetrySalesVideoJobRequest;
import com.marketinghub.salesvideo.dto.SalesVideoCommercialPlaybookDto;
import com.marketinghub.salesvideo.dto.SalesVideoConversionEventDto;
import com.marketinghub.salesvideo.dto.SalesVideoJobDto;
import com.marketinghub.salesvideo.dto.SalesVideoJobEventDto;
import com.marketinghub.salesvideo.dto.SalesVideoPerformanceSummaryDto;
import com.marketinghub.salesvideo.dto.SalesVideoProfileDto;
import com.marketinghub.salesvideo.dto.SalesVideoRolloutStatusDto;
import com.marketinghub.salesvideo.dto.SalesVideoScriptDto;
import com.marketinghub.salesvideo.dto.UpdateLandingVideoSlotRequest;
import com.marketinghub.salesvideo.dto.UpdateSalesVideoComplianceRequest;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Fachada única do módulo Avatar Sales Video no backend.
 */
@Service
public class SalesVideoService {
    private final SalesVideoProfileService profileService;
    private final SalesVideoJobService jobService;
    private final LandingVideoSlotService slotService;
    private final SalesVideoAssetService assetService;
    private final SalesVideoCommercialInsightsService commercialInsightsService;

    /** Inicializa a fachada com os componentes internos do módulo de vídeo. */
    public SalesVideoService(SalesVideoProfileService profileService,
                             SalesVideoJobService jobService,
                             LandingVideoSlotService slotService,
                             SalesVideoAssetService assetService,
                             SalesVideoCommercialInsightsService commercialInsightsService) {
        this.profileService = profileService;
        this.jobService = jobService;
        this.slotService = slotService;
        this.assetService = assetService;
        this.commercialInsightsService = commercialInsightsService;
    }

    /** Cria um perfil de vídeo para o produto informado. */
    public SalesVideoProfileDto createProfile(Long productId, CreateSalesVideoProfileRequest request) {
        return profileService.createProfile(productId, request);
    }

    /** Lista os perfis de vídeo de um produto. */
    public List<SalesVideoProfileDto> listProfiles(Long productId) {
        return profileService.listProfiles(productId);
    }

    /** Consulta um perfil de vídeo pelo identificador. */
    public SalesVideoProfileDto getProfile(Long profileId) {
        return profileService.getProfile(profileId);
    }

    /** Lista as versões de script do perfil. */
    public List<SalesVideoScriptDto> listScripts(Long profileId) {
        return profileService.listScripts(profileId);
    }

    /** Solicita a geração automática de script. */
    public SalesVideoJobDto requestScriptGeneration(Long profileId, GenerateSalesVideoScriptRequest request) {
        return profileService.requestScriptGeneration(profileId, request);
    }

    /** Aprova manualmente uma versão de script. */
    public SalesVideoScriptDto approveScript(Long profileId, ApproveSalesVideoScriptRequest request) {
        return profileService.approveScript(profileId, request);
    }

    /** Solicita a renderização do vídeo aprovado. */
    public SalesVideoJobDto requestRender(Long profileId, RequestVideoRenderRequest request) {
        return profileService.requestRender(profileId, request);
    }

    /** Atualiza o checklist de compliance do perfil. */
    public SalesVideoProfileDto updateCompliance(Long profileId, UpdateSalesVideoComplianceRequest request) {
        return profileService.updateCompliance(profileId, request);
    }

    /** Consulta o rollout de vídeo do tenant atual. */
    public SalesVideoRolloutStatusDto getTenantRolloutStatus() {
        return profileService.getTenantRolloutStatus();
    }

    /** Consulta o rollout de vídeo de um perfil. */
    public SalesVideoRolloutStatusDto getProfileRolloutStatus(Long profileId) {
        return profileService.getRolloutStatus(profileId);
    }

    /** Lista jobs filtrados por provider, status e tipo. */
    public List<SalesVideoJobDto> findJobs(SalesVideoProviderFamily providerFamily,
                                           SalesVideoStatus status,
                                           SalesVideoJobType jobType,
                                           int limit) {
        return jobService.findJobs(providerFamily, status, jobType, limit);
    }

    /** Lista os jobs de um perfil. */
    public List<SalesVideoJobDto> listJobsByProfile(Long profileId) {
        return jobService.listJobsByProfile(profileId);
    }

    /** Consulta um job pelo identificador. */
    public SalesVideoJobDto getJob(Long jobId) {
        return jobService.getJob(jobId);
    }

    /** Lista a timeline de eventos de um job. */
    public List<SalesVideoJobEventDto> getJobEvents(Long jobId) {
        return jobService.getJobEvents(jobId);
    }

    /** Reprocessa um job elegível. */
    public SalesVideoJobDto retry(Long jobId, RetrySalesVideoJobRequest request) {
        return jobService.retry(jobId, request);
    }

    /** Faz claim operacional de um job por um worker. */
    public SalesVideoJobDto claimJob(Long jobId, JobClaimRequest request) {
        return jobService.claimJob(jobId, request);
    }

    /** Registra heartbeat operacional de um job. */
    public SalesVideoJobDto heartbeat(Long jobId, JobHeartbeatRequest request) {
        return jobService.heartbeat(jobId, request);
    }

    /** Registra progresso operacional de um job. */
    public SalesVideoJobDto progress(Long jobId, JobProgressRequest request) {
        return jobService.progress(jobId, request);
    }

    /** Conclui um job com sucesso. */
    public SalesVideoJobDto complete(Long jobId, JobCompletionRequest request) {
        return jobService.complete(jobId, request);
    }

    /** Marca um job como falho. */
    public SalesVideoJobDto fail(Long jobId, JobFailureRequest request) {
        return jobService.fail(jobId, request);
    }

    /** Marca um job expirado como falho. */
    public SalesVideoJobDto expire(Long jobId, JobExpirationRequest request) {
        return jobService.expire(jobId, request);
    }

    /** Armazena um asset final ou auxiliar de vídeo. */
    public Asset storeAsset(MultipartFile file, AssetType assetType, MediaProvider provider, String metadata)
            throws IOException {
        return assetService.store(file, assetType, provider, metadata);
    }

    /** Lista slots de vídeo de uma landing page. */
    public List<LandingVideoSlotDto> listSlots(Long landingId) {
        return slotService.list(landingId);
    }

    /** Cria um slot de vídeo na landing page. */
    public LandingVideoSlotDto createSlot(Long landingId, CreateLandingVideoSlotRequest request) {
        return slotService.create(landingId, request);
    }

    /** Atualiza a configuração de um slot de vídeo. */
    public LandingVideoSlotDto updateSlot(Long landingId, Long slotId, UpdateLandingVideoSlotRequest request) {
        return slotService.update(landingId, slotId, request);
    }

    /** Lista o histórico de alterações de um slot de vídeo. */
    public List<LandingVideoSlotHistoryDto> slotHistory(Long landingId, Long slotId) {
        return slotService.history(landingId, slotId);
    }

    /** Cria um playbook comercial para o perfil de vídeo. */
    public SalesVideoCommercialPlaybookDto createPlaybook(Long profileId,
                                                          CreateSalesVideoCommercialPlaybookRequest request) {
        return commercialInsightsService.createPlaybook(profileId, request);
    }

    /** Lista os playbooks comerciais do perfil. */
    public List<SalesVideoCommercialPlaybookDto> listPlaybooks(Long profileId) {
        return commercialInsightsService.listPlaybooks(profileId);
    }

    /** Registra evento comercial ou conversão associada ao vídeo. */
    public SalesVideoConversionEventDto createConversionEvent(Long profileId,
                                                              CreateSalesVideoConversionEventRequest request) {
        return commercialInsightsService.createConversionEvent(profileId, request);
    }

    /** Resume a performance comercial do perfil em uma janela de tempo. */
    public SalesVideoPerformanceSummaryDto summarizePerformance(Long profileId, Instant from, Instant to) {
        return commercialInsightsService.summarizePerformance(profileId, from, to);
    }
}
