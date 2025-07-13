package com.marketinghub.product.service;

import com.marketinghub.product.Product;
import com.marketinghub.product.dto.CreateProductRequest;
import com.marketinghub.product.repository.ProductRepository;
import com.marketinghub.ads.InstagramAccountRepository;
import com.marketinghub.ads.InstagramAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for product management.
 */
@Service
public class ProductService {
    private final ProductRepository repository;
    private final InstagramAccountRepository accountRepository;

    public ProductService(ProductRepository repository, InstagramAccountRepository accountRepository) {
        this.repository = repository;
        this.accountRepository = accountRepository;
    }

    /**
     * Creates and stores a product.
     */
    @Transactional
    public Product createProduct(CreateProductRequest request) {
        Product product = Product.builder()
                .niche(request.getNiche())
                .avatar(request.getAvatar())
                .instagramAccount(resolveAccount(request.getInstagramAccountId()))
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
                .aiCost(request.getAiCost())
                .build();
        return repository.save(product);
    }

    private InstagramAccount resolveAccount(Long id) {
        if (id == null) {
            return null;
        }
        return accountRepository.findById(id).orElseThrow();
    }

    public Product getProduct(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public Iterable<Product> listProducts() {
        return repository.findAll();
    }
}
