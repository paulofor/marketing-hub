package com.marketinghub.productdiscovery.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdDtos;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdInvestigationService;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryMetaAttempt;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryMetaAttemptRepository;
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
  private final ProductDiscoveryMetaAttemptRepository metaAttemptRepository;
  private final MoisMetaAdInvestigationService investigationService;
  private final ObjectMapper objectMapper;

  /** Configura as fontes canônicas do ciclo e da investigação supervisionada. */
  public ProductDiscoveryMetaAdSessionLinkService(
      ProductDiscoveryCycleRepository cycleRepository,
      ProductDiscoveryMetaAttemptRepository metaAttemptRepository,
      MoisMetaAdInvestigationService investigationService,
      ObjectMapper objectMapper) {
    this.cycleRepository = cycleRepository;
    this.metaAttemptRepository = metaAttemptRepository;
    this.investigationService = investigationService;
    this.objectMapper = objectMapper;
  }

  /** Recupera a investigação exclusiva da tentativa sem criar vínculo por efeito de leitura. */
  public Optional<MoisMetaAdDtos.InvestigationResponse> linkedAttemptInvestigation(
      Long cycleId, int attemptNumber) {
    ProductDiscoveryCycle cycle =
        cycleRepository
            .findById(cycleId)
            .orElseThrow(() -> new IllegalArgumentException("Ciclo de descoberta não encontrado"));
    return linkedAttemptInvestigation(cycle, attemptNumber);
  }

  /** Resolve o vínculo persistido da tentativa dentro de um ciclo já carregado. */
  public Optional<MoisMetaAdDtos.InvestigationResponse> linkedAttemptInvestigation(
      ProductDiscoveryCycle cycle, int attemptNumber) {
    validateAttemptNumber(attemptNumber);
    return metaAttemptRepository
        .findByCycleIdAndAttemptNumber(cycle.getId(), attemptNumber)
        .map(link -> requiredInvestigation(cycle, link.getInvestigationId()));
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

  /** Congela uma investigação distinta em cada tentativa antes de executar o navegador público. */
  @Transactional
  public MoisMetaAdDtos.InvestigationResponse bindAttemptInvestigation(
      Long cycleId,
      int attemptNumber,
      long investigationId,
      String executionLeaseId,
      String searchQuery) {
    ProductDiscoveryCycle cycle =
        cycleRepository
            .findByIdForUpdate(cycleId)
            .orElseThrow(() -> new IllegalArgumentException("Ciclo de descoberta não encontrado"));
    if (cycle.getStatus() != ProductDiscoveryCycleStatus.RESEARCHING
        || !StringUtils.hasText(executionLeaseId)
        || !executionLeaseId.equals(cycle.getExecutionLeaseId())) {
      throw new IllegalStateException("Lease da execução de descoberta expirou ou foi substituído");
    }
    validateAttemptNumber(attemptNumber);
    String normalizedSearchQuery = normalizeSearchQuery(searchQuery);
    if (!StringUtils.hasText(normalizedSearchQuery) || normalizedSearchQuery.length() > 60) {
      throw new IllegalArgumentException("Consulta Meta da tentativa deve ter até 60 caracteres");
    }
    MoisMetaAdDtos.InvestigationResponse investigation =
        requiredInvestigation(cycle, investigationId);
    Optional<ProductDiscoveryMetaAttempt> existing =
        metaAttemptRepository.findByCycleIdAndAttemptNumber(cycleId, attemptNumber);
    if (existing.isPresent()) {
      ProductDiscoveryMetaAttempt link = existing.get();
      if (!link.getInvestigationId().equals(investigationId)
          || !link.getSearchQuery().equalsIgnoreCase(normalizedSearchQuery)) {
        throw new IllegalStateException("A tentativa já está vinculada a outra consulta Meta");
      }
      cycle.setMetaAdInvestigationId(investigationId);
      cycleRepository.save(cycle);
      return investigation;
    }
    if (attemptNumber > 1
        && metaAttemptRepository
            .findByCycleIdAndAttemptNumber(cycleId, attemptNumber - 1)
            .isEmpty()) {
      throw new IllegalStateException("A tentativa Meta anterior ainda não foi registrada");
    }
    if (metaAttemptRepository.existsByCycleIdAndInvestigationId(cycleId, investigationId)) {
      throw new IllegalStateException("A ampliação repetiu a investigação Meta de outra tentativa");
    }
    if (metaAttemptRepository.existsByCycleIdAndSearchQueryIgnoreCase(
        cycleId, normalizedSearchQuery)) {
      throw new IllegalStateException("A ampliação repetiu a consulta Meta de outra tentativa");
    }
    metaAttemptRepository.save(
        new ProductDiscoveryMetaAttempt(
            cycle, attemptNumber, investigationId, executionLeaseId, normalizedSearchQuery));
    cycle.setMetaAdInvestigationId(investigationId);
    cycleRepository.save(cycle);
    return investigation;
  }

  /** Limita a ampliação Meta às três tentativas previstas no contrato v1. */
  private void validateAttemptNumber(int attemptNumber) {
    if (attemptNumber < 1 || attemptNumber > 3) {
      throw new IllegalArgumentException("Tentativa Meta deve estar entre 1 e 3");
    }
  }

  /** Uniformiza espaços antes de comparar e persistir a consulta imutável. */
  private String normalizeSearchQuery(String searchQuery) {
    return StringUtils.hasText(searchQuery) ? searchQuery.trim().replaceAll("\\s+", " ") : "";
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
