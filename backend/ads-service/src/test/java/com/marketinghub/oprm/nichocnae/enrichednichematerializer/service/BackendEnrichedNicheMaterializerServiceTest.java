package com.marketinghub.oprm.nichocnae.enrichednichematerializer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.MarketNicheEnrichmentProfile;
import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import com.marketinghub.oprm.nichocnae.OprmNicheRoutineCard;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.completeStageExecution.CompleteEnrichedNicheMaterializerRequest;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.completeStageExecution.CompleteEnrichedNicheMaterializerResponse;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.pending.RecordEnrichedNicheMaterializerPending;
import com.marketinghub.repository.jpa.niche.MarketNicheEnrichmentProfileRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.repository.jpa.oprm.cnae.OprmNicheCandidateRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheRoutineCardRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

/** Responsabilidade: validar a etapa final que alimenta nicho e nicho enriquecido no backend. */
class BackendEnrichedNicheMaterializerServiceTest {
  private final OprmRoutineResearchCycleRepository cycleRepository = mock(OprmRoutineResearchCycleRepository.class);
  private final OprmNicheRoutineCardRepository cardRepository = mock(OprmNicheRoutineCardRepository.class);
  private final OprmNicheCandidateRepository candidateRepository = mock(OprmNicheCandidateRepository.class);
  private final MarketNicheRepository marketNicheRepository = mock(MarketNicheRepository.class);
  private final MarketNicheEnrichmentProfileRepository profileRepository = mock(MarketNicheEnrichmentProfileRepository.class);
  private final BackendEnrichedNicheMaterializerService service = new BackendEnrichedNicheMaterializerService(
      cycleRepository, cardRepository, candidateRepository, marketNicheRepository, profileRepository);

  /** Deve publicar uma unidade de trabalho completa para o coletor materializar o nicho enriquecido. */
  @Test
  void shouldListPendingMaterializationWithClosedPayload() {
    OprmNicheRoutineCard card = card();
    OprmRoutineResearchCycle cycle = cycle();
    OprmNicheCandidate candidate = candidate();
    when(cardRepository.findPendingEnrichedNicheMaterialization(any(Pageable.class))).thenReturn(List.of(card));
    when(cycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    when(candidateRepository.findById(77L)).thenReturn(Optional.of(candidate));

    List<RecordEnrichedNicheMaterializerPending> pending = service.listPending();

    assertThat(pending).hasSize(1);
    assertThat(pending.getFirst().routineCardId()).isEqualTo(10L);
    assertThat(pending.getFirst().sourceNicheCandidateId()).isEqualTo(77L);
    assertThat(pending.getFirst().existingMarketNicheId()).isNull();
    assertThat(pending.getFirst().mechanismOpportunitiesSummary()).contains("agenda");
  }

  /** Deve criar nicho base, perfil enriquecido e atualizar ciclo/candidato sem gerar hipótese. */
  @Test
  void shouldCompleteMaterializationCreatingNicheAndProfile() {
    OprmRoutineResearchCycle cycle = cycle();
    OprmNicheRoutineCard card = card();
    OprmNicheCandidate candidate = candidate();
    when(cycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
    when(candidateRepository.findById(77L)).thenReturn(Optional.of(candidate));
    when(profileRepository.existsBySourceRoutineCardId(10L)).thenReturn(false);
    when(marketNicheRepository.save(any(MarketNiche.class))).thenAnswer(invocation -> {
      MarketNiche niche = invocation.getArgument(0);
      niche.setId(200L);
      return niche;
    });
    when(profileRepository.save(any(MarketNicheEnrichmentProfile.class))).thenAnswer(invocation -> {
      MarketNicheEnrichmentProfile profile = invocation.getArgument(0);
      profile.setId(300L);
      return profile;
    });

    CompleteEnrichedNicheMaterializerResponse response = service.complete(1001L, new CompleteEnrichedNicheMaterializerRequest(
        1001L, 10L, "Persona", "Linguagem", "Gatilhos", "Objeções", "test"));

    assertThat(response.marketNicheId()).isEqualTo(200L);
    assertThat(response.enrichedNicheProfileId()).isEqualTo(300L);
    assertThat(cycle.getStatus()).isEqualTo("ENRICHED_NICHE_CREATED");
    assertThat(candidate.getMarketNicheId()).isEqualTo(200L);
    verify(marketNicheRepository).save(any(MarketNiche.class));
    verify(profileRepository).save(any(MarketNicheEnrichmentProfile.class));
  }

  /** Cria um cartão aprovado mínimo para testes da etapa final. */
  private OprmNicheRoutineCard card() {
    OprmNicheRoutineCard card = new OprmNicheRoutineCard();
    card.setId(10L);
    card.setResearchCycleId(1001L);
    card.setNicheName("IA para salões pequenos");
    card.setRoutineSummary("Rotina com agenda, atendimento e retorno de clientes.");
    card.setPainsSummary("Dores de falta de tempo e organização.");
    card.setResultsSummary("Resultado de preencher horários e reduzir esforço.");
    card.setMechanismOpportunitiesSummary("Mecanismo de agenda inteligente e mensagens prontas.");
    card.setEvidenceSummary("Evidência em fontes de gestão para salões.");
    card.setSourceDomains("exemplo.com");
    card.setConfidenceScore(90);
    card.setReadyForHypothesis(true);
    card.setSpecificityScore(84);
    card.setDuplicationScore(0);
    card.setQualityStatus("LIGHTLY_RESEARCHED");
    card.setQualityCheckedAt(Instant.parse("2026-06-05T00:00:00Z"));
    card.setSynthesizedBy("test");
    card.setCreatedAt(Instant.parse("2026-06-04T00:00:00Z"));
    return card;
  }

  /** Cria o ciclo pai do NichoCNAE usado pela etapa final. */
  private OprmRoutineResearchCycle cycle() {
    OprmRoutineResearchCycle cycle = new OprmRoutineResearchCycle();
    cycle.setId(1001L);
    cycle.setSourceNicheId(77L);
    cycle.setCnaeCode("9602501");
    cycle.setCnaeDescription("Cabeleireiros, manicure e pedicure");
    cycle.setNicheName("IA para salões pequenos");
    cycle.setSourceScore(new BigDecimal("90.00"));
    cycle.setStatus("LIGHTLY_RESEARCHED");
    cycle.setStartedAt(Instant.parse("2026-06-04T00:00:00Z"));
    cycle.setCreatedAt(Instant.parse("2026-06-04T00:00:00Z"));
    cycle.setUpdatedAt(Instant.parse("2026-06-04T00:00:00Z"));
    return cycle;
  }

  /** Cria o candidato de nicho de origem do ciclo. */
  private OprmNicheCandidate candidate() {
    OprmNicheCandidate candidate = new OprmNicheCandidate();
    candidate.setId(77L);
    candidate.setStatus("LIGHTLY_RESEARCHED");
    candidate.setRoutineResearchStatus("LIGHTLY_RESEARCHED");
    return candidate;
  }
}
