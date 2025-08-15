package com.marketinghub.successproduct.service;

import com.marketinghub.ads.InstagramAccount;
import com.marketinghub.ads.InstagramAccountRepository;
import com.marketinghub.successproduct.SuccessProduct;
import com.marketinghub.successproduct.dto.CreateSuccessProductRequest;
import com.marketinghub.successproduct.dto.UpdateSuccessProductRequest;
import com.marketinghub.successproduct.repository.SuccessProductRepository;
import com.marketinghub.successproduct.SuccessProductPlatform;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for success products.
 */
@Service
public class SuccessProductService {
    private final SuccessProductRepository repository;
    private final InstagramAccountRepository instagramAccountRepository;

    public SuccessProductService(SuccessProductRepository repository,
                                 InstagramAccountRepository instagramAccountRepository) {
        this.repository = repository;
        this.instagramAccountRepository = instagramAccountRepository;
    }

    /**
     * Creates and stores a success product.
     */
    @Transactional
    public SuccessProduct create(CreateSuccessProductRequest request) {
        SuccessProduct product = SuccessProduct.builder()
                .description(request.getDescription())
                .novo(true)
                .platform(request.getPlatform() != null ? request.getPlatform() : SuccessProductPlatform.COFRE)
                .build();
        return repository.save(product);
    }

    public SuccessProduct get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public Iterable<SuccessProduct> list() {
        return repository.findAll();
    }

    /** Update an existing success product. */
    @Transactional
    public SuccessProduct update(Long id, UpdateSuccessProductRequest request) {
        SuccessProduct product = repository.findById(id).orElseThrow();
        product.setDescription(request.getDescription());
        product.setName(request.getName());
        if (request.getNovo() != null) {
            product.setNovo(request.getNovo());
        }
        product.setNiche(request.getNiche());
        product.setAvatar(request.getAvatar());
        if (request.getPlatform() != null) {
            product.setPlatform(request.getPlatform());
        }
        product.setAudienceType(request.getAudienceType());
        product.setSalesPageUrl(request.getSalesPageUrl());
        product.setInstagramUrl(request.getInstagramUrl());
        product.setFacebookUrl(request.getFacebookUrl());
        product.setYoutubeUrl(request.getYoutubeUrl());
        if (request.getInstagramAccountId() != null) {
            InstagramAccount account = instagramAccountRepository
                    .findById(request.getInstagramAccountId())
                    .orElse(null);
            product.setInstagramAccount(account);
        } else {
            product.setInstagramAccount(null);
        }
        product.setExplicitPain(request.getExplicitPain());
        product.setPromise(request.getPromise());
        product.setUniqueMechanism(request.getUniqueMechanism());
        product.setTripwire(request.getTripwire());
        product.setRiskReversal(request.getRiskReversal());
        product.setSocialProof(request.getSocialProof());
        product.setCheckoutMonetization(request.getCheckoutMonetization());
        product.setSalesFunnel(request.getSalesFunnel());
        product.setCreativeVolume(request.getCreativeVolume());
        product.setStorytelling(request.getStorytelling());
        return repository.save(product);
    }
}
