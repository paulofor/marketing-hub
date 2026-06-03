package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.web;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto.MoisSalesLibraryDtos;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.MoisSalesLibraryService;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.MoisSalesLibrarySnapshotService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Expõe os contratos HTTP da Biblioteca de Páginas de Vendas do MOIS.
 */
@RestController
@RequestMapping("/api/mois/sales-library")
@RequiredArgsConstructor
@Validated
@Slf4j
public class MoisSalesLibraryController {

    private final MoisSalesLibraryService service;
    private final MoisSalesLibrarySnapshotService snapshotService;


    /**
     * Reserva a próxima URL de referência coletada para captura de HTML bruto pelo worker MOIS.
     */
    @PostMapping("/collected-reference-html:claim")
    public MoisSalesLibraryDtos.CollectedReferenceHtmlClaimResponse claimCollectedReferenceHtml(
            @Valid @RequestBody MoisSalesLibraryDtos.CollectedReferenceHtmlClaimRequest request
    ) {
        return service.claimCollectedReferenceHtml(request);
    }

    /**
     * Recebe o HTML bruto capturado pelo worker MOIS para a referência reservada.
     */
    @PostMapping("/collected-reference-html/{captureId}:complete")
    public MoisSalesLibraryDtos.CollectedReferenceHtmlPersistResponse completeCollectedReferenceHtml(
            @PathVariable long captureId,
            @Valid @RequestBody MoisSalesLibraryDtos.CollectedReferenceHtmlCompleteRequest request
    ) {
        return service.completeCollectedReferenceHtml(captureId, request);
    }

    /**
     * Registra falha na captura de HTML bruto de uma referência reservada.
     */
    @PostMapping("/collected-reference-html/{captureId}:fail")
    public MoisSalesLibraryDtos.CollectedReferenceHtmlPersistResponse failCollectedReferenceHtml(
            @PathVariable long captureId,
            @RequestBody MoisSalesLibraryDtos.CollectedReferenceHtmlFailRequest request
    ) {
        return service.failCollectedReferenceHtml(captureId, request);
    }

    /**
     * Lista entradas de URLs ingeridas pela biblioteca.
     */
    @GetMapping("/entries")
    public MoisSalesLibraryDtos.SalesLibraryEntryPageResponse listEntries(
            @RequestParam String workspaceId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return service.listEntries(workspaceId, page, pageSize);
    }

    /**
     * Lista jobs de processamento da biblioteca.
     */
    @GetMapping("/jobs")
    public MoisSalesLibraryDtos.SalesLibraryJobPageResponse listJobs(
            @RequestParam String workspaceId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return service.listJobs(workspaceId, status, page, pageSize);
    }

    /**
     * Busca um job de processamento pelo identificador.
     */
    @GetMapping("/jobs/{jobId}")
    public MoisSalesLibraryDtos.SalesLibraryJobResponse getJob(@PathVariable long jobId) {
        try {
            return service.getJob(jobId);
        } catch (IllegalArgumentException ex) {
            log.warn("Biblioteca de páginas de vendas não encontrou job solicitado. operacao=getJob, jobId={}, erro={}", jobId, ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    /**
     * Lista páginas canônicas registradas na biblioteca.
     */
    @GetMapping("/pages")
    public MoisSalesLibraryDtos.SalesLibraryPageListResponse listPages(
            @RequestParam String workspaceId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return service.listPages(workspaceId, page, pageSize);
    }

    /**
     * Busca os dados básicos de uma página canônica.
     */
    @GetMapping("/pages/{pageId}")
    public MoisSalesLibraryDtos.SalesLibraryPageResponse getPage(@PathVariable long pageId) {
        try {
            return service.getPage(pageId);
        } catch (IllegalArgumentException ex) {
            log.warn("Biblioteca de páginas de vendas não encontrou página solicitada. operacao=getPage, pageId={}, erro={}", pageId, ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    /**
     * Busca a análise mais recente de uma página canônica.
     */
    @GetMapping("/pages/{pageId}/analysis")
    public MoisSalesLibraryDtos.SalesLibraryPageAnalysisResponse getPageAnalysis(@PathVariable long pageId) {
        try {
            return service.getPageAnalysis(pageId);
        } catch (IllegalArgumentException ex) {
            log.warn("Biblioteca de páginas de vendas não encontrou análise solicitada. operacao=getPageAnalysis, pageId={}, erro={}", pageId, ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    /**
     * Solicita reanálise de uma página existente.
     */
    @PostMapping("/pages/{pageId}:reanalyze")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MoisSalesLibraryDtos.SalesLibraryReanalyzeResponse reanalyzePage(@PathVariable long pageId) {
        try {
            return service.reanalyzePage(pageId);
        } catch (IllegalArgumentException ex) {
            log.warn("Biblioteca de páginas de vendas não conseguiu solicitar reanálise. operacao=reanalyzePage, pageId={}, erro={}", pageId, ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }


    /**
     * Permite atualizar manualmente o status da análise para pendente ou anulado.
     */
    @PostMapping("/pages/{pageId}:status")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MoisSalesLibraryDtos.SalesLibraryStatusUpdateResponse updatePageStatus(
            @PathVariable long pageId,
            @Valid @RequestBody MoisSalesLibraryDtos.SalesLibraryStatusUpdateRequest request
    ) {
        try {
            return service.updatePageStatus(pageId, request);
        } catch (IllegalArgumentException ex) {
            log.warn("Biblioteca de páginas de vendas rejeitou atualização manual de status. operacao=updatePageStatus, pageId={}, status={}, erro={}", pageId, request.status(), ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /**
     * Solicita captura de snapshots para páginas sem captura recente.
     */
    @PostMapping("/snapshots:capture")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureResponse captureSnapshots(
            @Valid @RequestBody MoisSalesLibraryDtos.SalesLibrarySnapshotCaptureRequest request
    ) {
        return snapshotService.captureSnapshots(request);
    }

    /**
     * Lista snapshots já capturados para uma página.
     */
    @GetMapping("/pages/{pageId}/snapshots")
    public List<MoisSalesLibraryDtos.SalesLibraryPageSnapshotResponse> listPageSnapshots(@PathVariable long pageId) {
        return snapshotService.listSnapshots(pageId);
    }

    /**
     * Reserva o próximo job pendente para o worker.
     */
    @PostMapping("/jobs:claim")
    public MoisSalesLibraryDtos.SalesLibraryClaimResponse claimJob(
            @Valid @RequestBody MoisSalesLibraryDtos.SalesLibraryClaimRequest request
    ) {
        return service.claimJob(request);
    }

    /**
     * Recebe o resultado final de análise de um job.
     */
    @PostMapping("/jobs/{jobId}:complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void completeJob(
            @PathVariable long jobId,
            @RequestBody MoisSalesLibraryDtos.SalesLibraryCompleteRequest request
    ) {
        service.completeJob(jobId, request);
    }

    /**
     * Registra falha terminal de um job do worker.
     */
    @PostMapping("/jobs/{jobId}:fail")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void failJob(
            @PathVariable long jobId,
            @RequestBody MoisSalesLibraryDtos.SalesLibraryFailRequest request
    ) {
        service.failJob(jobId, request);
    }

    /**
     * Ingere URLs externas já selecionadas para a biblioteca.
     */
    @PostMapping("/urls:ingest")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MoisSalesLibraryDtos.SalesLibraryIngestResponse ingestUrls(
            @Valid @RequestBody MoisSalesLibraryDtos.SalesLibraryIngestRequest request
    ) {
        return service.ingestUrls(request);
    }

    /**
     * Dispara a ingestão das URLs dos produtos Hotmart já coletados para iniciar o MVP com até 400 produtos.
     */
    @PostMapping("/hotmart-products:ingest")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MoisSalesLibraryDtos.SalesLibraryHotmartCollectedIngestResponse ingestHotmartCollectedProducts(
            @Valid @RequestBody MoisSalesLibraryDtos.SalesLibraryHotmartCollectedIngestRequest request
    ) {
        return service.ingestHotmartCollectedProducts(request);
    }
}
