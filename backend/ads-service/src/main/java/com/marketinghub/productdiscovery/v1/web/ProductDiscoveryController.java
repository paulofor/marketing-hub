package com.marketinghub.productdiscovery.v1.web;

import com.marketinghub.productdiscovery.v1.service.CreateProductDiscoveryCycleRequest;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryCycleDetailResponse;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryCycleResponse;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryFailureRequest;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryLegacyCleanupResponse;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryMarketplaceEvidenceService;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryMarketplaceOfferListResponse;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryMaturityRankingResponse;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryMetaAdEvidenceListResponse;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryMetaAdEvidenceService;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryPendingResponse;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryResearchPlanRequest;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryResearchPlanResponse;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryResultRequest;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Expõe os contratos backend da descoberta de produtos PDE v1. */
@RestController
@RequestMapping("/api")
public class ProductDiscoveryController {

  private final ProductDiscoveryService service;
  private final ProductDiscoveryMarketplaceEvidenceService marketplaceEvidenceService;
  private final ProductDiscoveryMetaAdEvidenceService metaAdEvidenceService;

  /** Inicializa o controller com o serviço canônico do módulo. */
  public ProductDiscoveryController(
      ProductDiscoveryService service,
      ProductDiscoveryMarketplaceEvidenceService marketplaceEvidenceService,
      ProductDiscoveryMetaAdEvidenceService metaAdEvidenceService) {
    this.service = service;
    this.marketplaceEvidenceService = marketplaceEvidenceService;
    this.metaAdEvidenceService = metaAdEvidenceService;
  }

  /** Lista ciclos recentes de descoberta para a tela administrativa. */
  @GetMapping("/product-discovery/v1/cycles")
  public ResponseEntity<List<ProductDiscoveryCycleResponse>> listCycles() {
    return ResponseEntity.ok(service.listCycles());
  }

  /** Cria um novo ciclo de pesquisa pronto para o worker. */
  @PostMapping("/product-discovery/v1/cycles")
  public ResponseEntity<ProductDiscoveryCycleResponse> createCycle(
      @Valid @RequestBody CreateProductDiscoveryCycleRequest request) {
    ProductDiscoveryCycleResponse response = service.createCycle(request);
    return ResponseEntity.created(URI.create("/api/product-discovery/v1/cycles/" + response.id()))
        .body(response);
  }

  /** Busca ciclo com ranking de oportunidades para decisão humana. */
  @GetMapping("/product-discovery/v1/cycles/{cycleId}")
  public ResponseEntity<ProductDiscoveryCycleDetailResponse> getCycle(@PathVariable Long cycleId) {
    return ResponseEntity.ok(service.getCycle(cycleId));
  }

  /** Retorna o ranking gerencial por maturidade comercial para priorização de descoberta. */
  @GetMapping("/product-discovery/v1/maturity-ranking")
  public ResponseEntity<ProductDiscoveryMaturityRankingResponse> getMaturityRanking() {
    return ResponseEntity.ok(service.getMaturityRanking());
  }

  /** Arquiva evidências artificiais legadas sem apagar o histórico auditável. */
  @PostMapping("/product-discovery/v1/legacy-artificial-evidence/archive")
  public ResponseEntity<ProductDiscoveryLegacyCleanupResponse> archiveArtificialLegacyEvidence() {
    return ResponseEntity.ok(service.archiveArtificialLegacyEvidence());
  }

  /** Entrega pendências canônicas para o worker de pesquisa. */
  @GetMapping("/internal/product-discovery/productdiscovery/v1/research/stage-executions/pending")
  public ResponseEntity<List<ProductDiscoveryPendingResponse>> pending() {
    return ResponseEntity.ok(service.pending());
  }

  /** Persiste o plano de perguntas, fontes e coletores escolhido por Argos. */
  @PostMapping(
      "/internal/product-discovery/productdiscovery/v1/research/stage-executions/{cycleId}/plan")
  public ResponseEntity<ProductDiscoveryResearchPlanResponse> registerResearchPlan(
      @PathVariable Long cycleId, @Valid @RequestBody ProductDiscoveryResearchPlanRequest request) {
    return ResponseEntity.ok(service.registerResearchPlan(cycleId, request));
  }

  /** Entrega snapshots autenticados normalizados sem expor credenciais dos coletores. */
  @GetMapping("/internal/product-discovery/productdiscovery/v1/marketplace-offers")
  public ResponseEntity<ProductDiscoveryMarketplaceOfferListResponse> marketplaceOffers(
      @RequestParam String marketplace,
      @RequestParam String query,
      @RequestParam(defaultValue = "10") Integer limit) {
    return ResponseEntity.ok(marketplaceEvidenceService.search(marketplace, query, limit));
  }

  /** Entrega a Argos sinais históricos da Biblioteca Meta sem declarar vendas comprovadas. */
  @GetMapping("/internal/product-discovery/productdiscovery/v1/meta-ad-evidence")
  public ResponseEntity<ProductDiscoveryMetaAdEvidenceListResponse> metaAdEvidence(
      @RequestParam String query,
      @RequestParam(defaultValue = "BR") String country,
      @RequestParam(defaultValue = "25") Integer limit) {
    return ResponseEntity.ok(metaAdEvidenceService.search(query, country, limit));
  }

  /** Expõe o plano dirigido sem expor cookies, senhas ou tokens dos coletores. */
  @GetMapping("/product-discovery/v1/cycles/{cycleId}/research-plan")
  public ResponseEntity<ProductDiscoveryResearchPlanResponse> getResearchPlan(
      @PathVariable Long cycleId) {
    return ResponseEntity.ok(service.getResearchPlan(cycleId));
  }

  /** Recebe resultado funcional de pesquisa do worker. */
  @PostMapping(
      "/internal/product-discovery/productdiscovery/v1/research/stage-executions/{cycleId}/complete")
  public ResponseEntity<ProductDiscoveryCycleDetailResponse> complete(
      @PathVariable Long cycleId, @Valid @RequestBody ProductDiscoveryResultRequest request) {
    return ResponseEntity.ok(service.complete(cycleId, request));
  }

  /** Recebe falha operacional do worker com causa auditável. */
  @PostMapping(
      "/internal/product-discovery/productdiscovery/v1/research/stage-executions/{cycleId}/fail")
  public ResponseEntity<ProductDiscoveryCycleResponse> fail(
      @PathVariable Long cycleId, @Valid @RequestBody ProductDiscoveryFailureRequest request) {
    return ResponseEntity.ok(service.fail(cycleId, request));
  }
}
