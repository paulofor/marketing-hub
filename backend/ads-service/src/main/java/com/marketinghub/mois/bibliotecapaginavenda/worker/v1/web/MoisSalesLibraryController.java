package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.web;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto.MoisSalesLibraryDtos;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.MoisSalesLibraryService;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.MoisSalesLibrarySnapshotService;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.MoisSalesPageMarketWarmupService;
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
    private final MoisSalesPageMarketWarmupService marketWarmupService;

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
     * Lista páginas canônicas registradas no modelo consolidado da biblioteca.
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
     * Retorna contadores globais das páginas consolidadas sem depender da paginação da listagem.
     */
    @GetMapping("/pages/summary")
    public MoisSalesLibraryDtos.SalesLibraryPageSummaryResponse summarizePages(@RequestParam String workspaceId) {
        return service.summarizePages(workspaceId);
    }

    /**
     * Retorna o resumo de URLs únicas disponíveis na origem bruta de referências coletadas.
     */
    @GetMapping("/collected-references/url-summary")
    public MoisSalesLibraryDtos.CollectedReferenceUrlSummaryResponse summarizeCollectedReferenceUrls(@RequestParam String workspaceId) {
        return service.summarizeCollectedReferenceUrls(workspaceId);
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
     * Solicita a pesquisa de aquecimento de mercado da Etapa 3 para uma página de vendas.
     */
    @PostMapping("/pages/{pageId}/market-warmup:request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MoisSalesLibraryDtos.MarketWarmupRequestResponse requestMarketWarmup(@PathVariable long pageId) {
        try {
            return marketWarmupService.requestResearch(pageId);
        } catch (IllegalArgumentException ex) {
            log.warn("Biblioteca de páginas de vendas não encontrou página para aquecimento. operacao=requestMarketWarmup, pageId={}, erro={}", pageId, ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    /**
     * Consulta o resumo mais recente da pesquisa de aquecimento de mercado de uma página.
     */
    @GetMapping("/pages/{pageId}/market-warmup")
    public MoisSalesLibraryDtos.MarketWarmupSummaryResponse getMarketWarmup(@PathVariable long pageId) {
        try {
            return marketWarmupService.getSummary(pageId);
        } catch (IllegalArgumentException ex) {
            log.warn("Biblioteca de páginas de vendas não encontrou resumo de aquecimento. operacao=getMarketWarmup, pageId={}, erro={}", pageId, ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    /**
     * Lista as fontes públicas usadas para justificar a pesquisa de aquecimento da página.
     */
    @GetMapping("/pages/{pageId}/market-warmup/sources")
    public MoisSalesLibraryDtos.MarketWarmupSourceListResponse listMarketWarmupSources(@PathVariable long pageId) {
        try {
            return marketWarmupService.listSources(pageId);
        } catch (IllegalArgumentException ex) {
            log.warn("Biblioteca de páginas de vendas não encontrou fontes de aquecimento. operacao=listMarketWarmupSources, pageId={}, erro={}", pageId, ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    /**
     * Lista os sinais comerciais que explicam a pontuação de aquecimento da página.
     */
    @GetMapping("/pages/{pageId}/market-warmup/signals")
    public MoisSalesLibraryDtos.MarketWarmupSignalListResponse listMarketWarmupSignals(@PathVariable long pageId) {
        try {
            return marketWarmupService.listSignals(pageId);
        } catch (IllegalArgumentException ex) {
            log.warn("Biblioteca de páginas de vendas não encontrou sinais de aquecimento. operacao=listMarketWarmupSignals, pageId={}, erro={}", pageId, ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    /**
     * Reserva internamente o próximo job pendente de aquecimento para o worker MOIS.
     */
    @PostMapping("/market-warmup/jobs:claim")
    public MoisSalesLibraryDtos.MarketWarmupClaimResponse claimMarketWarmupJob(
            @Valid @RequestBody MoisSalesLibraryDtos.MarketWarmupClaimRequest request
    ) {
        return marketWarmupService.claimJob(request);
    }

    /**
     * Recebe do worker o dossiê final da pesquisa de aquecimento de mercado.
     */
    @PostMapping("/market-warmup/jobs/{jobId}:complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void completeMarketWarmupJob(
            @PathVariable long jobId,
            @Valid @RequestBody MoisSalesLibraryDtos.MarketWarmupCompleteRequest request
    ) {
        try {
            marketWarmupService.completeJob(jobId, request);
        } catch (IllegalArgumentException ex) {
            HttpStatus status = ex.getMessage() != null && ex.getMessage().contains("não encontrado")
                    ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            log.warn("Biblioteca de páginas de vendas rejeitou conclusão de aquecimento. operacao=completeMarketWarmupJob, jobId={}, statusHttp={}, erro={}",
                    jobId, status.value(), ex.getMessage(), ex);
            throw new ResponseStatusException(status, ex.getMessage(), ex);
        } catch (IllegalStateException ex) {
            log.warn("Biblioteca de páginas de vendas encontrou conflito na conclusão de aquecimento. operacao=completeMarketWarmupJob, jobId={}, erro={}",
                    jobId, ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        }
    }

    /**
     * Registra a falha operacional reportada pelo worker de aquecimento de mercado.
     */
    @PostMapping("/market-warmup/jobs/{jobId}:fail")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void failMarketWarmupJob(
            @PathVariable long jobId,
            @Valid @RequestBody MoisSalesLibraryDtos.MarketWarmupFailRequest request
    ) {
        try {
            marketWarmupService.failJob(jobId, request);
        } catch (IllegalStateException ex) {
            HttpStatus status = ex.getMessage() != null && ex.getMessage().contains("não encontrado")
                    ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT;
            log.warn("Biblioteca de páginas de vendas encontrou falha de estado ao registrar erro de aquecimento. operacao=failMarketWarmupJob, jobId={}, statusHttp={}, erro={}",
                    jobId, status.value(), ex.getMessage(), ex);
            throw new ResponseStatusException(status, ex.getMessage(), ex);
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
     * Reserva a próxima URL normalizada da biblioteca para obtenção de HTML bruto pelo worker.
     */
    @PostMapping("/html-captures:claim")
    public MoisSalesLibraryDtos.HtmlCaptureClaimResponse claimHtmlCapture(
            @Valid @RequestBody MoisSalesLibraryDtos.HtmlCaptureClaimRequest request
    ) {
        return snapshotService.claimHtmlCapture(request);
    }

    /**
     * Recebe o HTML bruto versionado capturado pelo worker para uma página da biblioteca.
     */
    @PostMapping("/html-captures/{snapshotId}:complete")
    public MoisSalesLibraryDtos.HtmlCapturePersistResponse completeHtmlCapture(
            @PathVariable long snapshotId,
            @Valid @RequestBody MoisSalesLibraryDtos.HtmlCaptureCompleteRequest request
    ) {
        return snapshotService.completeHtmlCapture(snapshotId, request);
    }

    /**
     * Registra falha terminal na obtenção de HTML bruto de uma página da biblioteca.
     */
    @PostMapping("/html-captures/{snapshotId}:fail")
    public MoisSalesLibraryDtos.HtmlCapturePersistResponse failHtmlCapture(
            @PathVariable long snapshotId,
            @RequestBody MoisSalesLibraryDtos.HtmlCaptureFailRequest request
    ) {
        return snapshotService.failHtmlCapture(snapshotId, request);
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
     * Lista snapshots legados já capturados para uma página durante a transição.
     */
    @GetMapping("/pages/{pageId}/snapshots")
    public List<MoisSalesLibraryDtos.SalesLibraryPageSnapshotResponse> listPageSnapshots(@PathVariable long pageId) {
        return snapshotService.listSnapshots(pageId);
    }

    /**
     * Lista o histórico consolidado de execuções da página no modelo novo.
     */
    @GetMapping("/pages/{pageId}/executions")
    public List<MoisSalesLibraryDtos.SalesLibraryPageExecutionResponse> listPageExecutions(@PathVariable long pageId) {
        return service.listPageExecutions(pageId);
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
