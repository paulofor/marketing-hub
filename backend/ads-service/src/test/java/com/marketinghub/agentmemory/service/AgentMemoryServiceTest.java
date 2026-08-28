package com.marketinghub.agentmemory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.marketinghub.agentmemory.PremiumAgentMemory;
import com.marketinghub.agentmemory.service.registerMemory.RegisterMemoryRequest;
import com.marketinghub.repository.jpa.agentmemory.PremiumAgentMemoryFeedbackRepository;
import com.marketinghub.repository.jpa.agentmemory.PremiumAgentMemoryRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

/** Valida segregacao, deduplicacao e limite da memoria premium. */
class AgentMemoryServiceTest {
  private final PremiumAgentMemoryRepository repository = mock(PremiumAgentMemoryRepository.class);
  private final PremiumAgentMemoryFeedbackRepository feedbackRepository =
      mock(PremiumAgentMemoryFeedbackRepository.class);
  private final Instant now = Instant.parse("2026-08-09T12:00:00Z");
  private final AgentMemoryService service =
      new AgentMemoryService(repository, feedbackRepository, Clock.fixed(now, ZoneOffset.UTC));

  /** Confirma que todo aprendizado criado pelo proprio agente nasce candidato. */
  @Test
  void registersOnlyCandidateMemory() {
    when(repository.findByAgentKeyAndTenantKeyAndScopeTypeAndScopeIdAndContentSha256(
            any(), any(), any(), any(), any()))
        .thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    var result = service.register("meta-ad-approver", request());
    assertThat(result.status()).isEqualTo("CANDIDATE");
    ArgumentCaptor<PremiumAgentMemory> captor = ArgumentCaptor.forClass(PremiumAgentMemory.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getAgentKey()).isEqualTo("meta-ad-approver");
  }

  /** Confirma que Apolo pode registrar hipótese sem se autoaprovar. */
  @Test
  void registersApolloLearningOnlyAsCandidate() {
    when(repository.findByAgentKeyAndTenantKeyAndScopeTypeAndScopeIdAndContentSha256(
            any(), any(), any(), any(), any()))
        .thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    assertThat(service.register("apollo", request()).status()).isEqualTo("CANDIDATE");
    verify(repository)
        .save(
            argThat(
                value ->
                    "apollo".equals(value.getAgentKey()) && "CANDIDATE".equals(value.getStatus())));
  }

  /** Confirma que Íris aprende de comunicação sem promover a própria hipótese. */
  @Test
  void registersIrisLearningOnlyAsCandidate() {
    when(repository.findByAgentKeyAndTenantKeyAndScopeTypeAndScopeIdAndContentSha256(
            any(), any(), any(), any(), any()))
        .thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    assertThat(service.register("communication-director", request()).status())
        .isEqualTo("CANDIDATE");
    verify(repository)
        .save(
            argThat(
                value ->
                    "communication-director".equals(value.getAgentKey())
                        && "CANDIDATE".equals(value.getStatus())));
  }

  /** Confirma que recuperacao fixa agente, escopo e teto de doze itens. */
  @Test
  void retrievesOnlyBoundedScopedMemory() {
    when(repository.retrieve(
            eq("financial-agent"),
            eq("__GLOBAL__"),
            eq("COMMERCIAL_PLAN"),
            eq("1"),
            eq(now),
            any(Pageable.class)))
        .thenReturn(List.of());
    assertThat(service.retrieve("financial-agent", null, "COMMERCIAL_PLAN", "1", 999)).isEmpty();
    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
    verify(repository)
        .retrieve(
            eq("financial-agent"),
            eq("__GLOBAL__"),
            eq("COMMERCIAL_PLAN"),
            eq("1"),
            eq(now),
            captor.capture());
    assertThat(captor.getValue().getPageSize()).isEqualTo(12);
  }

  /** Monta uma solicitacao valida com evidencia da execucao. */
  private RegisterMemoryRequest request() {
    return new RegisterMemoryRequest(
        null,
        "EXPERIMENT",
        "88",
        "copy",
        "Promessa especifica superou a generica",
        "Resultado observado no criativo 273",
        "/experiments/88",
        "273",
        new BigDecimal("0.7000"),
        null);
  }
}
