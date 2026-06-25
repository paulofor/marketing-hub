package com.marketinghub.oprm.nichocnae.enrichednichematerializer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.marketinghub.oprm.nichocnae.gateway.OprmEnrichedNicheGateway;
import com.marketinghub.oprm.nichocnae.gateway.OprmEnrichedNicheGateway.OprmEnrichedNicheMaterializationResult;
import com.marketinghub.oprm.nichocnae.gateway.OprmEnrichedNicheGateway.OprmEnrichedNicheProfileDraft;
import com.marketinghub.oprm.nichocnae.gateway.OprmEnrichedNicheGateway.OprmEnrichedNicheProfileSnapshot;
import com.marketinghub.oprm.nichocnae.gateway.OprmEnrichedNicheGateway.OprmMarketNicheSnapshot;
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
import org.mockito.ArgumentCaptor;
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
  private final OprmEnrichedNicheGateway enrichedNicheGateway = mock(OprmEnrichedNicheGateway.class);
  private final OprmMeiAudienceProfileRepository meiAudienceProfileRepository = mock(OprmMeiAudienceProfileRepository.class);
  private final OprmEnrichedNicheMetaSignalService metaSignalService = mock(OprmEnrichedNicheMetaSignalService.class);
  private final OprmCurrencyConversionService currencyConversionService = mock(OprmCurrencyConversionService.class);
  private final OprmEnrichedNicheMetaSignalService.MetaSignalPackage metaSignalPackage = new OprmEnrichedNicheMetaSignalService.MetaSignalPackage(
      List.of("Salão de beleza"), List.of("Cabeleireiro"), List.of("Small business owners"));
  private final BackendEnrichedNicheMaterializerService service = new BackendEnrichedNicheMaterializerService(
      cycleRepository, cardRepository, candidateRepository, seedRepository, researchQueryRepository, sourceCandidateRepository,
      sourceSnapshotRepository, extractedSignalRepository, enrichedNicheGateway, meiAudienceProfileRepository, metaSignalService,
      currencyConversionService);

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
    when(seedRepository.sumCostUsdByResearchCycleId(1001L)).thenReturn(new BigDecimal("0.0473"));
    when(currencyConversionService.usdToBrl(new BigDecimal("0.0473"))).thenReturn(new BigDecimal("0.24"));
    when(enrichedNicheGateway.materialize(any(), any()))
        .thenReturn(new OprmEnrichedNicheMaterializationResult(200L, 300L, Instant.now()));

    CompleteEnrichedNicheMaterializerResponse response = service.complete(1001L, new CompleteEnrichedNicheMaterializerRequest(
        1001L, 10L, "Persona", "Linguagem", "Gatilhos", "Objeções", "test"));

    assertThat(response.marketNicheId()).isEqualTo(200L);
    assertThat(response.enrichedNicheProfileId()).isEqualTo(300L);
    assertThat(cycle.getStatus()).isEqualTo("ENRICHED_NICHE_CREATED");
    assertThat(candidate.getMarketNicheId()).isEqualTo(200L);
    assertThat(candidate.getOfferIdea()).isNull();
    ArgumentCaptor<OprmEnrichedNicheProfileDraft> profileCaptor = ArgumentCaptor.forClass(OprmEnrichedNicheProfileDraft.class);
    verify(metaSignalService).buildReadableSignalSummary(metaSignalPackage);
    verify(enrichedNicheGateway).materialize(any(), profileCaptor.capture());
    assertThat(profileCaptor.getValue().personaDailyTasks()).contains("agenda");
    assertThat(profileCaptor.getValue().researchReportMarkdown()).contains("Pesquisa OPRM NichoCNAE");
  }

  /** Deve permitir reprocessar o mesmo cartão criando novo perfil para o mesmo nicho existente. */
  @Test
  void shouldCreateNewProfileWhenReprocessingAlreadyMaterializedCard() {
    OprmRoutineResearchCycle cycle = cycle();
    OprmNicheRoutineCard card = card();
    OprmNicheCandidate candidate = candidate();
    when(cycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
    when(candidateRepository.findById(77L)).thenReturn(Optional.of(candidate));
    when(meiAudienceProfileRepository.findFirstByResearchCycleIdOrderByIdDesc(1001L)).thenReturn(Optional.of(meiProfile()));
    when(enrichedNicheGateway.findByCnaeAndNormalizedNeutralName("9602501", "salões pequenos"))
        .thenReturn(Optional.of(new OprmMarketNicheSnapshot(200L)));
    when(metaSignalService.buildSignalPackage(cycle, card)).thenReturn(metaSignalPackage);
    when(enrichedNicheGateway.materialize(any(), any()))
        .thenReturn(new OprmEnrichedNicheMaterializationResult(200L, 301L, Instant.now()));

    CompleteEnrichedNicheMaterializerResponse response = service.complete(1001L, new CompleteEnrichedNicheMaterializerRequest(
        1001L, 10L, "Persona reprocessada", "Linguagem reprocessada", "Gatilhos", "Objeções", "test"));

    assertThat(response.marketNicheId()).isEqualTo(200L);
    assertThat(response.enrichedNicheProfileId()).isEqualTo(301L);
    assertThat(response.cycleStatus()).isEqualTo("ENRICHED_NICHE_UPDATED");
    verify(enrichedNicheGateway).materialize(any(), any());
  }

  /** Deve criar outro market_niche para subnicho diferente do mesmo CNAE, sem sobrescrever o nicho anterior do candidato. */
  @Test
  void shouldCreateAnotherMarketNicheForDifferentNeutralNameInSameCnae() {
    OprmRoutineResearchCycle cycle = cycle();
    cycle.setNicheName("Manicure autônoma domiciliar");
    cycle.setOriginalNicheName("Manicure autônoma domiciliar");
    cycle.setNeutralNicheName("Manicure autônoma domiciliar");
    OprmNicheRoutineCard card = card();
    card.setNicheName("Manicure autônoma domiciliar");
    OprmNicheCandidate candidate = candidate();
    candidate.setMarketNicheId(200L);
    OprmMeiAudienceProfile profile = meiProfile();
    profile.setMarketNicheId(200L);
    profile.setNeutralNicheName("Manicure autônoma domiciliar");
    when(cycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
    when(candidateRepository.findById(77L)).thenReturn(Optional.of(candidate));
    when(meiAudienceProfileRepository.findFirstByResearchCycleIdOrderByIdDesc(1001L)).thenReturn(Optional.of(profile));
    when(enrichedNicheGateway.findByCnaeAndNormalizedNeutralName("9602501", "manicure autônoma domiciliar"))
        .thenReturn(Optional.empty());
    when(metaSignalService.buildSignalPackage(cycle, card)).thenReturn(metaSignalPackage);
    when(enrichedNicheGateway.materialize(any(), any()))
        .thenReturn(new OprmEnrichedNicheMaterializationResult(201L, 301L, Instant.now()));

    CompleteEnrichedNicheMaterializerResponse response = service.complete(1001L, new CompleteEnrichedNicheMaterializerRequest(
        1001L, 10L, "Persona manicure", "Linguagem", "Gatilhos", "Objeções", "test"));

    assertThat(response.marketNicheId()).isEqualTo(201L);
    assertThat(response.cycleStatus()).isEqualTo("ENRICHED_NICHE_CREATED");
    assertThat(response.operationalMessage()).contains("mesmo CNAE pode ter outros nichos");
    assertThat(candidate.getMarketNicheId()).isEqualTo(201L);
    verify(enrichedNicheGateway).materialize(any(), any());
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
    verify(enrichedNicheGateway, never()).materialize(any(), any());
  }

  /** Deve localizar registros históricos contaminados para orientar novo ciclo neutro. */
  @Test
  void shouldDiagnoseHistoricalContamination() {
    OprmRoutineResearchCycle cycle = cycle();
    when(cycleRepository.findPotentiallyContaminatedByTerm(org.mockito.ArgumentMatchers.eq("ia"), any(Pageable.class))).thenReturn(List.of(cycle));
    when(enrichedNicheGateway.findPotentiallyContaminatedByTerm("ia", 10)).thenReturn(List.of());

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
    when(enrichedNicheGateway.requireProfileById(300L)).thenReturn(profileSnapshot());
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


  /** Deve gerar relatório Markdown por job mesmo quando ainda não existe perfil materializado. */
  @Test
  void shouldBuildPipelineMarkdownByResearchCycleIdWithoutProfile() {
    OprmRoutineResearchCycle cycle = cycle();
    cycle.setStatus("RUNNING");
    cycle.setTriggerSource("AUTO_QUALITY_REPROCESS");
    cycle.setErrorMessage("Reexecução de etapas do mesmo job solicitada por AUTO_QUALITY_REPROCESS.");
    when(enrichedNicheGateway.findLatestProfileByResearchCycleId(1001L)).thenReturn(Optional.empty());
    when(cycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    when(cardRepository.findFirstByResearchCycleIdOrderByIdDesc(1001L)).thenReturn(Optional.empty());
    when(seedRepository.findByResearchCycleId(1001L)).thenReturn(Optional.empty());
    when(researchQueryRepository.findByResearchCycleIdOrderByPriorityAscIdAsc(1001L)).thenReturn(List.of());
    when(sourceCandidateRepository.findByResearchCycleIdOrderByResearchQueryIdAscSearchPositionAscIdAsc(1001L)).thenReturn(List.of());
    when(sourceSnapshotRepository.findByResearchCycleIdOrderByIdAsc(1001L)).thenReturn(List.of());
    when(extractedSignalRepository.findByResearchCycleIdOrderByIdAsc(1001L)).thenReturn(List.of());

    String markdown = service.buildPipelineMarkdownByResearchCycleId(1001L);

    assertThat(markdown).contains("Job de pesquisa");
    assertThat(markdown).contains("AUTO_QUALITY_REPROCESS");
    assertThat(markdown).contains("Reexecução de etapas do mesmo job");
    assertThat(markdown).contains("Seed não encontrado para este ciclo");
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
    candidate.setOfferIdea("Oferta antiga que não deve sobreviver à materialização de rotina.");
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

  /** Cria um snapshot de perfil enriquecido usado pelo documento Markdown. */
  private OprmEnrichedNicheProfileSnapshot profileSnapshot() {
    return new OprmEnrichedNicheProfileSnapshot(
        300L,
        200L,
        1001L,
        "Salões pequenos",
        "9602501",
        "Cabeleireiros",
        "Salões pequenos",
        "APPROVED",
        87,
        82,
        72,
        35,
        "Rotina final",
        "- Organizar agenda de atendimentos.",
        "Dores finais",
        "Resultados finais",
        "Contexto operacional final",
        "Evidências finais",
        "exemplo.com",
        "# Relatório auditável",
        Instant.parse("2026-01-01T00:00:00Z"));
  }

}
