package com.marketinghub.productdiscovery.v1.web;

import com.marketinghub.productdiscovery.v1.service.CreateProductDiscoveryCycleRequest;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryCycleDetailResponse;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryCycleResponse;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryFailureRequest;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryPendingResponse;
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
import org.springframework.web.bind.annotation.RestController;

/** Expõe os contratos backend da descoberta de produtos PDE v1. */
@RestController
@RequestMapping("/api")
public class ProductDiscoveryController {

    private final ProductDiscoveryService service;

    /** Inicializa o controller com o serviço canônico do módulo. */
    public ProductDiscoveryController(ProductDiscoveryService service) {
        this.service = service;
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
        return ResponseEntity
                .created(URI.create("/api/product-discovery/v1/cycles/" + response.id()))
                .body(response);
    }

    /** Busca ciclo com ranking de oportunidades para decisão humana. */
    @GetMapping("/product-discovery/v1/cycles/{cycleId}")
    public ResponseEntity<ProductDiscoveryCycleDetailResponse> getCycle(@PathVariable Long cycleId) {
        return ResponseEntity.ok(service.getCycle(cycleId));
    }

    /** Entrega pendências canônicas para o worker de pesquisa. */
    @GetMapping("/internal/product-discovery/productdiscovery/v1/research/stage-executions/pending")
    public ResponseEntity<List<ProductDiscoveryPendingResponse>> pending() {
        return ResponseEntity.ok(service.pending());
    }

    /** Recebe resultado funcional de pesquisa do worker. */
    @PostMapping("/internal/product-discovery/productdiscovery/v1/research/stage-executions/{cycleId}/complete")
    public ResponseEntity<ProductDiscoveryCycleDetailResponse> complete(
            @PathVariable Long cycleId,
            @Valid @RequestBody ProductDiscoveryResultRequest request) {
        return ResponseEntity.ok(service.complete(cycleId, request));
    }

    /** Recebe falha operacional do worker com causa auditável. */
    @PostMapping("/internal/product-discovery/productdiscovery/v1/research/stage-executions/{cycleId}/fail")
    public ResponseEntity<ProductDiscoveryCycleResponse> fail(
            @PathVariable Long cycleId,
            @Valid @RequestBody ProductDiscoveryFailureRequest request) {
        return ResponseEntity.ok(service.fail(cycleId, request));
    }
}
