package com.marketinghub.product.service;

import com.marketinghub.product.Product;
import com.marketinghub.product.dto.CreateProductRequest;
import com.marketinghub.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for product management.
 */
@Service
public class ProductService {
    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates and stores a product.
     */
    @Transactional
    public Product createProduct(CreateProductRequest request) {
        Product product = Product.builder()
                .niche(request.getNiche())
                .avatar(request.getAvatar())
                .explicitPain(request.getExplicitPain())
                .promise(request.getPromise())
                .uniqueMechanism(request.getUniqueMechanism())
                .tripwire(request.getTripwire())
                .riskReversal(request.getRiskReversal())
                .socialProof(request.getSocialProof())
                .checkoutMonetization(request.getCheckoutMonetization())
                .funnel(request.getFunnel())
                .creativeVolume(request.getCreativeVolume())
                .storytelling(request.getStorytelling())
                .build();
        return repository.save(product);
    }

    public Product getProduct(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public Iterable<Product> listProducts() {
        return repository.findAll();
    }
}
