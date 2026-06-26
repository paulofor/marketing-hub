package com.marketinghub.experiment.web;

import com.marketinghub.experiment.dto.CreateExperimentRequest;
import com.marketinghub.experiment.dto.ExperimentDiagnosticsDto;
import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.experiment.dto.UpdateExperimentRequest;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.experiment.dto.UpdateSelectedSampleEmailRequest;
import com.marketinghub.experiment.mapper.ExperimentMapper;
import com.marketinghub.experiment.service.ExperimentDiagnosticsService;
import com.marketinghub.experiment.service.ExperimentService;
import com.marketinghub.experiment.service.ExperimentReadinessService;
import com.marketinghub.experiment.service.ExperimentPromiseGenerationService;
import com.marketinghub.experiment.service.generatepromise.GenerateExperimentPromiseOptionsRequest;
import com.marketinghub.experiment.service.generatepromise.GenerateExperimentPromiseOptionsResponse;
import com.marketinghub.experiment.service.generatepromise.latestdraft.ExperimentPromiseOptionsDraftResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Responsabilidade: expor endpoints administrativos para criação, leitura e ações dos experimentos.
 */
@RestController
@RequestMapping("/api/experiments")
public class ExperimentController {
    private final ExperimentService service;
    private final ExperimentMapper mapper;
    private final ExperimentDiagnosticsService diagnosticsService;
    private final ExperimentReadinessService readinessService;
    private final ExperimentPromiseGenerationService promiseGenerationService;

    /** Inicializa o controller com serviços de experimento, diagnóstico, prontidão e geração de promessas. */
    public ExperimentController(ExperimentService service, ExperimentMapper mapper,
                                ExperimentDiagnosticsService diagnosticsService,
                                ExperimentReadinessService readinessService,
                                ExperimentPromiseGenerationService promiseGenerationService) {
        this.service = service;
        this.mapper = mapper;
        this.diagnosticsService = diagnosticsService;
        this.readinessService = readinessService;
        this.promiseGenerationService = promiseGenerationService;
    }

    /** Cria um novo experimento com os dados comerciais informados na tela. */
    @PostMapping
    public ExperimentDto create(@RequestBody CreateExperimentRequest request) {
        return mapper.toDto(service.create(request));
    }

    /** Duplica um experimento existente preservando seu contrato comercial. */
    @PostMapping("/{id}/duplicate")
    public ExperimentDto duplicate(@PathVariable Long id) {
        return mapper.toDto(service.duplicate(id));
    }

    /** Busca os detalhes de um experimento pelo identificador. */
    @GetMapping("/{id}")
    public ExperimentDto get(@PathVariable Long id) {
        return mapper.toDto(service.get(id));
    }

    /** Retorna diagnósticos operacionais e comerciais do experimento. */
    @GetMapping("/{id}/diagnostics")
    public ExperimentDiagnosticsDto diagnostics(@PathVariable Long id) {
        return diagnosticsService.diagnose(id);
    }

    /** Resume a prontidão do experimento para publicação e execução. */
    @GetMapping("/{id}/readiness")
    public ExperimentReadinessSummaryDto readiness(@PathVariable Long id) {
        return readinessService.summarize(id);
    }

    /** Lista os experimentos cadastrados para acompanhamento administrativo. */
    @GetMapping
    public List<ExperimentDto> list() {
        return StreamSupport.stream(service.list().spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }

    /** Atualiza apenas o status do experimento. */
    @PatchMapping("/{id}/status")
    public ExperimentDto updateStatus(
            @PathVariable Long id,
            @RequestParam com.marketinghub.experiment.ExperimentStatus status) {
        return mapper.toDto(service.updateStatus(id, status));
    }

    /** Atualiza os campos editáveis de um experimento existente. */
    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ExperimentDto update(@PathVariable Long id, @RequestBody UpdateExperimentRequest request) {
        return mapper.toDto(service.update(id, request));
    }

    /** Solicita geração de criativos para o experimento. */
    @PatchMapping("/{id}/creatives-to-generate")
    public ExperimentDto requestCreatives(@PathVariable Long id, @RequestParam("quantity") int quantity) {
        return mapper.toDto(service.requestCreatives(id, quantity));
    }

    /** Solicita geração de anúncios pelo pipeline do experimento. */
    @PostMapping("/{id}/pipeline/ads")
    public ExperimentDto requestPipelineAds(@PathVariable Long id) {
        return mapper.toDto(service.requestPipelineCreatives(id));
    }

    /** Lista solicitações pendentes de criativos para consumo do AI Worker. */
    @GetMapping("/creatives/stage-executions/pending")
    public List<ExperimentDto> pendingCreativeGeneration(@RequestParam(defaultValue = "10") int limit) {
        return service.listPendingCreativeGeneration(limit).stream()
                .map(mapper::toDto)
                .toList();
    }

    /** Marca a geração de criativos como iniciada pelo AI Worker. */
    @PostMapping("/{id}/creatives/stage-execution/start")
    public ExperimentDto startCreativeGeneration(@PathVariable Long id) {
        return mapper.toDto(service.markCreativeGenerationStarted(id));
    }

    /** Marca a geração de criativos como concluída pelo AI Worker. */
    @PostMapping("/{id}/creatives/stage-execution/complete")
    public ExperimentDto completeCreativeGeneration(@PathVariable Long id) {
        return mapper.toDto(service.markCreativeGenerationCompleted(id));
    }

    /** Marca a geração de criativos como falha pelo AI Worker. */
    @PostMapping("/{id}/creatives/stage-execution/fail")
    public ExperimentDto failCreativeGeneration(
            @PathVariable Long id,
            @RequestBody CreativeGenerationFailureRequest request) {
        return mapper.toDto(service.markCreativeGenerationFailed(id, request != null ? request.error() : null));
    }

    /** Payload de falha operacional informado pelo AI Worker. */
    public record CreativeGenerationFailureRequest(String error) {
    }

    /** Solicita geração de formulários instantâneos para captação de leads. */
    @PatchMapping("/{id}/instant-forms-to-generate")
    public ExperimentDto requestInstantForms(@PathVariable Long id, @RequestParam("quantity") int quantity) {
        return mapper.toDto(service.requestInstantForms(id, quantity));
    }

    /** Solicita geração de e-mails do fluxo do experimento. */
    @PatchMapping("/{id}/emails-to-generate")
    public ExperimentDto requestEmails(@PathVariable Long id, @RequestParam("quantity") int quantity) {
        return mapper.toDto(service.requestEmails(id, quantity));
    }

    /** Solicita geração de e-mails de amostra para o experimento. */
    @PatchMapping("/{id}/sample-emails-to-generate")
    public ExperimentDto requestSampleEmails(@PathVariable Long id, @RequestParam("quantity") int quantity) {
        return mapper.toDto(service.requestSampleEmails(id, quantity));
    }

    /** Define o e-mail de amostra selecionado para uso no experimento. */
    @PutMapping("/{id}/selected-sample-email")
    public ExperimentDto updateSelectedSampleEmail(
            @PathVariable Long id,
            @RequestBody UpdateSelectedSampleEmailRequest request) {
        return mapper.toDto(service.updateSelectedSampleEmail(id, request.sampleEmailId()));
    }

    /** Solicita geração de entregáveis digitais do experimento. */
    @PatchMapping("/{id}/deliverables-to-generate")
    public ExperimentDto requestDeliverables(@PathVariable Long id, @RequestParam("quantity") int quantity) {
        return mapper.toDto(service.requestDeliverables(id, quantity));
    }

    /** Solicita geração de fluxos do portal do lead. */
    @PatchMapping("/{id}/lead-portal-flows-to-generate")
    public ExperimentDto requestLeadPortalFlows(@PathVariable Long id, @RequestParam("quantity") int quantity) {
        return mapper.toDto(service.requestLeadPortalFlows(id, quantity));
    }

    /** Libera o experimento para publicação no Facebook Ads. */
    @PostMapping("/{id}/facebook-release")
    public ExperimentDto releaseForFacebook(@PathVariable Long id) {
        return mapper.toDto(service.releaseForFacebook(id));
    }

    /** Registra uma solicitação para o AI Worker gerar três opções de contrato de promessa única. */
    @PostMapping("/promise-contract-options/generate")
    public GenerateExperimentPromiseOptionsResponse generatePromiseOptions(
            @RequestBody GenerateExperimentPromiseOptionsRequest request) {
        return promiseGenerationService.generate(request);
    }

    /** Retorna a solicitação de promessa mais recente para retomada sem depender do navegador. */
    @GetMapping("/promise-contract-options/stage-executions/latest")
    public ResponseEntity<ExperimentPromiseOptionsDraftResponse> latestPromiseOptionsDraft() {
        return promiseGenerationService.latestDraft()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Consulta o status e o resultado de uma solicitação de promessa feita pela tela. */
    @GetMapping("/promise-contract-options/stage-executions/{requestId}")
    public GenerateExperimentPromiseOptionsResponse getPromiseOptions(@PathVariable Long requestId) {
        return promiseGenerationService.get(requestId);
    }

    /** Lista solicitações pendentes para consumo canônico do AI Worker pelo método pending. */
    @GetMapping("/promise-contract-options/stage-executions/pending")
    public java.util.List<GenerateExperimentPromiseOptionsResponse> pendingPromiseOptions(
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return promiseGenerationService.listPending(limit != null ? limit : 10);
    }

    /** Marca uma solicitação pendente como assumida pelo AI Worker. */
    @PostMapping("/promise-contract-options/stage-executions/{requestId}/claim")
    public GenerateExperimentPromiseOptionsResponse claimPromiseOptions(
            @PathVariable Long requestId,
            @RequestParam(value = "workerId", required = false) String workerId) {
        return promiseGenerationService.claim(requestId, workerId);
    }

    /** Recebe as opções geradas pelo AI Worker e conclui a solicitação. */
    @PostMapping("/promise-contract-options/stage-executions/{requestId}/complete")
    public GenerateExperimentPromiseOptionsResponse completePromiseOptions(
            @PathVariable Long requestId,
            @RequestBody GenerateExperimentPromiseOptionsResponse response) {
        return promiseGenerationService.complete(requestId, response);
    }

    /** Descarta uma solicitação retomável após a criação do teste. */
    @PostMapping("/promise-contract-options/stage-executions/{requestId}/dismiss")
    public void dismissPromiseOptions(@PathVariable Long requestId) {
        promiseGenerationService.dismiss(requestId);
    }

    /** Registra falha informada pelo AI Worker ao processar a solicitação. */
    @PostMapping("/promise-contract-options/stage-executions/{requestId}/fail")
    public void failPromiseOptions(
            @PathVariable Long requestId,
            @RequestBody(required = false) String errorMessage) {
        promiseGenerationService.fail(requestId, errorMessage);
    }

}
