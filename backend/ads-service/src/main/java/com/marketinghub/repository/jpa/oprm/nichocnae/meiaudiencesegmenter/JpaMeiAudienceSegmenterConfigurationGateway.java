package com.marketinghub.repository.jpa.oprm.nichocnae.meiaudiencesegmenter;

import com.marketinghub.openai.OpenAiModel;
import com.marketinghub.oprm.nichocnae.meiaudiencesegmenter.service.MeiAudienceSegmenterConfigurationGateway;
import com.marketinghub.oprm.nichocnae.meiaudiencesegmenter.service.MeiAudienceSegmenterModel;
import com.marketinghub.repository.jpa.pipeline.PipelineStageConfigRepository;
import com.marketinghub.repository.jpa.pipeline.PipelineStageRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Implementa a leitura do modelo configurado para a etapa MEI usando o contrato canônico de pipeline. */
@Component
public class JpaMeiAudienceSegmenterConfigurationGateway implements MeiAudienceSegmenterConfigurationGateway {
  private static final String OPRM_NICHO_CNAE_PIPELINE_CODE = "oprm-nicho-cnae-pipeline";
  private static final String OPRM_NICHO_CNAE_CANONICAL_VERSION = "oprm-nichocnae-canon.v1";
  private static final String MEI_AUDIENCE_CANONICAL_CODE = "MEI_AUDIENCE_SEGMENTER";
  private static final String MEI_AUDIENCE_STAGE_CODE = "mei-audience-segmenter";
  private final PipelineStageConfigRepository pipelineStageConfigRepository;
  private final PipelineStageRepository pipelineStageRepository;

  /** Inicializa o gateway com os repositórios canônicos de configuração de pipeline. */
  public JpaMeiAudienceSegmenterConfigurationGateway(
      PipelineStageConfigRepository pipelineStageConfigRepository, PipelineStageRepository pipelineStageRepository) {
    this.pipelineStageConfigRepository = pipelineStageConfigRepository;
    this.pipelineStageRepository = pipelineStageRepository;
  }

  /** Recupera o modelo configurado priorizando a configuração oficial versionada e mantendo fallback operacional legado. */
  @Override
  public Optional<MeiAudienceSegmenterModel> findConfiguredModel() {
    return findPersistentConfiguredModel().or(this::findLegacyConfiguredModel).map(this::toOprmModel);
  }

  /** Busca o modelo na configuração oficial versionada do pipeline OPRM nicho CNAE. */
  private Optional<OpenAiModel> findPersistentConfiguredModel() {
    return pipelineStageConfigRepository
        .findOfficialStageConfig(OPRM_NICHO_CNAE_PIPELINE_CODE, OPRM_NICHO_CNAE_CANONICAL_VERSION, MEI_AUDIENCE_CANONICAL_CODE)
        .map(config -> config.getOpenAiModel());
  }

  /** Busca o modelo na configuração operacional legada da etapa para preservar compatibilidade. */
  private Optional<OpenAiModel> findLegacyConfiguredModel() {
    return pipelineStageRepository
        .findByPipelineCodeAndStageCode(OPRM_NICHO_CNAE_PIPELINE_CODE, MEI_AUDIENCE_STAGE_CODE)
        .map(stage -> stage.getOpenAiModel());
  }

  /** Converte o modelo canônico OpenAI para o contrato interno permitido no OPRM. */
  private MeiAudienceSegmenterModel toOprmModel(OpenAiModel model) {
    return new MeiAudienceSegmenterModel(model.getCode(), model.getName());
  }
}
