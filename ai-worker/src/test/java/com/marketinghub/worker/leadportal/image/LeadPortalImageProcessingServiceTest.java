package com.marketinghub.worker.leadportal.image;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkClientException;

@ExtendWith(MockitoExtension.class)
class LeadPortalImageProcessingServiceTest {

    @Mock
    private LeadPortalImagePackageClient packageClient;

    @Mock
    private LeadPortalStorageClient storageClient;

    @Mock
    private LeadPortalOpenAiImageClient imageClient;

    @InjectMocks
    private LeadPortalImageProcessingService service;

    private LeadPortalImagePackageClient.ImagePackage samplePackage;

    @BeforeEach
    void setUp() {
        samplePackage = new LeadPortalImagePackageClient.ImagePackage(
                1L,
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                "original.png",
                1,
                null,
                null,
                "prompt",
                null);
        when(imageClient.isEnabled()).thenReturn(true);
        doNothing().when(packageClient).markProcessing(anyLong());
    }

    @Test
    void schedulesRetryWhenTransientErrorOccurs() {
        when(packageClient.listRecentPackages()).thenReturn(List.of(samplePackage));
        when(storageClient.download("original.png")).thenReturn(new byte[] {1, 2, 3});
        when(imageClient.generateFromBase(any(), anyString()))
                .thenThrow(SdkClientException.builder().message("S3 indisponível").build());

        service.process();

        verify(packageClient).markRetry(anyLong(), anyString());
        verify(packageClient, never()).markFailed(anyLong(), anyString());
    }

    @Test
    void marksFailureWhenErrorIsNotTransient() {
        when(packageClient.listRecentPackages()).thenReturn(List.of(samplePackage));
        when(storageClient.download("original.png")).thenReturn(new byte[] {1, 2, 3});
        when(imageClient.generateFromBase(any(), anyString()))
                .thenThrow(new IllegalArgumentException("Entrada inválida"));

        service.process();

        verify(packageClient, never()).markRetry(anyLong(), anyString());
        verify(packageClient).markFailed(anyLong(), anyString());
    }
}
