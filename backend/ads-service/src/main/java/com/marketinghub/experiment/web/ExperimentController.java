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

    /** Gera três opções de contrato de promessa única para o formulário de novo experimento. */
    @PostMapping("/promise-contract-options/generate")
    public GenerateExperimentPromiseOptionsResponse generatePromiseOptions(
            @RequestBody GenerateExperimentPromiseOptionsRequest request) {
        return promiseGenerationService.generate(request);
    }

}
