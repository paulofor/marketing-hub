package com.marketinghub.codextelemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.marketinghub.repository.jpa.codextelemetry.CodexAgentExecutionTelemetryRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar contadores e ausência de estimativa na telemetria Codex. */
class CodexAgentExecutionTelemetryServiceTest {
  /** Preserva tokens nulos e mantém contadores monotônicos entre heartbeats. */
  @Test
  void shouldKeepTelemetryMonotonicWithoutInventingTokens() {
    CodexAgentExecutionTelemetryRepository repository =
        mock(CodexAgentExecutionTelemetryRepository.class);
    when(repository.findByAgentTypeAndExecutionId("CUSTOMER_AGENT", 1L))
        .thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    CodexAgentExecutionTelemetryService service =
        new CodexAgentExecutionTelemetryService(repository);

    var response =
        service.heartbeat(
            "CUSTOMER_AGENT",
            1L,
            new CodexAgentExecutionTelemetryService.HeartbeatRequest(
                22L, true, 4L, 180L, null, null, "OUTPUT"));

    assertThat(response.processAlive()).isTrue();
    assertThat(response.eventCount()).isEqualTo(4L);
    assertThat(response.outputBytes()).isEqualTo(180L);
    assertThat(response.inputTokens()).isNull();
    assertThat(response.outputTokens()).isNull();
  }
}
