package com.marketinghub.salesvideo.controller;

import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetType;
import com.marketinghub.media.MediaProvider;
import com.marketinghub.media.dto.AssetDto;
import com.marketinghub.media.mapper.AssetMapper;
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
import com.marketinghub.salesvideo.service.SalesVideoService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller único do módulo Avatar Sales Video para contratos administrativos e internos.
 */
@RestController
public class SalesVideoController {
    private final SalesVideoService salesVideoService;
    private final AssetMapper assetMapper;

    /** Inicializa o controller com a fachada única do módulo e o mapper de assets. */
    public SalesVideoController(SalesVideoService salesVideoService, AssetMapper assetMapper) {
        this.salesVideoService = salesVideoService;
        this.assetMapper = assetMapper;
    }

    /** Cria um perfil de vídeo para um produto. */
    @PostMapping("/api/products/{productId}/sales-videos/profiles")
    public SalesVideoProfileDto createProfile(@PathVariable Long productId,
                                              @Valid @RequestBody CreateSalesVideoProfileRequest request) {
        return salesVideoService.createProfile(productId, request);
    }

    /** Lista perfis de vídeo de um produto. */
    @GetMapping("/api/products/{productId}/sales-videos/profiles")
    public List<SalesVideoProfileDto> listProfiles(@PathVariable Long productId) {
        return salesVideoService.listProfiles(productId);
    }

    /** Consulta um perfil de vídeo. */
    @GetMapping("/api/sales-videos/profiles/{profileId}")
    public SalesVideoProfileDto getProfile(@PathVariable Long profileId) {
        return salesVideoService.getProfile(profileId);
    }

    /** Lista scripts de um perfil de vídeo. */
    @GetMapping("/api/sales-videos/profiles/{profileId}/scripts")
    public List<SalesVideoScriptDto> listScripts(@PathVariable Long profileId) {
        return salesVideoService.listScripts(profileId);
    }

    /** Solicita geração automática de script. */
    @PostMapping("/api/sales-videos/profiles/{profileId}/generate-script")
    public SalesVideoJobDto requestScript(@PathVariable Long profileId,
                                          @Valid @RequestBody GenerateSalesVideoScriptRequest request) {
        return salesVideoService.requestScriptGeneration(profileId, request);
    }

    /** Aprova manualmente um script. */
    @PostMapping("/api/sales-videos/profiles/{profileId}/approve-script")
    public SalesVideoScriptDto approveScript(@PathVariable Long profileId,
                                             @Valid @RequestBody ApproveSalesVideoScriptRequest request) {
        return salesVideoService.approveScript(profileId, request);
    }

    /** Solicita renderização de um vídeo aprovado. */
    @PostMapping("/api/sales-videos/profiles/{profileId}/request-render")
    public SalesVideoJobDto requestRender(@PathVariable Long profileId,
                                          @Valid @RequestBody RequestVideoRenderRequest request) {
        return salesVideoService.requestRender(profileId, request);
    }

    /** Atualiza checklist de compliance do perfil. */
    @PatchMapping("/api/sales-videos/profiles/{profileId}/compliance")
    public SalesVideoProfileDto updateCompliance(@PathVariable Long profileId,
                                                 @RequestBody UpdateSalesVideoComplianceRequest request) {
        return salesVideoService.updateCompliance(profileId, request);
    }

    /** Consulta status de rollout do tenant. */
    @GetMapping("/api/sales-videos/rollout/status")
    public SalesVideoRolloutStatusDto getTenantRolloutStatus() {
        return salesVideoService.getTenantRolloutStatus();
    }

    /** Consulta status de rollout de um perfil. */
    @GetMapping("/api/sales-videos/profiles/{profileId}/rollout-status")
    public SalesVideoRolloutStatusDto getProfileRolloutStatus(@PathVariable Long profileId) {
        return salesVideoService.getProfileRolloutStatus(profileId);
    }

    /** Lista jobs administrativos por perfil. */
    @GetMapping("/api/sales-videos/profiles/{profileId}/jobs")
    public List<SalesVideoJobDto> listJobsByProfile(@PathVariable Long profileId) {
        return salesVideoService.listJobsByProfile(profileId);
    }

    /** Consulta um job administrativo. */
    @GetMapping("/api/sales-videos/jobs/{jobId}")
    public SalesVideoJobDto getAdminJob(@PathVariable Long jobId) {
        return salesVideoService.getJob(jobId);
    }

    /** Lista eventos de um job administrativo. */
    @GetMapping("/api/sales-videos/jobs/{jobId}/events")
    public List<SalesVideoJobEventDto> getJobEvents(@PathVariable Long jobId) {
        return salesVideoService.getJobEvents(jobId);
    }

    /** Solicita reprocessamento de um job. */
    @PostMapping("/api/sales-videos/jobs/{jobId}/retry")
    public SalesVideoJobDto retryJob(@PathVariable Long jobId,
                                     @Valid @RequestBody RetrySalesVideoJobRequest request) {
        return salesVideoService.retry(jobId, request);
    }

    /** Lista jobs OpenAI para consumo interno do ai-worker. */
    @GetMapping("/internal/ai/openai-jobs")
    public List<SalesVideoJobDto> listOpenAiJobs(@RequestParam(required = false) SalesVideoStatus status,
                                                 @RequestParam(required = false, name = "type") SalesVideoJobType jobType,
                                                 @RequestParam(defaultValue = "25") int limit) {
        return salesVideoService.findJobs(SalesVideoProviderFamily.OPENAI, status, jobType, limit);
    }

    /** Consulta job OpenAI interno. */
    @GetMapping("/internal/ai/openai-jobs/{jobId}")
    public SalesVideoJobDto getOpenAiJob(@PathVariable Long jobId) {
        return salesVideoService.getJob(jobId);
    }

    /** Faz claim de job OpenAI interno. */
    @PostMapping("/internal/ai/openai-jobs/{jobId}/claim")
    public SalesVideoJobDto claimOpenAiJob(@PathVariable Long jobId,
                                           @Valid @RequestBody JobClaimRequest request) {
        return salesVideoService.claimJob(jobId, request);
    }

    /** Registra heartbeat de job OpenAI interno. */
    @PostMapping("/internal/ai/openai-jobs/{jobId}/heartbeat")
    public SalesVideoJobDto heartbeatOpenAiJob(@PathVariable Long jobId,
                                               @RequestBody JobHeartbeatRequest request) {
        return salesVideoService.heartbeat(jobId, request);
    }

    /** Registra progresso de job OpenAI interno. */
    @PostMapping("/internal/ai/openai-jobs/{jobId}/progress")
    public SalesVideoJobDto progressOpenAiJob(@PathVariable Long jobId,
                                              @RequestBody JobProgressRequest request) {
        return salesVideoService.progress(jobId, request);
    }

    /** Conclui job OpenAI interno. */
    @PostMapping("/internal/ai/openai-jobs/{jobId}/complete")
    public SalesVideoJobDto completeOpenAiJob(@PathVariable Long jobId,
                                              @RequestBody JobCompletionRequest request) {
        return salesVideoService.complete(jobId, request);
    }

    /** Marca job OpenAI interno como falho. */
    @PostMapping("/internal/ai/openai-jobs/{jobId}/fail")
    public SalesVideoJobDto failOpenAiJob(@PathVariable Long jobId,
                                          @RequestBody JobFailureRequest request) {
        return salesVideoService.fail(jobId, request);
    }

    /** Lista jobs internos do módulo externo de vídeo. */
    @GetMapping("/internal/video/jobs")
    public List<SalesVideoJobDto> listVideoJobs(@RequestParam(required = false) SalesVideoStatus status,
                                                @RequestParam(required = false, name = "type") SalesVideoJobType jobType,
                                                @RequestParam(required = false) SalesVideoProviderFamily providerFamily,
                                                @RequestParam(defaultValue = "25") int limit) {
        SalesVideoProviderFamily family = providerFamily != null
                ? providerFamily
                : SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE;
        return salesVideoService.findJobs(family, status, jobType, limit);
    }

    /** Consulta job interno do módulo externo de vídeo. */
    @GetMapping("/internal/video/jobs/{jobId}")
    public SalesVideoJobDto getVideoJob(@PathVariable Long jobId) {
        return salesVideoService.getJob(jobId);
    }

    /** Faz claim de job interno do módulo externo de vídeo. */
    @PostMapping("/internal/video/jobs/{jobId}/claim")
    public SalesVideoJobDto claimVideoJob(@PathVariable Long jobId,
                                          @Valid @RequestBody JobClaimRequest request) {
        return salesVideoService.claimJob(jobId, request);
    }

    /** Registra heartbeat de job interno do módulo externo de vídeo. */
    @PostMapping("/internal/video/jobs/{jobId}/heartbeat")
    public SalesVideoJobDto heartbeatVideoJob(@PathVariable Long jobId,
                                              @RequestBody JobHeartbeatRequest request) {
        return salesVideoService.heartbeat(jobId, request);
    }

    /** Registra progresso de job interno do módulo externo de vídeo. */
    @PostMapping("/internal/video/jobs/{jobId}/progress")
    public SalesVideoJobDto progressVideoJob(@PathVariable Long jobId,
                                             @RequestBody JobProgressRequest request) {
        return salesVideoService.progress(jobId, request);
    }

    /** Conclui job interno do módulo externo de vídeo. */
    @PostMapping("/internal/video/jobs/{jobId}/complete")
    public SalesVideoJobDto completeVideoJob(@PathVariable Long jobId,
                                             @RequestBody JobCompletionRequest request) {
        return salesVideoService.complete(jobId, request);
    }

    /** Marca job interno do módulo externo de vídeo como falho. */
    @PostMapping("/internal/video/jobs/{jobId}/fail")
    public SalesVideoJobDto failVideoJob(@PathVariable Long jobId,
                                         @RequestBody JobFailureRequest request) {
        return salesVideoService.fail(jobId, request);
    }

    /** Marca job interno do módulo externo de vídeo como expirado. */
    @PostMapping("/internal/video/jobs/{jobId}/expired")
    public SalesVideoJobDto expireVideoJob(@PathVariable Long jobId,
                                           @RequestBody JobExpirationRequest request) {
        return salesVideoService.expire(jobId, request);
    }

    /** Faz upload interno de asset final ou auxiliar de vídeo. */
    @PostMapping(value = "/internal/video/assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AssetDto upload(@RequestParam("file") MultipartFile file,
                           @RequestParam(name = "assetType", required = false) AssetType assetType,
                           @RequestParam(name = "provider", required = false) MediaProvider provider,
                           @RequestParam(name = "metadata", required = false) String metadata) throws IOException {
        Asset asset = salesVideoService.storeAsset(file, assetType, provider, metadata);
        return assetMapper.toDto(asset);
    }

    /** Lista slots de vídeo de uma landing page. */
    @GetMapping("/api/landing-pages/{landingId}/video-slots")
    public List<LandingVideoSlotDto> listSlots(@PathVariable Long landingId) {
        return salesVideoService.listSlots(landingId);
    }

    /** Cria um slot de vídeo em uma landing page. */
    @PostMapping("/api/landing-pages/{landingId}/video-slots")
    public LandingVideoSlotDto createSlot(@PathVariable Long landingId,
                                          @Valid @RequestBody CreateLandingVideoSlotRequest request) {
        return salesVideoService.createSlot(landingId, request);
    }

    /** Atualiza um slot de vídeo em uma landing page. */
    @PatchMapping("/api/landing-pages/{landingId}/video-slots/{slotId}")
    public LandingVideoSlotDto updateSlot(@PathVariable Long landingId,
                                          @PathVariable Long slotId,
                                          @RequestBody UpdateLandingVideoSlotRequest request) {
        return salesVideoService.updateSlot(landingId, slotId, request);
    }

    /** Lista histórico de alteração de um slot de vídeo. */
    @GetMapping("/api/landing-pages/{landingId}/video-slots/{slotId}/history")
    public List<LandingVideoSlotHistoryDto> slotHistory(@PathVariable Long landingId,
                                                        @PathVariable Long slotId) {
        return salesVideoService.slotHistory(landingId, slotId);
    }

    /** Cria um playbook comercial de vídeo. */
    @PostMapping("/api/sales-videos/profiles/{profileId}/commercial-playbooks")
    public SalesVideoCommercialPlaybookDto createPlaybook(@PathVariable Long profileId,
                                                          @Valid @RequestBody CreateSalesVideoCommercialPlaybookRequest request) {
        return salesVideoService.createPlaybook(profileId, request);
    }

    /** Lista playbooks comerciais de vídeo. */
    @GetMapping("/api/sales-videos/profiles/{profileId}/commercial-playbooks")
    public List<SalesVideoCommercialPlaybookDto> listPlaybooks(@PathVariable Long profileId) {
        return salesVideoService.listPlaybooks(profileId);
    }

    /** Registra evento de conversão de vídeo. */
    @PostMapping("/api/sales-videos/profiles/{profileId}/conversion-events")
    public SalesVideoConversionEventDto createConversionEvent(@PathVariable Long profileId,
                                                              @Valid @RequestBody CreateSalesVideoConversionEventRequest request) {
        return salesVideoService.createConversionEvent(profileId, request);
    }

    /** Consulta resumo de performance comercial de vídeo. */
    @GetMapping("/api/sales-videos/profiles/{profileId}/performance-summary")
    public SalesVideoPerformanceSummaryDto getPerformanceSummary(@PathVariable Long profileId,
                                                                 @RequestParam(required = false)
                                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                                 Instant from,
                                                                 @RequestParam(required = false)
                                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                                 Instant to) {
        return salesVideoService.summarizePerformance(profileId, from, to);
    }
}
