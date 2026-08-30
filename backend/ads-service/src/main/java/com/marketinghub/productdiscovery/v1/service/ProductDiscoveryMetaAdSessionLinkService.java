package com.marketinghub.productdiscovery.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdDtos;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdInvestigationService;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    if (cycle.getMetaAdInvestigationId() != null) {
      return Optional.of(requiredInvestigation(cycle, cycle.getMetaAdInvestigationId()));
    }
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
      return Optional.of(requiredInvestigation(cycle, investigationId));
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

  /** Congela a investigação na tentativa vigente antes que o navegador público seja executado. */
  @Transactional
  public MoisMetaAdDtos.InvestigationResponse bindActiveInvestigation(
      Long cycleId, long investigationId, String executionLeaseId) {
    ProductDiscoveryCycle cycle =
        cycleRepository
            .findByIdForUpdate(cycleId)
            .orElseThrow(() -> new IllegalArgumentException("Ciclo de descoberta não encontrado"));
    if (cycle.getStatus() != ProductDiscoveryCycleStatus.RESEARCHING
        || !StringUtils.hasText(executionLeaseId)
        || !executionLeaseId.equals(cycle.getExecutionLeaseId())) {
      throw new IllegalStateException("Lease da execução de descoberta expirou ou foi substituído");
    }
    if (cycle.getMetaAdInvestigationId() != null
        && cycle.getMetaAdInvestigationId().longValue() != investigationId) {
      throw new IllegalStateException("O ciclo já está vinculado a outra investigação Meta");
    }
    MoisMetaAdDtos.InvestigationResponse investigation =
        requiredInvestigation(cycle, investigationId);
    cycle.setMetaAdInvestigationId(investigationId);
    cycleRepository.save(cycle);
    return investigation;
  }

  /** Exige que a investigação vinculada continue disponível para a auditoria do ciclo. */
  private MoisMetaAdDtos.InvestigationResponse requiredInvestigation(
      ProductDiscoveryCycle cycle, long investigationId) {
    return investigationService
        .get(investigationId)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "A investigação Meta vinculada ao ciclo "
                        + cycle.getId()
                        + " não foi encontrada"));
  }
}
