package com.marketinghub.facebookadsworker.facebookplaybook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExperimentAdSetPlaybookServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ExperimentAdSetPlaybookClient client;
    private FacebookAdsService facebookAdsService;
    private ExperimentAdSetPlaybookService service;

    @BeforeEach
    void setUp() {
        client = mock(ExperimentAdSetPlaybookClient.class);
        facebookAdsService = mock(FacebookAdsService.class);
        service = new ExperimentAdSetPlaybookService(client, facebookAdsService, objectMapper, "worker-test");
    }

    @Test
    void processQueueLogsFallbackValidationEndpointAndSnakeCasePayloadWhenFacebookCallFailsBeforeHttp() throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("adAccountId", "939323521124952");
        payload.set("targetingSpec", objectMapper.readTree("{\"age_min\":18}"));

        PlaybookJob job = new PlaybookJob(
                42L,
                PlaybookJobType.FACEBOOK_VALIDATE_SPEC,
                1L,
                2L,
                payload,
                Instant.now());

        when(client.claimJobs("worker-test", 5)).thenReturn(List.of(job));
        doThrow(new RuntimeException("boom"))
                .when(facebookAdsService)
                .validateTargetingSpec(any(FacebookAdsService.TargetingValidationRequest.class));

        service.processQueue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ExperimentAdSetPlaybookClient.ApiCallPayload>> apiCallsCaptor = ArgumentCaptor.forClass(List.class);
        verify(client).failJob(eq(42L), eq("boom"), apiCallsCaptor.capture());

        List<ExperimentAdSetPlaybookClient.ApiCallPayload> apiCalls = apiCallsCaptor.getValue();
        assertNotNull(apiCalls);
        assertEquals(1, apiCalls.size());
        ExperimentAdSetPlaybookClient.ApiCallPayload apiCall = apiCalls.get(0);
        assertEquals("/act_939323521124952/targetingvalidation", apiCall.endpoint());
        JsonNode requestPayload = apiCall.requestPayload();
        assertNotNull(requestPayload);
        assertEquals(18, requestPayload.path("targeting_spec").path("age_min").asInt());
    }

    @Test
    void processQueueLogsFallbackReachEstimateEndpointAndSnakeCasePayloadWhenFacebookCallFailsBeforeHttp() throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("adAccountId", "act_123");
        payload.set("targetingSpec", objectMapper.readTree("{\"age_max\":45}"));

        PlaybookJob job = new PlaybookJob(
                99L,
                PlaybookJobType.FACEBOOK_REACH_ESTIMATE,
                1L,
                2L,
                payload,
                Instant.now());

        when(client.claimJobs("worker-test", 5)).thenReturn(List.of(job));
        doThrow(new RuntimeException("reach-error"))
                .when(facebookAdsService)
                .estimateReach(any(FacebookAdsService.ReachEstimateRequest.class));

        service.processQueue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ExperimentAdSetPlaybookClient.ApiCallPayload>> apiCallsCaptor = ArgumentCaptor.forClass(List.class);
        verify(client).failJob(eq(99L), eq("reach-error"), apiCallsCaptor.capture());

        List<ExperimentAdSetPlaybookClient.ApiCallPayload> apiCalls = apiCallsCaptor.getValue();
        assertNotNull(apiCalls);
        assertEquals(1, apiCalls.size());
        ExperimentAdSetPlaybookClient.ApiCallPayload apiCall = apiCalls.get(0);
        assertEquals("/act_123/reachestimate", apiCall.endpoint());
        JsonNode requestPayload = apiCall.requestPayload();
        assertNotNull(requestPayload);
        assertEquals(45, requestPayload.path("targeting_spec").path("age_max").asInt());
    }
}
