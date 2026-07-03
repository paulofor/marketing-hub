package com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.controller;

import com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.service.SalesPagePatternsPagePatternExtractionService;
import com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.service.pending.SalesPagePatternsPagePatternExtractionPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.service.pending.SalesPagePatternsPagePatternExtractionPendingResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.service.receberequest.SalesPagePatternsPagePatternExtractionRecebeRequestRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.service.receberequest.SalesPagePatternsPagePatternExtractionRecebeRequestResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.service.receberesponse.SalesPagePatternsPagePatternExtractionRecebeResponseRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.salespagepatterns.v1.pagepatternextraction.service.receberesponse.SalesPagePatternsPagePatternExtractionRecebeResponseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a borda HTTP interna da extração de padrões do pipeline salespagepatterns.v1. */
@RestController
@Slf4j
@RequestMapping("/api/internal/moissaleslibraryworker/salespagepatterns/v1/page-pattern-extraction/stage-executions")
@RequiredArgsConstructor
public class SalesPagePatternsPagePatternExtractionController {

    private final SalesPagePatternsPagePatternExtractionService service;

    /** Inicia manualmente a etapa para a página informada. */
    @PostMapping("/{idExterno}/start")
    public void start(@PathVariable("idExterno") String idExterno) {
        service.start(idExterno);
    }

    /** Recebe o request bruto enviado pelo worker. */
    @PostMapping("/{idExterno}/{jobId}/recebeRequest")
    public SalesPagePatternsPagePatternExtractionRecebeRequestResponse recebeRequest(
            @PathVariable("idExterno") String idExterno,
            @PathVariable("jobId") String jobId,
            @Valid @RequestBody SalesPagePatternsPagePatternExtractionRecebeRequestRequest request) {
        return service.recebeRequest(idExterno, jobId, request);
    }

    /** Recebe a resposta bruta e funcional enviada pelo worker. */
    @PostMapping("/{idExterno}/{jobId}/recebeResponse")
    public SalesPagePatternsPagePatternExtractionRecebeResponseResponse recebeResponse(
            @PathVariable("idExterno") String idExterno,
            @PathVariable("jobId") String jobId,
            @Valid @RequestBody SalesPagePatternsPagePatternExtractionRecebeResponseRequest request) {
        log.info("Recebendo response salespagepatterns.v1: etapa={}, idExterno={}, jobId={}, payload={}",
                "page-pattern-extraction", idExterno, jobId, request);
        return service.recebeResponse(idExterno, jobId, request);
    }

    /** Expõe o endpoint pending canônico consumido pelo módulo executor. */
    @PostMapping("/pending")
    public SalesPagePatternsPagePatternExtractionPendingResponse pending(
            @Valid @RequestBody SalesPagePatternsPagePatternExtractionPendingRequest request) {
        return service.pending(request);
    }
}
