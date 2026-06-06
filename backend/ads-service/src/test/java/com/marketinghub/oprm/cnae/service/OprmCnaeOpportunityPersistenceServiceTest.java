package com.marketinghub.oprm.cnae.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import com.marketinghub.oprm.cnae.dto.OprmCnaeEnrichmentRequestDto;
import com.marketinghub.oprm.cnae.dto.OprmNicheCandidateRequestDto;
import com.marketinghub.oprm.cnae.dto.OprmNicheCandidateResponseDto;
import com.marketinghub.oprm.cnae.repository.OprmCnaeOpportunityReadRepository;
import com.marketinghub.repository.jpa.oprm.cnae.OprmCnaeEnrichmentArtifactRepository;
import com.marketinghub.repository.jpa.oprm.cnae.OprmCnaeOpportunityScoreRepository;
import com.marketinghub.repository.jpa.oprm.cnae.OprmCnaeProcessingCycleRepository;
import com.marketinghub.repository.jpa.oprm.cnae.OprmNicheCandidateRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

/**
 * Teste responsável por validar a persistência consultiva do fluxo CNAE de oportunidade do OPRM.
 */
@ExtendWith(MockitoExtension.class)
class OprmCnaeOpportunityPersistenceServiceTest {

    @Mock
    private OprmCnaeOpportunityReadRepository opportunityReadRepository;

    @Mock
    private OprmCnaeOpportunityScoreRepository scoreRepository;

    @Mock
    private OprmCnaeProcessingCycleRepository cycleRepository;

    @Mock
    private OprmCnaeEnrichmentArtifactRepository artifactRepository;

    @Mock
    private OprmNicheCandidateRepository candidateRepository;

    private OprmCnaeOpportunityPersistenceService service;

    /**
     * Inicializa o serviço com dependências mockadas para isolar a consulta de candidatos enriquecidos.
     */
    @BeforeEach
    void setUp() {
        service = new OprmCnaeOpportunityPersistenceService(
                opportunityReadRepository,
                scoreRepository,
                cycleRepository,
                artifactRepository,
                candidateRepository);
    }

    /**
     * Garante que a listagem de nichos enriquecidos usa a consulta priorizada por maior score.
     */
    @Test
    void shouldListEnrichedCandidatesByHighestOpportunityScoreFirst() {
        OprmNicheCandidate topCandidate = candidate(1L, "8599604", "Mentoria infantil com IA", "90.00");
        OprmNicheCandidate lowerCandidate = candidate(2L, "8511200", "Playbook creche", "30.00");
        when(candidateRepository.findAllByOrderByOpportunityScoreDescCreatedAtDesc(any(Pageable.class)))
                .thenReturn(List.of(topCandidate, lowerCandidate));

        List<OprmNicheCandidateResponseDto> response = service.listEnrichedCandidates(100);

        assertThat(response)
                .extracting(OprmNicheCandidateResponseDto::opportunityScore)
                .containsExactly(new BigDecimal("90.00"), new BigDecimal("30.00"));
        verify(candidateRepository).findAllByOrderByOpportunityScoreDescCreatedAtDesc(any(Pageable.class));
    }

    /**
     * Garante que candidatos novos nascem pendentes para o pipeline de pesquisa de rotina.
     */
    @Test
    void shouldCreateEnrichedCandidateWithPendingRoutineResearchStatus() {
        when(candidateRepository.save(any(OprmNicheCandidate.class)))
                .thenAnswer(invocation -> {
                    OprmNicheCandidate saved = invocation.getArgument(0);
                    saved.setId(10L);
                    return saved;
                });

        service.saveEnrichment(new OprmCnaeEnrichmentRequestDto(
                "9602501",
                "OPRM-CNAE-ENRICHMENT-20260606-001",
                "rotina",
                "dores",
                "mecanismos",
                "provas",
                "ofertas",
                "fontes",
                List.of(new OprmNicheCandidateRequestDto(
                        "9602501",
                        "Cabeleireiros, manicure e pedicure",
                        "Cabeleireiros, manicure e pedicure",
                        "persona",
                        "dor",
                        "resultado",
                        "mecanismo",
                        "prova",
                        "oferta",
                        "volume",
                        new BigDecimal("90.00"),
                        "OPRM-CNAE-SCORE-20260606-001",
                        "OPRM-CNAE-ENRICHMENT-20260606-001",
                        "ENRICHED",
                        "artefatos"))));

        ArgumentCaptor<OprmNicheCandidate> candidateCaptor = ArgumentCaptor.forClass(OprmNicheCandidate.class);
        verify(candidateRepository).save(candidateCaptor.capture());
        assertThat(candidateCaptor.getValue().getRoutineResearchStatus()).isEqualTo("PENDING");
    }

    /**
     * Cria um candidato mínimo para validar o mapeamento de resposta do serviço.
     */
    private OprmNicheCandidate candidate(Long id, String cnaeCode, String nicheName, String opportunityScore) {
        OprmNicheCandidate candidate = new OprmNicheCandidate();
        candidate.setId(id);
        candidate.setCnaeCode(cnaeCode);
        candidate.setCnaeDescription("Educação infantil");
        candidate.setCandidateNicheName(nicheName);
        candidate.setPainHypothesis("Dor principal");
        candidate.setDesiredOutcome("Resultado desejado");
        candidate.setMechanismHypothesis("Mecanismo");
        candidate.setOpportunityScore(new BigDecimal(opportunityScore));
        candidate.setScoreCycleId("OPRM-CNAE-SCORE-20260601-001");
        candidate.setEnrichmentCycleId("OPRM-CNAE-ENRICHMENT-20260601-001");
        candidate.setStatus("ENRICHED");
        candidate.setCreatedAt(Instant.parse("2026-06-01T21:15:03Z"));
        candidate.setUpdatedAt(Instant.parse("2026-06-01T21:15:03Z"));
        return candidate;
    }
}
