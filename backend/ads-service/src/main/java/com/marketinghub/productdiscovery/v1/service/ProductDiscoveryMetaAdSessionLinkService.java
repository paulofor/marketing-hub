package com.marketinghub.productdiscovery.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdDtos;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdInvestigationService;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Responsabilidade: localizar a investigação Meta vinculada ao relatório auditável de um ciclo. */
@Service
public class ProductDiscoveryMetaAdSessionLinkService {
  private static final Logger log =
      LoggerFactory.getLogger(ProductDiscoveryMetaAdSessionLinkService.class);
  private final ProductDiscoveryCycleRepository cycleRepository;
  private final MoisMetaAdInvestigationService investigationService;
  private final ObjectMapper objectMapper;

  /** Configura as fontes canônicas do ciclo e da investigação supervisionada. */
  public ProductDiscoveryMetaAdSessionLinkService(
      ProductDiscoveryCycleRepository cycleRepository,
      MoisMetaAdInvestigationService investigationService,
      ObjectMapper objectMapper) {
    this.cycleRepository = cycleRepository;
    this.investigationService = investigationService;
    this.objectMapper = objectMapper;
  }

  /**
   * Recupera o último vínculo Instagram persistido sem criar investigação por efeito de leitura.
   */
  public Optional<MoisMetaAdDtos.InvestigationResponse> linkedInvestigation(Long cycleId) {
    ProductDiscoveryCycle cycle =
        cycleRepository
            .findById(cycleId)
            .orElseThrow(() -> new IllegalArgumentException("Ciclo de descoberta não encontrado"));
    return linkedInvestigation(cycle);
  }

  /** Interpreta o relatório do ciclo e valida que a investigação ainda existe no radar canônico. */
  public Optional<MoisMetaAdDtos.InvestigationResponse> linkedInvestigation(
      ProductDiscoveryCycle cycle) {
    if (!StringUtils.hasText(cycle.getResearchEvidenceReportJson())) {
      return Optional.empty();
    }
    try {
      JsonNode coverages =
          objectMapper.readTree(cycle.getResearchEvidenceReportJson()).path("metaCoverage");
      if (!coverages.isArray()) return Optional.empty();
      Long investigationId = null;
      for (JsonNode coverage : coverages) {
        if ("INSTAGRAM".equalsIgnoreCase(coverage.path("publisherPlatform").asText())
            && coverage.path("investigationId").canConvertToLong()) {
          investigationId = coverage.path("investigationId").asLong();
        }
      }
      if (investigationId == null) return Optional.empty();
      long linkedId = investigationId;
      return Optional.of(
          investigationService
              .get(linkedId)
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "A investigação Meta vinculada ao ciclo não foi encontrada")));
    } catch (IllegalStateException ex) {
      log.error(
          "Investigação Meta vinculada deixou de existir cycleId={} operação=linkedInvestigation",
          cycle.getId(),
          ex);
      throw ex;
    } catch (Exception ex) {
      log.error(
          "Falha ao ler vínculo da sessão Meta cycleId={} operação=linkedInvestigation",
          cycle.getId(),
          ex);
      throw new IllegalStateException("Vínculo da sessão Meta está inválido", ex);
    }
  }
}
