package com.marketinghub.salesvideo.mapper;

import com.marketinghub.researchintelligence.v1.service.ResearchIntelligenceService;
import com.marketinghub.researchintelligence.v1.service.select.ResearchIntelligenceCardResponse;
import com.marketinghub.researchintelligence.v1.service.select.ResearchIntelligenceRouteResponse;
import com.marketinghub.researchintelligence.v1.service.select.ResearchIntelligenceSelectionResponse;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.salesvideo.dto.ResearchIntelligenceSelectionDto;
import org.springframework.stereotype.Component;

/** Adapta a biblioteca comum ao contrato interno e estável do módulo SalesVideo. */
@Component
public class VideoProjectResearchIntelligenceMapper {
  private final ResearchIntelligenceService researchIntelligenceService;

  /** Recebe o compilador comum sem expô-lo às camadas de serviço ou DTO do SalesVideo. */
  public VideoProjectResearchIntelligenceMapper(
      ResearchIntelligenceService researchIntelligenceService) {
    this.researchIntelligenceService = researchIntelligenceService;
  }

  /** Seleciona e projeta as quatro rotas consultivas para um projeto audiovisual. */
  public ResearchIntelligenceSelectionDto selectForVideoProject(VideoProject project) {
    return map(researchIntelligenceService.selectForVideoProject(project));
  }

  /** Seleciona e projeta apenas a rota entregue ao executor informado. */
  public ResearchIntelligenceSelectionDto selectForVideoAgent(
      VideoProject project, String agentKey) {
    return map(researchIntelligenceService.selectForVideoAgent(project, agentKey));
  }

  /** Converte a seleção compartilhada no contrato pertencente ao SalesVideo. */
  private ResearchIntelligenceSelectionDto map(ResearchIntelligenceSelectionResponse selection) {
    if (selection == null) {
      return null;
    }
    return new ResearchIntelligenceSelectionDto(
        selection.contractVersion(),
        selection.contextFingerprint(),
        selection.totalAvailableCards(),
        selection.routes().stream().map(this::map).toList(),
        selection.limitations());
  }

  /** Converte uma rota compartilhada preservando sua autoridade e motivo de seleção. */
  private ResearchIntelligenceSelectionDto.Route map(ResearchIntelligenceRouteResponse route) {
    return new ResearchIntelligenceSelectionDto.Route(
        route.agentKey(),
        route.agentName(),
        route.purpose(),
        route.authority(),
        route.selectionReason(),
        route.cards().stream().map(this::map).toList());
  }

  /** Converte um cartão sem perder identidade, validade ou linhagem da fonte. */
  private ResearchIntelligenceSelectionDto.Card map(ResearchIntelligenceCardResponse card) {
    return new ResearchIntelligenceSelectionDto.Card(
        card.cardId(),
        card.collection(),
        card.title(),
        card.finding(),
        card.mechanism(),
        card.commercialApplication(),
        card.evidenceStrength(),
        card.publishedOn(),
        card.validUntil(),
        card.experimentHypothesis(),
        card.risks(),
        card.limits(),
        card.sourcePath(),
        card.sourceSha256(),
        card.evidenceKind());
  }
}
