package com.marketinghub.worker.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.report.ExperimentReportStatus;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto;
import com.marketinghub.experiment.report.dto.ExperimentReportRequestDetailDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExperimentReportServiceTest {

    @Mock
    private ExperimentReportBackendClient backendClient;
    @Mock
    private ExperimentReportStorageClient storageClient;

    private ExperimentReportService service;
    private ExperimentReportProperties properties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new ExperimentReportProperties();
        ExperimentReportRenderer renderer = new ExperimentReportRenderer(properties);
        objectMapper = new ObjectMapper();
        service = new ExperimentReportService(backendClient, renderer, storageClient, objectMapper, properties);
    }

    @Test
    void shouldSkipProcessingWhenDisabled() {
        properties.setEnabled(false);
        service.processPendingRequests();
        verifyNoInteractions(backendClient, storageClient);
    }

    @Test
    void shouldProcessPendingRequestAndMarkReady() throws Exception {
        ExperimentReportRequestDetailDto request = buildRequest(1L, 9L);
        when(backendClient.fetchPendingRequests()).thenReturn(List.of(request));
        when(backendClient.updateStatus(eq(1L), eq(ExperimentReportStatus.PROCESSING), isNull(), isNull()))
                .thenReturn(request);
        when(storageClient.upload(any(), anyString(), anyString()))
                .thenReturn(new ExperimentReportStorageClient.StoredReport("reports/file.html", "https://files/report.html"));
        when(backendClient.updateStatus(eq(1L), eq(ExperimentReportStatus.READY), anyString(), isNull()))
                .thenReturn(request);

        service.processPendingRequests();

        verify(backendClient).updateStatus(eq(1L), eq(ExperimentReportStatus.PROCESSING), isNull(), isNull());
        verify(backendClient).updateStatus(eq(1L), eq(ExperimentReportStatus.READY), anyString(), isNull());
        verify(backendClient, never()).updateStatus(eq(1L), eq(ExperimentReportStatus.FAILED), anyString(), anyString());
    }

    @Test
    void shouldMarkFailedWhenUploadFails() throws Exception {
        ExperimentReportRequestDetailDto request = buildRequest(2L, 9L);
        when(backendClient.fetchPendingRequests()).thenReturn(List.of(request));
        when(backendClient.updateStatus(eq(2L), eq(ExperimentReportStatus.PROCESSING), isNull(), isNull()))
                .thenReturn(request);
        when(storageClient.upload(any(), anyString(), anyString()))
                .thenThrow(new RuntimeException("upload error"));

        service.processPendingRequests();

        verify(backendClient).updateStatus(eq(2L), eq(ExperimentReportStatus.FAILED), isNull(), anyString());
    }

    private ExperimentReportRequestDetailDto buildRequest(long requestId, long experimentId) throws Exception {
        ExperimentReportMaterialDto material = ExperimentReportMaterialDto.builder()
                .experiment(ExperimentReportMaterialDto.ExperimentSnapshot.builder()
                        .id(experimentId)
                        .name("Teste")
                        .dailyBudget(new BigDecimal("50"))
                        .build())
                .build();
        ExperimentReportRequestDetailDto request = new ExperimentReportRequestDetailDto();
        request.setId(requestId);
        request.setExperimentId(experimentId);
        request.setRequestedAt(Instant.now());
        request.setPayloadSnapshot(objectMapper.writeValueAsString(material));
        return request;
    }
}
