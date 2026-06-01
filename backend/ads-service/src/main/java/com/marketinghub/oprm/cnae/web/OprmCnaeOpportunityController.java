package com.marketinghub.oprm.cnae.web;

import com.marketinghub.oprm.cnae.dto.OprmCnaeCycleResponseDto;
import com.marketinghub.oprm.cnae.dto.OprmCnaeCycleUpsertRequestDto;
import com.marketinghub.oprm.cnae.dto.OprmCnaeEnrichmentRequestDto;
import com.marketinghub.oprm.cnae.dto.OprmCnaeOpportunityCandidateDto;
import com.marketinghub.oprm.cnae.dto.OprmCnaeOpportunityScoreRequestDto;
import com.marketinghub.oprm.cnae.dto.OprmCnaeOpportunityScoreResponseDto;
import com.marketinghub.oprm.cnae.dto.OprmNicheCandidateApprovalRequestDto;
import com.marketinghub.oprm.cnae.dto.OprmNicheCandidateResponseDto;
import com.marketinghub.oprm.cnae.service.OprmCnaeOpportunityPersistenceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller OPRM responsável por expor APIs de leitura e gravação do fluxo CNAE de oportunidade.
 */
@RestController
@RequestMapping("/api/oprm")
public class OprmCnaeOpportunityController {
    private final OprmCnaeOpportunityPersistenceService service;

    /**
     * Inicializa o controller com o serviço de persistência do fluxo CNAE.
     */
    public OprmCnaeOpportunityController(OprmCnaeOpportunityPersistenceService service) {
        this.service = service;
    }

    /**
     * Lista CNAEs sem score para processamento automático pelo módulo OPRM.
     */
    @GetMapping("/cnaes/opportunity-scores/missing")
    public List<OprmCnaeOpportunityCandidateDto> listMissingScores(@RequestParam(defaultValue = "50") int limit) {
        return service.listMissingScores(limit);
    }

    /**
     * Grava score de oportunidade calculado pelo módulo OPRM para um CNAE.
     */
    @PutMapping("/cnaes/{cnaeCode}/opportunity-score")
    public OprmCnaeOpportunityScoreResponseDto saveScore(
            @PathVariable String cnaeCode,
            @Valid @RequestBody OprmCnaeOpportunityScoreRequestDto request) {
        return service.saveScore(cnaeCode, request);
    }

    /**
     * Lista os melhores scores já calculados para enriquecimento automático pelo OPRM.
     */
    @GetMapping("/cnaes/opportunity-scores/top")
    public List<OprmCnaeOpportunityScoreResponseDto> listTopScores(
            @RequestParam(defaultValue = "25") int limit,
            @RequestParam(defaultValue = "false") boolean notEnriched) {
        return service.listTopScores(limit, notEnriched);
    }

    /**
     * Cria ou atualiza ciclo operacional CNAE informado pelo OPRM.
     */
    @PostMapping("/cnae-cycles")
    public OprmCnaeCycleResponseDto createCycle(@Valid @RequestBody OprmCnaeCycleUpsertRequestDto request) {
        return service.upsertCycle(request);
    }

    /**
     * Atualiza ciclo operacional CNAE informado pelo OPRM.
     */
    @PatchMapping("/cnae-cycles/{cycleId}")
    public OprmCnaeCycleResponseDto updateCycle(
            @PathVariable String cycleId,
            @Valid @RequestBody OprmCnaeCycleUpsertRequestDto request) {
        return service.upsertCycle(new OprmCnaeCycleUpsertRequestDto(
                cycleId,
                request.cycleType(),
                request.cycleNumber(),
                request.status(),
                request.selectionCriteria(),
                request.processedCount(),
                request.failedCount(),
                request.startedAt(),
                request.finishedAt(),
                request.summary(),
                request.errorMessage()));
    }

    /**
     * Lista ciclos operacionais recentes para acompanhamento.
     */
    @GetMapping("/cnae-cycles")
    public List<OprmCnaeCycleResponseDto> listCycles(@RequestParam(defaultValue = "50") int limit) {
        return service.listCycles(limit);
    }

    /**
     * Retorna o próximo número sequencial de ciclo para o tipo solicitado.
     */
    @GetMapping("/cnae-cycles/next-number")
    public Long nextCycleNumber(@RequestParam String cycleType) {
        return service.nextCycleNumber(cycleType);
    }

    /**
     * Grava enriquecimento e candidatos de nicho produzidos pelo módulo OPRM.
     */
    @PostMapping("/cnae-enrichments")
    public List<OprmNicheCandidateResponseDto> saveEnrichment(
            @Valid @RequestBody OprmCnaeEnrichmentRequestDto request) {
        return service.saveEnrichment(request);
    }

    /**
     * Lista candidatos de nicho de um CNAE para decisão humana no frontend.
     */
    @GetMapping("/cnae-niche-candidates")
    public List<OprmNicheCandidateResponseDto> listCandidates(@RequestParam String cnaeCode) {
        return service.listCandidates(cnaeCode);
    }

    /**
     * Lista nichos já enriquecidos pelo OPRM para acompanhamento direto do usuário.
     */
    @GetMapping("/cnae-niche-candidates/enriched")
    public List<OprmNicheCandidateResponseDto> listEnrichedCandidates(@RequestParam(defaultValue = "100") int limit) {
        return service.listEnrichedCandidates(limit);
    }

    /**
     * Aprova candidato de nicho e opcionalmente vincula um nicho oficial já existente.
     */
    @PostMapping("/cnae-niche-candidates/{id}/approve")
    public OprmNicheCandidateResponseDto approveCandidate(
            @PathVariable Long id,
            @RequestBody(required = false) OprmNicheCandidateApprovalRequestDto request) {
        return service.approveCandidate(id, request);
    }

    /**
     * Rejeita candidato de nicho após decisão humana no frontend.
     */
    @PostMapping("/cnae-niche-candidates/{id}/reject")
    public OprmNicheCandidateResponseDto rejectCandidate(@PathVariable Long id) {
        return service.rejectCandidate(id);
    }
}
