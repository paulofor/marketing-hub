package com.marketinghub.productdiscovery.v1.service;

import com.marketinghub.mois.metaads.v1.service.MoisMetaAdDtos;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdInvestigationService;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryMarketType;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: governar a observação humana e a retomada auditável da pesquisa Meta de Argos.
 */
@Service
public class ProductDiscoverySupervisedMetaSessionService {
  private static final Logger log =
      LoggerFactory.getLogger(ProductDiscoverySupervisedMetaSessionService.class);
  private static final String WORKSPACE_ID = "workspace-001";
  private final ProductDiscoveryCycleRepository cycleRepository;
  private final ProductDiscoveryMetaAdSessionLinkService sessionLinkService;
  private final ProductDiscoveryMetaAdEvidenceService evidenceService;
  private final MoisMetaAdInvestigationService investigationService;
  private final ProductDiscoveryBpmAuditService bpmAuditService;

  /** Configura as fontes de verdade do ciclo, radar e histórico BPM. */
  public ProductDiscoverySupervisedMetaSessionService(
      ProductDiscoveryCycleRepository cycleRepository,
      ProductDiscoveryMetaAdSessionLinkService sessionLinkService,
      ProductDiscoveryMetaAdEvidenceService evidenceService,
      MoisMetaAdInvestigationService investigationService,
      ProductDiscoveryBpmAuditService bpmAuditService) {
    this.cycleRepository = cycleRepository;
    this.sessionLinkService = sessionLinkService;
    this.evidenceService = evidenceService;
    this.investigationService = investigationService;
    this.bpmAuditService = bpmAuditService;
  }

  /**
   * Exibe a busca oficial, as observações e a prontidão da reanálise sem criar efeitos colaterais.
   */
  @Transactional(readOnly = true)
  public ProductDiscoverySupervisedMetaSessionResponse get(Long cycleId) {
    ProductDiscoveryCycle cycle = requiredCycle(cycleId);
    requireInstagramConsumerCycle(cycle);
    MoisMetaAdDtos.InvestigationResponse investigation = requiredLinkedInvestigation(cycle);
    return response(cycle, investigation);
  }

  /**
   * Registra o payload humano bruto antes de delegar normalização e persistência ao radar canônico.
   */
  @Transactional
  public ProductDiscoverySupervisedMetaSessionResponse observe(
      Long cycleId, ProductDiscoverySupervisedMetaObservationRequest request) {
    ProductDiscoveryCycle cycle = requiredCycleForUpdate(cycleId);
    requireInstagramConsumerCycle(cycle);
    MoisMetaAdDtos.InvestigationResponse investigation = requiredLinkedInvestigation(cycle);
    if (cycle.getStatus() != ProductDiscoveryCycleStatus.COMPLETED) {
      throw conflict(
          "Aguarde a tentativa atual de Argos terminar antes de registrar nova observação.");
    }
    if (!"SUPERVISED".equals(investigation.collection().mode())) {
      throw conflict("A investigação vinculada não aceita observação humana supervisionada.");
    }
    log.info(
        "Product Discovery recebeu observação Meta bruta cycleId={} investigationId={} payload={}",
        cycleId,
        investigation.id(),
        request);
    investigationService.ingestSupervised(
        investigation.id(),
        new MoisMetaAdDtos.SupervisedObservationRequest(
            request.adReference(),
            request.advertiserName(),
            request.adLibraryUrl(),
            request.adText(),
            request.publisherPlatforms(),
            request.formatType(),
            request.mediaUrl(),
            request.destinationUrl(),
            request.pageActive(),
            request.commercialSignal(),
            request.observedAt()));
    return response(cycle, requiredInvestigation(investigation.id()));
  }

  /** Abre uma única tentativa nova depois que a evidência Instagram foi persistida e validada. */
  @Transactional
  public ProductDiscoverySupervisedMetaSessionResponse resume(Long cycleId) {
    ProductDiscoveryCycle cycle = requiredCycleForUpdate(cycleId);
    requireInstagramConsumerCycle(cycle);
    MoisMetaAdDtos.InvestigationResponse investigation = requiredLinkedInvestigation(cycle);
    if (cycle.getStatus() == ProductDiscoveryCycleStatus.READY_FOR_RESEARCH
        || cycle.getStatus() == ProductDiscoveryCycleStatus.RESEARCHING) {
      return response(cycle, investigation);
    }
    if (cycle.getStatus() != ProductDiscoveryCycleStatus.COMPLETED) {
      throw conflict(
          "Somente uma pesquisa concluída pode ser reanalisada com nova evidência Meta.");
    }
    ProductDiscoveryMetaAdEvidenceListResponse evidence =
        evidenceService.searchInvestigation(cycleId, investigation, 50);
    if (!"OBSERVED".equals(evidence.sourceStatus()) || evidence.activeAds() <= 0) {
      throw conflict(
          "Registre ao menos um anúncio atual, ativo e distribuído no Instagram antes de reanalisar.");
    }
    cycle.setStatus(ProductDiscoveryCycleStatus.READY_FOR_RESEARCH);
    cycle.setStageCode("research");
    cycle.setErrorMessage(null);
    cycle.setExecutionLeaseId(null);
    cycle.setLeaseExpiresAt(null);
    ProductDiscoveryCycle saved = cycleRepository.save(cycle);
    bpmAuditService.reopenForSupervisedMetaEvidence(saved, investigation.id());
    log.info(
        "Product Discovery reabriu pesquisa supervisionada cycleId={} investigationId={} status={}",
        cycleId,
        investigation.id(),
        saved.getStatus());
    return response(saved, investigation);
  }

  /** Consolida o contrato da tela com métricas reais e uma orientação acionável. */
  private ProductDiscoverySupervisedMetaSessionResponse response(
      ProductDiscoveryCycle cycle, MoisMetaAdDtos.InvestigationResponse investigation) {
    ProductDiscoveryMetaAdEvidenceListResponse evidence =
        evidenceService.searchInvestigation(cycle.getId(), investigation, 50);
    boolean activeExecution =
        cycle.getStatus() == ProductDiscoveryCycleStatus.READY_FOR_RESEARCH
            || cycle.getStatus() == ProductDiscoveryCycleStatus.RESEARCHING;
    boolean canResume =
        cycle.getStatus() == ProductDiscoveryCycleStatus.COMPLETED
            && "OBSERVED".equals(evidence.sourceStatus())
            && evidence.activeAds() > 0;
    return new ProductDiscoverySupervisedMetaSessionResponse(
        cycle.getId(),
        investigation.id(),
        cycle.getStatus().name(),
        evidence.query(),
        evidence.country(),
        evidence.publisherPlatform(),
        evidence.sourceStatus(),
        evidence.collectionMode(),
        investigation.collection().reason(),
        evidence.searchUrl(),
        investigation.collection().nextObservationAt(),
        evidence.adsObserved(),
        evidence.activeAds(),
        evidence.advertisersObserved(),
        evidence.latestObservationAt(),
        evidence.interpretation(),
        "SUPERVISED".equals(evidence.collectionMode())
            && cycle.getStatus() == ProductDiscoveryCycleStatus.COMPLETED,
        canResume,
        resumeReason(cycle, evidence, activeExecution),
        evidence.items());
  }

  /** Explica por que a ação de reanálise está disponível ou bloqueada. */
  private String resumeReason(
      ProductDiscoveryCycle cycle,
      ProductDiscoveryMetaAdEvidenceListResponse evidence,
      boolean activeExecution) {
    if (activeExecution) return "A reanálise de Argos já está na fila ou em execução.";
    if (cycle.getStatus() != ProductDiscoveryCycleStatus.COMPLETED) {
      return "A pesquisa precisa terminar antes de receber uma reanálise supervisionada.";
    }
    if (!"OBSERVED".equals(evidence.sourceStatus()) || evidence.activeAds() <= 0) {
      return "Registre um anúncio atual, ativo e observado no Instagram para liberar a reanálise.";
    }
    return "A evidência está pronta para uma nova tentativa auditável de Argos.";
  }

  /** Exige um ciclo existente antes de consultar ou gravar evidência supervisionada. */
  private ProductDiscoveryCycle requiredCycle(Long cycleId) {
    return cycleRepository
        .findById(cycleId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Ciclo de descoberta não encontrado"));
  }

  /** Serializa comandos humanos concorrentes que registram evidência ou retomam o ciclo. */
  private ProductDiscoveryCycle requiredCycleForUpdate(Long cycleId) {
    return cycleRepository
        .findByIdForUpdate(cycleId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Ciclo de descoberta não encontrado"));
  }

  /** Limita a sessão ao recorte B2C para Instagram definido no cânone da descoberta. */
  private void requireInstagramConsumerCycle(ProductDiscoveryCycle cycle) {
    String channel =
        StringUtils.hasText(cycle.getAcquisitionChannel())
            ? cycle.getAcquisitionChannel().trim().toLowerCase(Locale.ROOT)
            : "";
    if (cycle.getMarketType() != ProductDiscoveryMarketType.B2C || !channel.contains("instagram")) {
      throw conflict("A sessão supervisionada Meta exige uma pesquisa B2C orientada ao Instagram.");
    }
  }

  /** Exige o vínculo persistido pelo relatório da tentativa anterior. */
  private MoisMetaAdDtos.InvestigationResponse requiredLinkedInvestigation(
      ProductDiscoveryCycle cycle) {
    MoisMetaAdDtos.InvestigationResponse investigation =
        sessionLinkService
            .linkedInvestigation(cycle)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "A execução ainda não possui sessão supervisionada da Biblioteca Meta."));
    String cycleCountry =
        StringUtils.hasText(cycle.getCountry())
            ? cycle.getCountry().trim().toUpperCase(Locale.ROOT)
            : "BR";
    if (!WORKSPACE_ID.equals(investigation.workspaceId())
        || !cycleCountry.equalsIgnoreCase(investigation.countryCode())
        || !"INSTAGRAM".equalsIgnoreCase(investigation.publisherPlatform())) {
      throw conflict("A sessão Meta vinculada não pertence ao território e plataforma do ciclo.");
    }
    if (!"SUPERVISED".equals(investigation.collection().mode())) {
      throw conflict("A investigação vinculada não exige observação humana supervisionada.");
    }
    return investigation;
  }

  /** Recarrega a investigação depois de uma nova observação persistida. */
  private MoisMetaAdDtos.InvestigationResponse requiredInvestigation(long investigationId) {
    return investigationService
        .get(investigationId)
        .orElseThrow(() -> new IllegalStateException("Investigação Meta deixou de existir"));
  }

  /** Produz conflito funcional com mensagem legível pela tela administrativa. */
  private ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}
