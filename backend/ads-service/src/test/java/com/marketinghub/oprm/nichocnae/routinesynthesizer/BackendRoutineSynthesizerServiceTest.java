package com.marketinghub.oprm.nichocnae.routinesynthesizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.nichocnae.OprmNicheRoutineCard;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.routinesynthesizer.service.BackendRoutineSynthesizerService;
import com.marketinghub.oprm.nichocnae.routinesynthesizer.service.completeStageExecution.CompleteRoutineSynthesizerRequest;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmExtractedSignalRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheRoutineCardRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: validar o contrato backend da síntese de rotina OPRM NichoCNAE. */
class BackendRoutineSynthesizerServiceTest {
  private final OprmRoutineResearchCycleRepository cycleRepository = mock(OprmRoutineResearchCycleRepository.class);
  private final OprmExtractedSignalRepository signalRepository = mock(OprmExtractedSignalRepository.class);
  private final OprmNicheRoutineCardRepository cardRepository = mock(OprmNicheRoutineCardRepository.class);
  private final BackendRoutineSynthesizerService service =
      new BackendRoutineSynthesizerService(cycleRepository, signalRepository, cardRepository);

  /** Deve aceitar resumo de mecanismo acima de 4000 caracteres quando a coluna canônica é LONGTEXT. */
  @Test
  void shouldAcceptLongMechanismSummaryBackedByLongtextColumn() {
    OprmRoutineResearchCycle cycle = cycle();
    String longMechanismSummary = "mecanismo ".repeat(500).trim();
    when(cycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    when(cardRepository.existsByResearchCycleId(1001L)).thenReturn(false);
    when(cardRepository.save(any(OprmNicheRoutineCard.class))).thenAnswer(invocation -> {
      OprmNicheRoutineCard card = invocation.getArgument(0);
      card.setId(10L);
      return card;
    });
    when(cycleRepository.save(any(OprmRoutineResearchCycle.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.complete(1001L, request(longMechanismSummary));

    ArgumentCaptor<OprmNicheRoutineCard> captor = ArgumentCaptor.forClass(OprmNicheRoutineCard.class);
    verify(cardRepository).save(captor.capture());
    assertThat(captor.getValue().getMechanismOpportunitiesSummary()).isEqualTo(longMechanismSummary);
    assertThat(cycle.getStatus()).isEqualTo("ROUTINE_SYNTHESIZED");
  }

  /** Cria ciclo mínimo para concluir a etapa seis nos testes de contrato. */
  private OprmRoutineResearchCycle cycle() {
    OprmRoutineResearchCycle cycle = new OprmRoutineResearchCycle();
    cycle.setId(1001L);
    cycle.setStatus("RUNNING");
    cycle.setStartedAt(Instant.parse("2026-06-13T00:00:00Z"));
    cycle.setUpdatedAt(Instant.parse("2026-06-13T00:00:00Z"));
    return cycle;
  }

  /** Monta payload completo com o resumo de mecanismo variável. */
  private CompleteRoutineSynthesizerRequest request(String mechanismSummary) {
    return new CompleteRoutineSynthesizerRequest(
        1001L,
        "Cabeleireiros",
        "Rotina",
        "Comportamento de clientes",
        "Canais",
        "Dores operacionais",
        "Dores emocionais",
        "Sonhos",
        "Medos",
        "Linguagem",
        "Dores",
        "Resultados",
        mechanismSummary,
        "Evidências",
        "example.com",
        80,
        75,
        70,
        60,
        10,
        "test");
  }
}
