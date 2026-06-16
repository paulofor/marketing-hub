package com.marketinghub.hypothesis.web;

import com.marketinghub.hypothesis.dto.HypothesisDto;
import com.marketinghub.hypothesis.mapper.HypothesisMapper;
import com.marketinghub.hypothesis.service.finalizeHypothesis.FinalizeHypothesisRequest;
import com.marketinghub.hypothesis.service.finalizeHypothesis.HypothesisPipelineFinalizationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor a etapa de fechamento do pipeline de hipótese fora da etapa Dor/Pain. */
@RestController
@RequestMapping("/api")
public class HypothesisPipelineFinalizationController {
    private final HypothesisPipelineFinalizationService finalizationService;
    private final HypothesisMapper hypothesisMapper;

    /** Inicializa o controller da etapa de fechamento com o serviço dedicado e o mapper de hipótese. */
    public HypothesisPipelineFinalizationController(
            HypothesisPipelineFinalizationService finalizationService,
            HypothesisMapper hypothesisMapper) {
        this.finalizationService = finalizationService;
        this.hypothesisMapper = hypothesisMapper;
    }

    /** Fecha o pipeline concluído como hipótese disponível no backlog para gerar experimento. */
    @PostMapping("/niches/{nicheId}/hypothesis-pipeline/finalize")
    public ResponseEntity<HypothesisDto> finalizeHypothesis(
            @PathVariable Long nicheId,
            @Valid @RequestBody FinalizeHypothesisRequest request) {
        return ResponseEntity.status(201)
                .body(hypothesisMapper.toDto(finalizationService.finalizeHypothesis(nicheId, request)));
    }
}
