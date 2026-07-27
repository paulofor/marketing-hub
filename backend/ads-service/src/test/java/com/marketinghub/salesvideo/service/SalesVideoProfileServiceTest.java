package com.marketinghub.salesvideo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.LandingPageRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoProfileRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoScriptRepository;
import com.marketinghub.salesvideo.*;
import com.marketinghub.salesvideo.dto.RequestVideoRenderRequest;
import com.marketinghub.salesvideo.dto.SalesVideoJobDto;
import com.marketinghub.salesvideo.dto.UpdateSalesVideoComplianceRequest;
import com.marketinghub.salesvideo.exception.VideoModuleErrorCode;
import com.marketinghub.salesvideo.exception.VideoModuleException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida regras administrativas dos perfis de vídeo comercial. */
@ExtendWith(MockitoExtension.class)
class SalesVideoProfileServiceTest {

  @Mock private SalesVideoProfileRepository profileRepository;
  @Mock private ProductRepository productRepository;
  @Mock private LandingPageRepository landingPageRepository;
  @Mock private SalesVideoScriptRepository scriptRepository;
  @Mock private SalesVideoJobRepository jobRepository;
  @Mock private SalesVideoJobService jobService;
  @Mock private SalesVideoRolloutService rolloutService;

  private SalesVideoProfileService service;

  @BeforeEach
  void setUp() {
    service =
        new SalesVideoProfileService(
            profileRepository,
            productRepository,
            landingPageRepository,
            scriptRepository,
            jobRepository,
            jobService,
            rolloutService);
  }

  @Test
  void shouldBlockProductionRenderWithoutComplianceChecklist() {
    SalesVideoProfile profile = profileWithDefaults();
    profile.setRequiresConsent(true);
    SalesVideoScript script = approvedScript(profile);
    RequestVideoRenderRequest request = new RequestVideoRenderRequest();
    request.setRequestedBy("owner@tenant.io");
    request.setExecutionMode(SalesVideoExecutionMode.PRODUCTION);

    given(profileRepository.findById(profile.getId())).willReturn(Optional.of(profile));
    given(
            scriptRepository.findFirstByProfileIdAndStatusOrderByVersionDesc(
                profile.getId(), SalesVideoScriptStatus.APPROVED))
        .willReturn(Optional.of(script));

    assertThrows(VideoModuleException.class, () -> service.requestRender(profile.getId(), request));
  }

  @Test
  void shouldBlockProductionRenderWhenRolloutIsNotAllowed() {
    SalesVideoProfile profile = profileWithDefaults();
    profile.setRequiresConsent(false);
    profile.setHumanReviewApprovedBy("reviewer@tenant.io");
    profile.setHumanReviewApprovedAt(Instant.parse("2026-04-17T09:30:00Z"));
    SalesVideoScript script = approvedScript(profile);
    RequestVideoRenderRequest request = new RequestVideoRenderRequest();
    request.setRequestedBy("owner@tenant.io");
    request.setExecutionMode(SalesVideoExecutionMode.PRODUCTION);

    given(profileRepository.findById(profile.getId())).willReturn(Optional.of(profile));
    given(
            scriptRepository.findFirstByProfileIdAndStatusOrderByVersionDesc(
                profile.getId(), SalesVideoScriptStatus.APPROVED))
        .willReturn(Optional.of(script));
    org.mockito.Mockito.doThrow(
            VideoModuleException.conflict(
                VideoModuleErrorCode.ROLLOUT_NOT_ALLOWED, "Rollout bloqueado"))
        .when(rolloutService)
        .assertProductionRolloutAllowed(profile);

    VideoModuleException ex =
        assertThrows(
            VideoModuleException.class, () -> service.requestRender(profile.getId(), request));
    assertThat(ex.getErrorCode()).isEqualTo(VideoModuleErrorCode.ROLLOUT_NOT_ALLOWED);
  }

  @Test
  void shouldBuildAuditSnapshotForProductionRenderAfterComplianceIsCompleted() {
    SalesVideoProfile profile = profileWithDefaults();
    profile.setRequiresConsent(true);
    profile.setConsentRecordedBy("compliance@tenant.io");
    profile.setConsentRecordedAt(Instant.parse("2026-04-17T09:00:00Z"));
    profile.setConsentEvidenceUrl("https://evidence.local/term-001");
    profile.setHumanReviewApprovedBy("reviewer@tenant.io");
    profile.setHumanReviewApprovedAt(Instant.parse("2026-04-17T09:30:00Z"));
    SalesVideoScript script = approvedScript(profile);
    RequestVideoRenderRequest request = new RequestVideoRenderRequest();
    request.setRequestedBy("operator@tenant.io");
    request.setExecutionMode(SalesVideoExecutionMode.PRODUCTION);
    request.setProviderFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE);
    request.setProviderName("video-management-service");

    SalesVideoJob generatedJob =
        SalesVideoJob.builder()
            .id(11L)
            .profile(profile)
            .script(script)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .providerName("video-management-service")
            .executionMode(SalesVideoExecutionMode.PRODUCTION)
            .jobType(SalesVideoJobType.RENDER)
            .status(SalesVideoStatus.VIDEO_REQUESTED)
            .requestedBy("operator@tenant.io")
            .requestedAt(Instant.parse("2026-04-17T10:00:00Z"))
            .build();

    given(profileRepository.findById(profile.getId())).willReturn(Optional.of(profile));
    given(
            scriptRepository.findFirstByProfileIdAndStatusOrderByVersionDesc(
                profile.getId(), SalesVideoScriptStatus.APPROVED))
        .willReturn(Optional.of(script));
    org.mockito.Mockito.doNothing().when(rolloutService).assertProductionRolloutAllowed(profile);
    given(jobService.createJob(any(), any(), any(), any(), any(), any(), any()))
        .willReturn(generatedJob);
    given(jobRepository.save(any(SalesVideoJob.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(profileRepository.save(any(SalesVideoProfile.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    SalesVideoJobDto response = service.requestRender(profile.getId(), request);

    ArgumentCaptor<SalesVideoJob> captor = ArgumentCaptor.forClass(SalesVideoJob.class);
    org.mockito.Mockito.verify(jobRepository).save(captor.capture());
    SalesVideoJob persistedJob = captor.getValue();

    assertThat(response.getExecutionMode()).isEqualTo(SalesVideoExecutionMode.PRODUCTION);
    assertThat(persistedJob.getAuditSnapshotJson()).contains("\"executionMode\":\"PRODUCTION\"");
    assertThat(persistedJob.getAuditSnapshotJson())
        .contains("\"consentEvidenceUrl\":\"https://evidence.local/term-001\"");
    assertThat(persistedJob.getAuditSnapshotJson())
        .contains("\"humanReviewApprovedBy\":\"reviewer@tenant.io\"");
  }

  /** Bloqueia render direto do VEO quando o perfil comercial pede mais de 8 segundos. */
  @Test
  void shouldRejectDirectVeoRenderWhenProfileDurationExceedsProviderLimit() {
    SalesVideoProfile profile = profileWithDefaults();
    profile.setTargetDurationSeconds(30);
    SalesVideoScript script = approvedScript(profile);
    RequestVideoRenderRequest request = new RequestVideoRenderRequest();
    request.setRequestedBy("owner@tenant.io");
    request.setExecutionMode(SalesVideoExecutionMode.TEST);
    request.setProviderFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE);
    request.setProviderName("VEO");

    given(profileRepository.findById(profile.getId())).willReturn(Optional.of(profile));
    given(
            scriptRepository.findFirstByProfileIdAndStatusOrderByVersionDesc(
                profile.getId(), SalesVideoScriptStatus.APPROVED))
        .willReturn(Optional.of(script));

    VideoModuleException ex =
        assertThrows(
            VideoModuleException.class, () -> service.requestRender(profile.getId(), request));

    assertThat(ex.getMessage()).contains("VEO aceita no máximo 8 segundos");
  }

  /** Bloqueia render do Kling quando o perfil pede mais de 10 segundos. */
  @Test
  void shouldRejectKlingRenderWhenProfileDurationExceedsProviderLimit() {
    SalesVideoProfile profile = profileWithDefaults();
    profile.setTargetDurationSeconds(15);
    SalesVideoScript script = approvedScript(profile);
    RequestVideoRenderRequest request = new RequestVideoRenderRequest();
    request.setRequestedBy("owner@tenant.io");
    request.setExecutionMode(SalesVideoExecutionMode.TEST);
    request.setProviderFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE);
    request.setProviderName("KLING_3_0");

    given(profileRepository.findById(profile.getId())).willReturn(Optional.of(profile));
    given(
            scriptRepository.findFirstByProfileIdAndStatusOrderByVersionDesc(
                profile.getId(), SalesVideoScriptStatus.APPROVED))
        .willReturn(Optional.of(script));

    VideoModuleException ex =
        assertThrows(
            VideoModuleException.class, () -> service.requestRender(profile.getId(), request));

    assertThat(ex.getMessage()).contains("Kling aceita no máximo 10 segundos");
  }

  /** Bloqueia render do Runway quando o perfil pede mais de 10 segundos. */
  @Test
  void shouldRejectRunwayRenderWhenProfileDurationExceedsProviderLimit() {
    SalesVideoProfile profile = profileWithDefaults();
    profile.setTargetDurationSeconds(15);
    SalesVideoScript script = approvedScript(profile);
    RequestVideoRenderRequest request = new RequestVideoRenderRequest();
    request.setRequestedBy("owner@tenant.io");
    request.setExecutionMode(SalesVideoExecutionMode.TEST);
    request.setProviderFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE);
    request.setProviderName("RUNWAY");

    given(profileRepository.findById(profile.getId())).willReturn(Optional.of(profile));
    given(
            scriptRepository.findFirstByProfileIdAndStatusOrderByVersionDesc(
                profile.getId(), SalesVideoScriptStatus.APPROVED))
        .willReturn(Optional.of(script));

    VideoModuleException ex =
        assertThrows(
            VideoModuleException.class, () -> service.requestRender(profile.getId(), request));

    assertThat(ex.getMessage()).contains("Runway aceita no máximo 10 segundos");
  }

  /** Bloqueia render da Luma quando o perfil excede o limite do adapter com montagem atual. */
  @Test
  void shouldRejectLumaRenderWhenProfileDurationExceedsProviderLimit() {
    SalesVideoProfile profile = profileWithDefaults();
    profile.setTargetDurationSeconds(31);
    SalesVideoScript script = approvedScript(profile);
    RequestVideoRenderRequest request = new RequestVideoRenderRequest();
    request.setRequestedBy("owner@tenant.io");
    request.setExecutionMode(SalesVideoExecutionMode.TEST);
    request.setProviderFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE);
    request.setProviderName("LUMA_RAY_3_2");

    given(profileRepository.findById(profile.getId())).willReturn(Optional.of(profile));
    given(
            scriptRepository.findFirstByProfileIdAndStatusOrderByVersionDesc(
                profile.getId(), SalesVideoScriptStatus.APPROVED))
        .willReturn(Optional.of(script));

    VideoModuleException ex =
        assertThrows(
            VideoModuleException.class, () -> service.requestRender(profile.getId(), request));

    assertThat(ex.getMessage()).contains("Luma Ray 3.2 aceita no máximo 30 segundos");
  }

  @Test
  void shouldClearConsentFieldsWhenConsentBecomesOptional() {
    SalesVideoProfile profile = profileWithDefaults();
    profile.setRequiresConsent(true);
    profile.setConsentRecordedBy("compliance@tenant.io");
    profile.setConsentRecordedAt(Instant.parse("2026-04-17T09:00:00Z"));
    profile.setConsentEvidenceUrl("https://evidence.local/term-001");

    UpdateSalesVideoComplianceRequest request = new UpdateSalesVideoComplianceRequest();
    request.setRequiresConsent(false);

    given(profileRepository.findById(profile.getId())).willReturn(Optional.of(profile));
    given(profileRepository.save(any(SalesVideoProfile.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    service.updateCompliance(profile.getId(), request);

    assertThat(profile.isRequiresConsent()).isFalse();
    assertThat(profile.getConsentRecordedBy()).isNull();
    assertThat(profile.getConsentRecordedAt()).isNull();
    assertThat(profile.getConsentEvidenceUrl()).isNull();
  }

  private static SalesVideoProfile profileWithDefaults() {
    return SalesVideoProfile.builder()
        .id(7L)
        .product(Product.builder().id(99L).build())
        .tenantId("tenant-a")
        .status(SalesVideoStatus.SCRIPT_READY)
        .title("Avatar Hero")
        .videoKind(SalesVideoKind.HERO)
        .build();
  }

  private static SalesVideoScript approvedScript(SalesVideoProfile profile) {
    return SalesVideoScript.builder()
        .id(3L)
        .profile(profile)
        .version(2)
        .status(SalesVideoScriptStatus.APPROVED)
        .source(SalesVideoScriptSource.OPENAI)
        .scriptText("Script aprovado")
        .build();
  }
}
