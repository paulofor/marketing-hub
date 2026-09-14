package com.marketinghub.videomanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.BackendVideoClient;
import com.marketinghub.videomanagement.client.dto.AssetType;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoJobType;
import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;
import com.marketinghub.videomanagement.client.dto.SalesVideoProviderFamily;
import com.marketinghub.videomanagement.client.dto.SalesVideoScript;
import com.marketinghub.videomanagement.client.dto.SalesVideoScriptStatus;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import com.marketinghub.videomanagement.client.payload.JobClaimPayload;
import com.marketinghub.videomanagement.client.payload.JobCompletionPayload;
import com.marketinghub.videomanagement.client.payload.JobFailurePayload;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.exception.BackendIntegrationException;
import com.marketinghub.videomanagement.service.VideoAssetUploader.UploadedAssets;
import com.marketinghub.videomanagement.service.provider.ProviderArtifacts;
import com.marketinghub.videomanagement.service.provider.ProviderAssetRole;
import com.marketinghub.videomanagement.service.provider.ProviderFile;
import com.marketinghub.videomanagement.service.provider.VideoProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.Map;
import java.util.List;
import com.marketinghub.videomanagement.service.provider.AuditedVideoProviderException;
import com.marketinghub.videomanagement.service.provider.VideoProviderException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Responsabilidade: validar claim, execução, gates técnicos e callbacks de jobs de vídeo. */
@ExtendWith(MockitoExtension.class)
class VideoJobProcessorTest {

    @Mock
    private BackendVideoClient backendClient;

    @Mock
    private ProviderRegistry providerRegistry;

    @Mock
    private VideoAssetUploader assetUploader;

    @Mock
    private VideoProvider videoProvider;

    @Mock
    private VideoJobObservabilityService observabilityService;

    @Mock
    private ApolloStoryboardPlanner apolloStoryboardPlanner;

    @Mock
    private ApolloGovernedLearningReporter learningReporter;

    @Mock
    private ProductUgcPostProductionContractResolver productUgcContractResolver;

    @Captor
    private ArgumentCaptor<JobCompletionPayload> completionCaptor;

    @Captor
    private ArgumentCaptor<JobFailurePayload> failureCaptor;

    private VideoJobProcessor processor;

    /** Inicializa o processador com dependências simuladas e o gate técnico real. */
    @BeforeEach
    void setUp() {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.setWorkerId("worker-test");
        processor = new VideoJobProcessor(
                backendClient,
                providerRegistry,
                assetUploader,
                observabilityService,
                properties,
                new ObjectMapper(),
                apolloStoryboardPlanner,
                learningReporter,
                new ApolloTechnicalVideoQualityGate(new ObjectMapper(), properties),
                productUgcContractResolver);
        lenient()
                .when(productUgcContractResolver.resolve(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    /** Conclui o job e envia os ativos quando provider e gate retornam sucesso. */
    @Test
    void shouldCompleteJobWhenProviderSucceeds() {
        SalesVideoJob job = job();
        when(apolloStoryboardPlanner.planAndApprove(any(), any(), any())).thenReturn(job);
        SalesVideoProfile profile = profile();
        ProviderFile videoFile = new ProviderFile("video.mp4", MediaType.valueOf("video/mp4"), AssetType.VIDEO,
                ProviderAssetRole.VIDEO, new byte[]{1});
        ProviderArtifacts artifacts = new ProviderArtifacts("stub-1", videoFile, null, null, Map.of("key", "value"));
        when(backendClient.fetchProfile(2L)).thenReturn(profile);
        when(providerRegistry.resolve(job)).thenReturn(Optional.of(videoProvider));
        when(videoProvider.render(any(), any(), any())).thenReturn(artifacts);
        when(assetUploader.uploadAssets(job, artifacts)).thenReturn(new UploadedAssets(20L, null, null,
                "https://cdn.test/final.m3u8", Map.of("hls_delivery", Map.of("status", "READY"))));

        processor.process(job);

        verify(backendClient).claimJob(org.mockito.Mockito.eq(job.id()),
                org.mockito.ArgumentMatchers.any(JobClaimPayload.class));
        verify(backendClient).completeJob(org.mockito.Mockito.eq(job.id()), completionCaptor.capture());
        JobCompletionPayload payload = completionCaptor.getValue();
        assertThat(payload.assetId()).isEqualTo(20L);
        assertThat(payload.streamPlaybackUrl()).isEqualTo("https://cdn.test/final.m3u8");
        assertThat(payload.metadataJson()).contains("hls_delivery");
        assertThat(payload.status()).isEqualTo(SalesVideoStatus.VIDEO_READY);
        verify(learningReporter).observe(job, job);
        var order = org.mockito.Mockito.inOrder(videoProvider, apolloStoryboardPlanner);
        order.verify(videoProvider).validateInput(any(), any());
        order.verify(apolloStoryboardPlanner).planAndApprove(any(), any(), any());
        verify(backendClient, never()).failJob(any(), any());
    }

    /** Mantém áudio já consumido no backend antes de registrar a reprovação temporal. */
    @Test
    void shouldPersistAudioBeforeReportingProviderFailure() {
        var job = prepareAuditJob();
        var artifacts = audioAudit();
        when(videoProvider.render(any(), any(), any())).thenThrow(AuditedVideoProviderException.preserve(job.id(),
                new VideoProviderException("APOLLO_NARRATION_DURATION_EXCEEDED", "15.552s > 15s"), artifacts.auditFiles(), List.of(Map.of("status", "RECEIVED"))));
        when(assetUploader.uploadAuditAssets(any(), any())).thenReturn(List.of(Map.of("asset_id", 91001L)));
        processor.process(job);
        var order = org.mockito.Mockito.inOrder(assetUploader, backendClient);
        order.verify(assetUploader).uploadAuditAssets(any(), any());
        order.verify(backendClient).reportProgress(any(), any());
        order.verify(backendClient).failJob(any(), failureCaptor.capture());
        assertThat(failureCaptor.getValue().failureCode()).isEqualTo("APOLLO_NARRATION_DURATION_EXCEEDED");
        assertThat(failureCaptor.getValue().retryable()).isFalse();
        verify(assetUploader, never()).uploadAssets(any(), any());
        verify(backendClient, never()).completeJob(any(), any());
    }

    /** Falha de armazenamento bloqueia e exige conciliação, sem render aprovado nem nova tentativa. */
    @Test
    void shouldStopWhenFailedProviderAuditCannotBePersisted() {
        var job = prepareAuditJob(); var artifacts = audioAudit();
        when(videoProvider.render(any(), any(), any())).thenThrow(AuditedVideoProviderException.preserve(job.id(),
                new VideoProviderException("APOLLO_NARRATION_DURATION_EXCEEDED", "excedeu"), artifacts.auditFiles(), List.of()));
        when(assetUploader.uploadAuditAssets(any(), any())).thenThrow(new BackendIntegrationException("fixture unavailable"));
        processor.process(job);
        verify(backendClient).failJob(any(), failureCaptor.capture());
        assertThat(failureCaptor.getValue().failureCode()).isEqualTo("AUDIT_PERSISTENCE_FAILED");
        assertThat(failureCaptor.getValue().retryable()).isFalse();
        verify(backendClient, never()).completeJob(any(), any());
        verify(assetUploader, never()).uploadAssets(any(), any());
    }

    /** Uma reprovação técnica posterior ao provider conserva a voz, mas não publica o vídeo. */
    @Test
    void shouldPreserveAuditBeforeTechnicalGateRejection() {
        var job = prepareAuditJob(); var artifacts = audioAudit();
        when(videoProvider.render(any(), any(), any())).thenReturn(artifacts);
        when(assetUploader.uploadAuditAssets(any(), any())).thenReturn(List.of(Map.of("asset_id", 91001L)));
        var gate = org.mockito.Mockito.mock(ApolloTechnicalVideoQualityGate.class);
        when(gate.validate(any(), any())).thenThrow(new VideoProviderException("APOLLO_VIDEO_STABILITY_REJECTED", "fixture"));
        processorWithGate(gate).process(job);
        var order = org.mockito.Mockito.inOrder(assetUploader, gate, backendClient);
        order.verify(assetUploader).uploadAuditAssets(any(), any());
        order.verify(gate).validate(any(), any());
        order.verify(backendClient).failJob(any(), failureCaptor.capture());
        assertThat(failureCaptor.getValue().failureCode()).isEqualTo("APOLLO_VIDEO_STABILITY_REJECTED");
        verify(assetUploader, never()).uploadAssets(any(), any());
    }

    /** No sucesso, os recibos entram no resultado e os mesmos binários não são enviados novamente. */
    @Test
    void shouldUploadAuditOnlyOnceOnSuccess() {
        var job = prepareAuditJob(); var artifacts = audioAudit();
        when(videoProvider.render(any(), any(), any())).thenReturn(artifacts);
        when(assetUploader.uploadAuditAssets(any(), any())).thenReturn(List.of(Map.of("asset_id", 91001L)));
        when(assetUploader.uploadAssets(any(), any())).thenReturn(new UploadedAssets(91002L, null, null));
        processor.process(job);
        var captured = ArgumentCaptor.forClass(ProviderArtifacts.class);
        verify(assetUploader).uploadAssets(any(), captured.capture());
        assertThat(captured.getValue().auditFiles()).isEmpty();
        assertThat(captured.getValue().metadata()).containsKey("persisted_audit_assets");
        verify(assetUploader).uploadAuditAssets(any(), any());
        verify(backendClient).completeJob(any(), completionCaptor.capture());
        assertThat(completionCaptor.getValue().metadataJson()).contains("91001", "persisted_audit_assets");
    }

    /** Prepara somente execução local com provider e planejamento simulados. */
    private SalesVideoJob prepareAuditJob() {
        var job = job();
        when(apolloStoryboardPlanner.planAndApprove(any(), any(), any())).thenReturn(job);
        when(backendClient.fetchProfile(2L)).thenReturn(profile());
        when(providerRegistry.resolve(job)).thenReturn(Optional.of(videoProvider));
        return job;
    }

    /** Representa áudio pago recebido e vídeo ainda sujeito à validação técnica. */
    private ProviderArtifacts audioAudit() {
        return new ProviderArtifacts("fixture", new ProviderFile("video.mp4", MediaType.valueOf("video/mp4"), AssetType.VIDEO,
                ProviderAssetRole.VIDEO, new byte[]{1}), null, null, Map.of("tts_interactions", List.of()),
                List.of(new ProviderFile("tts.wav", MediaType.valueOf("audio/wav"), AssetType.AUDIO, ProviderAssetRole.AUDIO_AUDIT, new byte[]{2,3})));
    }

    /** Injeta a reprovação do gate sem executar FFmpeg sobre bytes sintéticos. */
    private VideoJobProcessor processorWithGate(ApolloTechnicalVideoQualityGate gate) {
        return new VideoJobProcessor(backendClient, providerRegistry, assetUploader, observabilityService,
                new VideoManagementProperties(), new ObjectMapper(), apolloStoryboardPlanner, learningReporter, gate, productUgcContractResolver);
    }

    /** Registra falha funcional quando nenhum provider atende ao contrato do job. */
    @Test
    void shouldFailJobWhenNoProviderIsFound() {
        SalesVideoJob job = job();
        when(backendClient.fetchProfile(2L)).thenReturn(profile());
        when(providerRegistry.resolve(job)).thenReturn(Optional.empty());

        processor.process(job);

        verify(backendClient).failJob(any(), failureCaptor.capture());
        assertThat(failureCaptor.getValue().failureCode()).isEqualTo("VIDEO_PROVIDER_ERROR");
        assertThat(failureCaptor.getValue().retryable()).isFalse();
        assertThat(failureCaptor.getValue().retryReason()).isEqualTo("OTHER");
    }

    /** Interrompe a execução sem falha quando outro worker já assumiu o job. */
    @Test
    void shouldSkipProcessingWhenClaimIsDuplicated() {
        SalesVideoJob job = job();
        when(backendClient.claimJob(any(), any()))
                .thenThrow(new BackendIntegrationException("claim conflict", 409));

        processor.process(job);

        verify(backendClient, never()).fetchProfile(any());
        verify(backendClient, never()).failJob(any(), any());
        verify(backendClient, never()).completeJob(any(), any());
    }

    /** Cria o job mínimo usado nos cenários de processamento. */
    private SalesVideoJob job() {
        return new SalesVideoJob(
                1L,
                2L,
                3L,
                "tenant-a",
                SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
                "STUB",
                null,
                SalesVideoJobType.RENDER,
                SalesVideoStatus.VIDEO_REQUESTED,
                1,
                null,
                null,
                null,
                0,
                null,
                null,
                null,
                Instant.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now());
    }

    /** Cria o perfil mínimo vinculado ao job de teste. */
    private SalesVideoProfile profile() {
        SalesVideoScript script = new SalesVideoScript(
                10L,
                1,
                "script text",
                "hook",
                "cta",
                "caption",
                null,
                "MANUAL",
                "gpt",
                "prompt",
                SalesVideoScriptStatus.APPROVED,
                "user",
                Instant.now(),
                Instant.now());
        return new SalesVideoProfile(
                2L,
                1L,
                null,
                "SHORT",
                "Título",
                "Persona",
                "Estilo",
                "Voz",
                "pt-BR",
                60,
                SalesVideoStatus.SCRIPT_READY,
                Instant.now(),
                Instant.now(),
                script,
                null);
    }
}
