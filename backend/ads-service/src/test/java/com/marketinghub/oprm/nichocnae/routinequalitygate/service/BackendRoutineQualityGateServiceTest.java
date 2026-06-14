package com.marketinghub.oprm.nichocnae.routinequalitygate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import com.marketinghub.oprm.nichocnae.OprmExtractedSignal;
import com.marketinghub.oprm.nichocnae.OprmNicheRoutineCard;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.OprmSourceSnapshot;
import com.marketinghub.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfile;
import com.marketinghub.oprm.nichocnae.routinequalitygate.service.completeStageExecution.CompleteRoutineQualityGateRequest;
import com.marketinghub.oprm.nichocnae.routinequalitygate.service.completeStageExecution.CompleteRoutineQualityGateResponse;
import com.marketinghub.oprm.nichocnae.routinequalitygate.service.detailStageExecution.RoutineQualityGateDetailResponse;
import com.marketinghub.oprm.nichocnae.routinequalitygate.service.pending.RecordRoutineQualityGatePending;
import com.marketinghub.repository.jpa.oprm.cnae.OprmNicheCandidateRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmExtractedSignalRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheRoutineCardRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmSourceSnapshotRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfileRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

/** Responsabilidade: validar contratos e persistência da etapa sete no backend OPRM NichoCNAE. */
class BackendRoutineQualityGateServiceTest {
  private final OprmRoutineResearchCycleRepository cycleRepository = mock(OprmRoutineResearchCycleRepository.class);
  private final OprmNicheRoutineCardRepository cardRepository = mock(OprmNicheRoutineCardRepository.class);
  private final OprmExtractedSignalRepository signalRepository = mock(OprmExtractedSignalRepository.class);
  private final OprmSourceSnapshotRepository snapshotRepository = mock(OprmSourceSnapshotRepository.class);
  private final OprmMeiAudienceProfileRepository meiAudienceProfileRepository = mock(OprmMeiAudienceProfileRepository.class);
  private final OprmNicheCandidateRepository nicheCandidateRepository = mock(OprmNicheCandidateRepository.class);
  private final BackendRoutineQualityGateService service =
      new BackendRoutineQualityGateService(
          cycleRepository,
          cardRepository,
          signalRepository,
          snapshotRepository,
          meiAudienceProfileRepository,
          nicheCandidateRepository);

  /** Deve montar pendência com aliases de tipos de sinais já gerados pela etapa cinco. */
  @Test
  void shouldListPendingWithSignalTypeAliases() {
    OprmNicheRoutineCard card = card();
    when(cardRepository.findPendingRoutineQualityGate(any(Pageable.class))).thenReturn(List.of(card));
    when(snapshotRepository.findByResearchCycleIdOrderByIdAsc(1001L)).thenReturn(List.of(new OprmSourceSnapshot(), new OprmSourceSnapshot()));
    OprmSourceSnapshot riskSnapshot = snapshot(true, 80, 70, false, false, "a.com.br");
    OprmSourceSnapshot routineSnapshot = snapshot(false, 75, 65, false, false, "b.com.br");
    when(snapshotRepository.findByResearchCycleIdOrderByIdAsc(1001L)).thenReturn(List.of(riskSnapshot, routineSnapshot));
    OprmMeiAudienceProfile profile = profile();
    when(meiAudienceProfileRepository.findFirstByResearchCycleIdOrderByIdDesc(1001L)).thenReturn(Optional.of(profile));
    when(signalRepository.findByResearchCycleIdOrderByIdAsc(1001L)).thenReturn(List.of(
        signal("CUSTOMER_QUESTION"), signal("PAIN_POINT"), signal("OPERATIONAL_FRICTION"), signal("LANGUAGE_MARKER"),
        signal("SOLUTION_LANGUAGE_RISK"), signal("MECHANISM_OPPORTUNITY"), signal("ROUTINE_TASK"), signal("COMMERCIAL_TASK"),
        signal("CUSTOMER_ACQUISITION_BEHAVIOR"), signal("EMOTIONAL_PAIN")));

    List<RecordRoutineQualityGatePending> pending = service.listPending();

    assertThat(pending).hasSize(1);
    assertThat(pending.getFirst().sourceCount()).isEqualTo(2);
    assertThat(pending.getFirst().questionSignalCount()).isEqualTo(1);
    assertThat(pending.getFirst().painSignalCount()).isEqualTo(1);
    assertThat(pending.getFirst().commercialObjectCount()).isEqualTo(1);
    assertThat(pending.getFirst().operationalDifficultyCount()).isEqualTo(1);
    assertThat(pending.getFirst().languageMarkerCount()).isEqualTo(1);
    assertThat(pending.getFirst().solutionLanguageRiskCount()).isEqualTo(3);
    assertThat(pending.getFirst().routineEvidenceScore()).isEqualTo(86);
    assertThat(pending.getFirst().brazilianSourceCount()).isEqualTo(2);
    assertThat(pending.getFirst().recentSourceCount()).isEqualTo(2);
    assertThat(pending.getFirst().customerBehaviorSummary()).contains("WhatsApp");
    assertThat(pending.getFirst().channelsSummary()).contains("Instagram");
    assertThat(pending.getFirst().customerAcquisitionEvidenceCount()).isEqualTo(3);
    assertThat(pending.getFirst().emotionalOutcomeEvidenceCount()).isEqualTo(4);
    assertThat(pending.getFirst().autonomousProfessionalFitScore()).isEqualTo(82);
  }


  /** Deve entregar resumos fracos ao coletor sem contar placeholders como evidência positiva de aquisição/canais. */
  @Test
  void shouldNotCountGenericAcquisitionAndChannelsAsPositiveEvidence() {
    OprmNicheRoutineCard card = card();
    OprmMeiAudienceProfile profile = profile();
    profile.setCustomerAcquisitionBehavior("Sem evidência suficiente sobre comportamento de clientes.");
    profile.setChannelsUsed("Sem evidência suficiente sobre canais usados.");
    when(cardRepository.findPendingRoutineQualityGate(any(Pageable.class))).thenReturn(List.of(card));
    when(snapshotRepository.findByResearchCycleIdOrderByIdAsc(1001L)).thenReturn(List.of(snapshot(false, 80, 70, false, false, "a.com.br")));
    when(meiAudienceProfileRepository.findFirstByResearchCycleIdOrderByIdDesc(1001L)).thenReturn(Optional.of(profile));
    when(signalRepository.findByResearchCycleIdOrderByIdAsc(1001L)).thenReturn(List.of(signal("ROUTINE_TASK")));

    List<RecordRoutineQualityGatePending> pending = service.listPending();

    assertThat(pending.getFirst().customerBehaviorSummary()).contains("Sem evidência suficiente");
    assertThat(pending.getFirst().channelsSummary()).contains("Sem evidência suficiente");
    assertThat(pending.getFirst().customerAcquisitionEvidenceCount()).isZero();
  }


  /** Deve persistir a decisão final no cartão e refletir o status de qualidade no ciclo. */
  @Test
  void shouldCompleteQualityGateAndUpdateCycleStatus() {
    OprmRoutineResearchCycle cycle = new OprmRoutineResearchCycle();
    cycle.setId(1001L);
    cycle.setStatus("ROUTINE_SYNTHESIZED");
    OprmNicheRoutineCard card = card();
    when(cycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
    when(cardRepository.save(any(OprmNicheRoutineCard.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(cycleRepository.save(any(OprmRoutineResearchCycle.class))).thenAnswer(invocation -> invocation.getArgument(0));

    CompleteRoutineQualityGateResponse response = service.complete(1001L, new CompleteRoutineQualityGateRequest(
        1001L, 10L, "MEI_AUDIENCE_READY", true, 75, 80, 10, "Aprovado", "test"));

    assertThat(response.qualityStatus()).isEqualTo("MEI_AUDIENCE_READY");
    assertThat(response.readyForHypothesis()).isTrue();
    assertThat(cycle.getStatus()).isEqualTo("MEI_AUDIENCE_READY");
    assertThat(card.getQualityCheckedAt()).isNotNull();
    verify(cardRepository).save(card);
    verify(cycleRepository).save(cycle);
  }


  /** Deve abrir automaticamente novo ciclo reprovado preservando subnicho e aprendizado para a etapa de seed. */
  @Test
  void shouldAutomaticallyReprocessRejectedQualityGateWithinLimit() {
    OprmRoutineResearchCycle cycle = new OprmRoutineResearchCycle();
    cycle.setId(1001L);
    cycle.setSourceNicheId(55L);
    cycle.setCnaeCode("9602501");
    cycle.setCnaeDescription("Cabeleireiros, manicure e pedicure");
    cycle.setNicheName("Manicure autônoma com agenda instável");
    cycle.setOriginalNicheName("Cabeleireiros, manicure e pedicure");
    cycle.setNeutralNicheName("Manicure autônoma com agenda instável");
    cycle.setResearchMode("ROUTINE_REALITY_RESEARCH");
    cycle.setSolutionLanguageRiskScore(BigDecimal.ZERO);
    cycle.setSourceScore(BigDecimal.valueOf(91));
    cycle.setStatus("ROUTINE_SYNTHESIZED");
    OprmNicheRoutineCard card = card();
    card.setQualityNotes(
        "status=TOO_CORPORATE; proximoMovimentoCodigo=TROCAR_PARA_DONO_OPERADOR; proximoMovimento=Focar dono-operador");
    OprmNicheCandidate candidate = new OprmNicheCandidate();
    candidate.setId(55L);
    when(cycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
    when(cardRepository.save(any(OprmNicheRoutineCard.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(cycleRepository.save(any(OprmRoutineResearchCycle.class))).thenAnswer(invocation -> {
      OprmRoutineResearchCycle saved = invocation.getArgument(0);
      if (saved.getId() == null) {
        saved.setId(1002L);
      }
      return saved;
    });
    when(cycleRepository.countBySourceNicheIdAndTriggerSource(55L, "AUTO_QUALITY_REPROCESS")).thenReturn(2L);
    when(nicheCandidateRepository.findById(55L)).thenReturn(Optional.of(candidate));
    when(nicheCandidateRepository.save(any(OprmNicheCandidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

    CompleteRoutineQualityGateResponse response = service.complete(1001L, new CompleteRoutineQualityGateRequest(
        1001L, 10L, "TOO_CORPORATE", false, 75, 80, 10, card.getQualityNotes(), "test"));

    assertThat(response.qualityStatus()).isEqualTo("TOO_CORPORATE");
    assertThat(cycle.getFinishedAt()).isNotNull();
    assertThat(candidate.getRoutineResearchStatus()).isEqualTo("RESEARCH_RUNNING");
    assertThat(candidate.getLastRoutineResearchCycleId()).isEqualTo(1002L);
    verify(cycleRepository).countBySourceNicheIdAndTriggerSource(55L, "AUTO_QUALITY_REPROCESS");
    verify(nicheCandidateRepository).save(candidate);
  }

  /** Deve parar o reprocessamento automático no limite para evitar gasto infinito. */
  @Test
  void shouldNotAutomaticallyReprocessWhenLimitWasReached() {
    OprmRoutineResearchCycle cycle = new OprmRoutineResearchCycle();
    cycle.setId(1001L);
    cycle.setSourceNicheId(55L);
    cycle.setStatus("ROUTINE_SYNTHESIZED");
    OprmNicheRoutineCard card = card();
    when(cycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
    when(cardRepository.save(any(OprmNicheRoutineCard.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(cycleRepository.save(any(OprmRoutineResearchCycle.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(cycleRepository.countBySourceNicheIdAndTriggerSource(55L, "AUTO_QUALITY_REPROCESS")).thenReturn(3L);

    service.complete(1001L, new CompleteRoutineQualityGateRequest(
        1001L, 10L, "GENERIC", false, 75, 80, 10, "status=GENERIC", "test"));

    verify(nicheCandidateRepository, never()).save(any(OprmNicheCandidate.class));
  }

  /** Deve expor notas de qualidade como objeto JSON tipado para a tela de detalhe. */
  @Test
  void shouldDetailQualityNotesAsStructuredObject() {
    OprmRoutineResearchCycle cycle = new OprmRoutineResearchCycle();
    cycle.setId(1001L);
    cycle.setStatus("SOLUTION_CONTAMINATED");
    OprmNicheRoutineCard card = card();
    card.setQualityStatus("SOLUTION_CONTAMINATED");
    card.setReadyForHypothesis(false);
    card.setSpecificityScore(72);
    card.setConfidenceScore(32);
    card.setDuplicationScore(10);
    card.setQualityNotes("status=SOLUTION_CONTAMINATED; proximoMovimentoCodigo=REFAZER_BUSCA_SEM_SOLUCAO; proximoMovimento=Reexecutar busca removendo fontes de solucao; fontes=46; mixMinimoMeiAutonomo=false; riscoLinguagemSolucao=70");
    when(cycleRepository.findById(1001L)).thenReturn(Optional.of(cycle));
    when(cardRepository.findFirstByResearchCycleIdOrderByIdDesc(1001L)).thenReturn(Optional.of(card));

    RoutineQualityGateDetailResponse response = service.detail(1001L);

    assertThat(response.qualityNotes())
        .containsEntry("status", "SOLUTION_CONTAMINATED")
        .containsEntry("proximoMovimentoCodigo", "REFAZER_BUSCA_SEM_SOLUCAO")
        .containsEntry("proximoMovimento", "Reexecutar busca removendo fontes de solucao")
        .containsEntry("fontes", 46)
        .containsEntry("mixMinimoMeiAutonomo", false)
        .containsEntry("riscoLinguagemSolucao", 70);
  }

  /** Deve bloquear liberação para hipótese quando o status final ainda exige mais pesquisa MEI/autônomo. */
  @Test
  void shouldRejectReadyForHypothesisWhenQualityStatusDoesNotApproveMeiAudience() {
    assertThatThrownBy(() -> service.complete(1001L, new CompleteRoutineQualityGateRequest(
            1001L, 10L, "NEEDS_MORE_MEI_RESEARCH", true, 75, 80, 10, "Ainda falta evidência MEI", "test")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("readyForHypothesis can only be true");
  }

  /** Deve bloquear status MEI aprovado sem a liberação explícita para evitar contrato contraditório. */
  @Test
  void shouldRejectMeiAudienceReadyWithoutHypothesisReadiness() {
    assertThatThrownBy(() -> service.complete(1001L, new CompleteRoutineQualityGateRequest(
            1001L, 10L, "MEI_AUDIENCE_READY", false, 75, 80, 10, "Aprovado", "test")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("MEI_AUDIENCE_READY requires readyForHypothesis true");
  }

  /** Cria um cartão de rotina mínimo para os testes do serviço da etapa sete. */
  private OprmNicheRoutineCard card() {
    OprmNicheRoutineCard card = new OprmNicheRoutineCard();
    card.setId(10L);
    card.setResearchCycleId(1001L);
    card.setNicheName("Cabeleireiros");
    card.setRoutineSummary("Rotina");
    card.setPainsSummary("Dores");
    card.setResultsSummary("Resultados");
    card.setMechanismOpportunitiesSummary("Mecanismos");
    card.setEvidenceSummary("Evidências");
    card.setSourceDomains("a.com,b.com");
    card.setConfidenceScore(70);
    card.setRoutineEvidenceScore(86);
    card.setDifficultyEvidenceScore(84);
    card.setSourceDiversityScore(72);
    card.setSolutionLanguageRiskScore(10);
    card.setSynthesizedBy("test");
    card.setCreatedAt(Instant.parse("2026-06-04T00:00:00Z"));
    return card;
  }


  /** Cria um snapshot mínimo com classificações de atualidade e aderência brasileira. */
  private OprmSourceSnapshot snapshot(
      boolean solutionRisk, Integer brazilScore, Integer freshnessScore, boolean outdatedRisk, boolean corporateRisk, String domain) {
    OprmSourceSnapshot snapshot = new OprmSourceSnapshot();
    snapshot.setSolutionLanguageRisk(solutionRisk);
    snapshot.setBrazilRelevanceScore(brazilScore);
    snapshot.setSourceFreshnessScore(freshnessScore);
    snapshot.setOutdatedSourceRisk(outdatedRisk);
    snapshot.setStructuredBusinessDriftRisk(corporateRisk);
    snapshot.setSourceDomain(domain);
    return snapshot;
  }

  /** Cria um perfil MEI/autônomo mínimo para testar os contadores comportamentais do gate. */
  private OprmMeiAudienceProfile profile() {
    OprmMeiAudienceProfile profile = new OprmMeiAudienceProfile();
    profile.setCustomerAcquisitionBehavior("Clientes vêm por WhatsApp e Instagram.");
    profile.setChannelsUsed("Canais usados na rotina: WhatsApp, Instagram, indicação e telefone para retorno de clientes.");
    profile.setEmotionalPainsSummary("Medo de agenda vazia.");
    profile.setDreamsSummary("Agenda previsível.");
    profile.setFearsSummary("Perder clientes para concorrentes.");
    profile.setAutonomousProfessionalFitScore(82);
    profile.setBehavioralEvidenceScore(80);
    profile.setSourceFreshnessScore(76);
    profile.setOutdatedSourceRiskScore(0);
    profile.setStructuredBusinessDriftRiskScore(0);
    profile.setSolutionLanguageRiskScore(0);
    return profile;
  }

  /** Cria um sinal mínimo com o tipo informado para contagem de aliases. */
  private OprmExtractedSignal signal(String type) {
    OprmExtractedSignal signal = new OprmExtractedSignal();
    signal.setSignalType(type);
    return signal;
  }
}
