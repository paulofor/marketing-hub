package com.marketinghub.landinggeneratoragent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Responsabilidade: proteger o contrato HTTP versionado usado por Dédalo. */
class LandingGeneratorBackendClientTest {
  /** Envia a identidade do deploy para permitir reconciliação segura da homologação. */
  @Test
  void shouldIdentifyCurrentBuildWhenClaimingPendingWork() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    LandingGeneratorAgentProperties properties = new LandingGeneratorAgentProperties();
    properties.setBackendUrl("http://backend.test");
    properties.setBuildReference("sha-4909");
    server
        .expect(
            requestTo(
                "http://backend.test/api/internal/geralanding/agent/v1/stage-executions/process-tasks/pending/activation"))
        .andRespond(withSuccess());
    server
        .expect(
            requestTo(
                "http://backend.test/api/internal/geralanding/agent/v1/stage-executions/pending?limit=1"))
        .andExpect(header("X-Agent-Build-Reference", "sha-4909"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    var result = new LandingGeneratorBackendClient(properties, builder).claimPending();

    assertThat(result).isEmpty();
    server.verify();
  }

  /** Materializa a tarefa BPM antes de reservar sua execução técnica. */
  @Test
  void shouldActivateClaimedBpmTaskBeforeTechnicalQueue() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    LandingGeneratorAgentProperties properties = new LandingGeneratorAgentProperties();
    properties.setBackendUrl("http://backend.test");
    server
        .expect(
            requestTo(
                "http://backend.test/api/internal/geralanding/agent/v1/stage-executions/process-tasks/pending/activation"))
        .andRespond(withSuccess());
    server
        .expect(
            requestTo(
                "http://backend.test/api/internal/geralanding/agent/v1/stage-executions/pending?limit=1"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    new LandingGeneratorBackendClient(properties, builder).claimPending();

    server.verify();
  }
}
