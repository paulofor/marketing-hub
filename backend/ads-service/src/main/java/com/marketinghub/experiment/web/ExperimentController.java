package com.marketinghub.experiment.web;

import com.marketinghub.experiment.dto.CreateExperimentRequest;
import com.marketinghub.experiment.dto.ExperimentDiagnosticsDto;
import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.experiment.dto.ExperimentSessionDurationSummaryDto;
import com.marketinghub.experiment.dto.ExperimentSessionDurationVariantDto;
import com.marketinghub.experiment.dto.ReactivateExperimentRequest;
import com.marketinghub.experiment.dto.UpdateExperimentLearnedLessonsRequest;
import com.marketinghub.experiment.dto.UpdateExperimentRequest;
import com.marketinghub.experiment.dto.UpdateExperimentStrategicPositioningRequest;
import com.marketinghub.experiment.dto.UpdateSelectedSampleEmailRequest;
import com.marketinghub.experiment.funnel.ExperimentFunnelService;
import com.marketinghub.experiment.funnel.dto.ExperimentPdeCockpitDiagnosticsDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsDto;
import com.marketinghub.experiment.mapper.ExperimentMapper;
import com.marketinghub.experiment.salespageab.service.ExperimentSalesPageAbTestService;
import com.marketinghub.experiment.service.ExperimentCampaignDestinationPolicy;
import com.marketinghub.experiment.service.ExperimentCockpitService;
import com.marketinghub.experiment.service.ExperimentConstructionService;
import com.marketinghub.experiment.service.ExperimentCostReconciliationService;
import com.marketinghub.experiment.service.ExperimentDeliverablesZipService;
import com.marketinghub.experiment.service.ExperimentDiagnosticsService;
import com.marketinghub.experiment.service.ExperimentPromiseGenerationService;
import com.marketinghub.experiment.service.ExperimentReadinessService;
import com.marketinghub.experiment.service.ExperimentService;
import com.marketinghub.experiment.service.cockpit.ExperimentCockpitDto;
import com.marketinghub.experiment.service.construction.ExperimentConstructionDto;
import com.marketinghub.experiment.service.generatepromise.GenerateExperimentPromiseOptionsRequest;
import com.marketinghub.experiment.service.generatepromise.GenerateExperimentPromiseOptionsResponse;
import com.marketinghub.experiment.service.generatepromise.latestdraft.ExperimentPromiseOptionsDraftResponse;
import java.util.List;
import java.util.stream.StreamSupport;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
  private final ExperimentCampaignDestinationPolicy campaignDestinationPolicy;
  private final ExperimentFunnelService funnelService;
  private final ExperimentSalesPageAbTestService salesPageAbTestService;
  private final ExperimentDeliverablesZipService deliverablesZipService;
  private final ExperimentConstructionService constructionService;
  private final ExperimentCostReconciliationService costReconciliationService;
  private final ExperimentCockpitService cockpitService;

  /**
   * Inicializa o controller com serviços de experimento, diagnóstico, prontidão e geração de
   * promessas.
   */
  public ExperimentController(
      ExperimentService service,
      ExperimentMapper mapper,
      ExperimentDiagnosticsService diagnosticsService,
      ExperimentReadinessService readinessService,
      ExperimentPromiseGenerationService promiseGenerationService,
      ExperimentCampaignDestinationPolicy campaignDestinationPolicy,
      ExperimentFunnelService funnelService,
      ExperimentSalesPageAbTestService salesPageAbTestService,
      ExperimentDeliverablesZipService deliverablesZipService,
      ExperimentConstructionService constructionService,
      ExperimentCostReconciliationService costReconciliationService,
      ExperimentCockpitService cockpitService) {
    this.service = service;
    this.mapper = mapper;
    this.diagnosticsService = diagnosticsService;
    this.readinessService = readinessService;
    this.promiseGenerationService = promiseGenerationService;
    this.campaignDestinationPolicy = campaignDestinationPolicy;
    this.funnelService = funnelService;
    this.salesPageAbTestService = salesPageAbTestService;
    this.deliverablesZipService = deliverablesZipService;
    this.constructionService = constructionService;
    this.costReconciliationService = costReconciliationService;
    this.cockpitService = cockpitService;
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
    com.marketinghub.experiment.Experiment experiment = service.get(id);
    return costReconciliationService.enrich(experiment, mapper.toDto(experiment));
  }

  /** Retorna a explicação de construção comercial e operacional do experimento. */
  @GetMapping("/{id}/construction")
  public ExperimentConstructionDto construction(@PathVariable Long id) {
    return constructionService.getConstruction(id);
  }

  /** Retorna o cockpit comercial consolidado para criação de decisão de venda do experimento. */
  @GetMapping("/{id}/cockpit")
  public ExperimentCockpitDto cockpit(@PathVariable Long id) {
    return cockpitService.getCockpit(id);
  }

  /** Retorna diagnóstico da integração PDE usada para montar o cockpit do experimento. */
  @GetMapping("/{id}/cockpit/pde-diagnostics")
  public ExperimentPdeCockpitDiagnosticsDto pdeCockpitDiagnostics(@PathVariable Long id) {
    return funnelService.diagnosePdeCockpitIntegration(id);
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
    return StreamSupport.stream(service.list().spliterator(), false).map(this::toListDto).toList();
  }

  /** Monta o contrato da lista com o resumo de engajamento da landing. */
  private ExperimentDto toListDto(com.marketinghub.experiment.Experiment experiment) {
    ExperimentDto dto = costReconciliationService.enrich(experiment, mapper.toDto(experiment));
    dto.setSessionDurationSummary(buildSessionDurationSummary(experiment.getId()));
    dto.setRevenue(funnelService.approvedRevenue(experiment.getId()));
    return dto;
  }

  /** Consolida tempo medio geral e por variante A/B quando houver teste separado. */
  private ExperimentSessionDurationSummaryDto buildSessionDurationSummary(Long experimentId) {
    ExperimentLandingAnalyticsDto analytics = funnelService.summarizeLandingAnalytics(experimentId);
    List<ExperimentSessionDurationVariantDto> variants =
        salesPageAbTestService.results(experimentId).stream()
            .flatMap(result -> result.variants().stream())
            .filter(variant -> variant.sessions() > 0)
            .map(
                variant ->
                    new ExperimentSessionDurationVariantDto(
                        variant.variant().variantKey(),
                        variant.variant().name(),
                        variant.sessions(),
                        variant.averageVisibleMsPerSession()))
            .toList();
    return new ExperimentSessionDurationSummaryDto(
        analytics.totalSessions(), analytics.averageVisibleMsPerSession(), variants);
  }

  /** Atualiza apenas o status do experimento. */
  @PatchMapping("/{id}/status")
  public ExperimentDto updateStatus(
      @PathVariable Long id, @RequestParam com.marketinghub.experiment.ExperimentStatus status) {
    return mapper.toDto(service.updateStatus(id, status));
  }

  /** Zera custos manuais e legados do experimento mantendo custos auditáveis reconciliados. */
  @PostMapping("/{id}/costs/reset")
  public ExperimentDto resetCosts(@PathVariable Long id) {
    com.marketinghub.experiment.Experiment experiment = service.resetCosts(id);
    return costReconciliationService.enrich(experiment, mapper.toDto(experiment));
  }

  /** Reativa um experimento parado registrando o motivo informado pelo usuário. */
  @PostMapping("/{id}/reactivate")
  public ExperimentDto reactivate(
      @PathVariable Long id, @RequestBody ReactivateExperimentRequest request) {
    return mapper.toDto(service.reactivate(id, request));
  }

  /** Atualiza os campos editáveis de um experimento existente. */
  @RequestMapping(
      value = "/{id}",
      method = {RequestMethod.PUT, RequestMethod.PATCH})
  public ExperimentDto update(@PathVariable Long id, @RequestBody UpdateExperimentRequest request) {
    return mapper.toDto(service.update(id, request));
  }

  /** Atualiza somente as lições aprendidas do experimento. */
  @PatchMapping("/{id}/learned-lessons")
  public ExperimentDto updateLearnedLessons(
      @PathVariable Long id, @RequestBody UpdateExperimentLearnedLessonsRequest request) {
    return mapper.toDto(service.updateLearnedLessons(id, request.learnedLessons()));
  }

  /** Atualiza o objetivo comercial e a função operacional atual do experimento. */
  @PatchMapping("/{id}/strategic-positioning")
  public ExperimentDto updateStrategicPositioning(
      @PathVariable Long id, @RequestBody UpdateExperimentStrategicPositioningRequest request) {
    return mapper.toDto(
        service.updateStrategicPositioning(
            id, request.commercialObjective(), request.currentOperationalFunction()));
  }

  /** Solicita geração de criativos para o experimento. */
  @PatchMapping("/{id}/creatives-to-generate")
  public ExperimentDto requestCreatives(
      @PathVariable Long id, @RequestParam("quantity") int quantity) {
    return mapper.toDto(service.requestCreatives(id, quantity));
  }

  /** Bloqueia a geração antiga de anúncios pelo pipeline do experimento. */
  @PostMapping("/{id}/pipeline/ads")
  public ExperimentDto requestPipelineAds(@PathVariable Long id) {
    return mapper.toDto(service.requestPipelineCreatives(id));
  }

  /** Lista solicitações pendentes de criativos para consumo do AI Worker. */
  @GetMapping("/creatives/stage-executions/pending")
  public List<ExperimentDto> pendingCreativeGeneration(
      @RequestParam(defaultValue = "10") int limit) {
    return service.listPendingCreativeGeneration(limit).stream().map(mapper::toDto).toList();
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
      @PathVariable Long id, @RequestBody CreativeGenerationFailureRequest request) {
    return mapper.toDto(
        service.markCreativeGenerationFailed(id, request != null ? request.error() : null));
  }

  /** Payload de falha operacional informado pelo AI Worker. */
  public record CreativeGenerationFailureRequest(String error) {}

  /** Solicita geração de formulários instantâneos para captação de leads. */
  @PatchMapping("/{id}/instant-forms-to-generate")
  public ExperimentDto requestInstantForms(
      @PathVariable Long id, @RequestParam("quantity") int quantity) {
    return mapper.toDto(service.requestInstantForms(id, quantity));
  }

  /** Solicita geração de e-mails do fluxo do experimento. */
  @PatchMapping("/{id}/emails-to-generate")
  public ExperimentDto requestEmails(
      @PathVariable Long id, @RequestParam("quantity") int quantity) {
    return mapper.toDto(service.requestEmails(id, quantity));
  }

  /** Solicita geração de e-mails de amostra para o experimento. */
  @PatchMapping("/{id}/sample-emails-to-generate")
  public ExperimentDto requestSampleEmails(
      @PathVariable Long id, @RequestParam("quantity") int quantity) {
    return mapper.toDto(service.requestSampleEmails(id, quantity));
  }

  /** Define o e-mail de amostra selecionado para uso no experimento. */
  @PutMapping("/{id}/selected-sample-email")
  public ExperimentDto updateSelectedSampleEmail(
      @PathVariable Long id, @RequestBody UpdateSelectedSampleEmailRequest request) {
    return mapper.toDto(service.updateSelectedSampleEmail(id, request.sampleEmailId()));
  }

  /** Solicita geração de entregáveis digitais do experimento. */
  @PatchMapping("/{id}/deliverables-to-generate")
  public ExperimentDto requestDeliverables(
      @PathVariable Long id, @RequestParam("quantity") int quantity) {
    return mapper.toDto(service.requestDeliverables(id, quantity));
  }

  /** Baixa um ZIP com os entregáveis persistidos para o experimento. */
  @GetMapping("/{id}/deliverables.zip")
  public ResponseEntity<byte[]> downloadDeliverablesZip(@PathVariable Long id) {
    byte[] zip = deliverablesZipService.generate(id);
    String fileName = "experimento-%d-entregaveis.zip".formatted(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("application/zip"))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(fileName).build().toString())
        .contentLength(zip.length)
        .body(zip);
  }

  /** Solicita geração de fluxos do portal do lead. */
  @PatchMapping("/{id}/lead-portal-flows-to-generate")
  public ExperimentDto requestLeadPortalFlows(
      @PathVariable Long id, @RequestParam("quantity") int quantity) {
    return mapper.toDto(service.requestLeadPortalFlows(id, quantity));
  }

  /** Libera o experimento para publicação no Facebook Ads. */
  @PostMapping("/{id}/facebook-release")
  public ExperimentDto releaseForFacebook(@PathVariable Long id) {
    ensurePurchaseIntentDoesNotBypassSalesPage(id);
    return mapper.toDto(service.releaseForFacebook(id));
  }

  /** Bloqueia liberação de tráfego frio com intenção de compra direto para checkout. */
  private void ensurePurchaseIntentDoesNotBypassSalesPage(Long id) {
    List<String> missing = campaignDestinationPolicy.missingConfiguration(service.get(id));
    if (missing.contains("commercialContract")) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Experimento com intenção de compra exige contrato comercial completo da etapa Oferta"
              + " antes da página e da campanha.");
    }
    if (missing.contains("geraSalesPagePipeline")) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Experimento com intenção de compra exige página de venda criada pelo GeraSalesPage v1"
              + " antes da campanha.");
    }
    if (missing.contains("salesPageAdDestination")) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Experimento com intenção de compra exige que o link do anúncio aponte para a página de"
              + " venda publicada, não para o checkout direto.");
    }
    if (missing.contains("salesPageAnalyticsCollectors")) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Experimento com intenção de compra exige página de venda com coletores page_view,"
              + " page_load_metric, section_view_time e checkout_click antes da campanha.");
    }
    if (missing.contains("pdeMembershipDestination")) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Experimento PDE MUSA exige que o link do anúncio aponte para um slot produtivo"
              + " versionado aprovado, como https://v5.clubemusa.com.br, com login gratuito e"
              + " paywall interno.");
    }
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
    return promiseGenerationService
        .latestDraft()
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
      @PathVariable Long requestId, @RequestBody(required = false) String errorMessage) {
    promiseGenerationService.fail(requestId, errorMessage);
  }
}
