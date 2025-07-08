package com.marketinghub.successproduct.service;

import com.marketinghub.successproduct.SuccessProduct;
import com.marketinghub.successproduct.dto.CreateSuccessProductRequest;
import com.marketinghub.successproduct.repository.SuccessProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for success products.
 */
@Service
public class SuccessProductService {
    private final SuccessProductRepository repository;

    public SuccessProductService(SuccessProductRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates and stores a success product.
     */
    @Transactional
    public SuccessProduct create(CreateSuccessProductRequest request) {
        SuccessProduct product = SuccessProduct.builder()
                .description(request.getDescription())
                .build();
        return repository.save(product);
    }

    public SuccessProduct get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public Iterable<SuccessProduct> list() {
        return repository.findAll();
    }
}
