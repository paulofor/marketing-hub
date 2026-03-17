package com.marketinghub.worker.leadportal.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.worker.creative.CreativeImageOptimizer;
import com.marketinghub.worker.imagegeneration.ImageGenerationPlan;
import com.marketinghub.worker.imagegeneration.ImageGenerationPlanService;
import com.marketinghub.worker.imagegeneration.ImageOrientation;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import software.amazon.awssdk.core.exception.SdkClientException;

@ExtendWith(MockitoExtension.class)
class LeadPortalImageProcessingServiceTest {

    @Mock
    private LeadPortalImagePackageClient packageClient;

    @Mock
    private LeadPortalStorageClient storageClient;

    @Mock
    private LeadPortalOpenAiImageClient imageClient;

    @Mock
    private ImageGenerationPlanService planService;

    @InjectMocks
    private LeadPortalImageProcessingService service;

    private LeadPortalImagePackageClient.ImagePackage samplePackage;
    private ImageGenerationPlan samplePlan;

    @BeforeEach
    void setUp() {
        samplePlan = new ImageGenerationPlan(
                1L,
                2L,
                "gpt-image-1",
                "high",
                ImageOrientation.SQUARE,
                1024,
                1024,
                "1024x1024",
                null);
        samplePackage = new LeadPortalImagePackageClient.ImagePackage(
                1L,
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                "original.png",
                1,
                0,
                "gpt-image-1",
                "prompt",
                "treatment",
                1L,
                2L);
        when(imageClient.isEnabled()).thenReturn(true);
        lenient().when(planService.detectOrientation(any())).thenReturn(ImageOrientation.SQUARE);
        lenient().when(planService.resolvePlan(any(), any())).thenReturn(samplePlan);
    }

    @Test
    void returnsEmptyListWhenListingPackagesTimesOut() {
        when(packageClient.listRecentPackages()).thenThrow(new RuntimeException(new TimeoutException(
                "Timeout ao buscar pacotes")));

        List<LeadPortalImagePackageClient.ImagePackage> result = service.process();

        assertThat(result).isEmpty();
        verify(packageClient, never()).markProcessing(anyLong());
        verify(storageClient, never()).download(anyString());
    }

    @Test
    void schedulesRetryWhenTransientErrorOccurs() {
        when(packageClient.listRecentPackages()).thenReturn(List.of(samplePackage));
        when(storageClient.download("original.png")).thenReturn(new byte[] {1, 2, 3});
        when(imageClient.generateFromBase(any(), anyString(), any(ImageGenerationPlan.class)))
                .thenThrow(SdkClientException.builder().message("S3 indisponível").build());

        service.process();

        verify(packageClient).markRetry(anyLong(), anyString());
        verify(packageClient, never()).markFailed(anyLong(), anyString());
    }

    @Test
    void marksFailureWhenErrorIsNotTransient() {
        when(packageClient.listRecentPackages()).thenReturn(List.of(samplePackage));
        when(storageClient.download("original.png")).thenReturn(new byte[] {1, 2, 3});
        when(imageClient.generateFromBase(any(), anyString(), any(ImageGenerationPlan.class)))
                .thenThrow(new IllegalArgumentException("Entrada inválida"));

        service.process();

        verify(packageClient, never()).markRetry(anyLong(), anyString());
        verify(packageClient).markFailed(anyLong(), anyString());
    }

    @Test
    void completesProcessingWhenTransientErrorRecovers() {
        when(packageClient.listRecentPackages()).thenReturn(List.of(samplePackage));
        when(storageClient.download("original.png")).thenReturn(new byte[] {1, 2, 3});
        when(imageClient.generateFromBase(any(), anyString(), any(ImageGenerationPlan.class)))
                .thenThrow(new LeadPortalOpenAiImageClient.ImageGenerationException(
                        HttpStatusCode.valueOf(500), "Erro no provedor"))
                .thenReturn(new CreativeImageOptimizer.OptimizedImage(new byte[] {9}, "jpg"));
        when(storageClient.upload(any(), anyString(), any()))
                .thenReturn(new LeadPortalStorageClient.StoredImage("object-key", "public-url", "jpg"));

        service.process();

        verify(imageClient, times(2)).generateFromBase(any(), anyString(), any(ImageGenerationPlan.class));
        verify(packageClient, never()).markRetry(anyLong(), anyString());
        verify(packageClient, never()).markFailed(anyLong(), anyString());
        verify(packageClient).submitResults(anyLong(), any(), anyString(), anyString());
    }

    @Test
    void supportsPromptLimitUpToThreeThousandCharacters() {
        String longPrompt = "a".repeat(2500);
        LeadPortalImagePackageClient.ImagePackage promptOnlyPackage = new LeadPortalImagePackageClient.ImagePackage(
                3L,
                UUID.fromString("123e4567-e89b-12d3-a456-426614174002"),
                null,
                1,
                0,
                "gpt-image-1",
                longPrompt,
                "treatment",
                1L,
                2L);

        when(packageClient.listRecentPackages()).thenReturn(List.of(promptOnlyPackage));
        when(imageClient.generateFromPrompt(anyString(), any(ImageGenerationPlan.class)))
                .thenReturn(new CreativeImageOptimizer.OptimizedImage(new byte[] {9}, "jpg"));
        when(storageClient.upload(any(), anyString(), any()))
                .thenReturn(new LeadPortalStorageClient.StoredImage("object-key", "public-url", "jpg"));

        service.process();

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(imageClient).generateFromPrompt(promptCaptor.capture(), any(ImageGenerationPlan.class));
        String generatedPrompt = promptCaptor.getValue();
        assertThat(generatedPrompt.length()).isLessThanOrEqualTo(3000);
        assertThat(generatedPrompt.length()).isGreaterThan(1000);
        assertThat(generatedPrompt).contains(longPrompt);
    }

    @Test
    void generatesFromPromptWhenPackageHasNoBaseImage() {
        LeadPortalImagePackageClient.ImagePackage promptOnlyPackage = new LeadPortalImagePackageClient.ImagePackage(
                2L,
                UUID.fromString("123e4567-e89b-12d3-a456-426614174001"),
                null,
                1,
                0,
                "gpt-image-1",
                "prompt sem imagem base",
                "treatment",
                1L,
                2L);

        when(packageClient.listRecentPackages()).thenReturn(List.of(promptOnlyPackage));
        when(imageClient.generateFromPrompt(anyString(), any(ImageGenerationPlan.class)))
                .thenReturn(new CreativeImageOptimizer.OptimizedImage(new byte[] {9}, "jpg"));
        when(storageClient.upload(any(), anyString(), any()))
                .thenReturn(new LeadPortalStorageClient.StoredImage("object-key", "public-url", "jpg"));

        service.process();

        verify(storageClient, never()).download(anyString());
        verify(imageClient, never()).generateFromBase(any(), anyString(), any(ImageGenerationPlan.class));
        verify(imageClient).generateFromPrompt(anyString(), any(ImageGenerationPlan.class));
        verify(packageClient).submitResults(anyLong(), any(), anyString(), anyString());
    }
}
