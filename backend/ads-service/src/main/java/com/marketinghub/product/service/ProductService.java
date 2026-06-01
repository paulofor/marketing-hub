package com.marketinghub.product.service;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.product.Product;
import com.marketinghub.product.dto.CreateProductRequest;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.ads.InstagramAccountRepository;
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
    private final MarketNicheRepository marketNicheRepository;

    public ProductService(
            ProductRepository repository,
            InstagramAccountRepository accountRepository,
            MarketNicheRepository marketNicheRepository) {
        this.repository = repository;
        this.accountRepository = accountRepository;
        this.marketNicheRepository = marketNicheRepository;
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
                .marketNiche(resolveNiche(request.getMarketNicheId()))
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

    private MarketNiche resolveNiche(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("marketNicheId is required");
        }
        return marketNicheRepository.findById(id).orElseThrow();
    }

    public Product getProduct(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public Iterable<Product> listProducts() {
        return repository.findAll();
    }
}
