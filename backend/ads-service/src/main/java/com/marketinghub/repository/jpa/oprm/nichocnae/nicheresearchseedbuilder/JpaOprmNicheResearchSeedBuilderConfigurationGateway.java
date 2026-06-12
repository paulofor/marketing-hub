package com.marketinghub.repository.jpa.oprm.nichocnae.nicheresearchseedbuilder;

import com.marketinghub.openai.OpenAiModel;
import com.marketinghub.openai.OpenAiResponse;
import com.marketinghub.openai.service.OpenAiPricingService;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.OprmNicheResearchSeedBuilderConfigurationGateway;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.OprmNicheResearchSeedBuilderModel;
import com.marketinghub.repository.jpa.pipeline.PipelineStageConfigRepository;
import com.marketinghub.repository.jpa.pipeline.PipelineStageRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Implementa o contrato OPRM de configuração da etapa dois usando os repositórios canônicos de pipeline e preços.
 */
@Component
public class JpaOprmNicheResearchSeedBuilderConfigurationGateway
    implements OprmNicheResearchSeedBuilderConfigurationGateway {
  private static final String OPRM_NICHO_CNAE_PIPELINE_CODE = "oprm-nicho-cnae-pipeline";
  private static final String OPRM_NICHO_CNAE_CANONICAL_VERSION = "oprm-nichocnae-canon.v1";
  private static final String SEED_BUILDER_CANONICAL_CODE = "NICHE_RESEARCH_SEED_BUILDER";
  private static final String SEED_BUILDER_STAGE_CODE = "niche-research-seed-builder";
  private final PipelineStageConfigRepository pipelineStageConfigRepository;
  private final PipelineStageRepository pipelineStageRepository;
  private final OpenAiPricingService openAiPricingService;

  /** Inicializa o gateway com os componentes canônicos que ficam fora do limite arquitetural direto do OPRM. */
  public JpaOprmNicheResearchSeedBuilderConfigurationGateway(
      PipelineStageConfigRepository pipelineStageConfigRepository,
      PipelineStageRepository pipelineStageRepository,
      OpenAiPricingService openAiPricingService) {
    this.pipelineStageConfigRepository = pipelineStageConfigRepository;
    this.pipelineStageRepository = pipelineStageRepository;
    this.openAiPricingService = openAiPricingService;
  }

  /** Recupera o modelo configurado priorizando o contrato persistente oficial e mantendo fallback legado. */
  @Override
  public Optional<OprmNicheResearchSeedBuilderModel> findConfiguredModel() {
    return findPersistentConfiguredModel().or(this::findLegacyConfiguredModel).map(this::toOprmModel);
  }

  /** Estima o custo padrão da chamada OpenAI para a etapa OPRM usando o serviço canônico de preços. */
  @Override
  public BigDecimal estimateCostUsd(String model, Integer inputTokens, Integer outputTokens) {
    return openAiPricingService.estimateStandardCost(
        model, new OpenAiResponse.OpenAiUsage(inputTokens, outputTokens, null, null, null));
  }

  /** Busca o modelo na configuração oficial versionada do pipeline OPRM nicho CNAE. */
  private Optional<OpenAiModel> findPersistentConfiguredModel() {
    return pipelineStageConfigRepository
        .findOfficialStageConfig(
            OPRM_NICHO_CNAE_PIPELINE_CODE, OPRM_NICHO_CNAE_CANONICAL_VERSION, SEED_BUILDER_CANONICAL_CODE)
        .map(config -> config.getOpenAiModel());
  }

  /** Busca o modelo na configuração operacional legada da etapa para preservar compatibilidade. */
  private Optional<OpenAiModel> findLegacyConfiguredModel() {
    return pipelineStageRepository
        .findByPipelineCodeAndStageCode(OPRM_NICHO_CNAE_PIPELINE_CODE, SEED_BUILDER_STAGE_CODE)
        .map(stage -> stage.getOpenAiModel());
  }

  /** Converte o modelo canônico OpenAI para o contrato interno permitido no OPRM. */
  private OprmNicheResearchSeedBuilderModel toOprmModel(OpenAiModel model) {
    return new OprmNicheResearchSeedBuilderModel(model.getCode(), model.getName());
  }
}
