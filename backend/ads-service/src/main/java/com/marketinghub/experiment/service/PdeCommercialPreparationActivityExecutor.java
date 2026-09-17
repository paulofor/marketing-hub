package com.marketinghub.experiment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityExecutionResult;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityExecutor;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityRequirementResponse;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: selecionar o subprocesso de preparação comercial pelo tipo cadastrado. */
@Component
@Slf4j
public class PdeCommercialPreparationActivityExecutor
    implements BackendProductProcessActivityExecutor {
  static final String PROCESS_CODE = "pde-commercial-homologation-activation";
  static final String ACTIVITY_ID = "commercialPreparation";
  private static final String ROUTER_VERSION = "COMMERCIAL_PREPARATION_BY_PRODUCT_TYPE_V1";

  private final BusinessProcessDefinitionRepository processes;
  private final LearningSalesCycleRepository cycles;
  private final ObjectMapper json;

  /** Configura catálogo, ciclos e leitor do contrato de roteamento versionado. */
  public PdeCommercialPreparationActivityExecutor(
      BusinessProcessDefinitionRepository processes,
      LearningSalesCycleRepository cycles,
      ObjectMapper json) {
    this.processes = processes;
    this.cycles = cycles;
    this.json = json;
  }

  /** Reconhece somente a atividade de roteamento comercial do Processo 5. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return process != null
        && activity != null
        && PROCESS_CODE.equals(process.getProcessCode())
        && ACTIVITY_ID.equals(activity.getActivityId())
        && ROUTER_VERSION.equals(
            metadata(activity).path("commercialPreparationRouterVersion").asText());
  }

  /** Resolve a rota exata e expõe o subprocesso sem executar trabalho do tipo no backend pai. */
  @Override
  @Transactional(readOnly = true)
  public BackendProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String sourceReference) {
    String productTypeCode = productTypeCode(product);
    JsonNode route = route(activity, productTypeCode);
    if (productTypeCode == null) {
      return blocked(
          "O produto não possui tipo cadastrado; defina o tipo antes da preparação comercial.",
          false,
          false);
    }
    if (route == null) {
      return blocked(
          "O tipo "
              + productTypeCode
              + " ainda não possui percurso de preparação comercial publicado no Processo 5.",
          true,
          false);
    }
    String subprocessCode = route.path("subprocessCode").asText();
    int subprocessVersion = route.path("subprocessVersion").asInt(-1);
    var target =
        processes
            .findByProcessCodeAndVersionNumber(subprocessCode, subprocessVersion)
            .filter(candidate -> "PUBLISHED".equals(candidate.getStatus()))
            .orElse(null);
    if (target == null) {
      return blocked(
          "O percurso "
              + subprocessCode
              + " v"
              + subprocessVersion
              + " não está publicado para o tipo "
              + productTypeCode
              + ".",
          true,
          false);
    }
    var cycle = cycle(product, sourceReference);
    String navigationUrl =
        "/products/"
            + product.getId()
            + "/value-chain-history/processes/"
            + target.getId()
            + "/activities?chainId="
            + cycle.getChainDefinitionId()
            + "&learningCycleId="
            + cycle.getId();
    return new BackendProductProcessActivityReadiness(
        true,
        "O tipo "
            + route.path("productTypeInternalName").asText(productTypeCode)
            + " usa o subprocesso "
            + target.getName()
            + ".",
        "Abrir subprocesso",
        "O backend selecionou o percurso pela definição estável do tipo e preservará produto, cadeia, ciclo, experimento e versão.",
        null,
        null,
        List.of(
            requirement(
                "PRODUCT_TYPE", "Tipo cadastrado", true, productTypeCode, "Preserve o tipo."),
            requirement(
                "TYPE_ROUTE",
                "Percurso do tipo",
                true,
                target.getProcessCode() + " v" + target.getVersionNumber(),
                "Execute somente este subprocesso.")),
        target.getId(),
        navigationUrl);
  }

  /** Mantém segura uma chamada direta; a automação normal delega pelo controle SUBPROCESS. */
  @Override
  public BackendProductProcessActivityExecutionResult execute(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String sourceReference) {
    var readiness = readiness(process, activity, product, sourceReference);
    if (!readiness.ready()) throw new IllegalStateException(readiness.reason());
    return new BackendProductProcessActivityExecutionResult(
        sourceReference,
        "PENDING",
        false,
        "Abra e conclua o subprocesso selecionado; o backend conciliará seu objetivo no Processo 5.");
  }

  /** Localiza a rota cujo código corresponde exatamente ao tipo persistido. */
  JsonNode route(BusinessProcessActivityDefinition activity, String productTypeCode) {
    if (productTypeCode == null) return null;
    for (JsonNode candidate : metadata(activity).path("subprocessRoutes"))
      if (productTypeCode.equals(candidate.path("productTypeCode").asText())) return candidate;
    return null;
  }

  /** Lê o contrato da atividade sem transformar JSON corrompido em ausência de rota. */
  private JsonNode metadata(BusinessProcessActivityDefinition activity) {
    try {
      return json.readTree(activity == null ? "{}" : activity.getDefinitionJson());
    } catch (Exception ex) {
      log.error(
          "Falha ao ler contrato de roteamento comercial. activityDefinitionId={}",
          activity == null ? null : activity.getId(),
          ex);
      throw new IllegalStateException("O contrato de roteamento comercial está inválido.", ex);
    }
  }

  /** Usa exclusivamente o tipo cadastrado e nunca o nome, formato ou tecnologia do produto. */
  private String productTypeCode(Product product) {
    return product == null || product.getProductTypeDefinition() == null
        ? null
        : product.getProductTypeDefinition().getCode();
  }

  /** Confere que a referência representa o ciclo e o produto recebidos. */
  private com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle cycle(
      Product product, String sourceReference) {
    if (sourceReference == null || !sourceReference.matches("experiment:[1-9][0-9]{0,17}"))
      throw new IllegalStateException("A preparação exige a referência exata do experimento.");
    long experimentId = Long.parseLong(sourceReference.substring("experiment:".length()));
    var cycle =
        cycles
            .findByExperimentId(experimentId)
            .orElseThrow(
                () -> new IllegalStateException("O experimento não possui ciclo comercial."));
    if (!Objects.equals(product.getId(), cycle.getProductId()))
      throw new IllegalStateException("O ciclo comercial pertence a outro produto.");
    return cycle;
  }

  /** Monta um bloqueio explicável sem inventar uma rota para o tipo. */
  private BackendProductProcessActivityReadiness blocked(
      String reason, boolean typeReady, boolean routeReady) {
    return new BackendProductProcessActivityReadiness(
        false,
        reason,
        "Configurar percurso do tipo",
        "Cadastre um subprocesso compatível antes de prosseguir; os gates comuns continuam obrigatórios.",
        null,
        null,
        List.of(
            requirement(
                "PRODUCT_TYPE",
                "Tipo cadastrado",
                typeReady,
                typeReady ? "Tipo identificado." : "Tipo ausente.",
                typeReady ? "Preserve o tipo." : "Cadastre o tipo do produto."),
            requirement(
                "TYPE_ROUTE",
                "Percurso do tipo",
                routeReady,
                routeReady ? "Percurso identificado." : reason,
                reason)));
  }

  /** Cria um requisito imutável apresentado pela tela a partir da verdade do backend. */
  private ProductProcessActivityRequirementResponse requirement(
      String code, String title, boolean satisfied, String detail, String recommendation) {
    return new ProductProcessActivityRequirementResponse(
        code, title, satisfied, detail, recommendation);
  }
}
