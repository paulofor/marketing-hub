package com.marketinghub.oprm.nichocnae.enrichednichematerializer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.MarketNicheEnrichmentProfile;
import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import com.marketinghub.oprm.nichocnae.OprmExtractedSignal;
import com.marketinghub.oprm.nichocnae.OprmNicheResearchSeed;
import com.marketinghub.oprm.nichocnae.OprmNicheRoutineCard;
import com.marketinghub.oprm.nichocnae.OprmResearchQuery;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.OprmSourceCandidate;
import com.marketinghub.oprm.nichocnae.OprmSourceSnapshot;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.completeStageExecution.CompleteEnrichedNicheMaterializerRequest;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.completeStageExecution.CompleteEnrichedNicheMaterializerResponse;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.pending.RecordEnrichedNicheMaterializerPending;
import com.marketinghub.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfile;
import com.marketinghub.repository.jpa.niche.MarketNicheEnrichmentProfileRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.repository.jpa.oprm.cnae.OprmNicheCandidateRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmExtractedSignalRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheResearchSeedRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheRoutineCardRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmResearchQueryRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmSourceCandidateRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmSourceSnapshotRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfileRepository;
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
  private final OprmNicheResearchSeedRepository seedRepository = mock(OprmNicheResearchSeedRepository.class);
  private final OprmResearchQueryRepository researchQueryRepository = mock(OprmResearchQueryRepository.class);
  private final OprmSourceCandidateRepository sourceCandidateRepository = mock(OprmSourceCandidateRepository.class);
  private final OprmSourceSnapshotRepository sourceSnapshotRepository = mock(OprmSourceSnapshotRepository.class);
  private final OprmExtractedSignalRepository extractedSignalRepository = mock(OprmExtractedSignalRepository.class);
  private final MarketNicheRepository marketNicheRepository = mock(MarketNicheRepository.class);
  private final MarketNicheEnrichmentProfileRepository profileRepository = mock(MarketNicheEnrichmentProfileRepository.class);
  private final OprmMeiAudienceProfileRepository meiAudienceProfileRepository = mock(OprmMeiAudienceProfileRepository.class);
  private final OprmEnrichedNicheMetaSignalService metaSignalService = mock(OprmEnrichedNicheMetaSignalService.class);
  private final OprmEnrichedNicheMetaSignalService.MetaSignalPackage metaSignalPackage = new OprmEnrichedNicheMetaSignalService.MetaSignalPackage(
      List.of("Salão de beleza"), List.of("Cabeleireiro"), List.of("Small business owners"));
  private final BackendEnrichedNicheMaterializerService service = new BackendEnrichedNicheMaterializerService(
      cycleRepository, cardRepository, candidateRepository, seedRepository, researchQueryRepository, sourceCandidateRepository,
      sourceSnapshotRepository, extractedSignalRepository, marketNicheRepository, profileRepository, meiAudienceProfileRepository, metaSignalService);

  /** Deve publicar uma unidade de trabalho completa para o coletor materializar o nicho enriquecido. */
  @Test
  void shouldListPendingMaterializationWithClosedPayload() {
    OprmNicheRoutineCard card = card();
    OprmRoutineResearchCycle cycle = cycle();
    OprmNicheCandidate candidate = candidate();
    when(cardRepository.findPendingEnrichedNicheMaterialization(any(Pageable.class))).thenReturn(List.of(card));
    when(meiAudienceProfileRepository.existsByResearchCycleId(1001L)).thenReturn(true);
    when(cycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    when(candidateRepository.findById(77L)).thenReturn(Optional.of(candidate));

    List<RecordEnrichedNicheMaterializerPending> pending = service.listPending();

    assertThat(pending).hasSize(1);
    assertThat(pending.getFirst().routineCardId()).isEqualTo(10L);
    assertThat(pending.getFirst().sourceNicheCandidateId()).isEqualTo(77L);
    assertThat(pending.getFirst().existingMarketNicheId()).isNull();
    assertThat(pending.getFirst().originalNicheName()).isEqualTo("IA para salões pequenos");
    assertThat(pending.getFirst().nicheName()).isEqualTo("Salões pequenos");
    assertThat(pending.getFirst().researchMode()).isEqualTo("ROUTINE_REALITY_RESEARCH");
    assertThat(pending.getFirst().routineEvidenceScore()).isEqualTo(87);
    assertThat(pending.getFirst().solutionLanguageRiskScore()).isEqualTo(35);
    assertThat(pending.getFirst().customerBehaviorSummary()).contains("indicação");
    assertThat(pending.getFirst().channelsSummary()).contains("WhatsApp");
    assertThat(pending.getFirst().mechanismOpportunitiesSummary()).contains("Contexto operacional");
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
    when(meiAudienceProfileRepository.findFirstByResearchCycleIdOrderByIdDesc(1001L)).thenReturn(Optional.of(meiProfile()));
    when(metaSignalService.buildSignalPackage(cycle, card)).thenReturn(metaSignalPackage);
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
    verify(marketNicheRepository).save(org.mockito.ArgumentMatchers.argThat(niche ->
        "Salões pequenos".equals(niche.getName())
            && niche.getPromises() == null
            && niche.getOffers() == null
            && niche.getDescription().contains("Nome original recebido para auditoria: IA para salões pequenos")
            && niche.getDescription().contains("Nome neutro pesquisado: Salões pequenos")
            && niche.getDescription().contains("Contexto operacional e linguagem pública:")
            && !niche.getDescription().contains("Oportunidades de mecanismo:")));
    verify(metaSignalService).buildReadableSignalSummary(metaSignalPackage);
    verify(profileRepository).save(org.mockito.ArgumentMatchers.argThat(profile ->
        "Salões pequenos".equals(profile.getNeutralNicheName())
            && "IA para salões pequenos".equals(profile.getOriginalNicheName())
            && "ROUTINE_REALITY_RESEARCH".equals(profile.getResearchMode())
            && Integer.valueOf(87).equals(profile.getRoutineEvidenceScore())
            && Integer.valueOf(82).equals(profile.getDifficultyEvidenceScore())
            && Integer.valueOf(72).equals(profile.getSourceDiversityScore())
            && Integer.valueOf(35).equals(profile.getSolutionLanguageRiskScore())
            && profile.getMechanismOpportunitiesSummary().contains("Contexto operacional")
            && !profile.getMechanismOpportunitiesSummary().contains("Mecanismo de agenda")
            && "Gatilhos".equals(profile.getCommercialTriggers())
            && "Objeções".equals(profile.getObjections())));
  }

  /** Deve permitir reprocessar o mesmo cartão criando novo perfil para o mesmo nicho existente. */
  @Test
  void shouldCreateNewProfileWhenReprocessingAlreadyMaterializedCard() {
    OprmRoutineResearchCycle cycle = cycle();
    OprmNicheRoutineCard card = card();
    OprmNicheCandidate candidate = candidate();
    MarketNiche existingNiche = new MarketNiche();
    existingNiche.setId(200L);
    MarketNicheEnrichmentProfile previousProfile = profile(existingNiche);
    when(cycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
    when(candidateRepository.findById(77L)).thenReturn(Optional.of(candidate));
    when(meiAudienceProfileRepository.findFirstByResearchCycleIdOrderByIdDesc(1001L)).thenReturn(Optional.of(meiProfile()));
    when(profileRepository.findMaterializedByCnaeAndNormalizedNeutralName(
        org.mockito.ArgumentMatchers.eq("9602501"),
        org.mockito.ArgumentMatchers.eq("salões pequenos"),
        any(Pageable.class)))
        .thenReturn(List.of(previousProfile));
    when(metaSignalService.buildSignalPackage(cycle, card)).thenReturn(metaSignalPackage);
    when(marketNicheRepository.save(any(MarketNiche.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(profileRepository.save(any(MarketNicheEnrichmentProfile.class))).thenAnswer(invocation -> {
      MarketNicheEnrichmentProfile profile = invocation.getArgument(0);
      profile.setId(301L);
      return profile;
    });

    CompleteEnrichedNicheMaterializerResponse response = service.complete(1001L, new CompleteEnrichedNicheMaterializerRequest(
        1001L, 10L, "Persona reprocessada", "Linguagem reprocessada", "Gatilhos", "Objeções", "test"));

    assertThat(response.marketNicheId()).isEqualTo(200L);
    assertThat(response.enrichedNicheProfileId()).isEqualTo(301L);
    assertThat(response.cycleStatus()).isEqualTo("ENRICHED_NICHE_UPDATED");
    verify(profileRepository).save(org.mockito.ArgumentMatchers.argThat(profile ->
        Long.valueOf(200L).equals(profile.getMarketNiche().getId())
            && Long.valueOf(1001L).equals(profile.getResearchCycleId())
            && Long.valueOf(10L).equals(profile.getSourceRoutineCardId())
            && "Persona reprocessada".equals(profile.getPersonaSummary())));
  }

  /** Deve bloquear materialização quando o ciclo sintetizado não tem perfil MEI/autônomo aprovado. */
  @Test
  void shouldNotReleaseMaterializationWithoutMeiAudienceProfile() {
    OprmRoutineResearchCycle cycle = cycle();
    cycle.setStatus("ROUTINE_SYNTHESIZED");
    OprmNicheRoutineCard card = card();
    OprmNicheCandidate candidate = candidate();
    when(cycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
    when(candidateRepository.findById(77L)).thenReturn(Optional.of(candidate));
    when(meiAudienceProfileRepository.findFirstByResearchCycleIdOrderByIdDesc(1001L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.complete(1001L, new CompleteEnrichedNicheMaterializerRequest(
        1001L, 10L, "Persona", "Linguagem", "Gatilhos", "Objeções", "test")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("aguardando perfil MEI/autônomo");

    assertThat(cycle.getStatus()).isEqualTo("ROUTINE_SYNTHESIZED");
    verify(marketNicheRepository, never()).save(any(MarketNiche.class));
    verify(profileRepository, never()).save(any(MarketNicheEnrichmentProfile.class));
  }

  /** Deve localizar registros históricos contaminados para orientar novo ciclo neutro. */
  @Test
  void shouldDiagnoseHistoricalContamination() {
    OprmRoutineResearchCycle cycle = cycle();
    when(cycleRepository.findPotentiallyContaminatedByTerm(org.mockito.ArgumentMatchers.eq("ia"), any(Pageable.class))).thenReturn(List.of(cycle));
    when(profileRepository.findPotentiallyContaminatedByTerm(org.mockito.ArgumentMatchers.eq("ia"), any(Pageable.class))).thenReturn(List.of());

    var diagnostic = service.diagnoseHistoricalContamination(10);

    assertThat(diagnostic.totalCycles()).isEqualTo(1);
    assertThat(diagnostic.items()).hasSize(1);
    assertThat(diagnostic.items().getFirst().matchedTerm()).isEqualTo("ia");
    assertThat(diagnostic.items().getFirst().recommendation()).contains("novo ciclo neutro");
  }


  /** Deve gerar um documento Markdown com todo o pipeline processado e a conclusão final. */
  @Test
  void shouldBuildPipelineMarkdownByProfileId() {
    OprmRoutineResearchCycle cycle = cycle();
    cycle.setStatus("ENRICHED_NICHE_CREATED");
    OprmNicheRoutineCard card = card();
    MarketNiche niche = new MarketNiche();
    niche.setId(200L);
    MarketNicheEnrichmentProfile profile = profile(niche);
    when(profileRepository.findById(300L)).thenReturn(Optional.of(profile));
    when(cycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    when(cardRepository.findFirstByResearchCycleIdOrderByIdDesc(1001L)).thenReturn(Optional.of(card));
    when(seedRepository.findByResearchCycleId(1001L)).thenReturn(Optional.of(seed()));
    when(researchQueryRepository.findByResearchCycleIdOrderByPriorityAscIdAsc(1001L)).thenReturn(List.of(researchQuery()));
    when(sourceCandidateRepository.findByResearchCycleIdOrderByResearchQueryIdAscSearchPositionAscIdAsc(1001L)).thenReturn(List.of(sourceCandidate()));
    when(sourceSnapshotRepository.findByResearchCycleIdOrderByIdAsc(1001L)).thenReturn(List.of(sourceSnapshot()));
    when(extractedSignalRepository.findByResearchCycleIdOrderByIdAsc(1001L)).thenReturn(List.of(extractedSignal()));

    String markdown = service.buildPipelineMarkdownByProfileId(300L);

    assertThat(markdown).contains("# Pesquisa OPRM NichoCNAE — Salões pequenos");
    assertThat(markdown).contains("## 3. Frases de pesquisa processadas");
    assertThat(markdown).contains("## 6. Sinais extraídos");
    assertThat(markdown).contains("## 8. Conclusão final do nicho enriquecido");
    assertThat(markdown).contains("Rotina final");
    assertThat(markdown).contains("Dores finais");
    assertThat(markdown).contains("ENRICHED_NICHE_CREATED");
  }

  /** Cria um cartão aprovado mínimo para testes da etapa final. */
  private OprmNicheRoutineCard card() {
    OprmNicheRoutineCard card = new OprmNicheRoutineCard();
    card.setId(10L);
    card.setResearchCycleId(1001L);
    card.setNicheName("IA para salões pequenos");
    card.setRoutineSummary("Rotina com agenda, atendimento e retorno de clientes.");
    card.setPainsSummary("Dores de falta de tempo e organização.");
    card.setCustomerBehaviorSummary("Comportamento de clientes: indicação, retorno e desmarcações.");
    card.setChannelsSummary("Canais usados: WhatsApp e conversa direta.");
    card.setResultsSummary("Perguntas do profissional sobre encaixes e retornos de clientes.");
    card.setMechanismOpportunitiesSummary("Contexto operacional e linguagem do nicho: agenda, encaixes, retorno e horários vagos.");
    card.setEvidenceSummary("Evidência em fontes de gestão para salões.");
    card.setSourceDomains("exemplo.com");
    card.setConfidenceScore(90);
    card.setRoutineEvidenceScore(87);
    card.setDifficultyEvidenceScore(82);
    card.setSourceDiversityScore(72);
    card.setSolutionLanguageRiskScore(35);
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
    cycle.setOriginalNicheName("IA para salões pequenos");
    cycle.setNeutralNicheName("Salões pequenos");
    cycle.setResearchMode("ROUTINE_REALITY_RESEARCH");
    cycle.setSolutionLanguageRiskScore(new BigDecimal("65.00"));
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

  /** Cria o perfil MEI/autônomo aprovado usado como trava de liberação comercial. */
  private OprmMeiAudienceProfile meiProfile() {
    OprmMeiAudienceProfile profile = new OprmMeiAudienceProfile();
    profile.setId(400L);
    profile.setResearchCycleId(1001L);
    profile.setRoutineCardId(10L);
    profile.setSourceNicheCandidateId(77L);
    profile.setCnaeCode("9602501");
    profile.setCnaeDescription("Cabeleireiros, manicure e pedicure");
    profile.setNeutralNicheName("Salões pequenos");
    profile.setAudienceName("MEI dono-operador de salão pequeno");
    profile.setCreatedAt(Instant.parse("2026-06-05T00:00:00Z"));
    profile.setUpdatedAt(Instant.parse("2026-06-05T00:00:00Z"));
    return profile;
  }

  /** Cria o perfil enriquecido final usado pelo download Markdown. */
  private MarketNicheEnrichmentProfile profile(MarketNiche niche) {
    MarketNicheEnrichmentProfile profile = new MarketNicheEnrichmentProfile();
    profile.setId(300L);
    profile.setMarketNiche(niche);
    profile.setResearchCycleId(1001L);
    profile.setNeutralNicheName("Salões pequenos");
    profile.setOriginalNicheName("IA para salões pequenos");
    profile.setCnaeCode("9602501");
    profile.setCnaeDescription("Cabeleireiros, manicure e pedicure");
    profile.setRoutineSummary("Rotina com agenda, atendimento e retorno de clientes.");
    profile.setPainsSummary("Dores de falta de tempo e organização.");
    profile.setResultsSummary("Perguntas do profissional sobre encaixes.");
    profile.setMechanismOpportunitiesSummary("Contexto operacional e linguagem do nicho.");
    profile.setEvidenceSummary("Evidência consolidada.");
    profile.setSourceDomains("exemplo.com");
    profile.setCreatedAt(Instant.parse("2026-06-05T00:00:00Z"));
    return profile;
  }

  /** Cria o seed de pesquisa operacional usado pelo documento Markdown. */
  private OprmNicheResearchSeed seed() {
    OprmNicheResearchSeed seed = new OprmNicheResearchSeed();
    seed.setId(1L);
    seed.setBusinessType("Salão de beleza");
    seed.setOperationType("Atendimento por agenda");
    seed.setCustomerType("Clientes recorrentes");
    seed.setCommercialObjects("Serviços de beleza");
    seed.setInitialAssumptions("Rotina depende de horários e retornos.");
    seed.setConfidenceLevel("Médio");
    return seed;
  }

  /** Cria uma frase de pesquisa processada usada pelo documento Markdown. */
  private OprmResearchQuery researchQuery() {
    OprmResearchQuery query = new OprmResearchQuery();
    query.setId(2L);
    query.setQueryGoal("ROUTINE_DISCOVERY");
    query.setPriority(1);
    query.setStatus("SEARCHED");
    query.setResultCount(10);
    query.setQueryText("rotina salão beleza agenda atendimento");
    return query;
  }

  /** Cria uma fonte candidata encontrada usada pelo documento Markdown. */
  private OprmSourceCandidate sourceCandidate() {
    OprmSourceCandidate candidate = new OprmSourceCandidate();
    candidate.setId(3L);
    candidate.setSourceDomain("exemplo.com");
    candidate.setStatus("SELECTED");
    candidate.setSourceIntent("ROUTINE_REPORT");
    candidate.setRoutineEvidenceScore(80);
    candidate.setSourceTitle("Rotina de salão");
    candidate.setSourceUrl("https://exemplo.com/rotina");
    candidate.setSourceSnippet("Agenda e atendimento estruturam a rotina.");
    return candidate;
  }

  /** Cria uma evidência curta coletada usada pelo documento Markdown. */
  private OprmSourceSnapshot sourceSnapshot() {
    OprmSourceSnapshot snapshot = new OprmSourceSnapshot();
    snapshot.setId(4L);
    snapshot.setSourceDomain("exemplo.com");
    snapshot.setFetchStatus("FETCHED");
    snapshot.setSignalExtractionStatus("EXTRACTED");
    snapshot.setSourceIntent("ROUTINE_REPORT");
    snapshot.setRoutineEvidenceScore(80);
    snapshot.setSourceTitle("Rotina de salão");
    snapshot.setShortExcerpt("Organiza agenda de atendimentos e retornos.");
    return snapshot;
  }

  /** Cria um sinal estruturado extraído usado pelo documento Markdown. */
  private OprmExtractedSignal extractedSignal() {
    OprmExtractedSignal signal = new OprmExtractedSignal();
    signal.setId(5L);
    signal.setSignalType("ROUTINE_TASK");
    signal.setSourceDomain("exemplo.com");
    signal.setConfidenceScore(82);
    signal.setSignalText("Organizar agenda de atendimentos.");
    signal.setEvidenceExcerpt("Organiza agenda de atendimentos e retornos.");
    return signal;
  }

}
