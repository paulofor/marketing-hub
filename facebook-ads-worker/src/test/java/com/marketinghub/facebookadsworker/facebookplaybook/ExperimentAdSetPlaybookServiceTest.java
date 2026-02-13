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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    @Test
    void processQueueUsesTargetingSearchByTypeForDiscoveryStep() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("adAccountId", "act_1234567890");
        payload.put("locale", "pt_BR");
        payload.put("country", "BR");
        payload.put("limit", 10);
        payload.putArray("interestQueries").add("escola particular");
        payload.putArray("behaviorQueries").add("education");
        payload.putArray("workPositionQueries").add("school principal");

        PlaybookJob job = new PlaybookJob(
                77L,
                PlaybookJobType.FACEBOOK_SEED_LOOKUP,
                1L,
                2L,
                payload,
                Instant.now());

        when(client.claimJobs("worker-test", 5)).thenReturn(List.of(job));
        when(facebookAdsService.searchTargetingOptions(any(FacebookAdsService.TargetingSearchRequest.class)))
                .thenAnswer(invocation -> {
                    FacebookAdsService.TargetingSearchRequest request = invocation.getArgument(0);
                    if (request.type() == FacebookAdsService.TargetingSearchType.AD_INTEREST) {
                        return List.of(new FacebookAdsService.FacebookTargetingSearchResult(
                                "6003139266461",
                                "Escola particular",
                                "adinterest",
                                "Education",
                                1000L,
                                2000L,
                                List.of("Interests")));
                    }
                    return List.of();
                });

        service.processQueue();

        ArgumentCaptor<FacebookAdsService.TargetingSearchRequest> requestCaptor = ArgumentCaptor.forClass(FacebookAdsService.TargetingSearchRequest.class);
        verify(facebookAdsService, atLeastOnce()).searchTargetingOptions(requestCaptor.capture());
        List<FacebookAdsService.TargetingSearchRequest> requests = requestCaptor.getAllValues();
        assertTrue(requests.stream().anyMatch(request -> request.type() == FacebookAdsService.TargetingSearchType.AD_INTEREST));
        assertTrue(requests.stream().anyMatch(request -> request.type() == FacebookAdsService.TargetingSearchType.AD_BEHAVIOR));
        assertTrue(requests.stream().anyMatch(request -> request.type() == FacebookAdsService.TargetingSearchType.AD_WORK_POSITION));
        assertTrue(requests.stream().noneMatch(request -> request.type() == FacebookAdsService.TargetingSearchType.ANY));
        verify(facebookAdsService, never()).searchGlobalTargetingOptions(any(FacebookAdsService.TargetingSearchRequest.class));
    }

}
