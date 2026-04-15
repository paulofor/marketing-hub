package com.marketinghub.oprm.integration.contract;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.oprm.integration.client.BackendArtifactPublishClient;
import com.marketinghub.oprm.integration.client.BackendHeartbeatClient;
import com.marketinghub.oprm.integration.client.BackendJobClient;
import com.marketinghub.oprm.integration.client.BackendStatusClient;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

@SpringBootTest(properties = {
        "oprm.backend.base-url=http://localhost:${wiremock.server.port}",
        "spring.task.scheduling.enabled=false",
        "oprm.jobs.polling-enabled=false"
})
@AutoConfigureWireMock(port = 0)
class OprmBackendContractTest {
    @Autowired
    private BackendJobClient backendJobClient;
    @Autowired
    private BackendStatusClient backendStatusClient;
    @Autowired
    private BackendArtifactPublishClient backendArtifactPublishClient;
    @Autowired
    private BackendHeartbeatClient backendHeartbeatClient;

    @Test
    void shouldClaimAndGetDetailUsingContractPayload() {
        stubFor(post(urlEqualTo("/api/oprm/jobs/claim"))
                .withRequestBody(equalToJson("""
                        {
                          "workerId":"oprm-worker-test",
                          "workerVersion":"0.1.0",
                          "contractVersion":"1.0",
                          "maxJobs":1,
                          "leaseSeconds":120
                        }
                        """))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "jobId":"8f4fd44f-29e5-4d6c-a3d7-7d1ebf5f2d5b",
                                  "jobType":"OCCUPATION_MAPPING",
                                  "occupationSeedRef":"personal-trainer",
                                  "correlationId":"corr-001",
                                  "parameters":{},
                                  "claimedAt":"2026-04-15T12:00:00Z",
                                  "leaseExpiresAt":"2026-04-15T12:02:00Z"
                                }
                                """)));
        stubFor(get(urlEqualTo("/api/oprm/jobs/8f4fd44f-29e5-4d6c-a3d7-7d1ebf5f2d5b"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "jobId":"8f4fd44f-29e5-4d6c-a3d7-7d1ebf5f2d5b",
                                  "jobType":"OCCUPATION_MAPPING",
                                  "jobStatus":"CLAIMED",
                                  "occupationSeedRef":"personal-trainer",
                                  "correlationId":"corr-001",
                                  "attemptCount":1,
                                  "createdAt":"2026-04-15T11:59:00Z",
                                  "claimedAt":"2026-04-15T12:00:00Z",
                                  "startedAt":null,
                                  "finishedAt":null,
                                  "parameters":{},
                                  "inputRefs":[],
                                  "errorCode":null,
                                  "errorMessage":null
                                }
                                """)));

        Optional<OprmJobClaimResponse> claimed = backendJobClient.claimNextJob(
                new OprmJobClaimRequest("oprm-worker-test", "0.1.0", "1.0", 1, 120));

        assertThat(claimed).isPresent();
        assertThat(claimed.get().jobType()).isEqualTo(OprmJobType.OCCUPATION_MAPPING);
        OprmJobDetailResponse detail = backendJobClient.getJobDetail(claimed.get().jobId());
        assertThat(detail.jobStatus()).isEqualTo(OprmJobStatus.CLAIMED);
        assertThat(detail.correlationId()).isEqualTo("corr-001");
    }

    @Test
    void shouldPublishStatusArtifactAndHeartbeatUsingContractEndpoints() {
        String jobId = "8f4fd44f-29e5-4d6c-a3d7-7d1ebf5f2d5b";
        stubFor(post(urlEqualTo("/api/oprm/jobs/" + jobId + "/status"))
                .withRequestBody(equalToJson("""
                        {
                          "workerId":"oprm-worker-test",
                          "status":"RUNNING",
                          "occurredAt":"2026-04-15T12:01:00Z",
                          "phase":"phase-run",
                          "message":"started",
                          "errorCode":null,
                          "errorMessage":null,
                          "metrics":{"oprm.loop.duration.ms":120}
                        }
                        """))
                .willReturn(aResponse().withStatus(202)));
        stubFor(post(urlEqualTo("/api/oprm/artifacts"))
                .withRequestBody(equalToJson("""
                        {
                          "jobId":"8f4fd44f-29e5-4d6c-a3d7-7d1ebf5f2d5b",
                          "correlationId":"corr-001",
                          "artifact":{
                            "artifactType":"occupationProfileSnapshot",
                            "artifactVersion":"1.0",
                            "artifactId":"artifact-001",
                            "moduleName":"oprm",
                            "producer":"oprm-worker",
                            "createdAt":"2026-04-15T12:01:10Z",
                            "correlationId":"corr-001",
                            "traceId":"trace-001",
                            "sourceRefs":["seed:occupation"],
                            "inputRefs":[],
                            "payload":{"occupation":"personal-trainer"},
                            "status":"PUBLISHED",
                            "confidenceScore":0.92,
                            "metadata":{"phase":"phase1"}
                          },
                          "lineage":{"sourceRefs":["seed:occupation"]},
                          "idempotencyKey":"8f4fd44f-29e5-4d6c-a3d7-7d1ebf5f2d5b:artifact-001"
                        }
                        """))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "artifactId":"artifact-001",
                                  "artifactType":"occupationProfileSnapshot",
                                  "artifactVersion":"1.0",
                                  "persistedAt":"2026-04-15T12:01:11Z",
                                  "status":"PUBLISHED",
                                  "duplicated":false
                                }
                                """)));
        stubFor(post(urlEqualTo("/api/oprm/heartbeat"))
                .withRequestBody(equalToJson("""
                        {
                          "workerId":"oprm-worker-test",
                          "workerVersion":"0.1.0",
                          "contractVersion":"1.0",
                          "sentAt":"2026-04-15T12:02:00Z",
                          "health":{"running":true},
                          "counters":{"jobsClaimed":1}
                        }
                        """))
                .willReturn(aResponse().withStatus(202)));

        backendStatusClient.updateStatus(jobId, new OprmJobStatusUpdateRequest(
                "oprm-worker-test",
                OprmJobStatus.RUNNING,
                "2026-04-15T12:01:00Z",
                "phase-run",
                "started",
                null,
                null,
                Map.of("oprm.loop.duration.ms", 120)
        ));

        OprmArtifactPublishResponse publishResponse = backendArtifactPublishClient.publish(
                new OprmArtifactPublishRequest(
                        jobId,
                        "corr-001",
                        new OprmArtifactEnvelopeDto(
                                "occupationProfileSnapshot",
                                "1.0",
                                "artifact-001",
                                "oprm",
                                "oprm-worker",
                                "2026-04-15T12:01:10Z",
                                "corr-001",
                                "trace-001",
                                java.util.List.of("seed:occupation"),
                                java.util.List.of(),
                                Map.of("occupation", "personal-trainer"),
                                OprmArtifactStatus.PUBLISHED,
                                0.92,
                                Map.of("phase", "phase1")
                        ),
                        Map.of("sourceRefs", java.util.List.of("seed:occupation")),
                        jobId + ":artifact-001"
                )
        );
        backendHeartbeatClient.publish(new OprmHeartbeatRequest(
                "oprm-worker-test",
                "0.1.0",
                "1.0",
                "2026-04-15T12:02:00Z",
                Map.of("running", true),
                Map.of("jobsClaimed", 1)
        ));

        assertThat(publishResponse).isNotNull();
        assertThat(publishResponse.artifactId()).isEqualTo("artifact-001");
    }
}
