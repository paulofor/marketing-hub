package com.marketinghub.oprm.cnae.service;

import com.marketinghub.oprm.cnae.OprmCnaeEnrichmentArtifact;
import com.marketinghub.oprm.cnae.OprmCnaeOpportunityScore;
import com.marketinghub.oprm.cnae.OprmCnaeProcessingCycle;
import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import com.marketinghub.oprm.cnae.dto.OprmCnaeCycleResponseDto;
import com.marketinghub.oprm.cnae.dto.OprmCnaeCycleUpsertRequestDto;
import com.marketinghub.oprm.cnae.dto.OprmCnaeEnrichmentRequestDto;
import com.marketinghub.oprm.cnae.dto.OprmCnaeOpportunityCandidateDto;
import com.marketinghub.oprm.cnae.dto.OprmCnaeOpportunityScoreRequestDto;
import com.marketinghub.oprm.cnae.dto.OprmCnaeOpportunityScoreResponseDto;
import com.marketinghub.oprm.cnae.dto.OprmNicheCandidateApprovalRequestDto;
import com.marketinghub.oprm.cnae.dto.OprmNicheCandidateRequestDto;
import com.marketinghub.oprm.cnae.dto.OprmNicheCandidateResponseDto;
import com.marketinghub.repository.jpa.oprm.cnae.OprmCnaeEnrichmentArtifactRepository;
import com.marketinghub.oprm.cnae.repository.OprmCnaeOpportunityReadRepository;
import com.marketinghub.repository.jpa.oprm.cnae.OprmCnaeOpportunityScoreRepository;
import com.marketinghub.repository.jpa.oprm.cnae.OprmCnaeProcessingCycleRepository;
import com.marketinghub.repository.jpa.oprm.cnae.OprmNicheCandidateRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Serviço responsável apenas por ler e persistir dados do fluxo CNAE de oportunidade solicitado pelo módulo OPRM.
 */
@Service
public class OprmCnaeOpportunityPersistenceService {
    private final OprmCnaeOpportunityReadRepository opportunityReadRepository;
    private final OprmCnaeOpportunityScoreRepository scoreRepository;
    private final OprmCnaeProcessingCycleRepository cycleRepository;
    private final OprmCnaeEnrichmentArtifactRepository artifactRepository;
    private final OprmNicheCandidateRepository candidateRepository;

    /**
     * Inicializa o serviço com repositórios de persistência e leitura operacional do fluxo CNAE.
     */
    public OprmCnaeOpportunityPersistenceService(
            OprmCnaeOpportunityReadRepository opportunityReadRepository,
            OprmCnaeOpportunityScoreRepository scoreRepository,
            OprmCnaeProcessingCycleRepository cycleRepository,
            OprmCnaeEnrichmentArtifactRepository artifactRepository,
            OprmNicheCandidateRepository candidateRepository) {
        this.opportunityReadRepository = opportunityReadRepository;
        this.scoreRepository = scoreRepository;
        this.cycleRepository = cycleRepository;
        this.artifactRepository = artifactRepository;
        this.candidateRepository = candidateRepository;
    }

    /**
     * Lista CNAEs sem score para que o módulo OPRM calcule a oportunidade fora do backend.
     */
    @Transactional(readOnly = true)
    public List<OprmCnaeOpportunityCandidateDto> listMissingScores(int limit) {
        return opportunityReadRepository.findMissingScores(normalizeLimit(limit));
    }

    /**
     * Persiste o score de oportunidade recebido do módulo OPRM para o CNAE informado.
     */
    @Transactional
    public OprmCnaeOpportunityScoreResponseDto saveScore(String cnaeCode, OprmCnaeOpportunityScoreRequestDto request) {
        OprmCnaeOpportunityScore score = scoreRepository.findById(cnaeCode).orElseGet(OprmCnaeOpportunityScore::new);
        score.setCnaeCode(cnaeCode);
        score.setCnaeDescription(request.cnaeDescription());
        score.setOpportunityScore(request.opportunityScore());
        score.setMarketVolumeScore(request.marketVolumeScore());
        score.setMeiDensityScore(request.meiDensityScore());
        score.setDigitalFitScore(request.digitalFitScore());
        score.setPainClarityScore(request.painClarityScore());
        score.setScoreJustification(request.scoreJustification());
        score.setAlgorithmVersion(request.algorithmVersion());
        score.setCycleId(request.cycleId());
        score.setScoredAt(request.scoredAt());
        score.setScoreStatus(request.scoreStatus());
        return toScoreResponse(scoreRepository.save(score));
    }

    /**
     * Lista scores já calculados pelo OPRM, com filtro opcional para retornar somente itens ainda não enriquecidos.
     */
    @Transactional(readOnly = true)
    public List<OprmCnaeOpportunityScoreResponseDto> listTopScores(int limit, boolean notEnriched) {
        List<OprmCnaeOpportunityScore> scores = notEnriched
                ? scoreRepository.findByEnrichedAtIsNullOrderByOpportunityScoreDescCnaeCodeAsc(PageRequest.of(0, normalizeLimit(limit)))
                : scoreRepository.findAllByOrderByOpportunityScoreDescCnaeCodeAsc(PageRequest.of(0, normalizeLimit(limit)));
        return scores.stream()
                .map(this::toScoreResponse)
                .toList();
    }

    /**
     * Cria ou atualiza um ciclo operacional enviado pelo módulo OPRM.
     */
    @Transactional
    public OprmCnaeCycleResponseDto upsertCycle(OprmCnaeCycleUpsertRequestDto request) {
        OprmCnaeProcessingCycle cycle = cycleRepository.findById(request.cycleId()).orElseGet(OprmCnaeProcessingCycle::new);
        cycle.setCycleId(request.cycleId());
        cycle.setCycleType(request.cycleType());
        cycle.setCycleNumber(request.cycleNumber());
        cycle.setStatus(request.status());
        cycle.setSelectionCriteria(request.selectionCriteria());
        cycle.setProcessedCount(request.processedCount() == null ? 0 : request.processedCount());
        cycle.setFailedCount(request.failedCount() == null ? 0 : request.failedCount());
        cycle.setStartedAt(request.startedAt());
        cycle.setFinishedAt(request.finishedAt());
        cycle.setSummary(request.summary());
        cycle.setErrorMessage(request.errorMessage());
        return toCycleResponse(cycleRepository.save(cycle));
    }

    /**
     * Lista ciclos operacionais recentes para acompanhamento do frontend e do OPRM.
     */
    @Transactional(readOnly = true)
    public List<OprmCnaeCycleResponseDto> listCycles(int limit) {
        return cycleRepository.findAllByOrderByStartedAtDesc(PageRequest.of(0, normalizeLimit(limit)))
                .stream()
                .map(this::toCycleResponse)
                .toList();
    }

    /**
     * Retorna o próximo número de ciclo para o tipo informado sem executar regra de negócio de oportunidade.
     */
    @Transactional(readOnly = true)
    public Long nextCycleNumber(String cycleType) {
        return cycleRepository.nextCycleNumber(cycleType);
    }

    /**
     * Persiste artefatos de enriquecimento e candidatos enviados pelo OPRM.
     */
    @Transactional
    public List<OprmNicheCandidateResponseDto> saveEnrichment(OprmCnaeEnrichmentRequestDto request) {
        OprmCnaeEnrichmentArtifact artifact = new OprmCnaeEnrichmentArtifact();
        artifact.setCnaeCode(request.cnaeCode());
        artifact.setEnrichmentCycleId(request.enrichmentCycleId());
        artifact.setRoutineSignals(request.routineSignals());
        artifact.setPainSignals(request.painSignals());
        artifact.setMechanismSignals(request.mechanismSignals());
        artifact.setProofSignals(request.proofSignals());
        artifact.setOfferSignals(request.offerSignals());
        artifact.setSourceSummary(request.sourceSummary());
        artifact.setCreatedAt(Instant.now());
        artifactRepository.save(artifact);

        scoreRepository.findById(request.cnaeCode()).ifPresent(score -> {
            score.setEnrichedAt(Instant.now());
            scoreRepository.save(score);
        });

        return request.candidates().stream()
                .map(this::saveCandidate)
                .map(this::toCandidateResponse)
                .toList();
    }

    /**
     * Lista candidatos de nicho persistidos para um CNAE específico.
     */
    @Transactional(readOnly = true)
    public List<OprmNicheCandidateResponseDto> listCandidates(String cnaeCode) {
        return candidateRepository.findByCnaeCodeOrderByOpportunityScoreDescCreatedAtDesc(cnaeCode)
                .stream()
                .map(this::toCandidateResponse)
                .toList();
    }

    /**
     * Lista os nichos já enriquecidos pelo OPRM para consulta direta no frontend.
     */
    @Transactional(readOnly = true)
    public List<OprmNicheCandidateResponseDto> listEnrichedCandidates(int limit) {
        return candidateRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, normalizeLimit(limit)))
                .stream()
                .map(this::toCandidateResponse)
                .toList();
    }

    /**
     * Marca um candidato como aprovado e opcionalmente vincula um nicho oficial já existente.
     */
    @Transactional
    public OprmNicheCandidateResponseDto approveCandidate(Long id, OprmNicheCandidateApprovalRequestDto request) {
        OprmNicheCandidate candidate = loadCandidate(id);
        candidate.setStatus("APPROVED");
        if (request != null && request.marketNicheId() != null) {
            candidate.setMarketNicheId(request.marketNicheId());
        }
        candidate.setUpdatedAt(Instant.now());
        return toCandidateResponse(candidateRepository.save(candidate));
    }

    /**
     * Marca um candidato como rejeitado após decisão humana no frontend.
     */
    @Transactional
    public OprmNicheCandidateResponseDto rejectCandidate(Long id) {
        OprmNicheCandidate candidate = loadCandidate(id);
        candidate.setStatus("REJECTED");
        candidate.setUpdatedAt(Instant.now());
        return toCandidateResponse(candidateRepository.save(candidate));
    }

    /**
     * Carrega um candidato existente ou retorna erro HTTP 404 quando o id não existe.
     */
    private OprmNicheCandidate loadCandidate(Long id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidato de nicho não encontrado: " + id));
    }

    /**
     * Normaliza limites de consulta para manter paginação previsível no backend.
     */
    private int normalizeLimit(int limit) {
        if (limit < 1) {
            return 50;
        }
        return Math.min(limit, 500);
    }

    /**
     * Persiste um candidato individual recebido do módulo OPRM.
     */
    private OprmNicheCandidate saveCandidate(OprmNicheCandidateRequestDto request) {
        Instant now = Instant.now();
        OprmNicheCandidate candidate = new OprmNicheCandidate();
        candidate.setCnaeCode(request.cnaeCode());
        candidate.setCnaeDescription(request.cnaeDescription());
        candidate.setCandidateNicheName(request.candidateNicheName());
        candidate.setPersona(request.persona());
        candidate.setPainHypothesis(request.painHypothesis());
        candidate.setDesiredOutcome(request.desiredOutcome());
        candidate.setMechanismHypothesis(request.mechanismHypothesis());
        candidate.setProofDirection(request.proofDirection());
        candidate.setOfferIdea(request.offerIdea());
        candidate.setMarketVolumeSignals(request.marketVolumeSignals());
        candidate.setOpportunityScore(request.opportunityScore());
        candidate.setScoreCycleId(request.scoreCycleId());
        candidate.setEnrichmentCycleId(request.enrichmentCycleId());
        candidate.setStatus(request.status());
        candidate.setSourceArtifacts(request.sourceArtifacts());
        candidate.setCreatedAt(now);
        candidate.setUpdatedAt(now);
        return candidateRepository.save(candidate);
    }

    /**
     * Converte entidade de score para DTO de resposta.
     */
    private OprmCnaeOpportunityScoreResponseDto toScoreResponse(OprmCnaeOpportunityScore score) {
        return new OprmCnaeOpportunityScoreResponseDto(
                score.getCnaeCode(),
                score.getCnaeDescription(),
                score.getOpportunityScore(),
                score.getMarketVolumeScore(),
                score.getMeiDensityScore(),
                score.getDigitalFitScore(),
                score.getPainClarityScore(),
                score.getScoreJustification(),
                score.getAlgorithmVersion(),
                score.getCycleId(),
                score.getScoredAt(),
                score.getScoreStatus(),
                score.getEnrichedAt());
    }

    /**
     * Converte entidade de ciclo para DTO de resposta.
     */
    private OprmCnaeCycleResponseDto toCycleResponse(OprmCnaeProcessingCycle cycle) {
        return new OprmCnaeCycleResponseDto(
                cycle.getCycleId(),
                cycle.getCycleType(),
                cycle.getCycleNumber(),
                cycle.getStatus(),
                cycle.getSelectionCriteria(),
                cycle.getProcessedCount(),
                cycle.getFailedCount(),
                cycle.getStartedAt(),
                cycle.getFinishedAt(),
                cycle.getSummary(),
                cycle.getErrorMessage());
    }

    /**
     * Converte entidade de candidato para DTO de resposta.
     */
    private OprmNicheCandidateResponseDto toCandidateResponse(OprmNicheCandidate candidate) {
        return new OprmNicheCandidateResponseDto(
                candidate.getId(),
                candidate.getCnaeCode(),
                candidate.getCnaeDescription(),
                candidate.getCandidateNicheName(),
                candidate.getPersona(),
                candidate.getPainHypothesis(),
                candidate.getDesiredOutcome(),
                candidate.getMechanismHypothesis(),
                candidate.getProofDirection(),
                candidate.getOfferIdea(),
                candidate.getMarketVolumeSignals(),
                candidate.getOpportunityScore(),
                candidate.getScoreCycleId(),
                candidate.getEnrichmentCycleId(),
                candidate.getStatus(),
                candidate.getSourceArtifacts(),
                candidate.getMarketNicheId(),
                candidate.getCreatedAt(),
                candidate.getUpdatedAt());
    }
}
