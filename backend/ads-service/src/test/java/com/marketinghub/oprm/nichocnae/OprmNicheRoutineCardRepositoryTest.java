package com.marketinghub.oprm.nichocnae;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfile;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheRoutineCardRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfileRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

/** Testes responsáveis por validar as filas derivadas dos cartões de rotina do OPRM NichoCNAE. */
@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class OprmNicheRoutineCardRepositoryTest {
  @Autowired OprmRoutineResearchCycleRepository cycleRepository;
  @Autowired OprmNicheRoutineCardRepository routineCardRepository;
  @Autowired OprmMeiAudienceProfileRepository meiAudienceProfileRepository;

  /** Garante que a fila MEI/autônomo expõe somente o ciclo ativo sintetizado mais recente. */
  @Test
  void findPendingMeiAudienceSegmentationShouldReturnOnlyLatestEligibleSynthesizedCycle() {
    OprmRoutineResearchCycle oldSynthesized = saveCycle(100L, "ROUTINE_SYNTHESIZED", "2026-06-01T10:00:00Z");
    OprmNicheRoutineCard oldCard = saveCard(oldSynthesized, "cartão antigo sintetizado");
    OprmRoutineResearchCycle latestSynthesized = saveCycle(100L, "ROUTINE_SYNTHESIZED", "2026-06-02T10:00:00Z");
    OprmNicheRoutineCard latestCard = saveCard(latestSynthesized, "cartão recente sintetizado");
    OprmRoutineResearchCycle failed = saveCycle(200L, "FAILED", "2026-06-03T10:00:00Z");
    OprmNicheRoutineCard failedCard = saveCard(failed, "cartão falho");
    OprmRoutineResearchCycle cancelled = saveCycle(300L, "CANCELLED_BY_MANUAL_RESTART", "2026-06-04T10:00:00Z");
    OprmNicheRoutineCard cancelledCard = saveCard(cancelled, "cartão cancelado");
    OprmRoutineResearchCycle segmented = saveCycle(400L, "MEI_AUDIENCE_SEGMENTED", "2026-06-05T10:00:00Z");
    OprmNicheRoutineCard segmentedCard = saveCard(segmented, "cartão já segmentado");
    saveProfile(segmented, segmentedCard);

    assertThat(routineCardRepository.findPendingMeiAudienceSegmentation(PageRequest.of(0, 10)))
        .extracting(OprmNicheRoutineCard::getId)
        .containsExactly(latestCard.getId())
        .doesNotContain(oldCard.getId(), failedCard.getId(), cancelledCard.getId(), segmentedCard.getId());
  }

  /** Garante que a fila do gate de qualidade não fica presa por cartões antigos sem perfil MEI/autônomo. */
  @Test
  void findPendingRoutineQualityGateShouldIgnoreCardsWithoutMeiAudienceProfileBeforeApplyingLimit() {
    for (int index = 0; index < 12; index++) {
      OprmRoutineResearchCycle oldCycle = saveCycle(500L + index, "ROUTINE_SYNTHESIZED", "2026-06-06T10:0" + (index % 10) + ":00Z");
      saveCard(oldCycle, "cartão antigo sem perfil " + index);
    }
    OprmRoutineResearchCycle eligibleCycle = saveCycle(900L, "MEI_AUDIENCE_SEGMENTED", "2026-06-07T10:00:00Z");
    OprmNicheRoutineCard eligibleCard = saveCard(eligibleCycle, "cartão elegível com perfil MEI");
    saveProfile(eligibleCycle, eligibleCard);

    assertThat(routineCardRepository.findPendingRoutineQualityGate(PageRequest.of(0, 10)))
        .extracting(OprmNicheRoutineCard::getId)
        .containsExactly(eligibleCard.getId());
  }

  /** Persiste um ciclo com os campos mínimos exigidos pelo contrato JPA do NichoCNAE. */
  private OprmRoutineResearchCycle saveCycle(Long sourceNicheId, String status, String startedAt) {
    Instant start = Instant.parse(startedAt);
    OprmRoutineResearchCycle cycle = new OprmRoutineResearchCycle();
    cycle.setSourceNicheId(sourceNicheId);
    cycle.setCnaeCode("9602501");
    cycle.setCnaeDescription("Cabeleireiros, manicure e pedicure");
    cycle.setNicheName("manicures autônomas");
    cycle.setOriginalNicheName("manicures autônomas");
    cycle.setNeutralNicheName("serviços pessoais de beleza");
    cycle.setResearchMode("NEUTRAL_ROUTINE_RESEARCH");
    cycle.setSolutionLanguageRiskScore(BigDecimal.ZERO);
    cycle.setSourceScore(BigDecimal.valueOf(85));
    cycle.setTriggerSource("TEST");
    cycle.setStatus(status);
    cycle.setTotalQueries(1);
    cycle.setTotalSourceCandidates(1);
    cycle.setTotalSourceSnapshots(1);
    cycle.setTotalExtractedSignals(1);
    cycle.setStartedAt(start);
    cycle.setFinishedAt(start.plusSeconds(60));
    cycle.setCreatedAt(start);
    cycle.setUpdatedAt(start);
    return cycleRepository.saveAndFlush(cycle);
  }

  /** Persiste um cartão de rotina vinculado ao ciclo informado para alimentar a fila MEI/autônomo. */
  private OprmNicheRoutineCard saveCard(OprmRoutineResearchCycle cycle, String routineSummary) {
    OprmNicheRoutineCard card = new OprmNicheRoutineCard();
    card.setResearchCycleId(cycle.getId());
    card.setNicheName(cycle.getNicheName());
    card.setRoutineSummary(routineSummary);
    card.setPainsSummary("Dores operacionais recorrentes da rotina.");
    card.setCustomerBehaviorSummary("Capta clientes por indicação e canais próprios.");
    card.setChannelsSummary("WhatsApp e Instagram.");
    card.setOperationalPainsSummary("Agenda instável e retrabalho.");
    card.setEmotionalPainsSummary("Insegurança de renda.");
    card.setDreamsSummary("Agenda previsível.");
    card.setFearsSummary("Ficar sem clientes.");
    card.setLanguageSummary("agenda cheia, cliente fixa, preço justo");
    card.setResultsSummary("Rotina mais previsível e organizada.");
    card.setMechanismOpportunitiesSummary("Organização operacional e priorização de tarefas.");
    card.setEvidenceSummary("Evidências brasileiras recentes sobre rotina autônoma.");
    card.setSourceDomains("example.com");
    card.setConfidenceScore(90);
    card.setRoutineEvidenceScore(88);
    card.setDifficultyEvidenceScore(82);
    card.setSourceDiversityScore(75);
    card.setSolutionLanguageRiskScore(0);
    card.setReadyForHypothesis(false);
    card.setSynthesizedBy("test");
    card.setCreatedAt(cycle.getStartedAt().plusSeconds(30));
    return routineCardRepository.saveAndFlush(card);
  }

  /** Persiste um perfil MEI/autônomo para simular ciclo que já saiu da fila de segmentação. */
  private void saveProfile(OprmRoutineResearchCycle cycle, OprmNicheRoutineCard card) {
    OprmMeiAudienceProfile profile = new OprmMeiAudienceProfile();
    profile.setResearchCycleId(cycle.getId());
    profile.setRoutineCardId(card.getId());
    profile.setSourceNicheCandidateId(cycle.getSourceNicheId());
    profile.setCnaeCode(cycle.getCnaeCode());
    profile.setCnaeDescription(cycle.getCnaeDescription());
    profile.setNeutralNicheName(cycle.getNeutralNicheName());
    profile.setAudienceName("manicures MEI que atendem por agenda própria");
    profile.setWorkMode("Atendimento autônomo com agenda própria.");
    profile.setDailyRoutineSummary("Agenda, atende, cobra e reorganiza horários.");
    profile.setOperationalPainsSummary("Cancelamentos e retrabalho.");
    profile.setRecentSourceSummary("Fontes recentes sobre rotina de manicures autônomas.");
    profile.setAutonomousProfessionalFitScore(92);
    profile.setBehavioralEvidenceScore(88);
    profile.setSourceFreshnessScore(81);
    profile.setOutdatedSourceRiskScore(12);
    profile.setStructuredBusinessDriftRiskScore(9);
    profile.setSolutionLanguageRiskScore(0);
    profile.setCreatedAt(cycle.getUpdatedAt());
    profile.setUpdatedAt(cycle.getUpdatedAt());
    meiAudienceProfileRepository.saveAndFlush(profile);
  }
}
