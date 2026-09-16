package com.marketinghub.catalogovivo.v1.service;

import com.marketinghub.businessprocess.automation.v1.service.ProcessRunService;
import com.marketinghub.businessprocess.automation.v1.service.commands.ProcessRunCommand;
import com.marketinghub.catalogovivo.v1.service.adoption.*;
import com.marketinghub.opala.commercial.v1.service.OpalaCommercialContext;
import com.marketinghub.repository.jdbc.catalogovivo.OpalaAdoptionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Instant;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: incorporar explicitamente o subprocesso Opala ao ciclo e iniciar sua
 * preparação.
 */
@Component
@Slf4j
public class OpalaCycleAdoption {
  private final OpalaAdoptionRepository repository;
  private final LearningSalesCycleRepository cycles;
  private final ProductRepository products;
  private final CatalogoVivoService catalog;
  private final OpalaCommercialContext context;
  private final ProcessRunService runs;

  /** Reutiliza o motor BPM e seus gates, sem controlar etapas no frontend ou no worker. */
  public OpalaCycleAdoption(
      OpalaAdoptionRepository repository,
      LearningSalesCycleRepository cycles,
      ProductRepository products,
      CatalogoVivoService catalog,
      OpalaCommercialContext context,
      @Lazy ProcessRunService runs) {
    this.repository = repository;
    this.cycles = cycles;
    this.products = products;
    this.catalog = catalog;
    this.context = context;
    this.runs = runs;
  }

  /** Expõe a situação real da adesão e todos os impedimentos relevantes à ação administrativa. */
  @Transactional(readOnly = true)
  public OpalaAdoptionResponse status(long productId, long cycleId) {
    var cycle =
        cycles
            .findById(cycleId)
            .filter(c -> Objects.equals(c.getProductId(), productId))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Ciclo do produto não encontrado."));
    var saved = repository.find(cycleId);
    if (saved.isPresent()) {
      var a = saved.get();
      return new OpalaAdoptionResponse(
          productId,
          cycleId,
          cycle.getExperimentId(),
          true,
          false,
          "Preparação Opala integrada. Acompanhe as atividades dos agentes e seus impedimentos.",
          a.processDefinitionId(),
          url(productId, cycleId, cycle.getChainDefinitionId(), a.processDefinitionId()),
          "/catalogo-vivo/opala",
          a.operatorName(),
          a.createdAt());
    }
    try {
      context.scope("experiment:" + cycle.getExperimentId());
      if (cycle.isBaseline())
        throw new IllegalStateException(
            "Uma referência histórica não pode iniciar preparação retroativa.");
      var product =
          products
              .findById(productId)
              .orElseThrow(
                  () ->
                      new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado."));
      if (Boolean.FALSE.equals(product.getAutomaticExecutionEnabled()))
        throw new IllegalStateException(
            "O produto está em STOP. Retome sua execução antes de iniciar a preparação.");
      catalog.requireReady(catalog.processId());
      return new OpalaAdoptionResponse(
          productId,
          cycleId,
          cycle.getExperimentId(),
          false,
          true,
          "Integre e inicie as sete atividades dos agentes, preservando orçamento, janela e aprovações existentes.",
          catalog.processId(),
          null,
          "/catalogo-vivo/opala",
          null,
          null);
    } catch (IllegalStateException | ResponseStatusException ex) {
      log.warn("catalogo-vivo adoption-blocked productId={} cycleId={}", productId, cycleId, ex);
      return new OpalaAdoptionResponse(
          productId,
          cycleId,
          cycle.getExperimentId(),
          false,
          false,
          ex.getMessage(),
          null,
          null,
          "/catalogo-vivo/opala",
          null,
          null);
    }
  }

  /**
   * Registra adesão idempotente e inicia o BPM na mesma transação, sem alterar orçamento ou cadeia.
   */
  @Transactional
  public OpalaAdoptionResponse adopt(long productId, long cycleId, OpalaAdoptionRequest request) {
    products
        .findLockedById(productId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado."));
    var cycle =
        cycles
            .findLocked(productId, cycleId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ciclo não encontrado."));
    if (repository.find(cycleId).isPresent()) return status(productId, cycleId);
    if (request == null || !Objects.equals(request.expectedRevision(), cycle.getRevision()))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O ciclo mudou. Atualize a tela antes de integrar.");
    if (request.operatorName() == null
        || request.operatorName().isBlank()
        || request.operatorName().length() > 160
        || request.reason() == null
        || request.reason().isBlank()
        || request.reason().length() > 1000)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Informe responsável e motivo da adesão.");
    var preview = status(productId, cycleId);
    if (!preview.canAdopt())
      throw new ResponseStatusException(HttpStatus.CONFLICT, preview.reason());
    repository.insert(
        new OpalaAdoption(
            cycleId,
            preview.processDefinitionId(),
            productId,
            cycle.getExperimentId(),
            cycle.getProductVersion(),
            cycle.getRevision(),
            request.operatorName().trim(),
            request.reason().trim(),
            Instant.now()));
    runs.start(
        productId,
        preview.processDefinitionId(),
        new ProcessRunCommand(
            cycle.getChainDefinitionId(), cycleId, "experiment:" + cycle.getExperimentId()));
    return status(productId, cycleId);
  }

  /** Produz navegação para a definição e passagem fixadas, sem selecionar a cadeia mais recente. */
  private String url(long productId, long cycleId, long chainId, long processId) {
    return "/products/"
        + productId
        + "/value-chain-history/processes/"
        + processId
        + "/activities?learningCycleId="
        + cycleId
        + "&chainId="
        + chainId;
  }
}
