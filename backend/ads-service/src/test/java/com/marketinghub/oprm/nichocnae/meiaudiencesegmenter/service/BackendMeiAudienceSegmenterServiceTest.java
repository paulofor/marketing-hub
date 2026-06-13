package com.marketinghub.oprm.nichocnae.meiaudiencesegmenter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.nichocnae.OprmNicheRoutineCard;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.meiaudienceprofile.service.BackendMeiAudienceProfileService;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmExtractedSignalRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheRoutineCardRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmSourceSnapshotRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

/** Testes responsáveis por validar a fila backend da etapa de segmentação MEI/autônomo. */
class BackendMeiAudienceSegmenterServiceTest {
  private final OprmRoutineResearchCycleRepository cycleRepository = mock(OprmRoutineResearchCycleRepository.class);
  private final OprmNicheRoutineCardRepository routineCardRepository = mock(OprmNicheRoutineCardRepository.class);
  private final OprmExtractedSignalRepository extractedSignalRepository = mock(OprmExtractedSignalRepository.class);
  private final OprmSourceSnapshotRepository sourceSnapshotRepository = mock(OprmSourceSnapshotRepository.class);
  private final BackendMeiAudienceProfileService profileService = mock(BackendMeiAudienceProfileService.class);
  private final MeiAudienceSegmenterConfigurationGateway configurationGateway = mock(MeiAudienceSegmenterConfigurationGateway.class);
  private final BackendMeiAudienceSegmenterService service = new BackendMeiAudienceSegmenterService(
      cycleRepository,
      routineCardRepository,
      extractedSignalRepository,
      sourceSnapshotRepository,
      profileService,
      configurationGateway);

  /** Garante que o serviço não exponha cartões caso a consulta retorne ciclo fora do status elegível. */
  @Test
  void listPendingShouldRevalidateEligibleCycleBeforeExposingQueue() {
    OprmNicheRoutineCard failedCard = card(10L, 1L);
    OprmNicheRoutineCard synthesizedCard = card(20L, 2L);
    when(routineCardRepository.findPendingMeiAudienceSegmentation(any(Pageable.class)))
        .thenReturn(List.of(failedCard, synthesizedCard));
    when(cycleRepository.findById(10L)).thenReturn(Optional.of(cycle(10L, "FAILED")));
    when(cycleRepository.findById(20L)).thenReturn(Optional.of(cycle(20L, "ROUTINE_SYNTHESIZED")));
    when(sourceSnapshotRepository.findByResearchCycleIdOrderByIdAsc(20L)).thenReturn(List.of());
    when(extractedSignalRepository.findByResearchCycleIdOrderByIdAsc(20L)).thenReturn(List.of());
    when(configurationGateway.findConfiguredModel()).thenReturn(Optional.of(new MeiAudienceSegmenterModel("gpt-5.4", "GPT-5.4")));

    assertThat(service.listPending())
        .extracting(pending -> pending.researchCycleId())
        .containsExactly(20L);
    assertThat(service.listPending().get(0).openAiModelCode()).isEqualTo("gpt-5.4");
  }

  /** Monta um ciclo mínimo usado pela revalidação da fila MEI/autônomo. */
  private OprmRoutineResearchCycle cycle(Long id, String status) {
    OprmRoutineResearchCycle cycle = new OprmRoutineResearchCycle();
    cycle.setId(id);
    cycle.setSourceNicheId(100L);
    cycle.setCnaeCode("9602501");
    cycle.setCnaeDescription("Cabeleireiros, manicure e pedicure");
    cycle.setNeutralNicheName("serviços pessoais de beleza");
    cycle.setStatus(status);
    return cycle;
  }

  /** Monta um cartão mínimo para conversão em item pendente da fila MEI/autônomo. */
  private OprmNicheRoutineCard card(Long researchCycleId, Long cardId) {
    OprmNicheRoutineCard card = new OprmNicheRoutineCard();
    card.setId(cardId);
    card.setResearchCycleId(researchCycleId);
    card.setNicheName("manicures autônomas");
    card.setRoutineSummary("Rotina operacional concreta.");
    card.setCustomerBehaviorSummary("Clientes chegam por indicação.");
    card.setChannelsSummary("WhatsApp e Instagram.");
    card.setOperationalPainsSummary("Agenda instável.");
    card.setEmotionalPainsSummary("Insegurança de renda.");
    card.setDreamsSummary("Agenda cheia.");
    card.setFearsSummary("Ficar sem clientes.");
    card.setLanguageSummary("agenda cheia, cliente fixa");
    card.setPainsSummary("Cancelamentos e retrabalho.");
    card.setResultsSummary("Rotina mais previsível.");
    card.setEvidenceSummary("Evidências brasileiras recentes.");
    card.setSourceDomains("example.com");
    card.setRoutineEvidenceScore(80);
    card.setDifficultyEvidenceScore(80);
    card.setSourceDiversityScore(70);
    card.setSolutionLanguageRiskScore(0);
    card.setCreatedAt(Instant.parse("2026-06-12T00:00:00Z"));
    return card;
  }
}
