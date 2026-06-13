package com.marketinghub.oprm.nichocnae;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmSourceCandidateRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

/** Testes responsáveis por validar as filas de fontes candidatas do OPRM NichoCNAE. */
@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class OprmSourceCandidateRepositoryTest {
  @Autowired OprmRoutineResearchCycleRepository cycleRepository;
  @Autowired OprmSourceCandidateRepository sourceCandidateRepository;

  /** Garante que a coleta ignora pendências residuais de ciclos falhados ou encerrados. */
  @Test
  void findPendingForFetchFromActiveCyclesShouldIgnoreClosedCycles() {
    OprmRoutineResearchCycle failedCycle = saveCycle("FAILED", "2026-06-13T02:04:18Z", true);
    OprmSourceCandidate failedCandidate = saveCandidate(failedCycle, 1);
    OprmRoutineResearchCycle cancelledCycle = saveCycle("CANCELLED_BY_MANUAL_RESTART", "2026-06-13T02:10:18Z", true);
    OprmSourceCandidate cancelledCandidate = saveCandidate(cancelledCycle, 2);
    OprmRoutineResearchCycle finishedRunningCycle = saveCycle("RUNNING", "2026-06-13T02:12:18Z", true);
    OprmSourceCandidate finishedRunningCandidate = saveCandidate(finishedRunningCycle, 3);
    OprmRoutineResearchCycle activeCycle = saveCycle("RUNNING", "2026-06-13T02:15:18Z", false);
    OprmSourceCandidate activeCandidate = saveCandidate(activeCycle, 4);

    assertThat(sourceCandidateRepository
            .findPendingForFetchFromActiveCycles("FOUND", "RUNNING", PageRequest.of(0, 10)))
        .extracting(OprmSourceCandidate::getId)
        .containsExactly(activeCandidate.getId())
        .doesNotContain(failedCandidate.getId(), cancelledCandidate.getId(), finishedRunningCandidate.getId());
  }

  /** Persiste um ciclo com status e finalização controlados para testar independência operacional. */
  private OprmRoutineResearchCycle saveCycle(String status, String startedAt, boolean finished) {
    Instant start = Instant.parse(startedAt);
    OprmRoutineResearchCycle cycle = new OprmRoutineResearchCycle();
    cycle.setSourceNicheId(77L);
    cycle.setCnaeCode("9602501");
    cycle.setCnaeDescription("Cabeleireiros, manicure e pedicure");
    cycle.setNicheName("manicures autônomas");
    cycle.setOriginalNicheName("manicures autônomas");
    cycle.setNeutralNicheName("serviços pessoais de beleza");
    cycle.setResearchMode("NEUTRAL_ROUTINE_RESEARCH");
    cycle.setSolutionLanguageRiskScore(BigDecimal.ZERO);
    cycle.setSourceScore(BigDecimal.valueOf(90));
    cycle.setTriggerSource("TEST");
    cycle.setStatus(status);
    cycle.setTotalQueries(1);
    cycle.setTotalSourceCandidates(1);
    cycle.setTotalSourceSnapshots(0);
    cycle.setTotalExtractedSignals(0);
    cycle.setStartedAt(start);
    cycle.setFinishedAt(finished ? start.plusSeconds(60) : null);
    cycle.setCreatedAt(start);
    cycle.setUpdatedAt(start);
    return cycleRepository.saveAndFlush(cycle);
  }

  /** Persiste uma fonte candidata encontrada para compor a fila de coleta curta. */
  private OprmSourceCandidate saveCandidate(OprmRoutineResearchCycle cycle, int position) {
    OprmSourceCandidate candidate = new OprmSourceCandidate();
    candidate.setResearchCycleId(cycle.getId());
    candidate.setResearchQueryId(cycle.getId() * 10);
    candidate.setSourceUrl("https://exemplo.com/fonte-" + position);
    candidate.setSourceTitle("Fonte " + position);
    candidate.setSourceSnippet("Rotina real de atendimento autônomo.");
    candidate.setSourceDomain("exemplo.com");
    candidate.setSourceGroup("ROUTINE_REPORT");
    candidate.setSourceIntent("ROUTINE_REPORT");
    candidate.setRoutineEvidenceScore(88);
    candidate.setCommercialPageRisk(false);
    candidate.setSolutionLanguageRisk(false);
    candidate.setSourceClassificationType("RECENT_SECTOR_CONTENT");
    candidate.setSourceFreshnessScore(95);
    candidate.setOutdatedSourceRisk(false);
    candidate.setBrazilRelevanceScore(90);
    candidate.setAutonomousProfessionalEvidenceScore(90);
    candidate.setStructuredBusinessDriftRisk(false);
    candidate.setSearchProvider("DUCKDUCKGO_HTML");
    candidate.setSearchPosition(position);
    candidate.setSelectedForFetch(false);
    candidate.setStatus("FOUND");
    candidate.setCreatedAt(cycle.getStartedAt());
    candidate.setUpdatedAt(cycle.getStartedAt());
    return sourceCandidateRepository.saveAndFlush(candidate);
  }
}
