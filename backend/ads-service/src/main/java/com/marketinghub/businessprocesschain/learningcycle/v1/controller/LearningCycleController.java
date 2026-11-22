package com.marketinghub.businessprocesschain.learningcycle.v1.controller;

import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleService;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.command.LearningCycleCommand;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.command.RevalidateCycleWindowRequest;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.costWorksheet.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.createCycle.CreateLearningCycleRequest;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.financialReview.CycleFinancialReview;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.financialReview.PreparationBudgetRequest;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.reconcileMeasurement.ReconcileLearningCycleMeasurementRequest;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.videoBudget.*;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Responsabilidade: expor decisões e leitura dos ciclos comerciais pela interface administrativa.
 */
@RestController
@RequestMapping("/api/business-process-chains/learning-cycles/v1")
@RequiredArgsConstructor
public class LearningCycleController {
  private final LearningCycleService service;

  /** Apresenta a oferta registrada e a elegibilidade real para corrigir o planejamento privado. */
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Conferir reconciliação da oferta antes de retomar Atena")
  @GetMapping("/products/{productId}/{cycleId}/planning-alignment")
  public com.marketinghub
          .businessprocesschain
          .learningcycle
          .v1
          .service
          .planningAlignment
          .PlanningAlignmentView
      planningAlignment(@PathVariable Long productId, @PathVariable Long cycleId) {
    return service.planningAlignment(productId, cycleId);
  }

  /** Registra a correção das entradas sem executar agentes nem alterar os limites financeiros. */
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Alinhar planejamento com a definição já registrada do produto")
  @PostMapping("/products/{productId}/{cycleId}/planning-alignment")
  public LearningCycleResponse reconcilePlanning(
      @PathVariable Long productId,
      @PathVariable Long cycleId,
      @Valid @RequestBody
          com.marketinghub
                  .businessprocesschain
                  .learningcycle
                  .v1
                  .service
                  .planningAlignment
                  .PlanningAlignmentRequest
              request) {
    return service.reconcilePlanning(productId, cycleId, request);
  }

  /** Expõe o grafo e o retorno oficiais, preservando a ocorrência selecionada quando informada. */
  @io.swagger.v3.oas.annotations.Operation(
      summary =
          "Catálogo do ciclo com retorno ao processo pai no mesmo produto, cadeia e ocorrência")
  @GetMapping("/catalog")
  public LearningCycleCatalog catalog(
      @RequestParam Long chainId,
      @RequestParam(required = false) Long productId,
      @RequestParam(required = false) Long cycleId) {
    return service.catalog(chainId, productId, cycleId);
  }

  /** Resolve a entrada pelo BPM pai ou subprocesso sem criar nenhuma ocorrência. */
  @GetMapping("/entry")
  public LearningCycleEntry entry(
      @RequestParam Long processDefinitionId,
      @RequestParam(required = false) Long productId,
      @RequestParam(required = false) Long chainId) {
    return service.entry(processDefinitionId, productId, chainId);
  }

  /** Lista o histórico segregado do produto e, opcionalmente, de uma cadeia. */
  @GetMapping("/products/{productId}")
  public List<LearningCycleResponse> list(
      @PathVariable Long productId, @RequestParam(required = false) Long chainId) {
    return service.list(productId, chainId);
  }

  /** Mostra a passagem atual e a próxima atividade com memória e identidade do ciclo. */
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Contexto do ciclo e continuidade no processo do produto")
  @GetMapping("/products/{productId}/process-context")
  public LearningCycleProcessContext processContext(
      @PathVariable Long productId,
      @RequestParam Long processDefinitionId,
      @RequestParam(required = false) Long cycleId,
      @RequestParam(required = false) Long chainId) {
    return service.processContext(productId, processDefinitionId, cycleId, chainId);
  }

  /** Inicia uma iteração a partir de um experimento escolhido explicitamente na tela. */
  @PostMapping("/products/{productId}")
  public LearningCycleResponse create(
      @PathVariable Long productId, @Valid @RequestBody CreateLearningCycleRequest request) {
    return service.create(productId, request);
  }

  /** Registra a decisão com revisão esperada e chave de idempotência. */
  @PostMapping("/products/{productId}/{cycleId}/commands")
  public LearningCycleResponse command(
      @PathVariable Long productId,
      @PathVariable Long cycleId,
      @Valid @RequestBody LearningCycleCommand request) {
    return service.command(productId, cycleId, request);
  }

  /** Registra a primeira implementação da versão planejada, sem concluir revisões independentes. */
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Registrar prova privada da mesma candidata planejada")
  @PostMapping("/products/{productId}/{cycleId}/private-prototype")
  public LearningCycleResponse registerPrototype(
      @PathVariable Long productId,
      @PathVariable Long cycleId,
      @Valid @RequestBody
          com.marketinghub.businessprocesschain.learningcycle.v1.service.command
                  .RegisterCyclePrototypeRequest
              request) {
    return service.registerPrototype(productId, cycleId, request);
  }

  /** Recebe somente os valores e registra o aceite no fluxo canônico de decisões. */
  @io.swagger.v3.oas.annotations.Operation(summary = "Aprovar orçamento diário e total do ciclo")
  @PostMapping("/products/{productId}/{cycleId}/budget-authorization")
  public LearningCycleResponse authorizeBudget(
      @PathVariable Long productId,
      @PathVariable Long cycleId,
      @Valid @RequestBody
          com.marketinghub.businessprocesschain.learningcycle.v1.service.command
                  .AuthorizeCycleBudgetRequest
              request,
      java.security.Principal principal) {
    return service.authorizeBudget(
        productId,
        cycleId,
        request,
        principal == null ? "Operador administrativo · aceite pela tela" : principal.getName());
  }

  /** Renova pela tela a janela ainda não utilizada, preservando versão e teto do ciclo. */
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Revalidar a janela de um ciclo planejado sem ampliar orçamento")
  @PostMapping("/products/{productId}/{cycleId}/window-revalidation")
  public LearningCycleResponse revalidateWindow(
      @PathVariable Long productId,
      @PathVariable Long cycleId,
      @Valid @RequestBody RevalidateCycleWindowRequest request,
      java.security.Principal principal) {
    return service.revalidateWindow(
        productId,
        cycleId,
        request,
        principal == null
            ? "Operador administrativo · revalidação pela tela"
            : principal.getName());
  }

  /** Registra a escolha de margem mínima do usuário no mesmo ciclo e sem autorizar orçamento. */
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Registrar meta de contribuição após custos variáveis e aquisição")
  @PostMapping("/products/{productId}/{cycleId}/contribution-target")
  public LearningCycleResponse contributionTarget(
      @PathVariable Long productId,
      @PathVariable Long cycleId,
      @Valid @RequestBody
          com.marketinghub.businessprocesschain.learningcycle.v1.service.command
                  .DefineContributionTargetRequest
              request,
      java.security.Principal principal) {
    return service.defineContributionTarget(
        productId,
        cycleId,
        request,
        principal == null ? "Operador administrativo · escolha pela tela" : principal.getName());
  }

  /** Solicita nova leitura das fontes oficiais sem receber métricas digitadas pela tela. */
  @PostMapping("/products/{productId}/{cycleId}/measurement-reconciliation")
  public LearningCycleResponse reconcileMeasurement(
      @PathVariable Long productId,
      @PathVariable Long cycleId,
      @Valid @RequestBody ReconcileLearningCycleMeasurementRequest request) {
    return service.reconcileMeasurement(productId, cycleId, request);
  }

  /** Expõe os seis grupos, histórico e previsões sem alterar limites ou executar tarefas. */
  @io.swagger.v3.oas.annotations.Operation(summary = "Consultar planilha de custos do ciclo")
  @GetMapping("/products/{productId}/{cycleId}/cost-worksheet")
  public CostWorksheet costWorksheet(
      @PathVariable Long productId, @PathVariable Long cycleId, @RequestParam Long chainId) {
    return service.costWorksheet(productId, cycleId, chainId);
  }

  /** Registra custos e previsões com aprovação administrativa explícita dos custos operacionais. */
  @io.swagger.v3.oas.annotations.Operation(summary = "Salvar planilha versionada sem liberar gasto")
  @PostMapping("/products/{productId}/{cycleId}/cost-worksheet")
  public CostWorksheet saveCostWorksheet(
      @PathVariable Long productId,
      @PathVariable Long cycleId,
      @Valid @RequestBody CostWorksheetRequest request) {
    return service.saveCostWorksheet(productId, cycleId, request);
  }

  /** Registra o teto da linha sem receber custos, orçamento proposto ou previsões do usuário. */
  @io.swagger.v3.oas.annotations.Operation(summary = "Aprovar teto de um grupo financeiro")
  @PostMapping("/products/{productId}/{cycleId}/cost-ceilings")
  public CostWorksheet approveCostCeiling(
      @PathVariable Long productId,
      @PathVariable Long cycleId,
      @Valid @RequestBody ApproveCostCeilingRequest request) {
    return service.approveCostCeiling(productId, cycleId, request);
  }

  /** Expõe demonstração do ciclo, custos e parecer de Plutus sem executar consumo. */
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Consultar demonstração financeira da execução")
  @GetMapping("/products/{productId}/{cycleId}/financial-review")
  public CycleFinancialReview financialReview(
      @PathVariable Long productId, @PathVariable Long cycleId, @RequestParam Long chainId) {
    return service.financialReview(productId, cycleId, chainId);
  }

  /** Registra confirmação explícita do teto de preparação com recibo e controle de concorrência. */
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Aprovar teto cumulativo de preparação e revisão")
  @PostMapping("/products/{productId}/{cycleId}/preparation-budget")
  public CycleFinancialReview preparationBudget(
      @PathVariable Long productId,
      @PathVariable Long cycleId,
      @Valid @RequestBody PreparationBudgetRequest request) {
    return service.authorizePreparationBudget(productId, cycleId, request);
  }

  /** Apresenta o teto de produção e revisão das duas peças e seu histórico por ciclo. */
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Consultar financeiro dos dois vídeos do ciclo")
  @GetMapping("/products/{productId}/{cycleId}/video-budget")
  public VideoBudgetResponse videoBudget(
      @PathVariable("productId") Long productId,
      @PathVariable("cycleId") Long cycleId,
      @RequestParam("chainId") Long chainId) {
    return service.videoBudget(productId, cycleId, chainId);
  }

  /** Registra autorização humana restrita sem gerar vídeos, mídia, cobrança ou publicação. */
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Autorizar teto total de produção e revisão dos dois vídeos")
  @PostMapping("/products/{productId}/{cycleId}/video-budget")
  public VideoBudgetResponse authorizeVideoBudget(
      @PathVariable("productId") Long productId,
      @PathVariable("cycleId") Long cycleId,
      @Valid @RequestBody AuthorizeVideoBudgetRequest request) {
    return service.authorizeVideoBudget(productId, cycleId, request);
  }

  /** Consulta recibos integrais de vídeos únicos sem consultar ou autorizar o histórico. */
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Consultar autorizações incrementais de um vídeo")
  @GetMapping("/products/{productId}/{cycleId}/single-video-budget")
  public com.marketinghub
          .businessprocesschain
          .learningcycle
          .v1
          .service
          .singleVideoBudget
          .SingleVideoBudgetResponse
      singleVideoBudget(
          @PathVariable Long productId, @PathVariable Long cycleId, @RequestParam Long chainId) {
    return service.singleVideoBudget(productId, cycleId, chainId);
  }

  /** Registra a origem da decisão sem representar aprovação de Plutus, mídia ou publicação. */
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Registrar teto integral para um único vídeo novo")
  @PostMapping("/products/{productId}/{cycleId}/single-video-budget")
  public com.marketinghub
          .businessprocesschain
          .learningcycle
          .v1
          .service
          .singleVideoBudget
          .SingleVideoBudgetResponse
      authorizeSingleVideoBudget(
          @PathVariable Long productId,
          @PathVariable Long cycleId,
          @Valid @RequestBody
              com.marketinghub
                      .businessprocesschain
                      .learningcycle
                      .v1
                      .service
                      .singleVideoBudget
                      .SingleVideoBudgetRequest
                  request) {
    return service.authorizeSingleVideoBudget(productId, cycleId, request);
  }
}
