package com.marketinghub.agentmonitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.agent.Agent;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agent.CodexAuthReconnectRepository;
import com.marketinghub.repository.jpa.agentmonitor.AgentExecutorAdminOperationRepository;
import com.marketinghub.repository.jpa.agentmonitor.AgentExecutorHealthCheckRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: proteger a classificação central da prontidão dos executores dos agentes. */
class AgentExecutorHealthServiceTest {
  /** Cria comando de atualização sem executar Docker dentro do backend. */
  @Test
  void shouldCreateAuditableExecutorUpdateCommand() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentExecutorHealthCheckRepository checks = mock(AgentExecutorHealthCheckRepository.class);
    CodexAuthReconnectRepository reconnects = mock(CodexAuthReconnectRepository.class);
    AgentExecutorAdminOperationRepository operations =
        mock(AgentExecutorAdminOperationRepository.class);
    Agent agent = Agent.builder().id(3L).agentKey("financial-agent").nickname("Plutus").build();
    when(agents.findById(3L)).thenReturn(Optional.of(agent));
    when(operations.existsByAgentIdAndStatusIn(
            org.mockito.ArgumentMatchers.eq(3L), org.mockito.ArgumentMatchers.anyList()))
        .thenReturn(false);
    when(operations.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    AgentExecutorHealthService service =
        new AgentExecutorHealthService(
            agents,
            checks,
            reconnects,
            operations,
            Clock.fixed(Instant.parse("2026-08-13T04:00:00Z"), ZoneOffset.UTC));

    AgentExecutorAdminOperationResponse response =
        service.requestOperation(3L, "update", "operador");

    assertThat(response.agentKey()).isEqualTo("financial-agent");
    assertThat(response.operationType()).isEqualTo("UPDATE");
    assertThat(response.status()).isEqualTo("REQUESTED");
  }

  /** Persiste solicitação sem qualquer token e impede duplicação concorrente. */
  @Test
  void shouldCreateAuditableReconnectWithoutCredentials() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentExecutorHealthCheckRepository checks = mock(AgentExecutorHealthCheckRepository.class);
    CodexAuthReconnectRepository reconnects = mock(CodexAuthReconnectRepository.class);
    Agent agent = Agent.builder().id(7L).agentKey("landing-generator").nickname("Dédalo").build();
    when(agents.findById(7L)).thenReturn(Optional.of(agent));
    when(reconnects.existsByAgentIdAndStatusIn(
            org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.anyList()))
        .thenReturn(false);
    when(reconnects.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    AgentExecutorHealthService service = new AgentExecutorHealthService(agents, checks, reconnects);

    CodexAuthReconnectResponse response = service.requestReconnect(7L, "operador");

    assertThat(response.status()).isEqualTo("REQUESTED");
    assertThat(response.verificationUrl()).isNull();
    assertThat(response.userCode()).isNull();
  }

  /** Permite que outro executor Codex crie sua própria sessão sem depender de Dédalo. */
  @Test
  void shouldCreateIndependentReconnectForEveryCodexExecutor() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentExecutorHealthCheckRepository checks = mock(AgentExecutorHealthCheckRepository.class);
    CodexAuthReconnectRepository reconnects = mock(CodexAuthReconnectRepository.class);
    Agent agent = Agent.builder().id(3L).agentKey("financial-agent").nickname("Plutus").build();
    when(agents.findById(3L)).thenReturn(Optional.of(agent));
    when(reconnects.existsByAgentIdAndStatusIn(
            org.mockito.ArgumentMatchers.eq(3L), org.mockito.ArgumentMatchers.anyList()))
        .thenReturn(false);
    when(reconnects.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CodexAuthReconnectResponse response =
        new AgentExecutorHealthService(agents, checks, reconnects).requestReconnect(3L, "operador");

    assertThat(response.agentKey()).isEqualTo("financial-agent");
    assertThat(response.status()).isEqualTo("REQUESTED");
  }

  /** Permite que Apolo crie a sessão Codex usada pelo planejador audiovisual híbrido. */
  @Test
  void shouldAllowApolloCodexReconnect() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentExecutorHealthCheckRepository checks = mock(AgentExecutorHealthCheckRepository.class);
    CodexAuthReconnectRepository reconnects = mock(CodexAuthReconnectRepository.class);
    Agent apollo = Agent.builder().id(2L).agentKey("videomaker").nickname("Apolo").build();
    when(agents.findById(2L)).thenReturn(Optional.of(apollo));
    when(reconnects.existsByAgentIdAndStatusIn(
            org.mockito.ArgumentMatchers.eq(2L), org.mockito.ArgumentMatchers.anyList()))
        .thenReturn(false);
    when(reconnects.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CodexAuthReconnectResponse response =
        new AgentExecutorHealthService(agents, checks, reconnects).requestReconnect(2L, "operador");

    assertThat(response.agentKey()).isEqualTo("videomaker");
  }

  /** Permite que Argos crie uma sessão Codex sem receber credenciais dos marketplaces. */
  @Test
  void shouldAllowMarketRadarCodexReconnect() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentExecutorHealthCheckRepository checks = mock(AgentExecutorHealthCheckRepository.class);
    CodexAuthReconnectRepository reconnects = mock(CodexAuthReconnectRepository.class);
    Agent argos = Agent.builder().id(11L).agentKey("market-radar").nickname("Argos").build();
    when(agents.findById(11L)).thenReturn(Optional.of(argos));
    when(reconnects.existsByAgentIdAndStatusIn(
            org.mockito.ArgumentMatchers.eq(11L), org.mockito.ArgumentMatchers.anyList()))
        .thenReturn(false);
    when(reconnects.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CodexAuthReconnectResponse response =
        new AgentExecutorHealthService(agents, checks, reconnects)
            .requestReconnect(11L, "operador");

    assertThat(response.agentKey()).isEqualTo("market-radar");
  }

  /** Permite que Íris use sessão Codex exclusiva como os demais executores premium. */
  @Test
  void shouldAllowIrisCodexReconnect() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentExecutorHealthCheckRepository checks = mock(AgentExecutorHealthCheckRepository.class);
    CodexAuthReconnectRepository reconnects = mock(CodexAuthReconnectRepository.class);
    Agent iris =
        Agent.builder().id(12L).agentKey("communication-director").nickname("Íris").build();
    when(agents.findById(12L)).thenReturn(Optional.of(iris));
    when(reconnects.existsByAgentIdAndStatusIn(
            org.mockito.ArgumentMatchers.eq(12L), org.mockito.ArgumentMatchers.anyList()))
        .thenReturn(false);
    when(reconnects.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CodexAuthReconnectResponse response =
        new AgentExecutorHealthService(agents, checks, reconnects)
            .requestReconnect(12L, "operador");

    assertThat(response.agentKey()).isEqualTo("communication-director");
    assertThat(response.status()).isEqualTo("REQUESTED");
  }

  /** Aprova o executor técnico vigente mesmo depois de uma edição do cadastro do agente. */
  @Test
  void shouldReportReadyOnlyWithAllThreeSignals() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentExecutorHealthCheckRepository checks = mock(AgentExecutorHealthCheckRepository.class);
    Agent agent =
        Agent.builder().agentKey("landing-generator").currentVersion(5).nickname("Dédalo").build();
    when(agents.findByAgentKey("landing-generator")).thenReturn(Optional.of(agent));
    when(checks.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    AgentExecutorHealthService service =
        new AgentExecutorHealthService(
            agents, checks, Clock.fixed(Instant.parse("2026-08-12T12:00:00Z"), ZoneOffset.UTC));

    AgentExecutorHealthResponse result =
        service.report(
            new AgentExecutorHealthReportRequest(
                "landing-generator", 4, "abc123", true, true, "Executor pronto."));

    assertThat(result.status()).isEqualTo("READY");
    assertThat(result.versionCurrent()).isTrue();
    assertThat(result.backendAccessible()).isTrue();
    assertThat(result.codexAuthenticated()).isTrue();
    assertThat(result.expectedVersion()).isEqualTo(4);
  }

  /** Bloqueia uma imagem antiga mesmo quando rede e autenticação funcionam. */
  @Test
  void shouldBlockOutdatedExecutorVersion() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentExecutorHealthCheckRepository checks = mock(AgentExecutorHealthCheckRepository.class);
    Agent agent =
        Agent.builder().agentKey("videomaker").currentVersion(2).nickname("Apolo").build();
    when(agents.findByAgentKey("videomaker")).thenReturn(Optional.of(agent));
    when(checks.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    AgentExecutorHealthService service = new AgentExecutorHealthService(agents, checks);

    AgentExecutorHealthResponse result =
        service.report(
            new AgentExecutorHealthReportRequest(
                "videomaker", 1, "old", true, true, "Imagem anterior."));

    assertThat(result.status()).isEqualTo("BLOCKED");
    assertThat(result.versionCurrent()).isFalse();
  }

  /** Invalida uma prova antiga para não manter um falso estado saudável. */
  @Test
  void shouldExpireStaleHealthProof() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentExecutorHealthCheckRepository checks = mock(AgentExecutorHealthCheckRepository.class);
    Agent agent =
        Agent.builder().agentKey("meta-ad-approver").currentVersion(1).nickname("Têmis").build();
    AgentExecutorHealthCheck old =
        new AgentExecutorHealthCheck(
            agent,
            1,
            "abc",
            true,
            true,
            "READY",
            "Executor pronto.",
            Instant.parse("2026-08-12T11:40:00Z"));
    when(checks.findTopByAgentAgentKeyOrderByCheckedAtDesc("meta-ad-approver"))
        .thenReturn(Optional.of(old));
    AgentExecutorHealthService service =
        new AgentExecutorHealthService(
            agents, checks, Clock.fixed(Instant.parse("2026-08-12T12:00:00Z"), ZoneOffset.UTC));

    AgentExecutorHealthResponse result = service.current(agent);

    assertThat(result.status()).isEqualTo("UNKNOWN");
    assertThat(result.detail()).contains("vencida");
  }

  /**
   * Preserva prontidão após curadoria do cadastro, mas recusa versão técnica antiga ou falta de
   * autenticação.
   */
  @Test
  void shouldSeparateCuratedDefinitionFromRuntimeCompatibility() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentExecutorHealthCheckRepository checks = mock(AgentExecutorHealthCheckRepository.class);
    Agent psique = Agent.builder().agentKey("customer-agent").currentVersion(7).build();
    when(agents.findByAgentKey("customer-agent")).thenReturn(Optional.of(psique));
    when(checks.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    AgentExecutorHealthService service = new AgentExecutorHealthService(agents, checks);
    for (int definition : new int[] {7, 8, 9}) {
      psique.setCurrentVersion(definition);
      var healthy =
          service.report(
              new AgentExecutorHealthReportRequest(
                  "customer-agent", 6, "build-testado", true, true, "Executor pronto."));
      assertThat(healthy.status()).isEqualTo("READY");
      assertThat(healthy.expectedVersion()).isEqualTo(6);
      assertThat(psique.getCurrentVersion()).isEqualTo(definition);
    }
    var outdated =
        service.report(
            new AgentExecutorHealthReportRequest(
                "customer-agent", 5, "build-antigo", true, true, "Versão anterior."));
    assertThat(outdated.versionCurrent()).isFalse();
    assertThat(outdated.status()).isEqualTo("BLOCKED");
    var unauthenticated =
        service.report(
            new AgentExecutorHealthReportRequest(
                "customer-agent", 6, "build-testado", true, false, "Sessão indisponível."));
    assertThat(unauthenticated.status()).isEqualTo("BLOCKED");
    var disconnected =
        service.report(
            new AgentExecutorHealthReportRequest(
                "customer-agent", 6, "build-testado", false, true, "Backend indisponível."));
    assertThat(disconnected.status()).isEqualTo("BLOCKED");
  }

  /**
   * Recalcula a leitura pelo manifesto vigente sem manter aprovação obsoleta ou bloqueio do
   * cadastro.
   */
  @Test
  void shouldRevalidateStoredHealthAgainstRuntimeManifest() {
    AgentRepository agents = mock(AgentRepository.class);
    AgentExecutorHealthCheckRepository checks = mock(AgentExecutorHealthCheckRepository.class);
    var now = Instant.parse("2026-09-10T04:00:00Z");
    Agent psique = Agent.builder().agentKey("customer-agent").currentVersion(7).build();
    AgentExecutorHealthService service =
        new AgentExecutorHealthService(agents, checks, Clock.fixed(now, ZoneOffset.UTC));
    when(checks.findTopByAgentAgentKeyOrderByCheckedAtDesc("customer-agent"))
        .thenReturn(
            Optional.of(
                new AgentExecutorHealthCheck(
                    psique, 6, "atual", true, true, "BLOCKED", "Cadastro alterado.", now)));
    assertThat(service.current(psique).status()).isEqualTo("READY");
    when(checks.findTopByAgentAgentKeyOrderByCheckedAtDesc("customer-agent"))
        .thenReturn(
            Optional.of(
                new AgentExecutorHealthCheck(
                    psique, 5, "antigo", true, true, "READY", "Prova antiga.", now)));
    assertThat(service.current(psique).status()).isEqualTo("BLOCKED");
  }

  /**
   * Exercita o callback HTTP real do worker e confirma o registro sem alterar o cadastro curado.
   */
  @Test
  void shouldAcceptActualReporterContractAfterCuration() throws Exception {
    AgentRepository agents = mock(AgentRepository.class);
    AgentExecutorHealthCheckRepository checks = mock(AgentExecutorHealthCheckRepository.class);
    Agent psique = Agent.builder().agentKey("customer-agent").currentVersion(7).build();
    when(agents.findByAgentKey("customer-agent")).thenReturn(Optional.of(psique));
    when(checks.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    var service = new AgentExecutorHealthService(agents, checks);
    var mvc =
        MockMvcBuilders.standaloneSetup(new AgentExecutorHealthController(service, null)).build();
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/api/internal/agents/executor-health")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {"agentKey":"customer-agent","deployedVersion":6,"buildReference":"test-local",
                 "backendAccessible":true,"codexAuthenticated":true,"detail":"Executor pronto."}
                """))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status")
                .value("READY"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$.expectedVersion")
                .value(6));
    org.mockito.Mockito.verify(checks)
        .save(
            org.mockito.ArgumentMatchers.argThat(
                check -> check.getDeployedVersion() == 6 && check.getStatus().equals("READY")));
    assertThat(psique.getCurrentVersion()).isEqualTo(7);
  }
}
