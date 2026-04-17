package com.marketinghub.mds.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mds.MdsRequest;
import com.marketinghub.mds.MdsRequestStatus;
import com.marketinghub.mds.dto.MdsCompleteRequest;
import com.marketinghub.mds.dto.MdsFailRequest;
import com.marketinghub.mds.dto.MdsHeartbeatRequest;
import com.marketinghub.mds.repository.MdsProcessingEventRepository;
import com.marketinghub.mds.repository.MdsRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MdsRequestServiceTest {

    @Mock
    private MdsRequestRepository requestRepository;

    @Mock
    private MdsProcessingEventRepository processingEventRepository;

    private MdsRequestService service;

    @BeforeEach
    void setUp() {
        service = new MdsRequestService(requestRepository, processingEventRepository, new ObjectMapper());
    }

    @Test
    void shouldRejectHeartbeatWhenRequestIsNotInProgress() {
        when(requestRepository.findById(11L)).thenReturn(Optional.of(requestWithStatus(MdsRequestStatus.PENDING)));

        assertThatThrownBy(() -> service.heartbeat(11L, new MdsHeartbeatRequest("pipeline", "tick", Map.of())))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode.value")
                .isEqualTo(409);
    }

    @Test
    void shouldCompleteInProgressRequest() {
        MdsRequest request = requestWithStatus(MdsRequestStatus.IN_PROGRESS);
        when(requestRepository.findById(11L)).thenReturn(Optional.of(request));

        var response = service.complete(11L, new MdsCompleteRequest("done"));

        assertThat(response.status()).isEqualTo(MdsRequestStatus.COMPLETED);
        assertThat(request.getFinishedAt()).isNotNull();
        verify(processingEventRepository).save(any());
    }

    @Test
    void shouldFailInProgressRequest() {
        MdsRequest request = requestWithStatus(MdsRequestStatus.IN_PROGRESS);
        when(requestRepository.findById(11L)).thenReturn(Optional.of(request));

        var response = service.fail(11L, new MdsFailRequest("network timeout", "pipeline", "error"));

        assertThat(response.status()).isEqualTo(MdsRequestStatus.FAILED);
        assertThat(response.failureReason()).isEqualTo("network timeout");
        verify(processingEventRepository).save(any());
    }

    private MdsRequest requestWithStatus(MdsRequestStatus status) {
        return MdsRequest.builder()
                .id(11L)
                .status(status)
                .market("weight-loss")
                .problem("plateau")
                .desiredOutcome("consistent fat loss")
                .contextJson("{}")
                .correlationId("corr-11")
                .build();
    }
}
